# SafeX AI 1.4.0 — practical protection features

## Live QR scanning

Open **Scan → QR code → Scan with camera**. Camera permission is requested inside the foreground scanner, and is optional: selected-image scanning still works without it. CameraX binds preview and analysis to the activity lifecycle. The bundled ML Kit QR decoder processes frames locally; frames and photographs are never saved or uploaded.

Recognition pauses when it finds a code. Users inspect the extracted contents and choose **Analyze privately**; nothing opens automatically. For multiple visible codes, users choose one of up to eight displayed candidates. Selected-image scanning refuses more than eight codes rather than silently ignoring additional content. Empty/oversized contents, denied permission, camera errors and recognition errors have recoverable states. QR HTTP(S) destinations use the normal URL pipeline and opening policy; payments and other payloads do not receive a browser/open-payment action.

## UPI payment review

UPI `upi://pay` contents expose the address, the name supplied by the QR, requested amount (or no fixed amount), and currency. This is **instruction parsing, not bank verification**. Display names are explicitly unverified. SafeX AI never requests a UPI PIN, initiates a payment, resolves an address against a bank, or certifies a QR as safe.

Users can compare an expected UPI address and optional expected amount. Mismatches can add a persisted strong warning to the current result. Applying a review clears text-field focus and returns to the updated risk summary. Decimal comparisons avoid floating-point rounding. Duplicate parameters, missing/malformed addresses, unsupported URI shapes/currency, hidden controls, invalid encoding and invalid amounts receive explicit caution. Account/IFSC-style and non-INR QR formats are not claimed as supported. Payment review checks address syntax, not whether an address exists. Bank-verified recipient details must be checked in a trusted payment app.

## Destination explanations

Results show the actual host and registrable website domain using the detection parser and OkHttp's public suffix list, including private hosting suffixes. They distinguish userinfo before `@`, numeric addresses, Unicode/punycode spellings, HTTP versus HTTPS, and visible embedded external destinations. Existing rule/model explanations remain available. No DNS lookup, webpage fetch or HTTP redirect following occurs. HTTPS and a familiar-looking domain are never presented as proof of legitimacy.

Review facts are also saved with message/QR results and available when reopening local history and notifications. Old records remain readable; added metadata is nullable and requires no Room schema migration. Full and observed history reads run in transactions so multi-window cursors retain a consistent snapshot during concurrent edits. A device regression exercises a history larger than 2 MB while records are replaced.

## Contextual review

Users can disclose an unexpected request, a request to share secrets, a payment request, an app/remote-access request or pressure to act immediately. Applicable statements add disclosed user-context reasons to the result. Credential disclosure and mismatched payment details raise a strong WARN; an unexpected/pressured installation does as well. Other selected statements raise caution.

These answers cannot reduce the original score, erase original evidence, or bypass a BLOCK. Context alone never independently blocks. Saved reviews retain the original record ID and timestamp; repeated reviews deduplicate reasons. Unsaved question selections survive recreation through a saveable boolean list. Previously added caution remains recorded even when later answers are reassuring.

## Incident help

Open **Home → Help after a scam**, the navigation drawer, the scanner, or an analysis result. Five offline checklists cover opening a link, sharing a password, sharing an OTP/PIN, installing an app, and sending money. Checklist completion is temporary UI state, not a submitted incident report.

The official portal button opens an external browser. The **1930** button opens the dialer; the user decides whether to call. No reports, messages or calls are submitted automatically. Official websites need internet in the browser; SafeX AI still has no INTERNET permission. Guidance does not promise recovery or diagnose device compromise.

## Warning tune

Threat notifications use an original short, three-pulse SafeX AI WAV asset. Two Android channels distinguish review and urgent warnings. **Settings → Warning sound** includes a clearly labeled test notification and shortcuts to both channel settings. The test creates no scan-history entry and expires after 30 seconds. Benign results do not post threat warnings; updates to the same warning use `onlyAlertOnce`.

Android controls channel audio, notification volume, permission and Do Not Disturb. There is no alarm playback or DND bypass. Versioned channels install the tune on upgrade; earlier channel silence, custom sounds, importance and vibration choices are preserved. Existing v2 channel choices are never rewritten. A muted/custom legacy channel may therefore retain its previous sound behavior until the user changes it in Android settings.

## Languages and presentation

All new trusted app copy is included in the English/Hindi/Gujarati catalog. User content, QR names, UPI addresses and domains are displayed verbatim. The new cards reuse the teal/navy Material theme, selectable identity fields, scrolling layouts, minimum-size actions and configurable text scale. New checkbox rows expose a combined accessible label.

## Official references

- [Bundled ML Kit barcode decoding](https://developers.google.com/ml-kit/vision/barcode-scanning/android)
- [CameraX image analysis and lifecycle](https://developer.android.com/media/camera/camerax/analyze)
- [Room transactions for consistent multi-window history reads](https://developer.android.com/reference/androidx/room/Transaction)
- [Android notification channel behavior and user control](https://developer.android.com/develop/ui/compose/notifications/channels)
- [NPCI UPI FAQs](https://www.npci.org.in/what-we-do/upi/faqs)
- [Google Pay: PIN is needed to send, not receive money](https://support.google.com/pay/india/answer/7296045/send-money-android?hl=en-GB)
- [Official financial cyber-fraud reporting and 1930](https://mrm-ncrp.mha.gov.in/restoration/)
- [National Cybercrime Reporting Portal](https://cybercrime.gov.in/)
- [Google account compromise guidance](https://support.google.com/accounts/answer/6294825?hl=en)
- [Google Play Protect](https://support.google.com/googleplay/answer/2812853?hl=en)

## Validation

The final APK passed **473 unit tests** and **25 native device tests** in airplane mode. Three opt-in research-export tests were skipped. Android lint completed with **zero errors and 48 warnings**. The device suite covers real camera-frame decoding, persisted payment/context reviews, notification audio decoding and delivery, all three languages at 150% text size, and concurrent reads/updates of a history larger than Android's cursor window.

Final results and the APK checksum are recorded in [the validation summary](test-results/practical-features-summary.json), [the device test output](test-results/practical-features-native.txt), and [the build output](test-results/practical-features-final-build.txt). [Screenshots](screenshots/practical-features/) show the new cards and incident-help screen. Tests are functional checks; they are not a new independent phishing-accuracy benchmark. The existing URL model and its documented evaluation limits are unchanged.

The emulator verifies Android decoding of the WAV, notification channel sound URIs, and actual warning posting. Its host audio driver is unavailable, so audible speaker playback must be checked on a physical phone using **Settings → Warning sound → Test warning notification**.

### Camera fixture reproduction

Generate the deterministic camera input with `uv run --with qrcode==8.2 python scripts/generate_camera_fixture.py`. Launch the emulator with `-camera-back imagefile:/absolute/project/path/android-app/app/src/androidTest/assets/camera-qr.png`. The imagefile camera samples a portrait portion of its source, so the authored code is positioned inside that field of view. This exercises actual CameraX frames, bundled decoding, the activity result, and the installed scan pipeline; it does not inject decoded text into the camera result.

Run device tests with `adb shell am instrument -w -e safexCameraFixture true com.sentinel.ai.test/androidx.test.runner.AndroidJUnitRunner`. The camera fixture flag is required only for the deterministic capture test; other functional tests work without that input.
