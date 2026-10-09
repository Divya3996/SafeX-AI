#!/usr/bin/env python3
"""Prepare a transient observed-English evaluation file, never bundle it in APKs."""
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'models/cache/fraud-2026-10-09'

def main():
    source=CACHE/'messages.jsonl'
    rows=[r for r in map(json.loads,source.read_text().splitlines()) if r['split']=='evaluation' and not r['synthetic']]
    target=CACHE/'native-observed-evaluation.jsonl'
    with target.open('w') as out:
        for row in rows:
            out.write(json.dumps(dict(id=hashlib.sha256(row['text'].encode()).hexdigest(),text=row['text'],label=row['label']),ensure_ascii=False)+'\n')
    print(json.dumps(dict(rows=len(rows),sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
        scope='Already-inspected historical development evaluation; transient device input only, never production/test APK asset')))

if __name__=='__main__':main()
