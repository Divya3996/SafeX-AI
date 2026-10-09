#!/usr/bin/env python3
"""Promote already-evaluated candidates without refitting or selecting thresholds."""
import hashlib
import json
import shutil
from pathlib import Path
import numpy as np
from train_url_model import predict
from train_fraud_text_model import model_scores

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'models/cache/fraud-2026-10-09'
ASSETS=ROOT/'android-app/app/src/main/assets'

def main():
    for kind,filename in [('text','candidate-text-model.json'),('url','candidate-url-model.tflite')]:
        report_path=ROOT/f'models/fraud-{kind}-evaluation.json'
        report=json.loads(report_path.read_text())
        if not report['promotion_gate_passed']:
            print(f'{kind}: gate failed; shipped artifact retained');continue
        candidate=CACHE/filename
        if hashlib.sha256(candidate.read_bytes()).hexdigest()!=report['candidate_sha256']:
            raise ValueError(f'{kind}: candidate changed after evaluation')
        if kind=='text':
            content=json.loads(candidate.read_text())
            if content['threshold']!=report['threshold'] or content['version']!=report['version']:
                raise ValueError('Text metadata does not match evaluated model')
            shutil.copy2(candidate,ASSETS/'text-model.json')
            card=dict(version=report['version'],sha256=report['candidate_sha256'],
                training_scope='Historical observed English + authored English/Hindi/Gujarati; not all current fraud',
                provenance_manifest_sha256=report['source_manifest_sha256'])
            (ASSETS/'text-model-card.json').write_text(json.dumps(card,indent=2)+'\n')
        else:
            content=json.loads((CACHE/'candidate-url-model-card.json').read_text())
            if content['sha256']!=report['candidate_sha256'] or content['warning_threshold']!=report['threshold']:
                raise ValueError('URL metadata does not match evaluated model')
            if (CACHE/'candidate-url-scaler.json').read_bytes()!=(CACHE/'baseline-scaler.json').read_bytes():
                raise ValueError('URL scaler differs from frozen training scaler')
            for source,target in [('candidate-url-model.tflite','model.tflite'),('candidate-url-scaler.json','scaler.json'),('candidate-url-model-card.json','url-model-card.json')]:
                shutil.copy2(CACHE/source,ASSETS/target)
            # Historical parity inputs only; recompute expected scores for the new artifact.
            fixture=json.loads((ROOT/'models/cache/native-url-evaluation.json').read_text())
            scaler=json.loads((ASSETS/'scaler.json').read_text())
            x=np.asarray([r['features'] for r in fixture['cases']],dtype=np.float32)
            z=np.clip((x-np.asarray(scaler['mean'],dtype=np.float32))/np.asarray(scaler['scale'],dtype=np.float32),-8,8)
            weights=np.load(CACHE/'candidate-url-weights.npz')
            probabilities=predict(z,[weights[f'p{i}'] for i in range(6)])
            for case,probability in zip(fixture['cases'],probabilities):case['predicted']=float(probability)
            fixture.update(threshold=report['threshold'],model_version=report['version'])
            (ROOT/'android-app/app/src/androidTest/assets/native-url-evaluation.json').write_text(json.dumps(fixture)+'\n')
        report.update(promoted=True,promotion_method='Copy checksum-verified first candidate; no refit or new threshold selection')
        report_path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        print(f'{kind}: promoted {report["version"]}; threshold {report["threshold"]}')
    # Only authored text is bundled as Android parity data.
    model=json.loads((ASSETS/'text-model.json').read_text())
    authored=json.loads((ROOT/'models/text-evaluation.json').read_text())['examples']
    scores=model_scores(model,[r['text'] for r in authored])
    fixture=dict(model_version=model['version'],threshold=model['threshold'],
        cases=[dict(text=r['text'],label=r['label'],predicted=float(p)) for r,p in zip(authored,scores)])
    (ROOT/'android-app/app/src/androidTest/assets/text-model-parity.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
    (ROOT/'models/text-model-parity.json').write_text(json.dumps(fixture,ensure_ascii=False,indent=2)+'\n')
    patterns=json.loads((ROOT/'models/fraud-patterns-2026.json').read_text())
    contracts=dict(scope=patterns['scope'],cases=[dict(id=pattern['id']+':'+language+':'+str(label),language=language,
        text=text,expected='WARN' if label else 'ALLOW') for pattern in patterns['patterns'] for language in ['en','hi','gu']
        for label,text in zip([1,0],pattern[language])])
    (ROOT/'android-app/app/src/androidTest/assets/fraud-app-contracts.json').write_text(json.dumps(contracts,ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':main()
