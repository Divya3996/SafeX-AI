# Verification report

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
