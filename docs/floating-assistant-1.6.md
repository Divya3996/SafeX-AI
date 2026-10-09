# SafeX AI 1.6 floating assistant

The floating assistant checks content chosen by the user on the phone. It provides a private review, explains available evidence and lets the user save the displayed result deliberately. It has no Internet permission, background clipboard reader, accessibility screen reader or continuously running screen capture.

## Everyday workflow

1. Open **Floating assistant** in SafeX AI and tap **Enable floating assistant**. Allow display over other apps in Android settings. Manual Paste and image import work independently of this permission.
2. Tap the SafeX shield outside the app. Choose **Scan screen area**, **Paste link or message**, or **Choose screenshot**. **Resume private review** appears while an existing session is available. **More** contains Move, preferences and Pause. Tap outside the menu or press Back to dismiss it.
3. Screen capture asks for fresh Android approval every time. Select the whole screen or an individual app where Android offers that choice. Capture stops after one frame, before crop editing. Some protected content cannot be captured; use its Share action or copy the original text instead.
4. Start from the complete image. Drag the crop handles, use precise edge sliders, or zoom and pan. **Read selected area** stays below the scrolling content.
5. Review the extracted text against the image. Choose All text, Selected lines, Edited text, Links or QR. Selecting zero lines selects zero content. Edits remain available when switching modes. **Change area** returns to the original image; reading a new area deliberately replaces extraction and edits.
6. For normalized or wrapped link candidates, compare the original spelling and the destination shown by **Confirm link spelling**. Optional surrounding-message analysis includes the reviewed message and its detected links. A screenshot cannot reveal a hidden hyperlink destination.
7. Read the private result, its coverage and top reasons. Expand additional reasons or add context. **Edit and rescan** returns to the retained input. **Save result** writes the displayed result, context and provenance to local history once. **Done** or **Close** discards the private session.

Back returns one step and retains the intended input. A new capture/image picker preserves the current review until usable new content is accepted. Starting Paste retains the previous input until the user accepts replacement content or goes Back. Locking the screen clears the session. Background reviews expire after two minutes. Activity recreation can resume the live in-memory session; process death cannot restore private pixels or drafts.

## Clear scope and coverage

Threat decisions and assessment coverage are separate. A low risk index is not a guarantee of safety. Missing OCR coverage or unsupported action QR payloads receive caution rather than a successful clean assessment. A stronger existing risk decision cannot be weakened by a coverage problem or older context update.

Latin, Devanagari, Gujarati and QR recognizers report separate outcomes. Successful outputs survive another recognizer's failure. Gujarati native confidence is a reading-quality indicator, not a scam probability or measured character accuracy. Original spelling, user edits, selected lines and crop scope remain distinguishable. JPEG EXIF orientation is applied before cropping; sampled imports disclose that small text may be affected.

Contact, Wi-Fi, phone, email, SMS and app-action QR payloads are identified but are not fully assessed or executed. Web and payment QR content uses the existing local link and payment pipelines. Nothing opens automatically.

## Language, reading size and appearance

English, Hindi and Gujarati use the same translation catalog. Raw messages, links and QR payloads remain unchanged. Preferences include language, reading size (85–150%), bounded shield size (52–72 dp) and reduced motion. The external shield, menu and header share the canonical SafeX logo and app palette. Primary actions have a separate layout area above the keyboard and system navigation.

## Samsung Galaxy S24 and A36 rehearsal

The intended presentation phones are a **Samsung Galaxy S24** and **Samsung Galaxy A36**. Their installed Android/One UI versions have not been supplied, and neither physical phone is connected to this workspace. Record the versions separately when rehearsing; emulator checks do not certify Samsung-specific behavior.

| Demo phone | Installed Android | Installed One UI | Physical rehearsal |
| --- | --- | --- | --- |
| Samsung Galaxy S24 | Awaiting device information | Awaiting device information | Not yet performed |
| Samsung Galaxy A36 | Awaiting device information | Awaiting device information | Not yet performed |

Install the same [SafeX AI 1.6.0 APK](../releases/SafeX-AI-1.6.0-debug.apk) on both phones. On each phone, use Settings search to locate display-over-other-apps permission and SafeX AI notification settings. Allow the shield and review/urgent channels, then run **Test warning sound**. Warning audio obeys notification volume, channel sound selection and Do Not Disturb. Status and capture notifications remain silent.

On both phones, rehearse whole-screen and individual-app consent where available, crop/zoom, English/Hindi/Gujarati reviews, large text with the keyboard open, Back/Edit, one explicit save, capture denial, lock cleanup, permission revocation and Pause. Check portrait/landscape, TalkBack focus, gesture navigation and physical speaker playback. Repeat the same authored examples in airplane mode, once after a fresh launch and once after the recognizers have initialized. Record capture, extraction and analysis times separately for each phone, along with any recovery or overlay-permission issue; emulator timings are not phone measurements. Use authored demonstration messages rather than private communications. Special-use foreground-service Play Store approval is a separate distribution step.

## Engineering and validation

`FloatingSessionController` owns one RAM session independently of Activity ViewModels. Generations reject stale operation completions. The tracked original/crop/native-consumer pixel budget is 72 MiB; imports sample within the available budget, including temporary orientation transforms. Native consumers hold bitmap leases; clearing UI state does not recycle pixels still being read. Capture authorization remains owned by the one-shot service and is never cached for reuse. Private notifications carry a session identifier and cannot silently restore an unrelated older review.

If an authorized display produces no frame, capture refreshes its surface at most twice within the existing ten-second timeout. It retains the same virtual display and consent, accepts one frame, and stops projection before cropping. The surface operation is provided by [Android VirtualDisplay](https://developer.android.com/reference/android/hardware/display/VirtualDisplay#setSurface(android.view.Surface)). Protected or unavailable content still returns a recoverable capture error.

Extraction has a forty-second outer recovery deadline to allow cold bundled-model setup and independently bounded recognition. This is a failure bound, not a typical latency claim; Cancel restores the input immediately while native bitmap leases remain protected. Capture, extraction and analysis timings are reported separately when measured.

History stores nullable structured provenance, coverage and measured phase timings inside the existing result JSON. Older records remain readable without a Room schema change. The shared result UI retains ordinary scanner behavior while the floating flow removes duplicate result chrome and shows three initial reasons.

See [the enhancement plan](floating-assistant-enhancement-plan.md) for the original findings and [implementation status](floating-assistant-implementation-status.md) for verification. Final checks passed: **518 active unit tests, 51 offline Android tests and zero lint errors** (54 warnings). See [the release record](test-results/floating-assistant-summary.json) and [verification report](validation.md).

## Detection evaluation

Primary framework references: [Android screen capture](https://developer.android.com/media/grow/media-projection), [window insets](https://developer.android.com/develop/ui/compose/system/insets-ui), and [Tesseract interruptible recognition](https://github.com/adaptech-cz/Tesseract4Android/blob/master/tesseract4android/src/main/java/com/googlecode/tesseract/android/TessBaseAPI.java).

The local model artifacts remain unchanged. The synthetic text classifier now needs corroborating sensitive-action context before raising a warning, reducing warnings on neutral educational text. This policy can miss scams outside that context; independent recall and false-warning measurement remain necessary. Link checks and deterministic message rules run independently. Authored development fixtures test contracts and known regressions; they are not independent current-world accuracy evidence. The tiny synthetic text classifier and historical URL dataset do not justify an all-fraud detection claim.

Generate the authored corpus with `python3 scripts/build_floating_review_corpus.py`. The installed-app evaluator records private pipeline decisions, language/category, coverage and actual elapsed analysis times without saving those reviews to history. `scripts/evaluate_floating_review.py` summarizes confusion counts, false warnings, limited assessments, intervals and measured latency for that named corpus.

For a future authorized independent collection, `scripts/prepare_security_evaluation.py` validates de-identified JSONL locally. Required fields are `id`, `content`, `language` (en/hi/gu), `label` (0/1), `category`, `source`, `license`, `source_group`, `split` (train/validation/evaluation), `authorized: true` and `input_type` (text/link/qr). Link rows also require explicit registrable `domain_groups`. IDs, content labels and source/domain partitions must be consistent. The tool neither trains a model nor uploads data. Near-duplicate, time/source coverage and authorization quality still require human review. Keep final evaluation sealed from training and threshold selection.

## Release and screenshots

[Install SafeX AI 1.6.0 debug APK](../releases/SafeX-AI-1.6.0-debug.apk). Development debug signing; version code 7. Size: 134,779,457 bytes. SHA-256: `48181f68f9f23546150a6760ec0a85a03a231f03e63a9aec4892daba9d09961f`.

[Crop](screenshots/floating-assistant/captured-crop.png) · [Extraction review](screenshots/floating-assistant/extracted-review.png) · [Private risk result](screenshots/floating-assistant/private-risk-result.png) · [Hindi result at 150%](screenshots/floating-assistant/result-hi-150.png) · [Gujarati paste at 150%](screenshots/floating-assistant/paste-gu-150.png). All exported images contain authored test content.
