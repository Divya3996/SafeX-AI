# SafeX AI — mentor meeting plan

**Prepared for:** 8 October 2026  
**Briefing snapshot:** SafeX AI 1.2.0, Android debug APK  
**Project stage:** Working hackathon prototype; independent detection accuracy and production readiness still need validation.

## 1. Opening pitch

> SafeX AI is an Android security assistant that checks suspicious messages, links, screenshots, QR images and supported notifications directly on the phone. It explains the warning in English, Hindi or Gujarati, so users can understand the suspicious request before acting. The current build works offline and has no Internet permission. Our next goal is to validate detection quality on independent multilingual examples and real phones, then improve the weaknesses we find.

**One-line description:** Private, multilingual, on-device scam awareness with clear reasons and practical next steps.

**Suggested meeting outcome:** Agree on the hackathon scope, evaluation method, most important remaining work and final demonstration.

## 2. Problem and intended users

The problem we want to address is the moment when a user receives a convincing message or link and must decide whether to trust it. Examples include a fake bank verification request, an OTP request, a job offer requiring an advance fee, a prize payment or a message using urgency and authority to pressure the recipient.

Our design hypothesis is that useful protection should be available in the user's language, explain the suspicious behavior and work without uploading private communications. This hypothesis still needs user research; we have not established adoption or impact through a field study.

Initial users to test with:

- People who receive messages in English, Hindi, Gujarati or a mixture of these languages.
- People who want help interpreting suspicious requests and unfamiliar links.
- Users who benefit from larger text and straightforward explanations.
- Users who prefer local analysis or have intermittent connectivity.

Initial use case: a user receives “Your account will be closed; share your OTP now.” SafeX AI identifies the credential request and pressure cues, explains the concern in the selected language and recommends checking through a trusted channel. A benign message such as “Never share your OTP” should be handled differently.

## 3. Product goals and success evidence

| Goal | Product behavior | Evidence we will use |
| --- | --- | --- |
| Protect privacy | Analyze content locally, avoid content uploads and let users delete saved results | Manifest checks, offline tests and a device network audit |
| Identify suspicious requests | Combine message rules, local models and link evidence | Independent labeled test set, with results by language and scam category |
| Explain decisions | Give concrete reasons and useful next steps | User tasks measuring whether people understand the warning and action |
| Support multilingual use | Translate interface, warnings and guidance into English, Hindi and Gujarati | Resource checks, native-speaker review and usability testing |
| Remain usable offline | Bundle inference, OCR and QR assets | Airplane-mode tests from a fresh installation |
| Respond promptly | Analyze readable notifications and user-submitted content efficiently | Cold/warm latency, memory and battery measurements on defined phones |
| Give users control | Optional permissions, per-app controls, language, font size and history retention | End-to-end user tasks and persistence checks |

The hackathon goal is a reliable, explainable demonstration of these behaviors, supported by honest evaluation results. Real-world effectiveness is a subsequent validation goal.

## 4. What is already implemented

| Area | Current working implementation | Coverage boundary |
| --- | --- | --- |
| Message analysis | Credential/OTP requests, impersonation, urgency, rewards, advance fees, payment scams and coercion cues; local text classifier | Rules and a small synthetic-data model can miss unfamiliar attacks or misunderstand legitimate text |
| Link analysis | 20 structural rules, bundled TensorFlow Lite classifier and optional imported local threat list | Does not fetch website content or follow redirects; no list match does not establish safety |
| Notification warnings | Opt-in analysis of readable notifications from supported apps, deduplication, private warnings and per-app switches | Android and the originating app determine what content is visible; this does not read whole conversations |
| Manual and Android inputs | Paste/type, share-sheet input and selected-text analysis | Content must reach SafeX AI through an available input path |
| Screenshots | Offline English, Hindi and Gujarati OCR followed by message/link analysis | Blurred or poorly rendered text may be missed; selected images are not retained |
| QR images | Offline decoding of a selected QR image, followed by content analysis | Live camera scanning is future work |
| Files | Bounded checks for file type, misleading names, disguised APKs and suspicious archive entries | Structural checks, not a complete antivirus; files are never executed |
| Results and actions | Risk index, reasons, guidance, coverage notes and ALLOW/WARN/BLOCK decisions | Risk index is not a probability; BLOCK prevents opening through the SafeX result UI, not across the whole phone |
| History | Local full evidence, search, warning filter, deletion and 7/30/90-day retention; maximum 1,000 records | Stored in private app storage; full database encryption is future work |
| Language and accessibility | English, Hindi and Gujarati; saved language choice; saved 85–150% text scaling | Human translation review and broader accessibility trials remain useful |
| Branding and interface | Original SafeX shield/X logo, teal/navy palette, matching icons and Home/Scan/History/Settings navigation | Broader physical-device usability testing remains pending |

The original content stays unchanged when the interface language changes. Choosing English does not restrict detection to English: the rules and screenshot pipeline inspect all three supported scripts.

Manual text and link checks do not require notification access. Notification protection requires explicit Android access and the user's protection setting. Clipboard access happens when the user chooses to paste.

## 5. User experience plan

The primary journey is:

1. Choose a language and comfortable text size during setup or in Settings.
2. Use a manual scan immediately; enable notification protection when desired.
3. Submit a message, link, screenshot, QR image or supported file.
4. See a readable result with the suspicious evidence, coverage notes and next steps.
5. For a WARN link, confirm before opening. A BLOCK result has no open/bypass action in the result screen.
6. Review saved evidence in History, or delete it and choose a shorter retention period.

UX principles: describe the suspicious request plainly; keep original URLs/messages visible; use wording and icons alongside risk colors; maintain readable layouts at larger font sizes; show actual permission/listener state; and disclose limited coverage without making users interpret technical internals.

The logo communicates protection through the shield, stopping suspicious actions through the X and local intelligence through three connected nodes. The brand palette is teal and navy, with green/amber/red reserved for risk states.

## 6. Technical design

**Current stack:** Kotlin, Jetpack Compose/Material 3, Hilt, Room, coroutines, WorkManager, TensorFlow Lite, bundled ML Kit OCR/QR and bundled Tesseract Gujarati OCR. The build supports Android 8/API 26 onward; native validation so far used Android 14/API 34.

Every active input uses one shared scan repository:

```text
Manual input / share / selected text / readable notification / image / file
    → local parsing and normalization
    → message rules + local text model + embedded-link checks
      OR URL structure + URL model + imported threat-list match
      OR image decoding → OCR/QR → message/link checks
      OR bounded file structure/signature checks
    → shared decision policy and explanation
    → save complete evidence locally
    → result screen; warning for suspicious notification results
```

The project separates Android integrations, notification parsing, shared contracts/storage, UI and background history maintenance into modules. A separate debug Demo Sender provides clearly labeled synthetic notifications for rehearsal.

Saving completes before result publication so that a warning can open its durable evidence. Android manages the notification listener; the app reports whether protection is listening, reconnecting, paused or ready for manual scans.

### Why use a hybrid detection engine?

Rules provide direct explanations for explicit requests such as sharing an OTP or paying an advance fee. Local models supply an additional signal for text and URL patterns. Link checks add evidence from the destination structure. Combining these signals gives a practical prototype that can operate offline and explain its findings.

The models do not certify safety. The text model can raise a warning but cannot independently create a BLOCK decision. Established rule evidence is not reduced by a low model score. Model failure falls back to rules and is disclosed.

## 7. Privacy and permission model

- The current APK has no Internet permission and no cloud inference dependency.
- Models and OCR/QR assets are bundled, so analysis does not require a first-use model download.
- No account, backend or API key is needed for scanning.
- Readable notification access is opt-in and subject to Android permissions.
- Scan history is local, excluded from backup/transfer, bounded and user-deletable.
- Warning content uses a generic public lock-screen preview.
- An offline threat list can be imported by the user; automatic online updates are not currently implemented.

Privacy work for a later release includes reviewing local data exposure, considering database encryption and auditing any future update channel. Local storage should be explained to users rather than implying that no data is saved.

## 8. Current verification and accuracy limits

The recorded validation for version 1.2.0 reports:

- **300 JVM tests passed**, with no failures/errors.
- **12 native Android tests passed** on an API 34 emulator in airplane mode.
- **466 strings per language**, with matching resource keys and placeholders.
- Android lint: **0 errors and 43 warnings**.
- Native tests exercised local inference, multilingual scam/advice handling, screenshots, QR decoding, file disguise checks, saved evidence, blocked actions and display preferences.

These establish tested functionality under those conditions. They do not establish general detection accuracy, performance on all Android phones or production readiness. The native suite duration is not scan latency.

**Text model:** trained on 100 authored synthetic examples. Its 36-example synthetic validation set was also used for threshold selection, so it is not an independent test. Model-only results were 11 true positives, 7 false negatives, 0 false positives and 18 true negatives: **61.1% recall on 18 synthetic scam examples**. This is not the accuracy of the complete hybrid engine or a real-world performance claim.

**URL model:** native inference works, but the repository does not contain a reproducible training dataset or independent benchmark for the inherited model. We should replace or validate that asset before making accuracy claims.

**Device coverage:** native testing has been on an emulator. Physical-device latency, battery usage and broader Android compatibility need measurement. The current debug APK is approximately 127 MiB; smaller delivery is another engineering goal.

## 9. Prioritized development plan

The schedule below is a suggested sequence. Agree actual dates after confirming the hackathon deadline, team capacity and mentor priorities.

| Priority / phase | Work | Deliverable and completion gate |
| --- | --- | --- |
| P0 — lock the presentation scope | Rehearse the current APK, choose a primary demo phone, prepare offline assets and screenshots, verify notification permissions | Complete demo rehearsal with fallback screenshots and accurate feature claims |
| P0 — independent evaluation | Collect permitted examples, label scams and benign controls, reserve an untouched test set across all three languages, freeze thresholds before testing | Per-language/category confusion matrices and documented mistakes for the full pipeline and model-only paths |
| P0 — improve the biggest errors | Address false alarms and missed scam categories revealed by evaluation; keep OTP safety advice and multilingual edge cases as regression cases | Updated validation results, followed by evaluation on fresh held-out examples if previous test examples influenced tuning |
| P1 — physical-device reliability | Test lower/midrange phones, permission denial/revocation, listener recovery, offline fresh install, large text and notification visibility | Device matrix, p50/p95 timings, memory/battery notes and documented compatibility limits |
| P1 — multilingual usability | Review translations with native speakers; ask users to scan, understand a reason, choose an action and change preferences | Translation corrections and observed task-completion results |
| P1 — stronger reproducible models | Build documented training/evaluation data and scripts, validate or replace the inherited URL model, compare gains with APK and latency costs | Model cards, versioned artifacts and reproducible evaluation; retain a new model only if evidence supports it |
| P2 — additional product capability | Live-camera QR scanning, more mixed-language coverage, verified/versioned offline threat packs and improved educational guidance | Implement individually with permissions, tests and coverage descriptions |
| P2 — release preparation | Review local-data protection, reduce APK size, configure release signing and complete broader security/device checks | Release candidate with a documented review checklist and remaining limitations |

Calls/audio analysis, a system-wide firewall, full malware scanning and guaranteed sender identity are separate research/platform projects. Do not include them as hackathon delivery commitments.

## 10. Evaluation plan and proposed targets

### Detection evaluation

Begin with a pilot held-out set of at least **200 examples per language**, including at least 100 scam and 100 benign examples per language. This is a proposed starting point, not data already collected or enough to certify production reliability. Use separate training/validation data when improving models.

Include banking/OTP, rewards, job/loan fees, payment requests, coercion, ordinary conversations and benign safety advice. Add mixed-language, spelling variation, zero-width characters, obfuscated URLs and unfamiliar scam templates. Use consented or appropriately licensed content; synthetic examples can supplement coverage but should be reported separately.

Group related templates, sources and domains into the same split to reduce leakage. Have disagreements reviewed. Freeze the rules/models/thresholds before running the held-out test. Report precision, recall, false-positive rate and confusion matrices by language and category, with sample counts. Measure the entire hybrid engine separately from each model. A balanced pilot dataset does not represent the prevalence of scams in everyday notifications.

Discuss the acceptable false-alarm/missed-scam tradeoff with the mentor before setting an accuracy gate. A single attractive accuracy percentage would conceal the errors that matter.

### Proposed engineering targets — not current measurements

| Measure | Suggested initial target | Measurement condition |
| --- | --- | --- |
| Text/link scan latency | Warm p95 at or below 500 ms | Agreed midrange phone, fixed corpus; report cold start separately |
| Screenshot scan latency | p95 at or below 3 seconds for clear screenshots | Defined image size, script and physical phone |
| Notification warning latency | p95 within 1 second after readable content reaches the listener | Excludes delivery delay controlled by Android/originating app |
| Offline operation | All supported scan paths usable without network | Fresh installation with bundled assets and airplane mode |
| Content upload | No sensitive-content transmission by the current build | Manifest inspection plus observed network behavior |
| Usability | At least 80% task completion in an initial 5–10-person formative trial | Include all three languages; report counts, not a population estimate |

Memory, battery and package-size targets should follow an initial physical-device baseline. These targets may need adjustment after measurement; they are goals, not promises.

## 11. Suggested hackathon demonstration — approximately 6 minutes

| Time | What to show | Point to explain |
| --- | --- | --- |
| 0:00–0:40 | Problem and short pitch | A suspicious request should receive understandable help without a content upload |
| 0:40–1:20 | Airplane mode; language and font settings | Offline availability and accessibility are part of the product |
| 1:20–2:10 | Synthetic OTP scam, then benign OTP safety advice | Explain the actual evidence and contrast the two requested behaviors |
| 2:10–2:50 | Synthetic deceptive URL | Structural evidence, model status and no open action for a BLOCK result |
| 2:50–3:50 | Clear screenshot and selected QR image | Local image decoding feeds the same analysis pipeline |
| 3:50–4:40 | Demo Sender notification and saved result | Actual Android warning flow, with synthetic samples clearly labeled |
| 4:40–5:20 | History and a Hindi/Gujarati result | Translated guidance, preserved original content and user control |
| 5:20–6:00 | Test evidence, limits and next milestone | Functionality is verified; independent accuracy evaluation is next |

If time is limited, prioritize scam versus benign advice, one deceptive link, one actual notification and language/privacy controls. Keep file disguise detection as a backup example.

Before the meeting: install both APKs, allow notification access/warnings where needed, turn protection on, copy the synthetic assets to the phone and rehearse offline. Keep screenshots available if the demonstration device or notification delivery fails. Use harmless synthetic examples and reserved-domain links.

## 12. Decisions to request from the mentor

1. Is the strongest hackathon story multilingual message/notification protection, or should screenshot/QR analysis receive equal presentation time?
2. What false-alarm versus missed-scam tradeoff should guide our warning policy, especially for strong blocking decisions?
3. Can the mentor suggest permitted representative datasets or reviewers who can label English, Hindi and Gujarati examples?
4. Which Android devices and versions should define our performance and compatibility targets?
5. Given the remaining time, should we prioritize independent evaluation, model replacement, translation review or an additional input feature?
6. What evidence would the judges expect to see for privacy, AI contribution and impact?

End the meeting with a chosen primary use case, agreed evaluation gates, a short priority list, named owners if working as a team and dates tied to the actual deadline.

## 13. Presentation outline

1. **Problem:** suspicious communications and the user's trust decision.
2. **Solution:** SafeX AI, local analysis and explanations in three languages.
3. **Working product:** current scan paths, warnings and accessibility.
4. **Technical approach:** shared pipeline, hybrid inference and local storage.
5. **Evidence:** tested functionality and transparent accuracy limitations.
6. **Demo:** offline scam/advice comparison, link and notification.
7. **Roadmap and mentor input:** independent evaluation, real-phone reliability and priorities.

Suggested closing: “We have a working private, multilingual prototype. Our next milestone is an independently evaluated build with measured device performance. I would like your guidance on the evaluation dataset, acceptable warning tradeoffs and the strongest scope for our final demo.”

## 14. Supporting project material

- [Validation record](validation.md) and [machine-readable test summary](test-results/summary.json)
- [Model card and accuracy limits](ml-model.md)
- [Architecture](architecture.md) and [feature coverage](features.md)
- [Demo rehearsal](demo.md) and [synthetic demo assets](../demo-assets/README.md)
- [Language, accessibility and branding](languages-and-branding.md)
- [Logo master](../branding/safex-logo.svg)
- [SafeX AI 1.2.0 debug APK](../android-app/app/build/outputs/apk/debug/SafeX-AI-1.2.0-debug.apk)
- [Demo Sender debug APK](../android-app/demo-sender/build/outputs/apk/debug/demo-sender-debug.apk)

This plan describes current repository evidence and proposed next work. It does not claim universal scam detection, calibrated risk probabilities, certified antivirus protection or a completed production security audit.


The later 1.3.0 fraud-link research, retraining and verification are recorded in [fraud-link audit](fraud-link-audit.md); the original briefing statistics above are a 1.2.0 snapshot.
