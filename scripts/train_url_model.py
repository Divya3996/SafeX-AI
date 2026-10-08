#!/usr/bin/env python3
"""Train a small offline URL model from domain-disjoint UCI data; export native TFLite.

Dependencies: NumPy, flatbuffers and the TFLite schema package. No TensorFlow runtime
is needed for training/export. Inputs are production-Kotlin feature vectors, not
the dataset's HTML, title, reputation or aggregate probability features.
"""
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np
import flatbuffers
import tflite

ROOT = Path(__file__).resolve().parents[1]
FEATURE_NAMES = ['URLLength','DomainLength','IsDomainIP','NoOfSubDomain','IsHTTPS',
                 'HasSuspiciousWords','SpecialCharRatio','DigitRatio','HasAtSymbol',
                 'SuspiciousTLD','BrandImpersonationScore','HyphenCount','PathQueryLength',
                 'KnownBrandDomain','DomainVowelRatio']
def sigmoid(x): return 1/(1+np.exp(-np.clip(x,-60,60)))
def metrics(y, predicted):
    tp=int(np.sum((y==1)&predicted)); fp=int(np.sum((y==0)&predicted))
    fn=int(np.sum((y==1)&~predicted)); tn=int(np.sum((y==0)&~predicted))
    return dict(tp=tp,fp=fp,fn=fn,tn=tn,recall=tp/max(1,tp+fn),precision=tp/max(1,tp+fp),
                false_positive_rate=fp/max(1,fp+tn),accuracy=(tp+tn)/max(1,len(y)))
def predict(x, parameters):
    a=x
    for i in range(0,len(parameters),2):
        a=a@parameters[i]+parameters[i+1]
        a=sigmoid(a) if i==len(parameters)-2 else np.maximum(a,0)
    return a[:,0]
def offsets(builder, values):
    builder.StartVector(4,len(values),4)
    for value in reversed(values):builder.PrependUOffsetTRelative(value)
    return builder.EndVector()
def ints(builder, values):return builder.CreateNumpyVector(np.asarray(values,dtype=np.int32))

def export_tflite(parameters, path):
    b=flatbuffers.Builder(4096)
    buffers=[]
    tflite.BufferStart(b);buffers.append(tflite.BufferEnd(b))
    for i,p in enumerate(parameters):
        data=b.CreateByteVector(np.asarray(p.T if i%2==0 else p,dtype='<f4').tobytes())
        tflite.BufferStart(b);tflite.BufferAddData(b,data);buffers.append(tflite.BufferEnd(b))
    tensors=[]
    def tensor(name,shape,buffer=0):
        n=b.CreateString(name);s=ints(b,shape)
        tflite.TensorStart(b);tflite.TensorAddName(b,n);tflite.TensorAddShape(b,s)
        tflite.TensorAddType(b,tflite.TensorType.FLOAT32);tflite.TensorAddBuffer(b,buffer)
        tensors.append(tflite.TensorEnd(b));return len(tensors)-1
    input_index=tensor('url_features',[1,15]); ops=[];previous=input_index
    for layer in range(len(parameters)//2):
        w=parameters[layer*2]; bias=parameters[layer*2+1]
        wi=tensor(f'layer_{layer}_weights',[w.shape[1],w.shape[0]],layer*2+1)
        bi=tensor(f'layer_{layer}_bias',[len(bias)],layer*2+2)
        output=tensor(f'layer_{layer}_output',[1,w.shape[1]])
        ins=ints(b,[previous,wi,bi]);outs=ints(b,[output])
        tflite.FullyConnectedOptionsStart(b)
        activation=tflite.ActivationFunctionType.NONE if layer==len(parameters)//2-1 else tflite.ActivationFunctionType.RELU
        tflite.FullyConnectedOptionsAddFusedActivationFunction(b,activation)
        options=tflite.FullyConnectedOptionsEnd(b)
        tflite.OperatorStart(b);tflite.OperatorAddOpcodeIndex(b,0)
        tflite.OperatorAddInputs(b,ins);tflite.OperatorAddOutputs(b,outs)
        tflite.OperatorAddBuiltinOptionsType(b,tflite.BuiltinOptions.FullyConnectedOptions)
        tflite.OperatorAddBuiltinOptions(b,options);ops.append(tflite.OperatorEnd(b));previous=output
    output=tensor('phishing_pattern_score',[1,1]);ins=ints(b,[previous]);outs=ints(b,[output])
    tflite.OperatorStart(b);tflite.OperatorAddOpcodeIndex(b,1);tflite.OperatorAddInputs(b,ins)
    tflite.OperatorAddOutputs(b,outs);ops.append(tflite.OperatorEnd(b))
    tv=offsets(b,tensors);ov=offsets(b,ops);inputs=ints(b,[input_index]);outputs=ints(b,[output]);name=b.CreateString('SafeX URL v1')
    tflite.SubGraphStart(b);tflite.SubGraphAddTensors(b,tv);tflite.SubGraphAddOperators(b,ov)
    tflite.SubGraphAddInputs(b,inputs);tflite.SubGraphAddOutputs(b,outputs);tflite.SubGraphAddName(b,name)
    graph=tflite.SubGraphEnd(b)
    codes=[]
    for code in [tflite.BuiltinOperator.FULLY_CONNECTED,tflite.BuiltinOperator.LOGISTIC]:
        tflite.OperatorCodeStart(b);tflite.OperatorCodeAddBuiltinCode(b,code);tflite.OperatorCodeAddDeprecatedBuiltinCode(b,code)
        tflite.OperatorCodeAddVersion(b,1);codes.append(tflite.OperatorCodeEnd(b))
    cv=offsets(b,codes);gv=offsets(b,[graph]);bv=offsets(b,buffers)
    description=b.CreateString('SafeX AI URL model; PhiUSIIL CC BY 4.0; domain-disjoint evaluation; URL-only')
    tflite.ModelStart(b);tflite.ModelAddVersion(b,3);tflite.ModelAddOperatorCodes(b,cv)
    tflite.ModelAddSubgraphs(b,gv);tflite.ModelAddBuffers(b,bv);tflite.ModelAddDescription(b,description)
    model=tflite.ModelEnd(b);b.Finish(model,file_identifier=b'TFL3');path.write_bytes(b.Output())

def legacy_predict(x):
    model=tflite.Model.GetRootAsModel((ROOT/'models/cache/legacy-model.tflite').read_bytes(),0)
    graph=model.Subgraphs(0); tensors={int(graph.Inputs(0)):x}
    def value(index):
        if index in tensors:return tensors[index]
        tensor=graph.Tensors(index)
        return np.frombuffer(model.Buffers(tensor.Buffer()).DataAsNumpy().tobytes(),dtype='<f4').reshape(tensor.ShapeAsNumpy())
    for i in range(graph.OperatorsLength()):
        op=graph.Operators(i);code=model.OperatorCodes(op.OpcodeIndex()).BuiltinCode()
        if code==tflite.BuiltinOperator.FULLY_CONNECTED:
            bias = 0 if int(op.Inputs(2)) < 0 else value(int(op.Inputs(2)))
            out=value(int(op.Inputs(0)))@value(int(op.Inputs(1))).T+bias
            opt=op.BuiltinOptions();fc=tflite.FullyConnectedOptions();fc.Init(opt.Bytes,opt.Pos)
            if fc.FusedActivationFunction()==tflite.ActivationFunctionType.RELU:out=np.maximum(out,0)
        elif code==tflite.BuiltinOperator.LOGISTIC:out=sigmoid(value(int(op.Inputs(0))))
        else:raise ValueError(f'Unsupported legacy operator {code}')
        tensors[int(op.Outputs(0))]=out
    return tensors[int(graph.Outputs(0))][:,0]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--features',type=Path,default=ROOT/'models/cache/features-updated.jsonl')
    parser.add_argument('--augmentation-features',type=Path,default=ROOT/'models/cache/features-augmented.jsonl')
    parser.add_argument('--epochs',type=int,default=18);args=parser.parse_args()
    rows=[];excluded=0
    with args.features.open() as f:
        for line in f:
            r=json.loads(line)
            if r['valid']:rows.append(r)
            else:excluded+=1
    original_count=len(rows)
    with args.augmentation_features.open() as f:
        for line in f:
            r=json.loads(line)
            if r['valid']:rows.append(r)
            else:excluded+=1
    original=np.arange(len(rows))<original_count
    x=np.asarray([r['features'] for r in rows],dtype=np.float32);y=np.asarray([r['label'] for r in rows],dtype=np.float32)
    splits={name:np.asarray([r['split']==name for r in rows]) for name in ['train','validation','test']}
    groups={name:{r['group'] for r in rows if r['split']==name} for name in splits}
    assert not(groups['train']&groups['test'] or groups['train']&groups['validation'] or groups['test']&groups['validation'])
    means=x[splits['train']].mean(axis=0);scales=x[splits['train']].std(axis=0);scales=np.maximum(scales,1e-6)
    scales[np.std(x[splits['train']],axis=0)<1e-6]=1
    z=np.clip((x-means)/scales,-8,8).astype(np.float32)
    train=z[splits['train']];labels=y[splits['train']];rng=np.random.default_rng(20261008)
    sizes=[15,24,12,1];parameters=[]
    for a,b in zip(sizes,sizes[1:]):parameters += [(rng.normal(size=(a,b))*np.sqrt(2/a)).astype(np.float32),np.zeros(b,dtype=np.float32)]
    moments=[np.zeros_like(p) for p in parameters];variances=[np.zeros_like(p) for p in parameters];step=0
    best=None;best_loss=float('inf');epochs=[]
    for epoch in range(args.epochs):
        order=rng.permutation(len(train))
        for start in range(0,len(order),512):
            idx=order[start:start+512];a=[train[idx]];pre=[]
            for layer in range(3):
                v=a[-1]@parameters[layer*2]+parameters[layer*2+1];pre.append(v)
                a.append(sigmoid(v) if layer==2 else np.maximum(v,0))
            delta=(a[-1]-labels[idx,None])/len(idx);grads=[]
            for layer in range(2,-1,-1):
                grads[0:0]=[a[layer].T@delta+1e-4*parameters[layer*2],delta.sum(axis=0)]
                if layer:delta=(delta@parameters[layer*2].T)*(pre[layer-1]>0)
            step+=1
            for i,(p,g) in enumerate(zip(parameters,grads)):
                moments[i]=.9*moments[i]+.1*g;variances[i]=.999*variances[i]+.001*g*g
                p-=.001*(moments[i]/(1-.9**step))/(np.sqrt(variances[i]/(1-.999**step))+1e-8)
        prediction=np.clip(predict(z[splits['validation']],parameters),1e-7,1-1e-7);truth=y[splits['validation']]
        loss=float(-np.mean(truth*np.log(prediction)+(1-truth)*np.log(1-prediction)))
        epochs.append({'epoch':epoch+1,'validation_loss':loss});print(f'Epoch {epoch+1}: validation loss {loss:.5f}',flush=True)
        if loss<best_loss:best_loss=loss;best=[p.copy() for p in parameters]
    parameters=best;validation_score=predict(z[splits['validation']],parameters)
    choices=[(float(t),metrics(y[splits['validation']],validation_score>=t)) for t in np.linspace(.05,.999,951)]
    val_original=original[splits['validation']]
    viable=[(t,m) for t,m in choices if m['false_positive_rate']<=.01 and
        metrics(truth[val_original],validation_score[val_original]>=t)['false_positive_rate']<=.01 and
        metrics(truth[~val_original],validation_score[~val_original]>=t)['false_positive_rate']<=.01]
    if not viable:
        raise RuntimeError("No warning threshold satisfies validation false-positive limits; do not export this model")
    threshold,validation=max(viable,key=lambda v:(v[1]['recall'],-v[1]['false_positive_rate'],v[0]))
    prediction=predict(z,parameters)
    assets=ROOT/'android-app/app/src/main/assets';export_tflite(parameters,assets/'model.tflite')
    scaler=dict(feature_names=FEATURE_NAMES,mean=means.tolist(),scale=scales.tolist(),clip=8.0)
    (assets/'scaler.json').write_text(json.dumps(scaler,indent=2)+'\n')
    model_hash=hashlib.sha256((assets/'model.tflite').read_bytes()).hexdigest()
    card=dict(version='url-phiusiil-augmented-v2',warning_threshold=threshold,kind='URL-only structural classifier; not calibrated scam probability',
              training_source='PhiUSIIL, Prasad and Chandra (2024), UCI dataset 967, CC BY 4.0',sha256=model_hash)
    (assets/'url-model-card.json').write_text(json.dumps(card,indent=2)+'\n')
    np.savez(ROOT/'models/cache/url-weights.npz',**{f'p{i}':p for i,p in enumerate(parameters)})
    report=dict(version=card['version'],seed=20261008,source_url='https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset',
                license='CC BY 4.0',dataset_zip_sha256='0a639fd03aea6308c5b1c10c92aa23c2ce1505447a9137271865cd0badc9a59a',
                input_features='15 production Kotlin URL features only; no HTML/title/reputation/dataset probability features',
                excluded_unparseable_or_oversized=excluded,split_method='SHA-256 of public-suffix-aware registrable domain; 70/15/15 buckets',
                splits={name:dict(rows=int(mask.sum()),phishing=int(y[mask].sum()),benign=int(mask.sum()-y[mask].sum()),groups=len(groups[name])) for name,mask in splits.items()},
                threshold=threshold,threshold_selection='Validation only: maximize recall with at most 1% model-only false positives in original and augmented validation sets separately',
                augmentation='Mechanical www/root alternatives and benign path/query variants; synthetic labels, not live site observations',
                original_rows=original_count,augmentation_rows=len(rows)-original_count,
                validation=validation,test=metrics(y[splits['test'] & original],prediction[splits['test'] & original]>=threshold),
                augmented_test=metrics(y[splits['test'] & ~original],prediction[splits['test'] & ~original]>=threshold),epochs=epochs,model_sha256=model_hash,
                limitations=['Historical single-source dataset; not current-world accuracy.','Dataset transport/source biases can inflate results.','Historical test partition inspected during candidate development; not an untouched final evaluation.',
                'URL text cannot reveal compromised pages, opaque redirects, consent abuse or changing website contents.',
                'Authored pattern corpus is a separate regression suite and is not independent evaluation data.'])
    original_rows=rows[:original_count]
    original_x=x[:original_count];original_y=y[:original_count];original_prediction=prediction[:original_count]
    original_test=splits['test'][:original_count]
    rule_scores=np.asarray([r['ruleScore'] for r in original_rows])
    report['new_pipeline_test']=metrics(original_y[original_test],((rule_scores>=30)|(original_prediction>=threshold))[original_test])
    # Before/after comparison is optional; historical baseline caches are not training inputs.
    legacy_files=[ROOT/'models/cache'/name for name in ['legacy-scaler.json','legacy-model.tflite','features-baseline.jsonl']]
    old_scores=None
    if all(p.exists() for p in legacy_files):
        old=json.loads(legacy_files[0].read_text());original_by_index={}
        with legacy_files[2].open() as f:
            for line in f:
                r=json.loads(line);original_by_index[r['index']]=r
        old_x=np.asarray([original_by_index[r['index']]['features'] for r in original_rows],dtype=np.float32)
        old_scores=legacy_predict((old_x-np.asarray(old['mean']))/np.asarray(old['scale']))
        old_rule_scores=np.asarray([original_by_index[r['index']]['ruleScore'] for r in original_rows])
        old_combined=np.maximum(old_rule_scores,.7*old_rule_scores+.3*np.minimum(old_scores*200,100))
        report['legacy_pipeline_test']=metrics(original_y[original_test],old_combined[original_test]>=30)
    (ROOT/'models/url-evaluation.json').write_text(json.dumps(report,indent=2)+'\n')
    # Native inference parity fixtures: held-out rows only, no live page access.
    held=np.flatnonzero(original_test);selected=np.concatenate([held[original_y[held]==label][:128] for label in [0,1]])
    wanted={rows[i]['index']:i for i in selected};fixtures=[]
    with (ROOT/'models/cache/urls.jsonl').open() as f:
        for line in f:
            raw=json.loads(line)
            if raw['index'] in wanted:
                i=wanted[raw['index']];fixtures.append(dict(url=raw['url'],label=int(y[i]),features=x[i].tolist(),
                    predicted=float(original_prediction[i])))
    # Raw historical URLs remain in ignored cache; native fixture copies are transient build inputs.
    (ROOT/'models/cache/native-url-evaluation.json').write_text(json.dumps(dict(threshold=threshold,cases=fixtures)))
    print(json.dumps({k:report[k] for k in ['splits','threshold','validation','test','augmented_test','new_pipeline_test','model_sha256']},indent=2))

if __name__=='__main__':main()
