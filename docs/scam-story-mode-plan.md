# SafeX AI — Scam Story Mode

Original design proposal, prepared 10 October 2026 for the final hackathon round. The Android 1.9.0 implementation and tested scope are documented in [Scam Story Mode](scam-story-mode.md); proposals below are design context, not additional shipped claims.

## Recommendation

Add one feature that connects multiple pieces of a suspicious interaction into a private, explainable timeline. A user can combine messages, screenshots, links and payment QR contents in one case. SafeX AI then explains the relationship between the evidence: what was promised, what was requested later, which details changed, and which actions need independent verification.

Pitch: **“SafeX AI connects the clues in a scam, explains the pattern, and helps you act — on your device.”**

This is a proposed product direction, not a claim that the approach is unique worldwide or that it guarantees a competition result.

## Why it fits this project

The current application already has private text/link/QR analysis, local OCR, floating capture, payment comparisons, context questions and incident help. Its results primarily review individual inputs. The new work is a case controller and correlation engine that preserve relationships across several inputs.

Reuse `ScanRepository.analyzeTextPrivately`, `analyzeLinkPrivately` and `analyzeQrPrivately` for item analysis. Reuse the consent/crop/OCR flow in `FloatingSessionController` and the existing incident-help routes. Keep user answers distinct from detector evidence, as the current `ContextReview` already does.

The practical need is supported by official examples. The FTC describes task scams that progress from unexpected work offers to supposed earnings and then demands to deposit money to access those earnings. Its February 2026 guidance also highlights pressure and upfront payments in side-hustle scams. These sources support the scenario design, not a claim that our proposed detector is already accurate. [FTC task-scam guidance](https://consumer.ftc.gov/consumer-alerts/2024/11/task-scams-create-illusion-making-money), [FTC side-hustle guidance](https://consumer.ftc.gov/consumer-alerts/2026/02/how-avoid-side-hustle-scam).

RBI describes impersonators using fictitious offers, account-freezing threats, secret-information requests and unverified installation links. These support a second group of case patterns. [RBI impersonation warning](https://www.rbi.org.in/Scripts/BS_PressReleaseDisplay.aspx?prid=58595).

## User experience

1. Choose **Review the whole situation** from Home or a scan result. Create an untitled private case; no registration is needed.
2. Add a message, link, screenshot or QR content. A floating review offers **Add to current case** only after the user has reviewed its extracted content.
3. Review the extraction before adding it. Show its source, whether OCR was edited, and an optional user-confirmed order/time. Import time must not masquerade as the time the sender sent the message.
4. Add another item. Update the case summary and explain what changed. Existing completed items do not need to be scanned again unless edited or the analysis version changes.
5. Read the evidence timeline. Tap a finding to see the exact items and passages supporting it.
6. Choose **Verify independently**, **Help after a scam**, **Save case** or **Delete case**. Calling, opening a website and sharing an export remain deliberate actions.

Use the existing teal/navy SafeX theme, shield/X logo, English/Hindi/Gujarati translations and 85–150% reading settings. Provide a short first-use guide for the new feature.

The main screen should contain a concise status card, a vertical timeline and fixed **Add evidence** and **What should I do?** actions. Use source icons and text labels together. Each connection needs a plain-language explanation; color alone must not carry the meaning.

Recommended labels are **More context needed**, **Review this request**, and **High concern**. A low-concern result must not read “verified safe.” Do not present an invented probability such as “98% scam.”

## The distinctive interaction

Show the developing story as an evidence-backed explanation:

> “An earlier message promised earnings. A later message requires you to deposit money to withdraw them. This combination matches a task-scam pattern.”

Under it, display links to the two supporting items. If a QR is included, show its decoded recipient and amount as unverified details. A matching payment address does not establish who owns the account.

An optional **What to watch for next** card can describe a common next step from the matched playbook, such as another deposit request. Label it “Common in this pattern”; it is guidance, not a prediction about this particular sender.

Some final requests will already trigger the existing single-item detector. The additional value is the connected explanation, earlier context where supported, and a coherent record. Any claimed detection improvement needs a comparison test.

## Initial pattern coverage

Start with three bounded playbooks:

| Pattern | Evidence to connect | User-facing action |
| --- | --- | --- |
| Task/job advance-fee scam | Work offer or promised earnings, followed by a fee/deposit to receive work or release earnings | Pause the payment and verify the employer independently |
| Bank/government impersonation | Claimed authority, followed by an account/legal threat and a request for a secret, transfer or installation | Use the institution's independently obtained contact route |
| Fake support/remote access | Claimed support problem, followed by installation/remote-control instructions and a payment or secret request | Stop granting access and use official support |

A normal job offer, ordinary payment QR, urgency, or a different payment-provider domain is insufficient by itself to assert a scam. Legitimate examples must be included in evaluation.

## Analysis design

Use the existing on-device models for each item and add a small local sequence engine. Do not depend on a cloud chatbot or introduce a large language model solely for this feature.

Proposed records:

- `StoryCase`: ID, title, ordered item IDs, creation time and optional save/expiry state.
- `StoryItem`: source, reviewed text or decoded payload, provenance, item result, import time and optional user-confirmed event order.
- `StorySignal`: typed observation such as promised earnings or a secret request, its supporting item/span, and whether it was detected or user-reported.
- `StoryFinding`: playbook ID, supporting signal IDs, evidence strength, explanation key and recommended next action.

Processing sequence: review input, run the current private detector, extract supported signals, correlate distinct items, apply the playbooks, and render translated explanations.

Rules for reliable behavior:

- Every finding must point to submitted evidence. Missing steps remain unknown.
- Keep per-item detector results visible. Case analysis must not erase an existing high-risk finding.
- Do not inflate concern by importing the same screenshot/message repeatedly; deduplicate while retaining visible provenance.
- Common words, shared brand names or proximity in time must not automatically join different cases. The user selects the case.
- Recognize negation, quoted scam examples and safety advice before treating a phrase as a live request.
- Separate user-reported context from extracted facts. Neither identifies a sender as a verified criminal.
- Recompute findings when an item is corrected, removed or reordered. Evidence withdrawn by the user must not remain in an explanation.
- Handle low-quality OCR, unsupported QR payloads and missing payment amounts explicitly.
- Cancel stale work when the user changes or deletes a case. A late result must not restore discarded private content.

For the first build, cap a case at eight items and 48,000 total reviewed text characters, within existing per-input limits. Process images sequentially using existing image limits, and release pixels after extraction. These are proposed resource bounds to verify, not measured performance claims.

## Privacy and saving

New cases are temporary and remain in app memory. Do not automatically collect chat history, read the clipboard, upload content or request broader permissions. The user can add shared text, an explicitly selected notification result or a consented capture.

Protect sensitive review screens and lock-time handling consistently with the existing floating assistant. Drafts expire after an explicit idle period. Closing/deleting a draft clears its content; already saved cases follow their selected retention.

Explicit **Save case** should persist a minimal encrypted local snapshot. Keep its key in Android Keystore and perform cryptographic work away from the UI thread. Do not claim hardware protection unless the device actually provides it. [Android Keystore documentation](https://developer.android.com/privacy-and-security/keystore).

Do not ship persistent cases until save/reopen/delete, key failure, corruption and retention behavior are verified. A demo MVP can safely use temporary cases and explain that closing discards them.

An optional export should show a redacted preview before Android sharing. It is a user-prepared summary, not a submitted complaint, certified evidence or proof of payment. Keep export outside the first MVP if time is short.

## Implementation order

| Stage | Deliverable | Completion check |
| --- | --- | --- |
| 1. Case engine | Typed case/items/signals, three playbooks, private detector reuse | Correct findings, provenance, duplicate handling and removal on authored positive and benign stories |
| 2. Android experience | Home/result entry, reviewed input imports, timeline, explanation detail and urgent help | A full case works in airplane mode through the actual app controls |
| 3. Floating and language integration | Explicit add-to-case handoff, first-use guide and translated layouts | All three languages work at 150%; consent and private draft behavior remain intact |
| 4. Evaluation and rehearsal | Held-out scenario comparison, regression checks and S24/A36 rehearsal | Publish measured results and remaining limits; demonstrate without live scam links or payments |
| 5. Optional continuation | Encrypted saved cases, redacted export and Chrome case mode | Validate each addition separately after the Android MVP is stable |

Planning allowance: roughly two focused development days for a polished temporary-case Android MVP, plus device rehearsal and debugging. This is an estimate; OCR/extraction quality and integration issues may extend it. Prioritize one complete working story over shipping several unfinished additions. Chrome integration and cross-device transfer are later work.

Likely new files: `core/model/StoryCase.kt`, `core/story/StoryAnalyzer.kt`, `core/story/StoryPlaybooks.kt`, and `ui/screens/story/StoryScreen.kt` with its ViewModel. Extend navigation, Home/results, the floating handoff, canonical localization and guidance. Keep the existing private-scan contracts and update their callers carefully.

## Verification and release gate

Prepare development stories for tuning, then a separate labeled set covering fraud and legitimate interactions in all three languages. Keep translations and paraphrases of the same underlying story in the same split; they must not become supposedly independent evaluation examples.

Compare the existing single-item baseline with case analysis on identical inputs. Report:

- Scam stories detected and missed, and warnings on legitimate stories.
- The first stage at which each detector raised the correct warning.
- Unsupported or incorrect evidence links, and missing/failed extraction separately.
- Text-only correlation timing separately from OCR, capture and full user-flow time.

For the initial demonstration, prepare at least ten fraud and ten legitimate base stories and multilingual variants. Treat them as authored functional fixtures. Freeze a separate evaluation set before further tuning; do not call a small authored set real-world accuracy.

Meaningful regression checks include wrong-case mixing, duplicate evidence, safety advice, negation, reordered stages, OCR edits, cancelled imports, mid-analysis deletion, app recreation, idle expiry and secret redaction. Existing scanner, floating-capture, guide and incident-help tests must continue to pass.

Physical S24/A36 rehearsal must cover large fonts, camera/import, background/lock behavior, memory use, airplane mode and a cancelled help-dialer action. Do not place a test emergency call.

## Jury demonstration — approximately 90 seconds

Use an explicitly labeled synthetic job/task scenario, provided in English, Hindi and Gujarati:

1. Show airplane mode and create a temporary case.
2. Add an authored ordinary introduction: “Would you like details about evening work?” Show that insufficient evidence is not a safety certification.
3. Add a synthetic screenshot showing a claim of earned commission. The app records the claim without treating it as verified income.
4. Add the request: “Deposit ₹500 to unlock your ₹800 earnings.” Display the linked promise/payment finding and its supporting passages.
5. Optionally add a non-payable QR fixture containing `https://task-payment.example/deposit?amount=500`; decode it without opening it. Explain that it shows a proposed destination, not a completed transfer.
6. Tap the finding, switch to Gujarati at 150%, and open the relevant help checklist.
7. Show a legitimate comparison case, then delete both cases. Keep official calling actions cancelled.

The comparison and deletion demonstrate judgment and privacy alongside the visual explanation. Use the actual detector and correlation engine; do not hard-code verdicts by fixture name or pre-render a result as though analysis ran.

Jury closing line: **“A scam can unfold across several messages and images. SafeX AI keeps those clues together and shows exactly why the situation needs caution.”**
