# SafeX AI

An Android security assistant and desktop Chrome companion for checking suspicious messages, links, screenshots and QR requests on the device. The Android app also reviews files. Built for a privacy-focused hackathon demonstration.

## What works

- Scam Story Mode connects reviewed evidence into a private timeline, links findings to exact passages, saves encrypted snapshots and previews redacted exports.
- Branded floating shield supports fresh consented capture, crop/zoom, independent OCR outcomes and private text/link/QR review.
- Explicit All/Selected/Edited modes retain drafts; Back, recrop and cancellation preserve intended input.
- Visible fixed actions, coverage explanations, link-normalization confirmation and typed QR payloads keep the review clear.
- Shared messages, selected text and manual input use the same local scam analysis.
- Links combine 26 structural rules, a bundled TensorFlow Lite classifier and an optional imported local threat list.
- Supported app notifications are checked after the user enables Android notification access. Findings produce a private warning with saved explanations.
- Bundled OCR reads screenshot text; foreground camera and selected-image QR scans use bundled offline recognition.
- UPI instruction review shows unverified recipient details and supports expected address/amount comparisons.
- Domain explanations identify the actual destination, Unicode spelling and visible embedded hosts.
- Context questions can add saved caution, and incident checklists help after clicks, disclosure, installation or payment.
- Threat notifications include a custom warning tune, a labeled test and Android sound settings.
- File checks inspect names, signatures and bounded archive contents, including disguised Android packages.
- Results explain evidence and limitations. Blocked links have no open action; suspicious links require confirmation.
- Searchable local history, deletion, retention, per-app controls and themes.
- Full English, हिन्दी and ગુજરાતી interface selection, translated warnings and adjustable text size (85–150%).
- Native Hindi/Gujarati scam checks and bundled offline screenshot recognition in all three scripts.
- Original SafeX shield/X logo with a consistent teal/navy interface.

The installed app has **no INTERNET permission**. Inference works offline; there is no server or API key to configure. Android decides which notifications and links it exposes to the app.

## Run

Open `android-app` in Android Studio with Java 17 and Android SDK 36, or run:

```sh
cd android-app
./gradlew :app:assembleDebug :demo-sender:assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`. The optional `demo-sender/build/outputs/apk/debug/demo-sender-debug.apk` posts clearly labeled local fixtures to demonstrate actual notification capture in debug builds.

See [the 1.6.0 floating assistant](docs/floating-assistant-1.6.md), [the 1.4.0 practical features](docs/practical-features.md), [setup](docs/setup.md), [demo rehearsal](docs/demo.md), [privacy](docs/privacy.md), [features and scope](docs/features.md), [architecture](docs/architecture.md), [model card](docs/ml-model.md), [languages and branding](docs/languages-and-branding.md), and the [project audit and upgrade plan](docs/hackathon-upgrade-plan.md).

Design rationale: [floating assistant implementation plan](docs/floating-assistant-plan.md) · [Enhancement audit: UX, detection and logo](docs/floating-assistant-enhancement-plan.md).

Desktop companion: [Chrome extension installation, features and demo](chrome-extension/README.md) · [Browser release and validation](docs/chrome-extension-release.md) · [Product and implementation plan](docs/chrome-extension-plan.md).

New-user setup: [Onboarding, highlighted feature tours, official scam help and release instructions](docs/onboarding-and-release.md).

Whole-situation review: [Scam Story Mode usage, jury demonstration, privacy and detection limits](docs/scam-story-mode.md).

## Screenshots

| Home | Scanner | Result |
| --- | --- | --- |
| ![Home](docs/screenshots/dashboard.png) | ![Scanner](docs/screenshots/scanner.png) | ![Result](docs/screenshots/scam-result.png) |

[History](docs/screenshots/history.png) · [Settings](docs/screenshots/settings.png) · [Hindi](docs/screenshots/settings-hindi.png) · [Gujarati, larger text](docs/screenshots/settings-gujarati-150.png)

[SafeX logo SVG](branding/safex-logo.svg) · [Language controls and brand guide](docs/languages-and-branding.md) · [Gujarati result at 150%](docs/screenshots/scam-result-gujarati-150.png)

## Validation

SafeX AI **1.9.0** adds Scam Story Mode: a private evidence timeline, three sequence playbooks, exact supporting passages, encrypted case snapshots and redacted sharing. The Android tour now contains 19 steps. **593 active unit tests and 73 active offline Android tests passed**, with zero lint errors (108 warnings). Four optional unit research-export checks and one optional native research-input check were skipped. The 20 authored story contracts matched; they establish functional behavior, not independent fraud accuracy. Chrome **1.1.0** is unchanged; its previously recorded baseline passed 431 unit checks and 43 browser scenarios.

[Android development APK](releases/SafeX-AI-1.9.0-debug.apk) · [Unsigned Android release bundle](releases/SafeX-AI-1.9.0-release-unsigned.aab) · [Chrome ZIP](chrome-extension/release/SafeX-AI-Chrome-1.1.0.zip) · [Story demo and usage](docs/scam-story-mode.md) · [Release verification](docs/test-results/story-release-summary.json) · [Verification report and limits](docs/validation.md)

```sh
cd android-app
./gradlew :core:testDebugUnitTest :agents:testDebugUnitTest :ui:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Device tests exercise native URL inference, the real local analysis pipeline, OCR, QR extraction, disguised APK detection, persistence, model score parity, and absence of Internet permission.

## Model honesty

The 1.7 research update trains the text classifier on historical observed English smishing/ham and authored English/Hindi/Gujarati examples, and adds a bounded reported-phishing snapshot to the URL model. Source overlap and campaign/domain grouping are checked before splitting; validation selects checkpoints and thresholds. Observed model evaluation, inspected runtime diagnostics and authored contracts have separate reports. The installed private scan pipeline improved from **13/78 to 71/78 scam detections**, with one warning on 769 legitimate controls in both versions. The model-only result was 76/78 detections and zero false warnings; the stricter app policy still misses seven scams. These historical development results do not establish current-world or native-language accuracy. Models can raise warnings and cannot independently block content or certify safety. See the [model card](docs/ml-model.md), [dated research and access limits](docs/fraud-research-2026-10-09.md), [source attribution](models/FRAUD-DATA-ATTRIBUTION.md) and [fraud-link pattern audit](docs/fraud-link-audit.md).

## Stack

Kotlin 2, Jetpack Compose / Material 3, Hilt, Room, coroutines, WorkManager, LiteRT 1.4.0, CameraX 1.4.2, bundled ML Kit Latin/Devanagari OCR, Tesseract Gujarati OCR and ML Kit QR models. Minimum Android 8 / API 26; target and compile API 36. Modules: `app`, `core`, `agents`, `services`, `ui`, and optional `demo-sender`.
