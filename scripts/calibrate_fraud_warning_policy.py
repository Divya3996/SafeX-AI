#!/usr/bin/env python3
"""Calibrate an additional WARN path using validation only; never refit weights.

The evaluation split has already been inspected during development. Follow-up
pipeline comparisons are regression diagnostics, not independent accuracy.
"""
import hashlib
import json
from pathlib import Path
import numpy as np
from fraud_text_features import words
from train_fraud_text_model import model_scores, metrics

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / 'models/cache/fraud-2026-10-09'
ASSETS = ROOT / 'android-app/app/src/main/assets'


def vocabulary_guard(text, vocabulary):
    tokens = set(words(text)) - {'urltoken', 'emailtoken', 'numbertoken'}
    letters = [char for token in tokens for char in token if char.isalpha()]
    return (len(tokens) >= 4 and bool(letters)
            and sum('a' <= c <= 'z' for c in letters) / len(letters) >= .95
            and sum(token in vocabulary for token in tokens) / len(tokens) >= .60)


def main():
    data = (CACHE / 'message-policy.jsonl').read_bytes()
    rows = [json.loads(line) for line in data.splitlines()]
    model_bytes = (ASSETS / 'text-model.json').read_bytes()
    assert model_bytes == (CACHE / 'candidate-text-model.json').read_bytes(), 'Use the unchanged first model'
    model = json.loads(model_bytes)
    vocabulary = set(model['vocabulary'])
    guards = np.asarray([vocabulary_guard(row['classifierInputRaw'], vocabulary) for row in rows])
    assert all(bool(guard) == row['contextFreeVocabularyEligible'] for row, guard in zip(rows, guards)), 'Kotlin/Python guard mismatch'
    labels = np.asarray([row['label'] for row in rows])
    scores = model_scores(model, [row['classifierInputRaw'] for row in rows])
    validation = np.asarray([row['split'] == 'validation' for row in rows])
    options = []
    for step in range(180, 200):
        threshold = step / 200
        result = metrics(labels[validation], (guards & (scores >= threshold))[validation])
        if result['fp'] == 0:
            options.append((result['tp'], -threshold, threshold, result))
    assert options, 'No safe validation threshold; do not publish a policy'
    _, _, threshold, result = max(options)
    policy = dict(guard_version='latin_vocabulary_v1', model_sha256=hashlib.sha256(model_bytes).hexdigest(),
                  minimum_distinct_words=4, minimum_latin_letter_fraction=.95,
                  minimum_vocabulary_fraction=.60, threshold=threshold,
                  threshold_selection='Validation only: grid .900-.995 in .005 increments; maximum recall with zero false warnings on all validation controls; ties choose the lower threshold',
                  validation=result, parity_rows=len(rows), policy_export_sha256=hashlib.sha256(data).hexdigest(),
                  scope='Additional WARN only. Latin-script vocabulary guard is a heuristic, not verified language identification or broad real-world coverage. Native-script inputs retain action-context gating.')
    output = json.dumps(policy, indent=2) + '\n'
    (ASSETS / 'text-warning-policy.json').write_text(output)
    (ROOT / 'models/fraud-text-warning-policy.json').write_text(output)
    print(output)


if __name__ == '__main__':
    main()
