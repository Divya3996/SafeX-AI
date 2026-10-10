#!/usr/bin/env python3
"""Check actual 64-bit ELF and uncompressed APK payload alignment for 16 KB pages."""
import argparse
import hashlib
import json
import struct
import subprocess
import tempfile
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('--output', type=Path)
args = parser.parse_args()
entries = []
with tempfile.TemporaryDirectory(prefix='safex-native-') as directory, zipfile.ZipFile(args.apk) as archive, args.apk.open('rb') as stream:
    for entry in archive.infolist():
        if not entry.filename.endswith('.so') or not any(abi in entry.filename for abi in ['arm64-v8a/', 'x86_64/']):
            continue
        target = Path(directory) / Path(entry.filename).name
        target.write_bytes(archive.read(entry))
        output = subprocess.check_output(['readelf', '-lW', str(target)], text=True)
        alignment = [int(line.split()[-1], 16) for line in output.splitlines() if line.strip().startswith('LOAD')]
        stream.seek(entry.header_offset)
        header = stream.read(30)
        name_length, extra_length = struct.unpack_from('<HH', header, 26)
        offset = entry.header_offset + 30 + name_length + extra_length
        entries.append({'library': entry.filename, 'elfLoadAlignment': alignment,
                        'elf16KB': bool(alignment) and all(value >= 16384 for value in alignment),
                        'zip16KB': entry.compress_type != zipfile.ZIP_STORED or offset % 16384 == 0})
report = {'apk': args.apk.name, 'sha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
          'libraries': entries, 'passed': bool(entries) and all(row['elf16KB'] and row['zip16KB'] for row in entries),
          'scope': 'Static packaged ELF/ZIP checks; not a 16 KB runtime test.'}
if args.output:
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
raise SystemExit(0 if report['passed'] else 1)
