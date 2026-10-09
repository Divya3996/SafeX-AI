#!/usr/bin/env python3
"""Evaluate an unchanged first candidate against updated Android action eligibility.

This follow-up policy comparison uses already-inspected development evaluation
data. It is a regression diagnostic, not a new independent test set.
"""
import json
from pathlib import Path
import numpy as np
from train_fraud_text_model import model_scores,metrics
from calibrate_fraud_warning_policy import vocabulary_guard

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'models/cache/fraud-2026-10-09'

def main():
    rows=[json.loads(l) for l in (CACHE/'message-policy.jsonl').read_text().splitlines()]
    previous={r['index']:r for r in map(json.loads,(CACHE/'message-policy-baseline.jsonl').read_text().splitlines())}
    candidate=json.loads((CACHE/'candidate-text-model.json').read_text())
    baseline=json.loads((CACHE/'baseline-text-model.json').read_text())
    new_scores=model_scores(candidate,[r['classifierInputRaw'] for r in rows])
    old_scores=model_scores(baseline,[previous[r['index']]['classifierInputRaw'] for r in rows])
    eligible=np.asarray([r['eligible'] for r in rows],dtype=bool)
    policy=json.loads((ROOT/'models/fraud-text-warning-policy.json').read_text())
    guard=np.asarray([vocabulary_guard(r['classifierInputRaw'],set(candidate['vocabulary'])) for r in rows])
    old_eligible=np.asarray([previous[r['index']]['eligible'] for r in rows],dtype=bool)
    y=np.asarray([r['label'] for r in rows])
    def compare(mask):
        return dict(baseline_text_branch=metrics(y[mask],old_eligible[mask]&(old_scores[mask]>=baseline['threshold'])),
            candidate_text_branch=metrics(y[mask],((eligible&(new_scores>=candidate['threshold'])) | (guard&(new_scores>=policy['threshold'])))[mask]))
    evaluation=np.asarray([r['split']=='evaluation' and not r['synthetic'] for r in rows])
    report=dict(kind='Development runtime-policy regression; first model weights and original threshold unchanged; additional context-free threshold selected on validation only; already-inspected evaluation data',
        excludes='Embedded URL model, other rules and payment QR decisions; not full-app accuracy',
        warning_policy=dict(action_context_threshold=candidate['threshold'],context_free_threshold=policy['threshold'],guard_version=policy['guard_version']),
        observed_english=compare(evaluation),
        authored_languages={lang:compare(np.asarray([r['synthetic'] and r['language']==lang for r in rows])) for lang in ['en','hi','gu']})
    (ROOT/'models/fraud-text-runtime-comparison.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))

if __name__=='__main__':main()
