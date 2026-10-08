#!/usr/bin/env python3
"""Mechanical URL-text augmentation, never visits a destination.

Same-owner www/root host assumptions and benign paths are synthetic labels, not
new observations of live page safety. Domain grouping remains unchanged.
"""
import json
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit, quote
ROOT=Path(__file__).resolve().parents[1]
paths=['/login','/signin','/account/settings','/help','/products','/documentation/security',
       '/?next=%2Fdashboard','/?utm_source=newsletter','/?email=person%40example.com',
       '/manual.pdf','/static/app.js','/common/oauth2/authorize?response_type=code',
       '/'+quote('हिन्दी ગુજરાતી'),'/search?q=help','/Path+A?Token=A+B',
       '/news/2026/10/08','/docs/downloads','/privacy','/','/?next=SAME_SITE']
count=0
with (ROOT/'models/cache/urls.jsonl').open() as src, (ROOT/'models/cache/augmented-urls.jsonl').open('w') as out:
    for line in src:
        r=json.loads(line);u=urlsplit(r['url'])
        if not u.hostname or u.username or u.port:continue
        host=u.hostname
        # Remove a superficial dataset source cue from both classes.
        if host.startswith('www.'):
            host=host[4:]
            alt=urlunsplit((u.scheme,host,u.path,u.query,u.fragment))
        else:
            alt=urlunsplit((u.scheme,'www.'+host,u.path,u.query,u.fragment))
        for kind,url in [('host_variant',alt)]+([] if r['label'] else [('benign_path',u.scheme+'://'+host+paths[r['index']%len(paths)].replace('SAME_SITE',quote(u.scheme+'://'+host+'/dashboard',safe='')))]):
            out.write(json.dumps(dict(index=-(count+1),source_index=r['index'],source_url=r['url'],augmentation=kind,url=url,label=r['label']),ensure_ascii=False)+'\n');count+=1
print(f'Wrote {count} synthetic URL variants. These do not establish live site safety.')
