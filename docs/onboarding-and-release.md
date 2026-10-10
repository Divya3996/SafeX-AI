# SafeX AI: onboarding, feature guidance and scam help

Android 1.9.0 / Chrome 1.1.0. Updated 10 October 2026.

## First use

Android starts with four short introduction screens: welcome, language and reading size, privacy and detection limits, and optional setup. English, Hindi and Gujarati update the screen immediately. Users can skip the introduction, review Android permissions without granting them, and continue to manual scanning. Choosing the guided setup starts the feature tour after the permission screen. Introduction completion is stored on the device; it does not require an account.

Chrome opens a four-page introduction the first time its panel is opened. It offers language and reading-size selectors, explains local processing and site-access choices, and starts a feature tour. Skipping persists across panel restarts. The introduction and guides work offline using packaged fonts and translations.

## Highlighted tours

The Android tour has 19 steps covering protection status; message, link, screenshot, QR and file modes; synthetic examples; Scam Story Mode; floating-assistant setup; protection switches; notification access; warning sound; reading settings; local history and saved-record review; incoming alerts; and scam help. The browser tour has 12 steps covering page checks, pasted messages, context questions, capture/crop, image and QR import, examples, reports, reading, sound, imported lists and official help.

Each step highlights the actual control and brings it into view. Next, Back and Close are available. The guide never grants permissions or starts a scan. Both products provide **Learn SafeX AI**, a readable feature reference, a full replay button and individual feature shortcuts. Android also exposes this from Home, Settings and the navigation drawer. Chrome exposes it in the panel footer.

Urgent help remains reachable during the introduction and the tour. Closing the guide leaves the highlighted screen available for normal use.

Scam Story Mode also provides a first-use guide beside the case and a replay link. Add reviewed messages, screenshot text, links or decoded QR content to a private timeline; inspect the connected reasons, then explicitly save an encrypted snapshot or preview a redacted summary. [Complete usage and jury demonstration](scam-story-mode.md).

## When someone has been scammed

Open **Help after a scam** in Android or **Help** in Chrome. Select what happened:

1. Opened a link: stop entering information and check for downloads or permission changes.
2. Shared a password: change it through the official service on a trusted device, revoke unknown sessions and review account recovery settings.
3. Shared an OTP or PIN: contact the affected bank/service through official contact details and secure the account.
4. Installed an app: stop remote access, secure accounts using another trusted device and remove the suspicious application and its permissions.
5. Sent money: contact the bank/payment provider promptly, preserve transaction evidence and use official reporting channels. Do not pay a recovery agent.

The checklists and completion ticks are local guidance, not a submitted complaint.

For **India**, financial cyber-fraud help is **1930**; immediate danger or general emergency help is **112**. These have distinct labels. Android displays a confirmation before an `ACTION_DIAL` intent: the user reviews the number and presses Call in their phone app. SafeX AI requests no calling permission. Chrome provides Copy number and a confirmed `tel:` calling-app link; a desktop without a calling app can use the shown number on a phone. No action calls automatically.

The report button opens [India's official cybercrime reporting website](https://cybercrime.gov.in/). Official reference: [cyber-fraud reporting and 1930](https://www.cybercrime.gov.in/Webform/crmcondi.aspx), [112 emergency response](https://112.gov.in/). Verified 10 October 2026. Choosing **Another country** hides India's calling actions and directs users to their bank and local official emergency service. Phone service is needed for calls; a browser connection is needed to open official websites. Reporting does not guarantee recovery.

## Release preparation

Current artifacts: [Android 1.9.0 development APK](../releases/SafeX-AI-1.9.0-debug.apk), [Android 1.9.0 unsigned release bundle](../releases/SafeX-AI-1.9.0-release-unsigned.aab), and [Chrome 1.1.0 ZIP](../chrome-extension/release/SafeX-AI-Chrome-1.1.0.zip). The APK is installable for a demonstration. The bundle needs publisher signing; the extension ZIP is loaded through **Load unpacked**.

The Android project compiles and targets API 36 using AGP 8.11.1, Gradle 8.13 and JDK 17. The private model uses LiteRT 1.4.0 with its API pinned to the same version. CameraX uses 1.4.2. Both updates address the previous native memory-page alignment gaps; packaged binaries are checked separately. The app keeps its existing application ID so a compatible signed update can retain user settings and records. Internet permission remains removed and device backup remains disabled.

Build commands:

```sh
python3 scripts/generate_localization.py --check
python3 scripts/generate_guidance_copy.py --check
cd android-app
./gradlew :app:assembleDebug :app:assembleRelease :app:bundleRelease :app:assembleDebugAndroidTest testDebugUnitTest :app:lintDebug
```

From the project root, inspect the actual packaged 64-bit libraries with `python3 scripts/check_android_native_alignment.py android-app/app/build/outputs/apk/debug/app-debug.apk`. This checks ELF segment alignment and uncompressed ZIP payload alignment. It does not replace testing on a device with 16 KB memory pages.

Release signing accepts four environment variables: `SAFEX_KEYSTORE_PATH`, `SAFEX_KEYSTORE_PASSWORD`, `SAFEX_KEY_ALIAS` and `SAFEX_KEY_PASSWORD`. All four must be supplied together. Keep the publisher's keystore outside source control. Without these variables the release bundle is unsigned; the development APK is signed with the debug key. The unsigned bundle must be signed with the owner's upload key before store submission. A new upload certificate does not let an installed app signed by an unrelated key update in place.

For Chrome, run `npm run verify`, `npm run package` and `npm run record:validation` in `chrome-extension`. Extract the versioned ZIP into a permanent directory and load it using Chrome's **Load unpacked** command. An existing developer installation can reload the current `dist` folder. Store installation requires Chrome Web Store publication; a ZIP alone is not a store listing.

## Verified results

The final Android build produced the development APK, unsigned release APK, unsigned release bundle and instrumentation APK successfully. **593 active unit tests and 73 active offline Android tests passed**. Four optional unit research-export checks and one optional native research-input check were skipped. The native runner's 74 discovered checks completed in 379.654 seconds on Android 14/API 34 with airplane mode and Wi-Fi off. Installed checks cover first use, all 19 guide targets, English/Hindi/Gujarati at 150% text, real camera QR, capture consent/crop/OCR, Story Mode review/import/edit/reorder/save/export, clipboard recovery, and screen-lock cleanup. Confirmed dial intents are intercepted; no emergency service was called.

The 20 authored story contracts matched. Individual detector warnings, connected findings and timings are recorded separately. Encrypted-save device checks cover plaintext exclusion, authentication failures, retention and the 20-case cap. These are functional development checks, not independent fraud accuracy.

Chrome's unchanged 1.1.0 baseline previously passed **431 unit/privacy checks and 43 browser scenarios**, including its 12-step tour, with zero known dependency vulnerabilities in that recorded audit. Chrome was not rebuilt or retested for this Android feature. Android lint reported zero errors and 108 warnings. Static ELF/ZIP checks passed for all 22 packaged 64-bit libraries in each Android APK; a 16 KB runtime was not tested.

[Machine-readable release record and checksums](test-results/story-release-summary.json) · [Installed story evaluation](test-results/story-evaluation.json) · [Complete verification report](validation.md) · [Android test output](test-results/story-native-final.log) · [Previous Chrome release record](test-results/chrome-extension-summary.json).

## Presentation and remaining release checks

On the Samsung S24 and A36, rehearse language selection at 150% text, skipping optional permissions, replaying guidance, scanning the provided harmless examples, camera QR, floating capture consent/crop/review, notification permission revocation and warning sound. Check the installed Android/One UI versions and system notification volume. Demonstrate the scam-help confirmation, then cancel; a demonstration must not place a test emergency call.

Automated emulator/browser results are recorded alongside this document. They establish tested functionality, not independent real-world fraud accuracy. Physical S24/A36 behavior, Android 16 and a 16 KB runtime still need device validation. Publisher signing and store reviews require the owner's credentials. These items remain release gates; this implementation does not claim completed store publication or perfect detection.
