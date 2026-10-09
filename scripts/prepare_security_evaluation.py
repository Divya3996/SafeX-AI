#!/usr/bin/env python3
"""Validate authorized, de-identified JSONL data locally. Never trains or uploads user scans."""
import argparse, hashlib, json, unicodedata
from pathlib import Path

def prepare(path):
    if path.stat().st_size > 50 * 1024 * 1024: raise ValueError("Dataset exceeds 50 MB")
    rows=[];ids=set();content_splits={};group_splits={}
    for number,line in enumerate(path.read_text().splitlines(),1):
        if not line.strip():continue
        r=json.loads(line)
        required=('id','content','language','label','category','source','license','source_group','split','authorized','input_type')
        if any(k not in r for k in required):raise ValueError(f'Row {number}: missing required metadata')
        if r['authorized'] is not True:raise ValueError(f'Row {number}: source authorization missing')
        if r['language'] not in ('en','hi','gu') or r['label'] not in (0,1):raise ValueError(f'Row {number}: invalid label/language')
        if r['split'] not in ('train','validation','evaluation'):raise ValueError(f'Row {number}: invalid partition')
        if r['input_type'] not in ('text','link','qr'):raise ValueError(f'Row {number}: unsupported input')
        if not all(isinstance(r[k],str) and r[k].strip() for k in ('id','source','license','source_group','category','content')):raise ValueError(f'Row {number}: empty metadata/content')
        if len(r['content'])>(8192 if r['input_type']=='link' else 12000):raise ValueError(f'Row {number}: input exceeds app bounds')
        if r['id'] in ids:raise ValueError(f'Row {number}: duplicate ID')
        ids.add(r['id'])
        normalized=unicodedata.normalize('NFKC',r['content']).casefold()
        digest=hashlib.sha256(normalized.encode()).hexdigest()
        prior=content_splits.setdefault(digest,(r['split'],r['label']))
        if prior!=(r['split'],r['label']):raise ValueError(f'Row {number}: duplicate content crosses partitions or has conflicting labels')
        groups=[r['source_group']]+r.get('domain_groups',[])
        if r['input_type']=='link' and not r.get('domain_groups'):raise ValueError(f'Row {number}: links need explicit registrable-domain groups')
        for group in groups:
            if not isinstance(group,str) or not group.strip():raise ValueError(f'Row {number}: empty group')
            if group_splits.setdefault(group,r['split'])!=r['split']:raise ValueError(f'Row {number}: source/domain group crosses partitions')
        rows.append(r)
        if len(rows)>50000:raise ValueError("Dataset exceeds 50,000 rows")
    if not rows:raise ValueError('Dataset is empty')
    return rows
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('input',type=Path);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    rows=prepare(a.input);a.output.write_text(json.dumps(dict(schema_version=1,kind='User-authorized evaluation data; representativeness and near-duplicate audit still required',cases=rows),ensure_ascii=False,indent=2)+'\n')
    print(f'{len(rows)} rows validated locally; no training or upload performed.')
