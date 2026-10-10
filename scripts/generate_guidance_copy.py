#!/usr/bin/env python3
"""Keep the browser introduction and incident guide on the shared language catalog."""
import argparse
import ast
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
rows = {}
for line in (root / 'localization/translations.tsv').read_text().splitlines():
    if line and not line.startswith('#'):
        english, hindi, gujarati = line.split('|')
        rows[english] = [hindi, gujarati]
needed = set()
for name in ['guidance.ts', 'incident-help.ts', 'sidepanel.ts']:
    source = (root / 'chrome-extension/src' / name).read_text()
    # In the side panel, only g(...) calls use this catalog; other controls use i18n.ts.
    pattern = r'''g\(\s*(["'])((?:\\.|(?!\1).)*?)\1''' if name == 'sidepanel.ts' else r'''(["'])((?:\\.|(?!\1).)*?)\1'''
    for match in re.finditer(pattern, source, re.S):
        literal = match.group(0).split('(', 1)[-1].strip() if name == 'sidepanel.ts' else match.group(0)
        value = ast.literal_eval(literal)
        if re.match(r'^[A-Z]', value) and (' ' in value or value in ['Next', 'Finish', 'India', 'Help', 'Language', 'Cancel', 'Back']):
            needed.add(value)
missing = needed - rows.keys()
if missing:
    raise SystemExit('Missing guide translations: ' + ', '.join(sorted(missing)))
generated = json.dumps({key: rows[key] for key in sorted(needed)}, ensure_ascii=False, indent=2) + '\n'
target = root / 'chrome-extension/src/guidance-copy.json'
if args.check:
    if target.read_text() != generated:
        raise SystemExit('Run python3 scripts/generate_guidance_copy.py')
else:
    target.write_text(generated)
print(f'{len(needed)} guidance strings verified in English, Hindi and Gujarati.')
