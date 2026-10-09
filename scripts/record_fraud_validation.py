#!/usr/bin/env python3
"""Write a verification record only after final build/native/APK checks pass."""
import hashlib
import json
import re
import xml.etree.ElementTree as ET
import zipfile
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from generate_localization import load as load_localization

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / 'docs/test-results'
ANDROID = ROOT / 'android-app'


def read(name):
    return json.loads((REPORTS / name).read_text())


def main():
    native = (REPORTS / 'fraud-native-final.txt').read_text()
    passed = re.search(r'OK \((\d+) tests\)', native)
    assert passed and int(passed.group(1)) == 55 and 'FAILURES!!!' not in native and 'Error in ' not in native
    build = (REPORTS / 'fraud-final-build.txt').read_text()
    assert 'BUILD SUCCESSFUL' in build
    contracts = read('fraud-native-contracts.json')
    assert len(contracts['cases']) == 120 and not contracts['mismatches']
    floating = read('fraud-floating-summary.json')
    assert floating['overall']['cases'] == 55 and not floating['overall']['contract_mismatches']
    url_parity = read('fraud-url-native-parity.json')
    assert url_parity['cases'] == 256 and url_parity['maximum_score_difference'] <= .00002
    baseline, current = read('fraud-native-baseline-evaluation.json'), read('fraud-native-evaluation.json')
    assert baseline['input_sha256'] == current['input_sha256'] and baseline['app_version'] == '1.6.0' and current['app_version'] == '1.7.0'
    for result in (baseline, current):
        assert len(result['cases']) == 847 and result['errors'] == 0
        assert result['tp'] + result['fn'] == 78 and result['fp'] + result['tn'] == 769
    assert {r['id'] for r in baseline['cases']} == {r['id'] for r in current['cases']}
    modules, totals = {}, dict(tests=0, failures=0, errors=0, skipped=0)
    for module in ('app', 'core', 'agents', 'ui'):
        counts = {key: 0 for key in totals}
        files = list((ANDROID / module / 'build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
        assert files, module
        for path in files:
            suite = ET.parse(path).getroot()
            for key in counts:
                counts[key] += int(suite.get(key, '0'))
        assert counts['errors'] == counts['failures'] == 0
        modules[module] = counts
        for key in totals:
            totals[key] += counts[key]
    lint = ET.parse(ANDROID / 'app/build/reports/lint-results-debug.xml').getroot().findall('issue')
    severity = Counter(issue.get('severity') for issue in lint)
    assert severity['Error'] == severity['Fatal'] == 0
    apk = ROOT / 'releases/SafeX-AI-1.7.0-debug.apk'
    model_hashes = {}
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        assert 'lib/arm64-v8a/libtensorflowlite_jni.so' in names
        assert any(name.startswith('lib/arm64-v8a/') and 'tesseract' in name for name in names)
        assert not any('observed-evaluation' in name or 'messages.jsonl' in name or 'fraud-app-contracts' in name for name in names)
        for name, card_name in [('text-model.json', 'text-model-card.json'), ('model.tflite', 'url-model-card.json')]:
            blob = archive.read('assets/' + name)
            digest = hashlib.sha256(blob).hexdigest()
            assert blob == (ANDROID / 'app/src/main/assets' / name).read_bytes()
            card = json.loads(archive.read('assets/' + card_name))
            assert digest == card['sha256']
            model_hashes[name] = digest
        assert json.loads(archive.read('assets/text-warning-policy.json'))['model_sha256'] == model_hashes['text-model.json']
        assert b'MIT License' in archive.read('assets/security-model-attribution.txt')
    def counts(result):
        return {key: result[key] for key in ('app_version', 'tp', 'fp', 'fn', 'tn', 'errors', 'recall', 'false_positive_rate', 'duration_ms_p50', 'duration_ms_p95')}
    summary = dict(version='1.7.0', version_code=8, verified_at_utc=datetime.now(timezone.utc).isoformat(),
                   research_as_of='2026-10-09', apk=dict(path=str(apk.relative_to(ROOT)), bytes=apk.stat().st_size,
                   sha256=hashlib.sha256(apk.read_bytes()).hexdigest(), build_type='debug'),
                   unit_tests=dict(totals=totals, active=totals['tests']-totals['skipped'], modules=modules),
                   native_tests=dict(passed=55, failures=0, duration_seconds=float(re.search(r'Time: ([\d.]+)', native).group(1)),
                   device='Android 14/API 34 Google APIs x86_64 emulator; 720x1600; airplane mode',
                   camera_fixture='imagefile:/tmp/safex-camera-qr.png; safexCameraFixture=true'),
                   lint=dict(errors=0, warnings=severity['Warning'], issues=dict(Counter(issue.get('id') for issue in lint))),
                   localization=dict(languages=['English', 'Hindi', 'Gujarati'], strings_per_language=len(load_localization())),
                   models=dict(sha256=model_hashes, warning_policy='models/fraud-text-warning-policy.json'),
                   native_model_parity=dict(text_authored_cases=36, url_historical_cases=256,
                   maximum_url_score_difference=url_parity['maximum_score_difference']),
                   observed_pipeline=dict(scope=current['scope'], rows=847, positives=78, legitimate_controls=769,
                   input_sha256=current['input_sha256'], baseline=counts(baseline), current=counts(current)),
                   authored_contracts=dict(fraud=120, floating=55, mismatches=0, scope='Authored development fixtures, including training examples; functional contracts only'),
                   privacy=dict(internet_permission=False, raw_research_data_in_apk=False, on_device_inference=True),
                   evidence=['docs/test-results/fraud-final-build.txt', 'docs/test-results/fraud-native-final.txt',
                   'docs/test-results/fraud-test-sync-build.txt', 'docs/test-results/fraud-context-retest.txt',
                   'models/fraud-url-weight-integrity.json'],
                   test_environment_recovery='Earlier cold boots had a System UI keyguard-service ANR; the final complete warm run passed after System UI recovery and bounded test rendering synchronization.',
                   limitations=['Historical English development evaluation was inspected during implementation; this is not independent contemporary accuracy.',
                   'Reported URL-feed labels were not independently adjudicated; fresh representative benign URL controls are missing.',
                   'Hindi/Gujarati detection contracts use authored examples; observed native-language evaluation is missing.',
                   'No physical Galaxy S24 or A36 was connected; emulator timings exclude capture/OCR and physical-phone performance.',
                   'Voice/deepfake authenticity, hidden redirects, remote page behavior and malware binary classification are outside this training update.'])
    output = REPORTS / 'fraud-training-summary.json'
    output.write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps(dict(output=str(output), unit_active=summary['unit_tests']['active'], native_passed=55, apk_sha256=summary['apk']['sha256'], pipeline=summary['observed_pipeline']), indent=2))


if __name__ == '__main__':
    main()
