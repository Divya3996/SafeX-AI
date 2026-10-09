#!/usr/bin/env python3
"""Summarize actual installed-app outputs, with coverage separate from threat decisions."""
import argparse, json, math
from collections import defaultdict
from pathlib import Path

def interval(success,total):
    if not total:return None
    z=1.96;p=success/total;d=1+z*z/total
    mid=(p+z*z/(2*total))/d
    radius=z*math.sqrt(p*(1-p)/total+z*z/(4*total*total))/d
    return [max(0,mid-radius),min(1,mid+radius)]
def metrics(rows):
    evaluable=[r for r in rows if r['expected']!='UNSUPPORTED']
    tp=fp=fn=tn=0
    for r in evaluable:
        positive=r['decision']!='ALLOW'
        if r['label']==1:
            if positive:tp+=1
            else:fn+=1
        elif positive:fp+=1
        else:tn+=1
    durations=sorted(r['duration_ms'] for r in rows)
    def q(p):return durations[max(0,math.ceil(len(durations)*p)-1)] if durations else None
    return dict(cases=len(rows),evaluable=len(evaluable),tp=tp,fp=fp,fn=fn,tn=tn,
        precision=tp/(tp+fp) if tp+fp else None,recall=tp/(tp+fn) if tp+fn else None,
        false_warning_rate=fp/(fp+tn) if fp+tn else None,recall_95pct_interval=interval(tp,tp+fn),
        false_warning_95pct_interval=interval(fp,fp+tn),
        limited_assessments=sum(r.get('coverage') in ('LIMITED','UNSUPPORTED') for r in rows),
        analysis_duration_ms=dict(p50=q(.5),p95=q(.95),maximum=max(durations) if durations else None),
        contract_mismatches=[r['id'] for r in rows if (r['expected']=='WARN' and r['decision']=='ALLOW') or
            (r['expected']=='ALLOW' and r['decision']!='ALLOW') or (r['expected']=='UNSUPPORTED' and r.get('coverage')!='UNSUPPORTED')])
def summarize(data):
    groups=defaultdict(list)
    for row in data['results']:groups[row['language']].append(row)
    categories=defaultdict(list)
    for row in data['results']:categories[row['category']].append(row)
    return dict(kind=data['kind'],device=data['device'],models_retrained=False,
        limitations=['Authored cases were used during development; results are not independent real-world accuracy estimates.',
            'Wilson intervals are descriptive calculations on grouped authored fixtures, not population accuracy confidence.', 'Timings exclude Android consent and user editing.', 'OCR accuracy and physical Samsung S24 behavior require separate validation.'],
        overall=metrics(data['results']),by_language={k:metrics(v) for k,v in groups.items()},
        by_category={k:metrics(v) for k,v in categories.items()})
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('input',type=Path);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
    args.output.write_text(json.dumps(summarize(json.loads(args.input.read_text())),ensure_ascii=False,indent=2)+'\n')
    print(args.output.read_text())
