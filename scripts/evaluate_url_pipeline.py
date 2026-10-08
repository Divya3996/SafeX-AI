#!/usr/bin/env python3
"""Evaluate the actual model abstention + rules policy on original held-out rows."""
import json
from pathlib import Path
import numpy as np
from train_url_model import predict, metrics
ROOT=Path(__file__).resolve().parents[1]
def evaluate():
    cache=ROOT/'models/cache'
    coverage={}
    with (cache/'model-coverage.jsonl').open() as f:
        for line in f:
            r=json.loads(line);coverage[r['index']]=r['eligible']
    rows=[]
    with (cache/'features-updated.jsonl').open() as f:
        for line in f:
            r=json.loads(line)
            if r['valid'] and r['split']=='test':rows.append(r)
    scaler=json.loads((ROOT/'android-app/app/src/main/assets/scaler.json').read_text())
    x=np.asarray([r['features'] for r in rows],dtype=np.float32)
    z=np.clip((x-np.asarray(scaler['mean'],dtype=np.float32))/np.asarray(scaler['scale'],dtype=np.float32),-8,8)
    weights=np.load(cache/'url-weights.npz');p=[weights[f'p{i}'] for i in range(6)]
    prediction=predict(z,p);eligible=np.asarray([coverage[r['index']] for r in rows],dtype=bool)
    y=np.asarray([r['label'] for r in rows]);rules=np.asarray([r['ruleScore'] for r in rows])
    report_path=ROOT/'models/url-evaluation.json';report=json.loads(report_path.read_text())
    report['ungated_pipeline_test']=metrics(y,(rules>=30)|(prediction>=report['threshold']))
    report['new_pipeline_test']=metrics(y,(rules>=30)|(eligible & (prediction>=report['threshold'])))
    report['model_abstention_policy']='Standalone model warnings disabled for IDN, exact official-brand domains and structured email/login_hint query contexts; heuristics/snapshot remain active'
    report['test_model_eligible_rows']=int(eligible.sum());report['test_model_limited_rows']=int((~eligible).sum())
    report_path.write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ['legacy_pipeline_test','new_pipeline_test','test_model_limited_rows']},indent=2))
if __name__=='__main__':evaluate()
