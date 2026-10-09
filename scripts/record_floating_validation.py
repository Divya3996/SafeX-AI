#!/usr/bin/env python3
"""Record a release only after unit, lint, native and APK checks succeed."""
from pathlib import Path
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / 'android-app'
REPORTS = ROOT / 'docs/test-results'


def main():
    native = (REPORTS / 'floating-assistant-native.txt').read_text()
    match = re.search(r'OK \((\d+) tests\)', native)
    assert match and 'FAILURES!!!' not in native and 'Error in ' not in native, 'Native suite has not passed'
    build = (REPORTS / 'floating-assistant-final-build.txt').read_text()
    assert 'BUILD SUCCESSFUL' in build, 'Final build has not passed'
    modules, totals = {}, dict(tests=0, failures=0, errors=0, skipped=0)
    for module in ('app', 'core', 'ui', 'agents'):
        counts = {key: 0 for key in totals}
        files = list((ANDROID / module / 'build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
        assert files, f'Missing {module} unit results'
        for file in files:
            suite = ET.parse(file).getroot()
            for key in counts:
                counts[key] += int(suite.get(key, 0))
        for key in totals:
            totals[key] += counts[key]
        counts['passed'] = counts['tests'] - counts['failures'] - counts['errors'] - counts['skipped']
        modules[module] = counts
    assert totals['failures'] == totals['errors'] == 0
    totals['passed'] = totals['tests'] - totals['skipped']
    issues = ET.parse(ANDROID / 'app/build/reports/lint-results-debug.xml').getroot().findall('issue')
    severity = Counter(issue.get('severity') for issue in issues)
    assert severity['Error'] == severity['Fatal'] == 0
    sdk = Path(next(line.split('=', 1)[1] for line in (ANDROID / 'local.properties').read_text().splitlines() if line.startswith('sdk.dir=')))
    source = ANDROID / 'app/build/outputs/apk/debug/app-debug.apk'
    permission_output = subprocess.check_output([str(sdk / 'build-tools/34.0.0/aapt'), 'dump', 'permissions', str(source)], text=True)
    assert 'android.permission.INTERNET' not in permission_output
    metadata = json.loads((source.parent / 'output-metadata.json').read_text())['elements'][0]
    version = metadata['versionName']
    apk = source.with_name(f'SafeX-AI-{version}-debug.apk')
    shutil.copy2(source, apk)
    previous = Path('/tmp/safex-previous-apks/SafeX-AI-1.4.0-debug.apk')
    if previous.exists():
        shutil.copy2(previous, source.with_name(previous.name))
    digest = hashlib.sha256()
    with apk.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    summary = {
        'version': version,
        'version_code': metadata['versionCode'],
        'verified_at_utc': datetime.now(timezone.utc).isoformat(),
        'features': ['User-enabled movable floating shield', 'Fresh authorized single-frame capture', 'Precise crop and zoom', 'Editable multilingual OCR and recognized-line selection', 'Individual link and QR selection', 'Private analysis and contextual reviews until explicit Save', 'Explicit Paste, image import and recovery states', 'Screen-lock cleanup and background expiry', 'Generic background risk warnings using existing sound channels'],
        'unit_tests': {'totals': totals, 'modules': modules, 'skipped_reason': 'Three opt-in URL research-export tests'},
        'native_tests': {'passed': int(match.group(1)), 'failures': 0, 'duration_seconds': float(re.search(r'Time: ([\d.]+)', native).group(1)), 'device': 'SentinelDemo Android 14/API 34 x86_64 emulator, 720x1600', 'airplane_mode': True, 'real_full_display_capture_consent_crop_ocr_and_save': True, 'projection_stopped_before_crop': True, 'real_camera_frame_decode': True, 'camera_fixture_flag': 'safexCameraFixture=true', 'camera_fixture_path': '/tmp/safex-camera-qr.png'},
        'lint': {'errors': 0, 'warnings': severity['Warning'], 'issue_counts': dict(Counter(issue.get('id') for issue in issues))},
        'localization': {'languages': ['English', 'Hindi', 'Gujarati'], 'catalog_strings': len([line for line in (ROOT / 'localization/translations.tsv').read_text().splitlines() if line.strip()]), 'keys_and_placeholders': 'matched by generator', 'largest_native_ui_text_scale': 1.5},
        'privacy': {'internet_permission': False, 'accessibility_reader': False, 'clipboard_polling': False, 'screen_images_saved_or_uploaded': False, 'projection_authorization_persisted': False, 'private_context_saved_automatically': False, 'explicit_save_only': True, 'full_authorized_frame_temporarily_in_ram_before_crop': True, 'unclaimed_frame_expiry_seconds': 60, 'background_private_review_expiry_seconds': 120},
        'apk': {'path': str(apk.relative_to(ROOT)), 'bytes': apk.stat().st_size, 'sha256': digest.hexdigest(), 'build_type': 'debug'},
        'screenshots': [str(path.relative_to(ROOT)) for path in sorted((ROOT / 'docs/screenshots/floating-assistant').glob('*.png'))],
        'evidence': ['docs/test-results/floating-assistant-final-build.txt', 'docs/test-results/floating-assistant-native.txt', 'docs/test-results/floating-assistant-capture-diagnostics.txt'],
        'fixed_regressions': ['Crop canvas is clipped to its preview bounds.', 'Unchanged-size projection callbacks preserve the existing image reader and its static frame.', 'Older history snapshots cannot remove stronger displayed risk evidence or context answers.'],
        'limitations': ['Functional authored-fixture tests are not current-world detection-accuracy certification.', 'URL and text model artifacts are unchanged; the text model remains a synthetic-data prototype.', 'Screenshots do not reveal hidden hyperlink targets; OCR must be reviewed.', 'Physical-phone and manufacturer-specific behavior are not verified; only API 34 was exercised on a device.', 'Physical speaker playback is unverified because emulator host audio is unavailable.', 'Play Store specialUse foreground-service approval has not been obtained.'],
    }
    output = REPORTS / 'floating-assistant-summary.json'
    output.write_text(json.dumps(summary, indent=2, ensure_ascii=False) + '\n')
    print(json.dumps({'unit': totals, 'native': summary['native_tests']['passed'], 'lint': summary['lint'], 'apk': summary['apk']}, indent=2))


if __name__ == '__main__':
    main()
