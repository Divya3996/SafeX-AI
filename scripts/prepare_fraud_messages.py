#!/usr/bin/env python3
"""Group duplicates and near-duplicate campaigns before splitting research SMS."""
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path
from urllib.parse import urlsplit
import re
from fraud_text_features import campaign_template, normalize

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT/'models/cache/fraud-2026-10-09'

def sha(text):
    return hashlib.sha256(text.encode()).hexdigest()

def main():
    rows = [json.loads(line) for line in (CACHE/'observed-messages.jsonl').read_text().splitlines()]
    for i, row in enumerate(json.loads((ROOT/'models/text-training.json').read_text())):
        rows.append(dict(text=row['text'], label=row['label'], source='safex-authored-legacy',
            language='hi' if re.search('[\u0900-\u097f]', row['text']) else 'gu' if re.search('[\u0a80-\u0aff]', row['text']) else 'en',
            category='legacy_authored', synthetic=True))
    patterns = json.loads((ROOT/'models/fraud-patterns-2026.json').read_text())
    for pattern in patterns['patterns']:
        for language in ['en', 'hi', 'gu']:
            for label, text in zip([1, 0], pattern[language]):
                for variant in [text, text.upper(), text.replace(' ', '  ')]:
                    rows.append(dict(text=variant, label=label, source='safex-authored-2026',
                        language=language, category=pattern['id'], synthetic=True,
                        family='authored:'+pattern['id']))
    # Conflicting exact templates are quarantined, not silently relabeled.
    duplicates = defaultdict(list)
    for row in rows:
        key = campaign_template(row['text'])
        if key:
            duplicates[key].append(row)
    selected, conflicting, removed = [], 0, 0
    for key, candidates in sorted(duplicates.items()):
        if len({r['label'] for r in candidates}) != 1:
            conflicting += len(candidates)
            continue
        chosen = candidates[0].copy()
        chosen.update(template=key, sources=sorted({r['source'] for r in candidates}))
        selected.append(chosen)
        removed += len(candidates)-1
    parent = list(range(len(selected)))
    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i
    def union(a, b):
        a, b = find(a), find(b)
        if a != b:
            parent[max(a,b)] = min(a,b)
    # Same authored category across languages stays in one partition.
    families = {}
    for i, row in enumerate(selected):
        if family := row.get('family'):
            if family in families:
                union(i, families[family])
            else:
                families[family] = i
    # Deterministic minhash candidates, followed by actual Jaccard comparison.
    shingles, buckets = [], defaultdict(list)
    for i, row in enumerate(selected):
        words = row['template'].split()
        sets = {' '.join(words[j:j+3]) for j in range(max(0,len(words)-2))}
        shingles.append(sets)
        if len(sets) < 4:
            continue
        for salt in range(8):
            key = (salt, min(sha(str(salt)+':'+s)[:16] for s in sets))
            for other in buckets[key]:
                overlap = len(sets & shingles[other])/len(sets | shingles[other])
                if overlap >= .80:
                    union(i, other)
            buckets[key].append(i)
    components = defaultdict(list)
    for i, row in enumerate(selected):
        components[find(i)].append(row['template'])
    groups = {i:sha('SafeX fraud messages v3:'+min(values)) for i, values in components.items()}
    exports = []
    for i, row in enumerate(selected):
        group = groups[find(i)]
        bucket = int(group[:8],16)%100
        row.update(index=i, group=group, split='train' if bucket<70 else 'validation' if bucket<85 else 'evaluation')
        row.pop('template')
        exports.append(row)
    with (CACHE/'messages.jsonl').open('w') as out:
        for row in exports:
            out.write(json.dumps(row, ensure_ascii=False)+'\n')
    summary = dict(input_rows=len(rows), unique_rows=len(exports), removed_duplicate_rows=removed,
        quarantined_conflicting_template_rows=conflicting, campaign_groups=len(groups),
        split_method='SHA256 campaign groups 70/15/15; exact masked templates + deterministic minhash Jaccard >=0.80; authored translations share category group',
        limitations=['Near-duplicate grouping is heuristic, not complete campaign attribution.',
            'Observed SMS is historical English. Hindi/Gujarati augmentation is authored, not an independent language benchmark.'],
        splits={split:dict(rows=sum(r['split']==split for r in exports),
            smishing=sum(r['split']==split and r['label']==1 for r in exports),
            observed=sum(r['split']==split and not r['synthetic'] for r in exports)) for split in ['train','validation','evaluation']})
    (ROOT/'models/fraud-message-data-summary.json').write_text(json.dumps(summary, indent=2)+'\n')
    print(json.dumps(summary, indent=2))

if __name__ == '__main__':
    main()
