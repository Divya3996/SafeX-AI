#!/usr/bin/env python3
"""Fit a bounded offline text model; validation selects checkpoint and threshold.

Uses exact production-Kotlin policy/features exported by TextResearchExportTest.
Writes a candidate first. Promotion requires measured held-out improvement.
"""
import argparse
import hashlib
import json
import math
import re
import shutil
import unicodedata
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
import numpy as np
from fraud_text_features import FEATURE_VERSION, features

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT/'models/cache/fraud-2026-10-09'

def sigmoid(logit):
    return 1/(1+np.exp(-np.clip(logit,-30,30)))

def metrics(labels, predictions):
    y=np.asarray(labels,dtype=bool); p=np.asarray(predictions,dtype=bool)
    tp=int(np.sum(y&p)); fp=int(np.sum(~y&p)); fn=int(np.sum(y&~p)); tn=int(np.sum(~y&~p))
    return dict(rows=len(y), tp=tp,fp=fp,fn=fn,tn=tn,recall=tp/max(1,tp+fn),
        precision=tp/max(1,tp+fp),false_positive_rate=fp/max(1,fp+tn),accuracy=(tp+tn)/max(1,len(y)))

def old_features(text):
    text=unicodedata.normalize('NFKC',text).lower()
    text=re.sub('[\u200b-\u200d\ufeff]','',text)
    tokens=[]; word=''
    for c in text:
        if unicodedata.category(c)[0] in 'LMN' or c=='_': word+=c
        else:
            if len(word)>=2:tokens.append(word)
            word=''
    if len(word)>=2:tokens.append(word)
    return set(tokens+[' '.join(tokens[i:i+2]) for i in range(len(tokens)-1)])

def model_scores(model, texts):
    weights=dict(zip(model['vocabulary'],model['weights']))
    extractor=features if model.get('feature_version')==FEATURE_VERSION else old_features
    return np.asarray([float(sigmoid(model['intercept']+sum(weights.get(t,0) for t in extractor(text)))) if text.strip() else 0.0 for text in texts])

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--promote',action='store_true')
    parser.add_argument('--iterations',type=int,default=550);args=parser.parse_args()
    rows=[json.loads(l) for l in (CACHE/'message-policy.jsonl').read_text().splitlines()]
    previous={r['index']:r for r in map(json.loads,(CACHE/'message-policy-baseline.jsonl').read_text().splitlines())} if (CACHE/'message-policy-baseline.jsonl').exists() else {}
    for row in rows:
        if set(row['terms']) != features(row['classifierInput']):
            raise ValueError(f"Python/Kotlin feature mismatch at local row {row['index']}")
    splits={name:np.asarray([r['split']==name for r in rows]) for name in ['train','validation','evaluation']}
    groups={name:{r['group'] for r in rows if r['split']==name} for name in splits}
    if any(groups[a]&groups[b] for a,b in [('train','validation'),('train','evaluation'),('validation','evaluation')]):
        raise ValueError('Campaign leakage')
    counts=Counter(t for r in rows if r['split']=='train' for t in r['terms'])
    vocab=sorted(t for t,n in counts.most_common(16000) if n>=2)
    index={term:i for i,term in enumerate(vocab)}
    row_ids=[];term_ids=[]
    for i,row in enumerate(rows):
        for term in row['terms']:
            if term in index:
                row_ids.append(i);term_ids.append(index[term])
    rid=np.asarray(row_ids,dtype=np.int32);tid=np.asarray(term_ids,dtype=np.int32)
    y=np.asarray([r['label'] for r in rows],dtype=np.float64)
    train=splits['train']; validation=splits['validation']
    if min(int(y[train].sum()),int((1-y[train]).sum()),int(y[validation].sum()),int((1-y[validation]).sum()))<10:
        raise ValueError('Insufficient validation support')
    sample=np.where(y==1,.5/max(1,int(y[train].sum())),.5/max(1,int((1-y[train]).sum())))*train
    w=np.zeros(len(vocab));b=-1.0;mw=np.zeros_like(w);vw=np.zeros_like(w);mb=vb=0.0
    best=None;best_loss=float('inf');epochs=[]
    for step in range(1,args.iterations+1):
        score=np.bincount(rid,weights=w[tid],minlength=len(rows))+b
        probability=sigmoid(score); delta=(probability-y)*sample
        grad=np.bincount(tid,weights=delta[rid],minlength=len(vocab))+.001*w
        gb=delta.sum();mw=.9*mw+.1*grad;vw=.999*vw+.001*grad*grad
        mb=.9*mb+.1*gb;vb=.999*vb+.001*gb*gb
        w-=.035*(mw/(1-.9**step))/(np.sqrt(vw/(1-.999**step))+1e-8)
        b-=.035*(mb/(1-.9**step))/(math.sqrt(vb/(1-.999**step))+1e-8)
        if step%10==0:
            val=sigmoid(np.bincount(rid,weights=w[tid],minlength=len(rows))+b)[validation]
            loss=float(-np.mean(y[validation]*np.log(np.clip(val,1e-7,1))+(1-y[validation])*np.log(np.clip(1-val,1e-7,1))))
            epochs.append(dict(iteration=step,validation_loss=loss))
            if loss<best_loss:best_loss=loss;best=(w.copy(),float(b),step)
    w,b,checkpoint=best
    scores=sigmoid(np.bincount(rid,weights=w[tid],minlength=len(rows))+b)
    scores[np.asarray([not r['classifierInput'].strip() for r in rows])]=0.0
    eligible=np.asarray([r['eligible'] for r in rows],dtype=bool)
    observed=np.asarray([not r['synthetic'] for r in rows],dtype=bool)
    validation_observed=validation&observed
    # Threshold choice never reads evaluation labels.
    viable=[]
    for threshold in np.linspace(.10,.999,900):
        m=metrics(y[validation_observed],scores[validation_observed]>=threshold)
        all_m=metrics(y[validation],scores[validation]>=threshold)
        if m['false_positive_rate']<=.01 and all_m['false_positive_rate']<=.01:
            viable.append((float(threshold),m,all_m))
    if not viable:raise ValueError('No threshold satisfies validation false-warning budget')
    threshold,_,_=max(viable,key=lambda t:(t[1]['recall'],-t[1]['false_positive_rate'],t[0]))
    candidate=dict(version='fraud-text-research-v3',feature_version=FEATURE_VERSION,threshold=threshold,
        training_scope='Historical English smishing/ham plus authored English/Hindi/Gujarati augmentation; limited research coverage',
        vocabulary=vocab,weights=w.round(10).tolist(),intercept=round(b,10))
    path=CACHE/'candidate-text-model.json';path.write_text(json.dumps(candidate,ensure_ascii=False,indent=2)+'\n')
    asset=ROOT/'android-app/app/src/main/assets/text-model.json'
    backup=CACHE/'baseline-text-model.json'
    if not backup.exists():shutil.copy2(asset,backup)
    baseline=json.loads(backup.read_text())
    baseline_scores=model_scores(baseline,[previous.get(r['index'],r)['classifierInputRaw'] for r in rows])
    baseline_eligible=np.asarray([previous.get(r['index'],r)['eligible'] for r in rows],dtype=bool)
    baseline_threshold=baseline.get('threshold',.6)
    report=dict(version=candidate['version'],trained_at=datetime.now(timezone.utc).isoformat(),
        source_manifest_sha256=hashlib.sha256((ROOT/'models/fraud-data-manifest.json').read_bytes()).hexdigest(),
        dataset_sha256=hashlib.sha256((CACHE/'messages.jsonl').read_bytes()).hexdigest(),
        feature_parity_rows=len(rows),vocabulary_size=len(vocab),checkpoint_iteration=checkpoint,
        threshold=threshold,threshold_selection='Validation only; maximize observed smishing recall with <=1% model-only false warnings on observed and all validation controls',
        candidate_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        baseline_sha256=hashlib.sha256(backup.read_bytes()).hexdigest(),splits={},epochs=epochs)
    for name,mask in splits.items():
        report['splits'][name]=dict(rows=int(mask.sum()),groups=len(groups[name]))
    def slice_report(mask):
        return dict(baseline_model=metrics(y[mask],baseline_scores[mask]>=baseline_threshold),
            candidate_model=metrics(y[mask],scores[mask]>=threshold),
            baseline_model_with_eligibility=metrics(y[mask],baseline_eligible[mask]&(baseline_scores[mask]>=baseline_threshold)),
            candidate_model_with_eligibility=metrics(y[mask],eligible[mask]&(scores[mask]>=threshold)))
    report['validation_observed']=slice_report(validation_observed)
    evaluation=splits['evaluation']
    report['evaluation_observed']=slice_report(evaluation&observed)
    report['evaluation_by_language']={language:slice_report(evaluation&np.asarray([r['language']==language for r in rows])) for language in ['en','hi','gu']}
    report['authored_category_diagnostics']={category:slice_report(np.asarray([r['category']==category for r in rows])) for category in sorted({r['category'] for r in rows if r['source']=='safex-authored-2026'})}
    report['authored_category_diagnostics_scope']='Includes training examples; coverage diagnostics only, never accuracy evidence'
    new=report['evaluation_observed']['candidate_model'];old=report['evaluation_observed']['baseline_model']
    paired_mask=evaluation&observed&(y==1)
    gain=(scores[paired_mask]>=threshold).astype(float)-(baseline_scores[paired_mask]>=baseline_threshold).astype(float)
    rng=np.random.default_rng(20261009)
    interval=np.quantile([rng.choice(gain,len(gain),replace=True).mean() for _ in range(2000)],[.025,.975])
    report['paired_observed_recall_gain_interval']=dict(method='Row bootstrap, 2000 fixed-seed resamples; does not account for all correlated campaigns',lower=float(interval[0]),upper=float(interval[1]))
    accepted=(new['false_positive_rate']<=.01 and new['recall']>old['recall'] and interval[0]>0 and new['tp']+new['fn']>=30)
    report['promotion_gate_passed']=bool(accepted)
    report['promoted']=bool(args.promote and accepted)
    report['limitations']=['Named historical English corpus, not all current fraud or production accuracy.',
        'Observed evaluation was not used for fitting, checkpoint or threshold selection; source and transport bias remain.',
        'Hindi/Gujarati samples are authored. No independent native-language recall estimate is available.',
        'Eligibility figures cover the text-model branch only; full rules/link/payment decisions require Android pipeline checks.',
        'Category diagnostics include training cases. Neither model score nor risk index is calibrated fraud probability.']
    if report['promoted']:shutil.copy2(path,asset)
    (ROOT/'models/fraud-text-evaluation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:report[k] for k in ['splits','threshold','evaluation_observed','paired_observed_recall_gain_interval','promotion_gate_passed','promoted','candidate_sha256']},indent=2))

if __name__=='__main__':main()
