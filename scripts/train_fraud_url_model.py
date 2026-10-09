#!/usr/bin/env python3
"""Train a fresh-feed URL candidate using exact Kotlin URL features.

Candidate artifacts remain separate unless a predeclared validation/evaluation
gate passes. Feed labels are reports, not independently verified page findings.
"""
import argparse
import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path
import numpy as np
from train_url_model import predict, sigmoid, metrics, export_tflite

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'models/cache/fraud-2026-10-09'
ASSETS=ROOT/'android-app/app/src/main/assets'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--promote',action='store_true')
    parser.add_argument('--epochs',type=int,default=12);args=parser.parse_args()
    # Baseline artifacts are immutable across reruns of this experiment.
    for name in ['model.tflite','scaler.json','url-model-card.json']:
        target=CACHE/('baseline-'+name)
        if not target.exists():shutil.copy2(ASSETS/name,target)
    if not (CACHE/'baseline-url-weights.npz').exists():
        shutil.copy2(ROOT/'models/cache/url-weights.npz',CACHE/'baseline-url-weights.npz')
    rows=[];historical_groups=set()
    with (ROOT/'models/cache/features-updated.jsonl').open() as source:
        for line in source:
            r=json.loads(line)
            if not r['valid']:continue
            historical_groups.add(r['group'])
            if r['split']=='train' and r['index']%4!=0:continue
            rows.append(dict(features=r['features'],label=r['label'],group=r['group'],split=r['split'],source='historical',index=r['index'],ruleScore=r['ruleScore']))
    # Synthetic URL variants stay attached to their owner's partition.
    with (ROOT/'models/cache/features-augmented.jsonl').open() as source:
        for line in source:
            r=json.loads(line)
            if r['valid'] and abs(r['index'])%8==0:
                rows.append(dict(features=r['features'],label=r['label'],group=r['group'],split=r['split'],source='augmented',index=r['index'],ruleScore=r['ruleScore']))
    feed_coverage={r['index']:r['eligible'] for r in map(json.loads,(CACHE/'url-coverage.jsonl').read_text().splitlines())}
    invalid=0
    with (CACHE/'url-features.jsonl').open() as source:
        for line in source:
            r=json.loads(line)
            if not r['valid']:invalid+=1;continue
            r.update(source='reported_feed',novel_group=r['group'] not in historical_groups,eligible=feed_coverage[r['index']])
            rows.append(r)
    groups={s:{r['group'] for r in rows if r['split']==s} for s in ['train','validation','test']}
    if any(groups[a]&groups[b] for a,b in [('train','validation'),('train','test'),('validation','test')]):
        raise ValueError('Registrable-domain leakage')
    x=np.asarray([r['features'] for r in rows],dtype=np.float32)
    y=np.asarray([r['label'] for r in rows],dtype=np.float32)
    source=np.asarray([r['source'] for r in rows]);split=np.asarray([r['split'] for r in rows])
    scaler=json.loads((CACHE/'baseline-scaler.json').read_text())
    z=np.clip((x-np.asarray(scaler['mean'],dtype=np.float32))/np.asarray(scaler['scale'],dtype=np.float32),-8,8).astype(np.float32)
    archive=np.load(CACHE/'baseline-url-weights.npz');baseline=[archive[f'p{i}'].copy() for i in range(6)]
    verification=CACHE/'baseline-weight-verification.tflite'
    try:
        export_tflite(baseline,verification)
        if verification.read_bytes() != (CACHE/'baseline-model.tflite').read_bytes():
            raise ValueError('Baseline parameter cache does not match the preserved native model; restore consistent artifacts before training')
    finally:
        verification.unlink(missing_ok=True)
    parameters=[p.copy() for p in baseline];card=json.loads((CACHE/'baseline-url-model-card.json').read_text())
    train_indices=np.flatnonzero(split=='train');rng=np.random.default_rng(20261009)
    moments=[np.zeros_like(p) for p in parameters];variances=[np.zeros_like(p) for p in parameters]
    step=0;epochs=[];best=None;best_loss=float('inf')
    validation=split=='validation'
    # Source-balanced checkpoint loss: guard historical controls while learning new reports.
    validation_weights=np.where(source=='reported_feed',.30/max(1,np.sum(validation&(source=='reported_feed'))),
        .70/max(1,np.sum(validation&(source!='reported_feed'))))
    for epoch in range(args.epochs):
        order=rng.permutation(train_indices)
        for start in range(0,len(order),512):
            idx=order[start:start+512];a=[z[idx]];pre=[]
            for layer in range(3):
                value=a[-1]@parameters[layer*2]+parameters[layer*2+1];pre.append(value)
                a.append(sigmoid(value) if layer==2 else np.maximum(value,0))
            delta=(a[-1]-y[idx,None])/len(idx)
            gradients=[]
            for layer in range(2,-1,-1):
                gradients[0:0]=[a[layer].T@delta+1e-4*parameters[layer*2],delta.sum(axis=0)]
                if layer:delta=(delta@parameters[layer*2].T)*(pre[layer-1]>0)
            step+=1
            for i,(p,g) in enumerate(zip(parameters,gradients)):
                moments[i]=.9*moments[i]+.1*g;variances[i]=.999*variances[i]+.001*g*g
                p-=.00025*(moments[i]/(1-.9**step))/(np.sqrt(variances[i]/(1-.999**step))+1e-8)
        vp=np.clip(predict(z[validation],parameters),1e-7,1-1e-7);truth=y[validation]
        loss=float(np.sum(-(truth*np.log(vp)+(1-truth)*np.log(1-vp))*validation_weights[validation]))
        epochs.append(dict(epoch=epoch+1,validation_loss=loss))
        if loss<best_loss:best_loss=loss;best=[p.copy() for p in parameters]
        print(f'URL epoch {epoch+1}: validation loss {loss:.6f}',flush=True)
    parameters=best;score=predict(z,parameters);old_score=predict(z,baseline)
    choices=[]
    for threshold in np.linspace(.05,.999,951):
        h=metrics(y[validation&(source=='historical')],score[validation&(source=='historical')]>=threshold)
        a=metrics(y[validation&(source=='augmented')],score[validation&(source=='augmented')]>=threshold)
        f=metrics(y[validation&(source=='reported_feed')],score[validation&(source=='reported_feed')]>=threshold)
        if h['false_positive_rate']<=.01 and a['false_positive_rate']<=.01:
            choices.append((float(threshold),.7*h['recall']+.3*f['recall']))
    if not choices:raise ValueError('No warning threshold satisfies validation controls')
    threshold=max(choices,key=lambda v:(v[1],v[0]))[0]
    path=CACHE/'candidate-url-model.tflite';export_tflite(parameters,path)
    (CACHE/'candidate-url-scaler.json').write_text(json.dumps(scaler,indent=2)+'\n')
    np.savez(CACHE/'candidate-url-weights.npz',**{f'p{i}':p for i,p in enumerate(parameters)})
    new_card=dict(version='url-phiusiil-reported-feed-v3',warning_threshold=threshold,
        kind='URL-only structural classifier; not calibrated scam probability',
        training_source='PhiUSIIL CC BY 4.0, authored augmentation and Phishing.Database MIT reported-feed snapshot retrieved 2026-10-09',
        sha256=hashlib.sha256(path.read_bytes()).hexdigest())
    (CACHE/'candidate-url-model-card.json').write_text(json.dumps(new_card,indent=2)+'\n')
    test=split=='test';novel=np.asarray([r.get('novel_group',False) for r in rows])
    report=dict(version=new_card['version'],trained_at=datetime.now(timezone.utc).isoformat(),epochs=epochs,
        threshold=threshold,threshold_selection='Validation only, <=1% model false warnings on historical and augmented controls; maximize source-weighted recall',
        scaler_policy='Frozen baseline scaler learned on original training groups, never current evaluation data',
        source_manifest_sha256=hashlib.sha256((ROOT/'models/fraud-data-manifest.json').read_bytes()).hexdigest(),
        splits={s:dict(rows=int(np.sum(split==s)),groups=len(groups[s])) for s in ['train','validation','test']},
        excluded_invalid_feed_rows=invalid,candidate_sha256=new_card['sha256'])
    for name,mask in [('historical',test&(source=='historical')),('augmented',test&(source=='augmented')),
        ('reported_feed',test&(source=='reported_feed')),('novel_reported_feed',test&(source=='reported_feed')&novel)]:
        report[name]=dict(baseline=metrics(y[mask],old_score[mask]>=card['warning_threshold']),
            candidate=metrics(y[mask],score[mask]>=threshold))
        if 'reported_feed' in name:
            report[name]['scope']='Positive reported URLs only; precision, false-positive rate and general accuracy cannot be estimated'
            for variant in ['baseline','candidate']:
                report[name][variant].update(precision=None,false_positive_rate=None,accuracy=None)
    fresh=report['novel_reported_feed'];historical=report['historical'];augmented=report['augmented']
    accepted=(fresh['candidate']['recall']>fresh['baseline']['recall'] and fresh['candidate']['tp']+fresh['candidate']['fn']>=100
        and historical['candidate']['false_positive_rate']<=.01 and augmented['candidate']['false_positive_rate']<=.01
        and historical['candidate']['recall']>=historical['baseline']['recall']-.02)
    report.update(promotion_gate_passed=bool(accepted),promoted=bool(args.promote and accepted),
        limitations=['Feed snapshot labels are maintainer reports; sites were not visited or independently confirmed.',
            'Fresh feed contains positives only; contemporary independently verified benign URLs are unavailable.',
            'Historical evaluation partition was inspected during prior development; it is a regression comparison.',
            'New-domain reported-feed holdout was not used for fitting/checkpoint/threshold selection.',
            'Model-only figures exclude runtime abstention, URL rules and imported snapshot evidence.',
            'Opaque redirects, compromised pages, consent phishing and website contents cannot be inferred reliably from URL text alone.'])
    if report['promoted']:
        shutil.copy2(path,ASSETS/'model.tflite')
        shutil.copy2(CACHE/'candidate-url-scaler.json',ASSETS/'scaler.json')
        shutil.copy2(CACHE/'candidate-url-model-card.json',ASSETS/'url-model-card.json')
    (ROOT/'models/fraud-url-evaluation.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ['threshold','historical','augmented','novel_reported_feed','promotion_gate_passed','promoted','candidate_sha256']},indent=2),flush=True)

if __name__=='__main__':main()
