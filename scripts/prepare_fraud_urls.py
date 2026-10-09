#!/usr/bin/env python3
"""Bounded deterministic sample of reported URLs; never visits destinations."""
import hashlib
import heapq
import json
from collections import Counter
from pathlib import Path
from urllib.parse import urlsplit

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'models/cache/fraud-2026-10-09'

def main():
    wanted=40000; heap=[]; hosts=set(); counts=Counter()
    with (CACHE/'phishing-active.txt').open(errors='strict') as source:
        for line in source:
            url=line.strip();counts['feed_lines']+=1
            if not url or url.startswith('#'):
                counts['comments_or_blank']+=1;continue
            try:
                parsed=urlsplit(url)
                if parsed.scheme not in {'http','https'} or not parsed.hostname or len(url)>8192:
                    counts['unsupported_or_oversized']+=1;continue
                host=parsed.hostname.lower().rstrip('.')
                if host in hosts:
                    counts['repeated_host']+=1;continue
                hosts.add(host)
            except ValueError:
                counts['malformed']+=1;continue
            counts['distinct_http_hosts']+=1
            # Keep the lowest deterministic host hashes, avoiding file-order sampling.
            key=int(hashlib.sha256(('SafeX feed sample:'+host).encode()).hexdigest(),16)
            item=(-key,url)
            if len(heap)<wanted:heapq.heappush(heap,item)
            elif item>heap[0]:heapq.heapreplace(heap,item)
    with (CACHE/'urls.jsonl').open('w') as out:
        for index,(_,url) in enumerate(sorted(heap,reverse=True)):
            out.write(json.dumps(dict(index=index,url=url,label=1,source='phishing-database-active'))+'\n')
    summary=dict(counts=dict(counts),sample_rows=len(heap),label_scope='Maintainer-reported phishing, not independently confirmed',
        sample_method='Lowest SHA256 host hashes; first reported URL per exact host. Kotlin export then groups public-suffix-aware registrable domains.',
        scope='Feed snapshot is an archive of reported links; retrieval today does not establish that each incident occurred today.')
    (ROOT/'models/fraud-url-data-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps(summary,indent=2))

if __name__=='__main__':main()
