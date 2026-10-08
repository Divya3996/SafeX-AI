#!/usr/bin/env python3
"""Prepare licensed research URLs locally, without visiting any URL in the dataset."""
import csv
import hashlib
import io
import json
import urllib.request
import zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'models/cache'
URL='https://archive.ics.uci.edu/static/public/967/phiusiil+phishing+url+dataset.zip'
EXPECTED='0a639fd03aea6308c5b1c10c92aa23c2ce1505447a9137271865cd0badc9a59a'
CACHE.mkdir(parents=True,exist_ok=True)
archive=CACHE/'phiusiil.zip'
if not archive.exists():
    with urllib.request.urlopen(URL,timeout=120) as response, archive.open('wb') as out:
        size=0
        while chunk:=response.read(1024*1024):
            size+=len(chunk)
            if size>32*1024*1024:raise ValueError('Research archive exceeds expected download bound')
            out.write(chunk)
if hashlib.sha256(archive.read_bytes()).hexdigest()!=EXPECTED:
    raise ValueError('Dataset changed; review provenance before using a different archive')
with zipfile.ZipFile(archive) as z:
    name=next(n for n in z.namelist() if n.endswith('.csv'))
    if z.getinfo(name).file_size>80*1024*1024:raise ValueError('Dataset expansion exceeds expected bound')
    with z.open(name) as raw, (CACHE/'urls.jsonl').open('w') as out:
        reader=csv.DictReader(io.TextIOWrapper(raw,encoding='utf-8-sig'))
        count=0
        for index,row in enumerate(reader):
            # UCI 1=legitimate / 0=phishing; the app model uses 1=phishing.
            out.write(json.dumps({'index':index,'url':row['URL'],'label':1-int(row['label'])},ensure_ascii=False)+'\n')
            count+=1
print(f'Prepared {count} historical URL records. No dataset destination was opened.')
