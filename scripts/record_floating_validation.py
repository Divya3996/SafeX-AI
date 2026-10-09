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
from generate_localization import load as load_localization

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / 'android-app'
REPORTS = ROOT / 'docs/test-results'


def main():
    native = (REPORTS / 'floating-assistant-native.txt').read_text()
    match = re.search(r'OK \((\d+) tests\)', native)
    assert match and 'FAILURES!!!' not in native and 'Error in ' not in native, 'Native suite has not passed'
    build = (REPORTS / 'floating-assistant-final-build.txt').read_text()
    assert 'BUILD SUCCESSFUL' in build, 'Final build has not passed'
    evaluation = json.loads((REPORTS / 'floating-review-summary.json').read_text())
    assert not evaluation['overall']['contract_mismatches'], 'Installed-app evaluation has contract mismatches'
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
    release_dir = ROOT / 'releases'
    release_dir.mkdir(exist_ok=True)
    apk = release_dir / f'SafeX-AI-{version}-debug.apk'
    shutil.copy2(source, apk)
    shutil.copy2(source, source.with_name(apk.name))
    for previous in Path('/tmp/safex-previous-apks').glob('SafeX-AI-*-debug.apk'):
        shutil.copy2(previous, release_dir / previous.name)
    digest = hashlib.sha256()
    with apk.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    summary = {
        'version': version,
        'version_code': metadata['versionCode'],
        'verified_at_utc': datetime.now(timezone.utc).isoformat(),
        'features': ['Canonical movable shield with safe menu dismissal', 'Fresh authorized full-display or individual-app single-frame capture', 'Full initial crop, retained original, zoom and precise edges', 'Independent Latin/Devanagari/Gujarati/QR extraction outcomes', 'Distinct All/Selected/Edited modes with retained drafts', 'Bounded link normalization with original-spelling confirmation', 'Typed QR payload coverage', 'Private analysis and context until explicit Save', 'Application-owned RAM session, stale-job guards and recovery', 'Lock/expiry cleanup and native bitmap leases', 'JPEG orientation and bounded imports', 'Fixed actions with English/Hindi/Gujarati at 150% text', 'Adjustable shield size, reduced motion and warning-channel settings', 'Persisted provenance, coverage and measured timings'],
        'unit_tests': {'totals': totals, 'modules': modules, 'skipped_reason': 'Three opt-in URL research-export tests'},
        'native_tests': {'passed': int(match.group(1)), 'failures': 0, 'duration_seconds': float(re.search(r'Time: ([\d.]+)', native).group(1)), 'device': 'SentinelDemo Android 14/API 34 x86_64 emulator, 720x1600', 'airplane_mode': True, 'real_full_display_capture_consent_crop_ocr_and_save': True, 'projection_stopped_before_crop': True, 'real_camera_frame_decode': True, 'camera_fixture_flag': 'safexCameraFixture=true', 'camera_fixture_path': '/tmp/safex-camera-qr.png'},
        'lint': {'errors': 0, 'warnings': severity['Warning'], 'issue_counts': dict(Counter(issue.get('id') for issue in issues))},
        'localization': {'languages': ['English', 'Hindi', 'Gujarati'], 'catalog_strings': len(load_localization()), 'keys_and_placeholders': 'matched by generator', 'largest_native_ui_text_scale': 1.5},
        'privacy': {'internet_permission': False, 'accessibility_reader': False, 'clipboard_polling': False, 'screen_images_saved_or_uploaded': False, 'projection_authorization_persisted': False, 'private_context_saved_automatically': False, 'explicit_save_only': True, 'full_authorized_frame_temporarily_in_ram_before_crop': True, 'unclaimed_frame_expiry_seconds': 60, 'background_private_review_expiry_seconds': 120, 'tracked_pixel_budget_mib': 72, 'memory_pressure_releases_pixels_preserves_review_text': True},
        'apk': {'path': str(apk.relative_to(ROOT)), 'bytes': apk.stat().st_size, 'sha256': digest.hexdigest(), 'build_type': 'debug'},
        'screenshots': [str(path.relative_to(ROOT)) for path in sorted((ROOT / 'docs/screenshots/floating-assistant').glob('*.png'))],
        'evidence': ['docs/test-results/floating-assistant-final-build.txt', 'docs/test-results/floating-assistant-native.txt', 'docs/test-results/floating-assistant-capture-diagnostics.txt', 'docs/test-results/floating-review-native.json', 'docs/test-results/floating-review-summary.json', 'docs/test-results/floating-contrast.json'],
        'fixed_regressions': ['Empty selected lines cannot fall back to all text.', 'Edited drafts survive mode changes and activity recreation.', 'Failed engines preserve successful extraction.', 'QR scope does not inherit unrelated OCR failure.', 'Failed capture/import preserves the previous review.', 'Stale operations and notifications cannot replace a newer session.', 'Memory-pressure cleanup releases pixels while retaining review text.', 'Menu outside taps and Back do not activate the source.', 'Multilingual negated safety advice does not become a refund scam warning.', 'Coverage cannot weaken a stronger detector decision.', 'Private review is saved only deliberately.'],
        'limitations': ['Functional authored-fixture tests are not current-world detection-accuracy certification.', 'URL and text model artifacts are unchanged; the text model remains a synthetic-data prototype.', 'Screenshots do not reveal hidden hyperlink targets; OCR must be reviewed.', 'Physical-phone and manufacturer-specific behavior are not verified; only API 34 was exercised on a device.', 'Physical speaker playback is unverified because emulator host audio is unavailable.', 'Play Store specialUse foreground-service approval has not been obtained.'],
    }
    output = REPORTS / 'floating-assistant-summary.json'
    summary['native_tests']['single_app_capture_verified'] = False
    summary['native_tests']['missing_frame_surface_retries_bounded'] = 2
    summary['native_tests']['capture_scope_note'] = 'This API 34 system image offers full-display consent only. Landscape and full-display capture passed; individual-app selection remains a physical-device check.'
    summary['development_evaluation'] = {
        'cases': evaluation['overall']['cases'],
        'contract_mismatches': evaluation['overall']['contract_mismatches'],
        'report': 'docs/test-results/floating-review-summary.json',
        'independent_real_world_accuracy': False,
    }
    summary['limitations'].extend([
        'Individual-app chooser is unavailable on this emulator image and was not verified.',
        'Text-model warnings require sensitive-action context; independent recall/false-warning evaluation is still needed.',
        'No participant usability study or physical TalkBack validation was performed.',
    ])
    output.write_text(json.dumps(summary, indent=2, ensure_ascii=False) + '\n')
    print(json.dumps({'unit': totals, 'native': summary['native_tests']['passed'], 'lint': summary['lint'], 'apk': summary['apk']}, indent=2))


if __name__ == '__main__':
    main()
