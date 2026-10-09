#!/usr/bin/env python3
"""Collect fixed licensed sources, never fetch a URL contained in a dataset.

Only this development script has network access. Raw research data stays in the
ignored cache. Source timestamps and retrieval timestamps have separate meanings.
"""
import argparse
import csv
import hashlib
import io
import json
import urllib.request
import zipfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MISHRA_URL = 'https://data.mendeley.com/public-files/datasets/f45bkkt8pr/files/edb361de-918d-469f-9106-e84823830665/file_downloaded'
MISHRA_HASH = '9bbf3188fdad81495d8e82825648b9b63b53fc86841a3d26c02629990b233cc3'
UCI_URL = 'https://archive.ics.uci.edu/static/public/228/sms+spam+collection.zip'
UCI_HASH = '1587ea43e58e82b14ff1f5425c88e17f8496bfcdb67a583dbff9eefaf9963ce3'
PHISH_BASE = 'https://raw.githubusercontent.com/Phishing-Database/Phishing.Database/master/'

def download(cache, name, url, limit, expected=None, refresh=False):
    target = cache / name
    metadata = cache / (name + '.metadata.json')
    if not target.exists() or not metadata.exists() or refresh:
        temp = target.with_suffix(target.suffix + '.part')
        digest = hashlib.sha256()
        size = 0
        try:
            request = urllib.request.Request(url, headers={'User-Agent': 'SafeX-AI-offline-research/1.0'})
            with urllib.request.urlopen(request, timeout=45) as response, temp.open('wb') as out:
                headers = dict(response.headers)
                while chunk := response.read(1024 * 1024):
                    size += len(chunk)
                    if size > limit:
                        raise ValueError(f'{name}: source exceeds download limit')
                    out.write(chunk)
                    digest.update(chunk)
            if expected and digest.hexdigest() != expected:
                raise ValueError(f'{name}: provenance hash changed; review before use')
            temp.replace(target)
            metadata.write_text(json.dumps(dict(url=url, retrieved_at=datetime.now(timezone.utc).isoformat(),
                bytes=size, sha256=digest.hexdigest(), source_last_modified=headers.get('Last-Modified'),
                etag=headers.get('ETag')), indent=2) + '\n')
        finally:
            temp.unlink(missing_ok=True)
    with target.open('rb') as source:
        digest = hashlib.file_digest(source, 'sha256').hexdigest()
    if expected and digest != expected:
        raise ValueError(f'{name}: cached checksum mismatch')
    record = json.loads(metadata.read_text()) if metadata.exists() else dict(url=url,
        retrieved_at=None, sha256=digest, bytes=target.stat().st_size,
        note='Existing research download; retrieval time was not recorded')
    if record['sha256'] != digest:
        raise ValueError(f'{name}: metadata checksum mismatch')
    return target, record

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--cache', type=Path, default=ROOT/'models/cache/fraud-2026-10-09')
    parser.add_argument('--refresh-feed', action='store_true')
    parser.add_argument('--skip-feed', action='store_true')
    args = parser.parse_args()
    cache = args.cache
    cache.mkdir(parents=True, exist_ok=True)
    sources, messages = [], []
    archive, record = download(cache, 'sms-smishing.zip', MISHRA_URL, 1024*1024, MISHRA_HASH)
    record.update(id='mishra-soni-2022', license='CC BY 4.0', published_at='2022-06-20',
        citation='Mishra, Sandhya; Soni, Devpriya (2022), DOI 10.17632/f45bkkt8pr.1')
    with zipfile.ZipFile(archive) as z:
        info = z.getinfo('Dataset_5971.csv')
        if info.file_size > 2*1024*1024:
            raise ValueError('Unexpected archive expansion')
        reader = csv.DictReader(io.StringIO(z.read(info).decode('utf-8-sig')))
        counts = Counter()
        for row in reader:
            label = row['LABEL'].lower().strip()
            counts[label] += 1
            if label not in {'ham', 'smishing', 'spam'}:
                raise ValueError('Unknown source label')
            if label == 'spam':
                continue  # Spam does not establish either fraud or legitimate content.
            messages.append(dict(text=row['TEXT'], label=int(label == 'smishing'),
                source=record['id'], language='en', category='observed_smishing' if label == 'smishing' else 'observed_ham',
                synthetic=False))
    record.update(raw_label_counts=dict(counts), excluded_spam=counts['spam'])
    sources.append(record)
    archive, record = download(cache, 'sms-spam.zip', UCI_URL, 1024*1024, UCI_HASH)
    record.update(id='uci-sms-2011', license='CC BY 4.0', published_at='2012-06-21',
        citation='Almeida, T.; Hidalgo, J. (2011), DOI 10.24432/C5CC84')
    with zipfile.ZipFile(archive) as z:
        info = z.getinfo('SMSSpamCollection')
        if info.file_size > 2*1024*1024:
            raise ValueError('Unexpected archive expansion')
        counts = Counter()
        for line in z.read(info).decode('utf-8-sig').splitlines():
            label, text = line.split('\t', 1)
            counts[label] += 1
            if label == 'ham':
                messages.append(dict(text=text, label=0, source=record['id'],
                    language='en', category='observed_ham', synthetic=False))
            elif label != 'spam':
                raise ValueError('Unknown UCI source label')
    record.update(raw_label_counts=dict(counts), excluded_spam=counts['spam'])
    sources.append(record)
    with (cache/'observed-messages.jsonl').open('w') as out:
        for row in messages:
            out.write(json.dumps(row, ensure_ascii=False)+'\n')
    if not args.skip_feed:
        try:
            license_path, license_record = download(cache, 'phishing-license.txt', PHISH_BASE+'LICENSE', 65536)
            if not license_path.read_text().startswith('MIT License'):
                raise ValueError('Feed license changed; review before use')
            feed, record = download(cache, 'phishing-active.txt', PHISH_BASE+'phishing-links-ACTIVE.txt',
                160*1024*1024, refresh=args.refresh_feed)
            record.update(id='phishing-database-active', license='MIT', license_sha256=license_record['sha256'],
                note='Maintainer-reported phishing links. Active means reachability, not independently confirmed fraud. No destinations visited.')
            sources.append(record)
        except (OSError, ValueError) as error:
            sources.append(dict(id='phishing-database-active', status='unavailable', reason=str(error)))
    manifest = dict(research_as_of='2026-10-09', generated_at=datetime.now(timezone.utc).isoformat(),
        sources=sources, observed_message_rows_before_deduplication=len(messages),
        collection_scope='Named sources only; not all fraud. Current retrieval does not make historical SMS current.',
        privacy='Local ignored research cache; identifiers masked in model features; no raw messages or threat URLs bundled into the APK')
    (ROOT/'models/fraud-data-manifest.json').write_text(json.dumps(manifest, indent=2)+'\n')
    print(json.dumps(manifest, indent=2), flush=True)

if __name__ == '__main__':
    main()
