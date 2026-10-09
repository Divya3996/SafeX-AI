#!/usr/bin/env python3
"""Export the verified Android models for offline browser inference; no refitting."""
import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))
from train_url_model import export_tflite, predict

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    assets = ROOT / 'android-app/app/src/main/assets'
    dest = ROOT / 'chrome-extension/public/models'
    dest.mkdir(parents=True, exist_ok=True)
    weights = np.load(ROOT / 'models/cache/fraud-2026-10-09/candidate-url-weights.npz')
    params = [weights[f'p{i}'] for i in range(6)]
    with tempfile.TemporaryDirectory(prefix='safex-extension-export-') as temp:
        native = Path(temp) / 'model.tflite'
        export_tflite(params, native)
        if sha(native) != sha(assets / 'model.tflite'):
            raise RuntimeError('Parameter cache does not reproduce the shipped model')
    card = json.loads((assets / 'url-model-card.json').read_text())
    scaler = json.loads((assets / 'scaler.json').read_text())
    url = dict(version=card['version'], threshold=card['warning_threshold'],
               mean=scaler['mean'], scale=scaler['scale'], clip=scaler['clip'],
               parameters=[p.tolist() for p in params], android_sha256=sha(assets / 'model.tflite'))
    (dest / 'url-model.json').write_text(json.dumps(url, separators=(',', ':')) + '\n')
    for name in ['text-model.json', 'text-model-card.json', 'text-warning-policy.json', 'url-model-card.json', 'security-model-attribution.txt']:
        shutil.copyfile(assets / name, dest / name)
    manifest = {p.name: sha(p) for p in sorted(dest.iterdir()) if p.is_file() and p.name != 'manifest.json'}
    (dest / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    fixture_dest = ROOT / 'chrome-extension/tests/fixtures'
    fixture_dest.mkdir(parents=True, exist_ok=True)
    native = json.loads((ROOT / 'models/cache/native-url-evaluation.json').read_text())['cases']
    x = np.asarray([c['features'] for c in native], dtype=np.float32)
    scaled = np.clip((x - np.asarray(scaler['mean'], dtype=np.float32)) / np.asarray(scaler['scale'], dtype=np.float32), -scaler['clip'], scaler['clip'])
    scores = predict(scaled, params)
    cases = [dict(features=c['features'], predicted=float(p)) for c, p in zip(native, scores)]
    (fixture_dest / 'url-vector-parity.json').write_text(json.dumps(dict(scope='256 Kotlin feature vectors; exported model regression, not accuracy evidence; raw URL corpus excluded', cases=cases), indent=2) + '\n')
    shutil.copyfile(ROOT / 'models/text-model-parity.json', fixture_dest / 'text-model-parity.json')
    shutil.copyfile(ROOT / 'android-app/app/src/androidTest/assets/fraud-app-contracts.json', fixture_dest / 'fraud-app-contracts.json')
    print(json.dumps(dict(artifacts=manifest, url_parity_rows=len(cases), weights_match_android=True), indent=2))

if __name__ == '__main__':
    main()
