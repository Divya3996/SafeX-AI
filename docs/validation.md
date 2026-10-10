# Verification report

## Current SafeX AI 1.9.0 Scam Story Mode — 10 October 2026

Android passed **593 active unit tests and 73 active offline Android tests**, with **zero lint errors and 108 warnings**. Four of 597 discovered unit checks were optional research-export skips. The native runner reported `OK (74 tests)` in **379.654 seconds**; its event log records one optional transient research-dataset assumption skip. The other 73 checks passed. Testing used Android 14/API 34, airplane mode, Wi-Fi disabled and the authored image-file camera fixture. The tested APK SHA-256 is `e356ce597f26f207fc8475366471d8d82d823375dfda73897ae7dabd799daa38`. [Release record and checksums](test-results/story-release-summary.json) · [Final build](test-results/story-release-final-build.log) · [Native output](test-results/story-native-final.log) · [Runner events](test-results/story-native-final-runner.log).

The 58 new Story Mode unit checks and 15 new installed-app checks cover supported sequences, protective advice and quoted examples, original evidence spans, Unicode duplicate handling, per-item warning/coverage preservation, bounded private routing, edits/removal/reordering, late cancellation, timeout/expiry/lock cleanup, encryption authentication and identity binding, capacity/retention, editable OCR/QR imports, handoff recovery and redacted export. The complete suite also verifies the existing models, actual capture consent/crop/OCR, live camera QR, first use and all 19 highlighted guide targets. All three interface languages work at 150% text. Official dial intents are intercepted; no emergency call is placed.

The 20 authored story contracts matched: 10 suspicious sequences and 10 legitimate comparisons across English/Hindi/Gujarati. The individual checks warned in 9 suspicious cases; the connected engine matched supported patterns in 10. Existing individual warnings occur in 6 legitimate comparisons. One paid-support comparison intentionally receives a connected review finding, and none of the ten legitimate comparisons receives overall high concern. The report preserves those distinctions and the exact fixture IDs. These are authored development contracts, including language variants, not an independent accuracy benchmark. [Installed evaluation and analysis-only timings](test-results/story-evaluation.json) · [Fixture](../android-app/app/src/androidTest/assets/story-corpus.json) · [Usage and jury demonstration](scam-story-mode.md).

Saved cases use AES-256-GCM in the internal no-backup directory with an Android Keystore key and authenticated case identity. The destination's secure-window behavior is checked. Installed UI checks assert translated controls and exact supporting evidence at 150% text. The final screenshot helper returned black protected-window content, so those unusable images are excluded; manual visual review on the demo phones remains.

Both development and unsigned release APKs passed static ELF/ZIP alignment for all **22 packaged 64-bit libraries**. These checks do not establish runtime behavior on a 16 KB device. Internet and calling permissions are absent, and device backup is disabled. [Development alignment](test-results/story-debug-native-alignment.json) · [Release alignment](test-results/story-release-native-alignment.json) · [Packaged manifest](test-results/story-debug-manifest.txt).

Development testing found a cold-launch navigation race, fixed by waiting for an initialized back stack. A summary-tag collision and asynchronous test queries were corrected, retaining strict privacy assertions. Final review also fixed empty/unavailable Paste and unchanged input/source interactions so a pending reviewed handoff retains its original warning. The complete suite passed before that final clipboard correction and again on the packaged final build. Earlier attempts remain labeled development records. [Cold-launch failure](test-results/story-native-development.log) · [Test synchronization record](test-results/story-native-ui-synchronization-development.log) · [Earlier complete pass](test-results/story-native-before-clipboard-fix.log).

Chrome 1.1.0 and the bundled message/URL model assets are unchanged for this feature. Chrome's prior release record contains 431 unit/privacy checks and 43 browser scenarios; those were not rerun here. The development APK is debug-signed and installable; the release bundle is unsigned and needs the owner's upload key. Physical Samsung S24/A36, Android 16, a 16 KB runtime, manual assistive-technology checks, physical speaker playback, independent representative fraud evaluation and store review remain release gates. [APK](../releases/SafeX-AI-1.9.0-debug.apk) · [Unsigned bundle](../releases/SafeX-AI-1.9.0-release-unsigned.aab) · [Signing and rehearsal instructions](onboarding-and-release.md).

## Historical SafeX AI 1.8.0 onboarding release — 10 October 2026

Android passed **535 active unit tests and 58 active offline device tests**, with **zero lint errors and 108 warnings**. Four of 539 discovered unit checks were optional research-export skips. The native runner reported `OK (59 tests)` in **303.75 seconds**; its event log identifies one assumption skip because the optional transient research dataset was absent. The other 58 checks passed. Testing used Android 14/API 34, airplane mode, Wi-Fi disabled and the authored image-file camera fixture. [Release record](test-results/onboarding-release-summary.json) · [Final build](test-results/onboarding-release-build.log) · [Native output](test-results/onboarding-native-final.log) · [Runner events, including the optional skip](test-results/onboarding-native-runner.log).

The four new installed-app experience tests verify first-use persistence, all 18 highlighted controls, Back/Close/replay, English/Hindi/Gujarati at 150% text, urgent help before finishing setup, country selection and confirmed `ACTION_DIAL` actions. Dial intents are intercepted in tests; no emergency call is placed. Existing tests verify the native models, authored fraud controls, private storage, clipboard access, actual capture consent/crop/OCR, live camera QR and recovery. [Gujarati introduction](screenshots/onboarding/introduction-gu-150.png) · [Gujarati tour](screenshots/onboarding/tour-gu-150.png).

Chrome 1.1.0 passed **431 unit/privacy checks, 32 existing browser scenarios and 11 new guidance/help scenarios**. The guide and checklists work offline, and the recorded dependency audit found zero known vulnerabilities. [Browser release record](test-results/chrome-extension-summary.json) · [Guidance checks](test-results/chrome-extension-guidance.json) · [Chrome release details](chrome-extension-release.md).

Both Android development and unsigned release APKs passed static ELF/ZIP alignment checks for all **22 packaged 64-bit native libraries**. These checks do not establish behavior on a 16 KB runtime. The project now targets API 36 with AGP 8.11.1/Gradle 8.13/JDK 17, LiteRT 1.4.0 and CameraX 1.4.2. Internet and calling permissions are absent, and device backup is disabled. [Development alignment report](test-results/onboarding-native-alignment.json) · [Release alignment report](test-results/onboarding-release-native-alignment.json).

The initial cold emulator attempt had six failures while a launcher ANR dialog held focus; Android also denied the clipboard read because the application lacked focus. After stopping the emulator launcher, dismissing keyguard and verifying app focus, the complete suite passed without production-source changes. [Preserved initial output](test-results/onboarding-native-cold-environment-failure.log).

The installable APK is debug-signed; the release bundle is unsigned. Physical Samsung S24/A36, Android 16, a 16 KB runtime, manual assistive-technology checks, publisher signing and store reviews remain release gates. Current checks do not establish independent real-world detection accuracy. [APK](../releases/SafeX-AI-1.8.0-debug.apk) · [Unsigned bundle](../releases/SafeX-AI-1.8.0-release-unsigned.aab) · [Chrome ZIP](../chrome-extension/release/SafeX-AI-Chrome-1.1.0.zip) · [First-use, scam-help and signing instructions](onboarding-and-release.md).

## Historical SafeX AI 1.7.0 research update — 9 October 2026

The final release passed **535 active unit tests and all 55 offline Android tests**, with **zero lint errors and 54 warnings**. Of 539 discovered unit tests, four optional research-export checks were skipped. The full native suite passed on Android 14/API 34 in **335.11 seconds**, in airplane mode with Wi-Fi disabled. [Release record](test-results/fraud-training-summary.json) · [Final build](test-results/fraud-final-build.txt) · [Native output](test-results/fraud-native-final.txt) · [Debug APK](../releases/SafeX-AI-1.7.0-debug.apk).

Verification covers the trained text/URL models, absence of Internet permission, private inference, real capture consent/crop/OCR, live camera QR decoding, explicit clipboard access, private-context/save behavior, session recovery, history, warning audio decoding and all three interface languages at 150% text. Every one of the 120 authored multilingual fraud/legitimate contracts and 55 floating-review contracts matched. The authored examples include training and development content and establish functional behavior, not independent accuracy. Kotlin/Python features and vocabulary-guard decisions match on all 5,606 prepared message rows. The 256-case native URL parity comparison had a maximum scorer difference of 0.0000011921. [Authored fraud results](test-results/fraud-native-contracts.json) · [Floating results](test-results/fraud-floating-summary.json) · [Native URL parity](test-results/fraud-url-native-parity.json).

The complete installed private scan pipeline used the same 847-message historical development input in both versions:

| Result | SafeX AI 1.6.0 | SafeX AI 1.7.0 |
| --- | --- | --- |
| Labeled scam messages detected | 13 / 78 | **71 / 78** |
| Labeled scams missed | 65 | **7** |
| Warnings on legitimate controls | 1 / 769 | 1 / 769 |
| Analysis errors | 0 | 0 |

This is an inspected historical English development comparison, including rules, links and model eligibility. It is not current-world accuracy, an independent final test or Hindi/Gujarati detection accuracy. Current analysis-only timings were p50 12 ms and p95 45 ms on the emulator; they exclude capture, OCR and user editing and do not establish physical-phone latency. [Baseline decisions](test-results/fraud-native-baseline-evaluation.json) · [Current decisions](test-results/fraud-native-evaluation.json) · [Research, source attribution and data gaps](fraud-research-2026-10-09.md).

Earlier cold attempts encountered a blocking Android System UI keyguard-service ANR. A UI query also needed a bounded wait for the expanded context question. After environment recovery and test synchronization, the complete suite passed. These attempts are preserved in the labeled `fraud-*-failure.txt` reports; an interrupted recovery run is not an application crash diagnosis. [Focused UI retest](test-results/fraud-context-retest.txt) · [Test rebuild](test-results/fraud-test-sync-build.txt).

The final debug APK is 134,714,579 bytes, version code 8, SHA-256 `fa8344d03f55624a281d8e45b7e6e40db919ee36fa27586050749867ae505d08`. ARM64 TensorFlow Lite and Tesseract libraries, model metadata and source attribution are included. Raw message collections and the phishing feed are excluded. Neither a physical Samsung Galaxy S24 nor A36 was connected; their Android/One UI behavior and speaker playback still need a phone rehearsal. Individual-app capture selection was not offered by the emulator, and API 26 compatibility was checked by lint rather than a second device.

## Historical SafeX AI 1.6.0 verification

The [SafeX AI 1.6.0](floating-assistant-1.6.md) release passed **518 active unit tests and 51 Android device tests in airplane mode**, with **zero lint errors and 54 warnings**. Of 521 discovered unit tests, three optional research-export tests were skipped. The native suite ran on Android 14/API 34 in 342.818 seconds. Its [verification record](test-results/floating-assistant-summary.json), [device output](test-results/floating-assistant-native.txt) and [build output](test-results/floating-assistant-final-build.txt) preserve the APK checksum and results.

Verification covers real full-display consent, repeated capture, projection stopped before cropping, landscape bounds, crop/OCR/private analysis and explicit saving; denial recovery; outside/Back menu dismissal without activating the source; independent extraction/coverage; explicit selection and retained edited drafts; recreation and stale-operation rejection; memory-pressure pixel release with text retained; JPEG orientation and sampling; lock cleanup; explicit clipboard access; new/old history JSON; and all three interface languages at 150% with actual tap-to-result actions and translated coverage.

The installed-app [55-case authored evaluation](test-results/floating-review-summary.json) had no contract mismatches: 28 intended warnings, 21 benign controls and six unsupported QR payloads with explicit caution. Analysis-only timings were p50 13 ms, p95 45 ms and maximum 48 ms on the named emulator. They exclude consent, capture, OCR, editing and physical-phone performance. The fixtures were used during development and do not establish independent real-world accuracy. OCR setup can be slower on a cold, constrained device; its forty-second outer recovery deadline includes model setup, and individual engine deadlines remain bounded.

Capture diagnostics contain lifecycle and dimensions, not content. The implementation bounds missing-frame surface refresh to two attempts on the same authorized display. The final suite passed after capture/lifecycle cleanup, translated coverage, prototype-model sensitive-context gating and cold extraction deadline fixes. Unit-level palette checks are recorded in [the contrast report](test-results/floating-contrast.json); they are not a whole-app accessibility certification.

The selected demo phones are a Samsung Galaxy S24 and Samsung Galaxy A36; their installed Android/One UI versions have not been provided and neither physical phone was connected. Individual-app selection was not offered by this emulator image, so that chooser, Samsung-specific behavior on either phone, physical TalkBack and speaker playback remain unverified. Model artifacts are unchanged. Independent representative evaluation/retraining, participant studies and Play Store foreground-service review remain external work. See [implementation status](floating-assistant-implementation-status.md) and [the S24 and A36 rehearsal guide](floating-assistant-1.6.md#samsung-galaxy-s24-and-a36-rehearsal). The [1.5.0 record](test-results/1.5.0/floating-assistant-summary.json) and its screenshots are archived separately. Historical checks below describe their labeled release versions.

The **1.3.0 fraud-link update** passed 452 JVM and 14 offline Android tests, with zero lint errors. It has a separate [research and verification record](fraud-link-audit.md). The record below preserves the previous 1.2.0 release checks.

Verified on 2026-10-08 with Java 17, Gradle 8.7, Android SDK 34 and an Android 14 / API 34 Google APIs x86_64 emulator. Build version: 1.2.0, version code 3. APK is signed with the development debug key.

[Machine-readable summary](test-results/summary.json) · [Native offline test output](test-results/native-offline.txt)

The translation generator checks 466 nonempty strings per language, identical resource keys, matched numbered placeholders and freshness of generated resources. [Language/branding details](languages-and-branding.md) · [Logo SVG](../branding/safex-logo.svg).

## Automated checks

| Check | Result |
| --- | --- |
| Core JVM tests | 51 passed |
| Agents JVM tests | 18 passed |
| UI JVM tests | 3 passed |
| App JVM tests | 228 passed |
| Total JVM tests | **300 passed, zero failures** |
| Android lint | **Zero errors**; 43 warnings |
| Native device tests | **12 passed** in airplane mode |
| Installed app Internet permission | Denied / absent |

Lint warnings are LogNotTimber, PluralsCandidate, ObsoleteSdkInt, Overdraw, UnusedResources, UnusedAttribute, ButtonStyle and UseTomlInstead. They remain visible; no baseline suppresses the errors found during this upgrade.

Native tests exercise the actual Hilt analysis pipeline, native TensorFlow Lite inference, credential scam vs safety advice, full Room result storage, blocked-link policy, disguised APK archive contents, bundled screenshot OCR, bundled QR decoding and prototype text-model parity with its published validation scores. The new tests also cover English/Hindi/Gujarati UI selection, 150% text sizing, stable drag/release behavior, activity recreation, dynamic translated warnings with intact URLs, Hindi/Gujarati message detection and safety advice, and real Hindi/Gujarati screenshot OCR. Native suite time was 48.356 seconds, including UI startup and interaction; this is not a scan-latency benchmark. Tests and reports are under each module's `build/` directory.

Commands:

```sh
cd android-app
./gradlew :app:assembleDebug :demo-sender:assembleDebug :app:assembleDebugAndroidTest
./gradlew :core:testDebugUnitTest :agents:testDebugUnitTest :ui:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell cmd connectivity airplane-mode enable
adb shell am instrument -w com.sentinel.ai.test/androidx.test.runner.AndroidJUnitRunner
```

## Manual device checks

- Actual notification from the separate synthetic Demo Sender reached the Android listener and produced a private high-priority SafeX AI warning.
- Posting a synthetic scam created one active warning; posting safety advice kept the warning count at one. Full notification evidence persisted across an app restart; demo-origin results retain their synthetic flag.
- In-app OTP example produced a labeled synthetic BLOCK result with explanations.
- Android SEND text routed into the shared result screen; a blocked message had no open / bypass action, including at the end of the scrollable result.
- Dashboard, scanner, result, history and settings were captured from the running app in `docs/screenshots/`.
- System-bar icon contrast and standalone shared-result / warning insets were corrected after visual inspection.
- English, Hindi and Gujarati interfaces were inspected on the running app. Gujarati at 150% remained readable, including the wrapped result score/duration. Screenshots are in `docs/screenshots/`.
- A physical tap on the reading slider saved 150% correctly after the drag/release fix. A full force-stop/relaunch retained Gujarati and 150% text; the restarted dashboard was captured.
- A real synthetic Gujarati notification from Demo Sender generated a Gujarati Android warning title, severity and private lock-screen preview, with the SafeX shield/X notification icon.

## Practical limits

These are functionality checks, not independent detection-accuracy certification. The text model uses authored synthetic training / validation data; the original URL asset has no verified benchmark. See the [model card](ml-model.md).

During the earlier 1.1.0 work, one cold OCR attempt hit the 15-second timeout while this low-memory host was compiling and the emulator was settling. A dedicated offline retry passed, followed by the complete native suite. The scanner provides a timeout error and allows retry. Rehearse OCR on the presentation device; no physical-phone performance benchmark is claimed.

The current 1.2.0 offline native suite passed all three OCR scripts. The software-rendered emulator showed occasional System UI stalls under host memory pressure during manual work; the app had no AndroidRuntime crash, and the native test suites completed. Only API 34 was exercised on a device. API 26 compatibility was checked by lint, not by a second emulator. Notification redaction / visibility, verified app links, poor image quality and unsupported file contents limit coverage. Read [features and scope](features.md) before presenting.
