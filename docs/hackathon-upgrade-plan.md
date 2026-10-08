# SafeX AI: project audit and hackathon upgrade plan

Prepared on 8 October 2026. This is an implementation plan based on the current working tree; application behavior has not been changed in this analysis task.

## Recommendation

Keep the Kotlin Android app and its existing Compose, Hilt, Room, and TensorFlow Lite foundations. Build one consistent on-device analysis pipeline for links, messages, and supported content. Make privacy and offline operation demonstrable, then add screenshot/QR scanning if the core flows pass their acceptance checks.

The best presentation claim is: **“SafeX AI analyzes shared content and supported incoming notifications on your phone, warns about phishing and scam signals, and explains what to do without uploading your private content.”**

Do not claim universal monitoring of every app, guaranteed malware detection, actual sender blocking, or verified model accuracy until those capabilities are implemented and measured.

## 1. Audit scope and verification

Reviewed the five-module structure, Gradle configuration and manifests, documentation, scan contracts and orchestration, URL rules and feature extraction, TFLite/scaler integration, reputation providers, notification capture/parser/builder/scoring, warning delivery, services, local persistence, settings, onboarding, scanner/results, dashboard/history/details, and existing test coverage.

The working tree already contains extensive changes. Implementation should preserve them and start with a baseline commit or patch snapshot made by the developer, without overwriting unrelated work.

Build verification attempted:

```text
:app:testDebugUnitTest
:core:testDebugUnitTest
:agents:testDebugUnitTest
:ui:testDebugUnitTest
:app:assembleDebug
```

The shell wrapper fails because `android-app/gradlew` has Windows CRLF line endings. Invoking the Gradle wrapper through Java starts Gradle 8.7, but task dependency resolution fails because `android-app/local.properties` points to a nonexistent Windows SDK directory. JDK 17 is available. No unit-test pass, APK build, device behavior, latency, or model accuracy is claimed by this audit.

## 2. What already exists

| Area | Current implementation | Assessment |
| --- | --- | --- |
| Android architecture | `app`, `core`, `agents`, `services`, `ui`; Kotlin/Compose/Hilt/coroutines/Room | Good foundation; no rewrite needed |
| URL analysis | Normalization, 20 local heuristic rules, structured findings | Implemented; needs calibration and consistency fixes |
| On-device URL AI | Bundled approximately 4 KB `model.tflite`, scaler, 15 numeric features, tensor validation | Inference integration exists; predictive quality unverified |
| Reputation | OpenPhish feed matching, optional VirusTotal submission, deterministic mock provider | Needs privacy boundaries and separation of demo fixtures |
| Incoming notifications | Registry for WhatsApp, Telegram, Gmail, Google Messages, Instagram, Messenger, Signal, Discord, Slack | Local rule analysis exists; per-app capture must be device-tested |
| Scam messages | Urgency/credential/financial keywords and basic extracted URL signals | Rules exist; no semantic text ML model |
| Manual scanner | Link and file flows; `TEXT` state is routed to URL scanning | Genuine free-text message scanning missing |
| Shared/selected content | VIEW/SEND/PROCESS_TEXT handling | Primarily URL/file; arbitrary scam text missing |
| Files | MIME/display-name/extension heuristics | Metadata risk checks only; file bytes are not analyzed |
| Warnings | Warning notifications and warning/detail screens | Implemented but lifecycle and action consistency need work |
| History | Private Room database and in-memory journal | Persistent; detailed verdict/evidence fields are lost on restore |
| Service scaffolding | Guard, monitor, background-sync worker | Placeholder implementations |
| Other coordinators | SMS/call/message/link/file coordinator classes | Return null results; separate link/file intent implementations do work |
| UI | Dashboard, scanner, results, alerts, history, permissions, settings, theme | Extend existing screens instead of redesigning everything |
| Tests | Rule, schema, provider, parsing, regression and native ML test sources | Useful baseline; execution blocked by SDK setup |

## 3. Fixes required before adding presentation features

### P0: inconsistent blocking

`ui/.../scanner/ScanResultScreens.kt` always renders a continuation button; any non-ALLOW result calls `onBypass`. `ScannerScreen.kt` binds that callback to browser launch for links. A BLOCK result therefore remains openable in the manual scanner. `ScanLoadingActivity.kt` correctly removes continuation for BLOCK.

Implement one shared action policy used by both result flows and browser launch. BLOCK must have no continuation callback. WARN may have an explicit confirmation. ALLOW can continue. Describe this as preventing navigation through SafeX AI, not blocking links system-wide.

**Acceptance:** a BLOCK result cannot open a browser through either screen or a stale callback.

### P0: offline latency and network isolation

`IntentThreatAnalyzerImpl` waits for reputation enrichment before URL ML inference. `ReputationManagerImpl` waits for supported providers, with a configured 10-second timeout. OpenPhish fetches its feed for each scan; no persistent offline feed cache exists. `ScanLoadingActivity` also adds a deliberate one-second delay.

Remove the artificial delay. Run local URL rules/model/bundled reputation first and return without waiting for the network. Move feed downloads to optional background maintenance. All scan entry points must honor strict local mode.

**Acceptance:** first launch in airplane mode supports local scans; online network slowness cannot delay a local result; strict mode makes zero app network calls.

### P0: privacy exposure

`IntentScanRepository`, `FeatureExtractor`, intent activities, and `NotificationAgentCoordinator` log raw URLs, payloads, message text, or serialized events. The manifest enables backup; no exclusion rules for history were found. VirusTotal posts the scanned URL when a key is configured, although its default key is blank. OpenPhish feed downloading does not itself submit the scanned target.

Remove private-content logging and HTTP body logging for sensitive flows. Exclude history and private preferences from cloud backup using the applicable Android backup configurations. For the hackathon, remove URL submission from the normal build and provide a local build variant without INTERNET permission. Optional feed updates must download generic intelligence only.

**Acceptance:** inspect logs and captured app traffic using synthetic secrets; no message bodies, private URLs, credentials, or sender identifiers appear outside permitted local storage.

### P0: event delivery and persistence

`ThreatEventBus` is a SharedFlow with replay 0 and DROP_OLDEST. With no subscriber, events are not retained; under load, older events can be dropped. History depends on the subscriber service receiving the event. `ThreatJournal.initialize` blocks startup using `runBlocking`; writes suppress failures. `ThreatRecordEntity` omits decision, reasons, provider findings, confidence, and local evidence.

Save the complete final analysis transactionally before publishing UI/notification events. Observe Room as the source of truth. Add a migration and schema export. Restore asynchronously without overwriting results recorded during restoration. Surface persistence failures appropriately. Keep the event bus for transient UI updates only.

**Acceptance:** evidence and action remain identical after restart; scans are retained when a subscriber is unavailable; a burst of inputs does not silently lose completed results.

### P0: background services and protection status

`SentinelApp` starts normal services from application startup. Guard/monitor services contain placeholders, and the guard never enters foreground mode. The UI uses service presence and permission snapshots rather than actual detector readiness.

Prefer the system-bound NotificationListenerService plus an application-scope result handler with direct persistence. Remove unused always-running services. If a foreground service is justified, implement its supported type, visible notification, launch rules, and lifecycle correctly. Catching a start exception alone is insufficient because it can leave protection disabled while the UI suggests otherwise.

Add readiness states: enabled, listening, listener disconnected, warnings disabled, model unavailable, optional feed stale. Treat contact/overlay access as optional.

**Acceptance:** background notification scans work on the chosen device after leaving the app; revoked permissions/disconnected listener produce accurate status; force-stop is described as stopping protection until user relaunch.

### P0/P1: routing, scoring, and explanations

When click protection is disabled, `IntentRouterActivity` finishes without forwarding a received link. Plain shared messages are rejected, and selected text is reduced to an extracted URL. SEND_MULTIPLE accepts only the first supported item. The manual scanner uses mutable input for browser handoff instead of the analyzed immutable target.

Fix disabled-link forwarding with recursion-safe browser selection. Add TextPayload and shared-message support. Either process all supported shared items with limits or explicitly reject unsupported batches. Bind displayed/opened targets to the completed result.

The URL ML blend is `0.7 * evidenceScore + 0.3 * min(2 * modelPercent, 100)`. Risk level/action are prevented from downgrading, but the numerical score can decrease below the preserved risk band. Model-related summary/reasons may differ from the legacy explanation field. Message scores use different thresholds, substring keywords, and overlapping evidence; benign “never share your OTP” can accumulate scam points.

Centralize decision policy and explanations. Keep model probability separate from the fused risk index. Use evaluated thresholds, explicit high-risk floors, and UNKNOWN/incomplete-analysis status. Deduplicate correlated signals. Fix detail chips that iterate `indicator` but render only the risk-level chip without its text. Give messages/files suitable headlines instead of link-specific defaults.

**Acceptance:** the same content yields the same decision across entry points; scores/actions/explanations agree; benign OTP safety advice and ordinary transaction notifications are included in regression tests.

## 4. Target architecture

```text
Manual link/text/file, Share, selected text, supported notifications
    -> typed, size-bounded ScanInput with source and coverage metadata
    -> normalization + URL extraction
    -> local detectors
         URL: rules + URL TFLite + local reputation snapshot
         Message: scam rules + text classifier + embedded URL analysis
         Content: supported file checks / optional OCR / optional QR decode
    -> shared evidence + decision policy
    -> complete AnalysisResult persisted to Room
    -> foreground result / warning notification / history

Optional generic feed updates -> validated local snapshot
                                (outside the scan request path)
```

Keep contracts and evidence types in `core`; notification capture/adapters in `agents` and `app`; runtime model bindings and scan implementation in `app`; maintenance jobs in `services`; user flows in `ui`. Do not make `agents` depend on `app`. Dependencies can use a core analysis interface implemented and bound by `app`.

Extend `ScanRepository` with `scanText` or migrate to `analyze(ScanInput)` through adapters. Extend the result with content type, threat category, structured reasons, analyzed target, model status/version, coverage limitations, duration, intelligence version, and suggested user actions. Distinguish a risk index from a calibrated probability.

## 5. Feature specification and priorities

| Priority | Feature | Implementation | Demonstration acceptance |
| --- | --- | --- | --- |
| P0 | Reliable URL detector | Existing rules/model, consistent policy, cached local reputation, strict local mode | Safe and adversarial URLs produce explainable results offline |
| P0 | Genuine message scanner | Multiline input, `scanText`, TextPayload, full message plus embedded URLs | A scam without any URL is detected; benign advice stays low risk |
| P0 | Real-time warning | Shared analyzer from notifications; direct persistence; dedup/rate control | Supported incoming notification triggers a warning with reasons |
| P0 | Privacy controls | Content logging removal, backup exclusions, offline variant, minimal permission onboarding | Local variant cannot access network; core scans need no account |
| P0 | Complete local history | Store full results, Room observation/migration, delete controls | Original evidence survives restart; user can delete records |
| P0 | Honest demo mode | Synthetic reserved-domain examples use the real local pipeline | Mock verdicts cannot affect ordinary scans |
| P1 | On-device message AI | Compact quantized classifier and bundled tokenizer/vocabulary | Rephrased held-out scams detected; inference runs offline |
| P1 | Scam categories | Credential theft, bank/KYC, delivery/refund, payment/UPI, job/loan, prize, impersonation/coercion | Each supported category has evidence and relevant advice |
| P1 | Stronger URL checks | Public-suffix-aware domain parsing, IDN/lookalike handling, local brand dictionary | `brand.com.evil.example` is understood; benign IDNs are not automatically malicious |
| P1 | Better file checks | System picker; size-bounded header/MIME/extension checks; APK metadata and limited archive checks | Fake document with APK content is flagged without executing it |
| P1 | Screenshot scanning | Bundled offline OCR; extracted text/links use common analyzer | First-use screenshot scan works in airplane mode |
| P1 | QR scanning | Decode user-selected QR image offline; analyze URL/payment text before navigation | QR destination shown and checked before any opening |
| P1 | Privacy/history settings | Per-app scanning, pause, retention, delete all, local false-positive feedback | Preferences actually change behavior and persist |
| P1 | Demo diagnostics | Non-sensitive latency/model/coverage fields; separate synthetic input injector | Judges can see measured processing and offline status |
| P2 | Language expansion | Evaluate English/Hindi/Hinglish first; add other languages with data | Show per-language measured results, not blanket multilingual claims |
| P2 | Calls / deeper attachments | Phone reputation or permitted call-screening; broader supported parsing | Separate scope and platform validation required |

If time is short, prioritize a reliable message/link experience over screenshot/QR/file breadth. Do not present rules-only message analysis as a trained semantic AI model.

### Message intelligence

Reuse and improve `ScamRuleEngine` first: normalize Unicode/spacing, use word boundaries and phrase relationships, recognize negative/safety advice, and require combinations such as authority + pressure + request for credentials/payment. Unknown contact status is supporting evidence, never proof. Missing contact permission means unknown, not confirmed stranger. Sender display names cannot authenticate a person.

Add a compact model trained for benign/scam text with relevant scam categories. Begin with a small quantized classifier; choose a larger encoder only if the measured accuracy justifies its size and latency. Bundle preprocessing assets. Evaluate English/Hindi/Hinglish only where sufficient labeled data exists. Keep the URL model for URLs rather than applying its numeric features to message text.

Run every bounded, distinct embedded URL through the shared local URL detector. One ordinary URL must not cancel a dangerous URL or credential request. Mark truncated/hidden content as partial coverage. Parse Android MessagingStyle message arrays where available instead of relying solely on title/text/bigText. Correct schema channels for SMS and generic apps instead of labeling them as WhatsApp.

### Files, OCR, and QR

Replace typed “file paths” with a system document picker. Query actual display names and read only granted content URIs; validate MIME claims against file signatures. Add limits for input size, nesting, expansion ratio, processing time, and item count before archive inspection. Do not execute scripts/APKs or render untrusted active content.

A clean extension or header cannot establish that a file is malware-free. Label the scope, for example “file type and suspicious-name checks.” Broader malware detection requires additional engines and evaluation.

OCR/QR dependencies must be packaged for first-use offline operation. User-selected screenshots should be processed transiently and not copied into permanent history by default. A payment QR shows payee/amount risk signals; it does not certify the recipient.

## 6. UI updates

| Screen | Changes |
| --- | --- |
| Onboarding | Manual scanning works immediately; notification access and warning permission requested for real-time mode; explain supported coverage |
| Dashboard | Actual listener/model readiness, local-only status, last scan, measured scan duration, separate scans/warnings/blocked handoffs counters |
| Scanner | Real Message/Link/File tabs; optional Screenshot/QR; paste button; file picker; input limits; cancellation; errors distinct from low risk |
| Result | Verdict, category, readable reasons, highlighted phrases/domain deception, practical next step, coverage and model status; BLOCK has no open action |
| Alerts | Source app, redacted sender, concise reason, open details; rate limits and deduplication; private lock-screen preview |
| History | Search/filter, full evidence, delete individual/all, retention; synthetic demo records clearly labeled |
| Settings | Per-app notification scanning, pause/resume, strict local mode, optional generic feed update, retention and privacy controls |
| About/demo | Accurate feature coverage, model/version, benchmark summary, limitations, synthetic sample buttons |

For suspicious messages, “do not share your OTP” or “verify using the bank’s official app” is more useful than a generic percentage. The current app cannot block a WhatsApp contact or remove a message; expose guidance and navigation only where supported, and label actions honestly.

## 7. Model and quality evidence

`docs/ml-model.md` acknowledges absent training data/scripts/evaluation, but also includes unsupported dataset-size/effectiveness claims and example predictions. Replace those with a versioned model card and reproducible measurements.

Required artifacts:

- Dataset provenance/licensing, labels, and class/language counts; no real private messages collected for the demo.
- Training/export scripts and preprocessing parity checks against Android.
- Domain/campaign/time-separated URL splits and template/source-separated message splits to reduce leakage.
- Model/scaler/tokenizer versions and hashes, float/quantized model comparison.
- Precision, recall, F1, false-positive rate, confusion matrix, and per-category/language results.
- Rules-only, model-only, and combined-system comparison on held-out examples.
- Device, OS, model size, warm/cold latency, median/p95 timings, and peak memory measurements.

Use a small synthetic regression corpus to verify product behavior, separately from a held-out evaluation corpus used for accuracy claims. Local feedback may improve later labels but should not retrain the running detector automatically.

## 8. Implementation sequence

Effort estimates assume one developer familiar with this code, SDK/device access, and usable training data. They are planning ranges, not delivery guarantees. Model/data work is the largest uncertainty.

| Phase | Tasks and principal files | Exit condition | Indicative effort |
| --- | --- | --- | --- |
| 0: baseline | Fix SDK setup and wrapper line endings; record existing tests/build; review existing critical-bug plan against actual code | Reproducible debug APK and baseline checks | Half day |
| 1: reliability/privacy | Result action policy; disabled-link fallback; local-first orchestration; no scan-time network; remove logging; backup exclusions; direct durable persistence; correct status/lifecycle | Offline URL flow and restart evidence checks pass | 1–2 days |
| 2: messages | Core text contract; TextPayload; scanner/share/selection flows; stronger rules; embedded URL analyzer; accurate app/channel mapping | Text scams and benign controls work across entry points | 1–2 days |
| 3: AI/evaluation | Version/model card for URL model; compact text model; Android preprocessing parity; thresholds and regression corpus | Native local inference and held-out evidence available | 2–4+ days |
| 4: demo/UI | Unified explanations, category actions, history controls, privacy page, synthetic examples, warning rehearsal | End-to-end presentation reliable on target phone | 1 day |
| 5: optional breadth | OCR/QR first, then bounded file inspection and multilingual expansion | Each added feature passes first-use offline and robustness checks | 1–3+ days |

Key modification map:

- `core/.../data/ScanRepository.kt`, `core/.../model/ScanResult.kt`: typed message/content contracts and complete results.
- `app/.../protection/intent/model/IntentPayload.kt`, `IntentRouterActivity.kt`, `TextSelectionProcessActivity.kt`, `ScanLoadingActivity.kt`: text routing and correct handoff.
- `app/.../protection/intent/IntentScanRepository.kt`, `IntentThreatAnalyzerImpl.kt`, reputation package: unified local analysis and explicit network policy.
- `agents/.../whatsapp/ScamRuleEngine.kt`, `NotificationAgentCoordinator.kt`, `NotificationEventBuilder.kt`, `NotificationParser.kt`: shared analysis, capture quality, categories, dedup, channels.
- `app/.../ml/`: model lifecycle, CPU dispatch, text classifier, preprocessing assets, degraded status.
- `core/.../data/local/`, `core/.../event/ThreatJournal.kt`: complete persisted result, migrations, retention/deletion and observable queries.
- `app/SentinelApp.kt`, `app/.../warning/`, `services/`, `ui/.../protection/ProtectionControl.kt`: reliable result handling, readiness and supported background lifecycle.
- `ui/.../screens/scanner/`, dashboard/history/settings/permissions/threat screens: complete visible product flows.
- App manifest and backup resources: minimum permissions, backup exclusions, local build manifest.
- `docs/`: measured capabilities, reproducible setup, privacy boundaries, model card and demo script.

### If only 48 hours remain

Deliver phases 0–2 at reduced scope, plus demo rehearsal. Keep the existing local URL model, improved message rules, supported notification warnings, strict local mode, full evidence history, and consistent result actions. Omit new text-model training, OCR/QR and deeper file inspection unless already validated. Clearly explain that URL AI is trained-model inference and message detection is currently rules-based.

### If roughly one week remains

Complete the reliability and message core, establish model evaluation, add a compact message classifier if data permits, and choose one polished extension: screenshot or QR scanning. Reserve the final day for device testing and presentation rehearsal.

## 9. Definition of working

```text
./gradlew :core:testDebugUnitTest :agents:testDebugUnitTest \
  :app:testDebugUnitTest :ui:testDebugUnitTest :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Run connected tests only with an available configured device/emulator. Add behavior tests for the changed contracts, not screenshots of implementation details.

Required scenarios:

- Benign official URL, lookalike/subdomain deception, IP/userinfo/encoded URL, malformed input, short link with unresolved destination.
- Credential request, KYC pressure, payment/refund, job/prize scams, and a no-link scam.
- Benign OTP safety advice, expected bank/payment updates, and legitimate urgent communication.
- Multiple URLs, mixed-case/Unicode text, oversized/truncated content, repeated critical notifications, contact access unavailable.
- Manual, share, selected text, and notification paths produce compatible results for equivalent content.
- BLOCK cannot open; WARN confirmation works; ALLOW opens the analyzed target; disabling click protection forwards normally without looping.
- Airplane mode on first use, unavailable/stale optional feeds, failed model initialization, cancellation and denied file access.
- Process restart preserves complete evidence; listener reconnect and screen-off work within platform limits; no false “protected” state.
- Zero sensitive logs/traffic, backup exclusions, history deletion, no secret API keys in APK.
- Native models run on the target device; synthetic samples alone are insufficient to prove detection accuracy.

Proposed performance goals, to be measured: warm local URL/text analysis p95 below 200 ms; notification receipt-to-warning p95 below 500 ms on the chosen demo phone, excluding OS delivery delay; cold model initialization reported separately; OCR measured separately. Adjust targets based on real hardware and model size.

## 10. Three-to-five-minute presentation

1. Show the local-only build and turn on airplane mode. State the threat problem and privacy approach.
2. Scan a benign URL, then a synthetic phishing URL on a reserved `.example` domain. Explain specific domain/credential signals; do not visit a malicious site.
3. Paste a no-link scam message. Show the category, risky phrases, and recommended action. Compare with benign OTP safety advice to demonstrate false-positive handling.
4. Show a supported incoming-notification warning using controlled test accounts, or a clearly labeled developer injector that feeds the same analyzer. An injected event demonstrates the detector, not real OS capture; rehearse OS capture separately.
5. Restart and show preserved evidence in history, then show deletion/privacy controls and measured scan time.
6. If validated, show one screenshot or QR scan. Finish with measured results and accurate coverage limits.

Use the actual detector for presentation examples. Move `MockReputationProvider` out of normal runtime bindings into test/developer-only configuration. A hard-coded malicious fixture is not evidence that AI detected phishing.

## 11. Platform constraints and sources

- A generic HTTP/HTTPS intent filter cannot guarantee interception of every link on Android 12+. Verified link/default-browser handling and in-app navigation affect coverage. Make sharing/selected text the dependable core; treat browser interception as a separately tested integration. [Android deep-link documentation](https://developer.android.com/training/app-links/create-deeplinks).
- Notification access exposes available notification content, not full chats or all app content. Android 15 redacts OTP-bearing notifications from untrusted notification listeners; handle hidden/truncated content honestly. [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService), [Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-all).
- Background execution has platform restrictions; selecting a foreground service requires a valid lifecycle and launch path. [Background service limits](https://developer.android.com/about/versions/oreo/background), [Foreground-service start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).
- User-selected documents can be accessed using the Storage Access Framework rather than broad storage permission. [Android document access](https://developer.android.com/training/data-storage/shared/documents-files).
- App-private storage does not alone establish a no-cloud guarantee; configure backup exclusions. [Android Auto Backup](https://developer.android.com/identity/data/autobackup).
- TFLite introduces native dependencies; verify compatibility on the selected phone, including 16 KB page-size environments if applicable. [Android page-size guidance](https://developer.android.com/guide/practices/page-sizes).

Keep compile/target SDK changes separate from the first reliability fixes, then test against the actual presentation OS. There is no requirement to introduce a backend, login, cloud LLM, or universal VPN interceptor for this hackathon scope.

## SafeX AI 1.2.0 language and identity update

The app now includes English/Hindi/Gujarati interface selection, persistent 85–150% text scaling, translated warnings and guidance, native-script message rules, bundled offline Hindi/Gujarati OCR, and an original shield/X logo with a teal/navy UI. The internal application ID remains stable to preserve data. See [language/branding details](languages-and-branding.md) and [current validation](validation.md) for implementation and measured checks. Independent real-world accuracy evaluation remains future work.


## SafeX AI 1.3.0 fraud-link update

The subsequent research and implementation add 26-rule URL coverage, canonical numeric host handling, improved brand/redirect checks, explicit incomplete-message warnings, reproducible PhiUSIIL model training with synthetic robustness variants, and limited-context model abstention. The pattern inventory, measured results and remaining gaps are maintained in [fraud-link audit](fraud-link-audit.md). Earlier release checks above are historical snapshots.
