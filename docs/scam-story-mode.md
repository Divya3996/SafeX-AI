# SafeX AI — Scam Story Mode

Android 1.9.0, rule version 1. Updated 10 October 2026.

## What it does

**Review the whole situation** joins user-reviewed evidence into a private case. Each message, link or decoded QR uses the existing installed private detector. A small local sequence engine then connects supported requests across distinct items. It does not call a cloud chatbot, fetch webpages, identify criminals or assign a fraud probability.

Three playbooks connect work/earnings to an advance fee, a claimed bank/government identity to pressure and a demand, and a support claim to remote access and a money/secret request. Paid remote support alone receives a review finding; an accompanying secret request raises high concern. Different UPI addresses receive a separate caution, because different addresses can be legitimate and matching addresses do not prove ownership.

Every connection links to original reviewed passages. Individual detector warnings remain visible. A case without a matching pattern says **More context needed**, never “verified safe.” The engine recomputes after editing, removal or reordering. Submitted order is the user's chosen sequence; displayed timestamps mean import time.

## How to use it

1. From Home, choose **Review the whole situation**, or open **Scam Story Mode** from the drawer.
2. Paste a message/link/QR payload, choose a screenshot or QR image, or use the floating assistant. Image recognition prepares editable text or selectable QR content; it does not add evidence automatically.
3. Check that the content belongs to this situation, correct recognition errors, then press **Add reviewed evidence**. A result's **Review the whole situation** action and the floating result's **Add to a private case** action also prepare a review draft, preserving the original warning.
4. Add related evidence. Tap a connected finding to read its exact supporting passages and suggested next action. **View full evidence** retains the per-item detector explanation and unverified payment details.
5. Edit, remove or reorder the timeline as needed. Replacing pending input and deleting evidence require confirmation. Duplicates do not inflate concern.
6. Explicitly **Save encrypted case** to keep a snapshot, or start a new private case to discard the RAM draft. **Saved cases** opens or deletes snapshots. Deleting a saved copy and clearing an open draft are distinct actions.
7. **Preview redacted summary** shows exactly what would be shared. Sharing starts only after another explicit press. The summary excludes raw content, links, payment details, secrets, custom titles and case IDs.
8. **Help after a scam** opens the existing incident checklists and confirmed official-contact actions. A demonstration should cancel the dialer confirmation, never place a test emergency call.

English, Hindi and Gujarati menus, explanations, guides and examples follow the app's language setting. Reading size follows the existing 85–150% preference. Evidence is displayed as entered rather than translated.

## Jury demonstration

Choose **Task-scam example** and add each of its three messages in order. Use **Next example message** only after adding the current one. Explain that individual checks and the connected finding use actual local code, while the input is a clearly marked synthetic example. Open the task finding and point to the two supporting passages. Move the fee before the work promise, then explain why the ordered finding is withdrawn; individual warnings still remain.

Start a new case and choose **Legitimate comparison**. The job interview, no-fee statement and protective advice should produce no invented sequence. Import an authored QR or screenshot, demonstrate the review step, and save/reopen an encrypted snapshot. Preview the redacted export. Keep airplane mode on to demonstrate local analysis; do not open an example link or make a payment.

The claim supported by this demonstration is **connected, evidence-backed explanation on the device**. It does not establish that every scam is detected or that adding the sequence engine improves independent accuracy.

For a second authored demonstration, start a fresh case and add these messages separately:

1. `Your commission balance is ₹5000.`
2. `Transfer ₹1000 to release the balance.`

The `withdrawal-en` fixture records the individual decisions and the connected finding separately. In this development example, the existing individual checks raise no warning, while the sequence engine connects the promised balance to the later release payment. Show the supporting passages, then remove the payment request and show the finding disappear. This illustrates the added context in one controlled example; it is not a general accuracy claim.

## Privacy and recovery

Temporary drafts live in the application-owned RAM controller, not `SavedStateHandle`, scan history or Intent text extras. Rotation/recreation can retain the same in-process draft. Locking the screen clears it; leaving this destination for two minutes clears it too. Process termination loses unsaved drafts. Detail, rename, confirmation and export dialogs clear when leaving the activity or replacing the case. The story destination uses Android's secure window flag.

Saved snapshots are AES-256-GCM authenticated ciphertext in the app's private no-backup directory. Android Keystore holds the key. File identity is authenticated so ciphertext cannot be swapped between case IDs. Atomic writes, schema/UUID validation, byte limits and a mutex protect persistence. Key availability or damaged files can prevent opening a case; unreadable snapshots are reported and can be deleted. No hardware-backed or biometric guarantee is asserted. [Android Keystore documentation](https://developer.android.com/privacy-and-security/keystore).

Cases follow the local history-retention setting when the vault is listed. Limits are eight items, 12,000 characters per item, 48,000 per case and 20 saved cases. Link input retains the scanner's 8,192-character limit. Image import uses bounded decoding and recognition. Cancellation, lock, expiry and the analysis watchdog invalidate the work generation; a late native result cannot restore discarded content.

## Verification and limits

The release passed **593 active unit tests and 73 active offline Android tests**, with zero lint errors (108 warnings). The highlighted tour has 19 tested targets. [Release checks and limitations](validation.md).

The authored 20-story fixture contains 10 suspicious sequences and 10 legitimate comparisons across English/Hindi/Gujarati. One legitimate paid-support comparison intentionally warrants review. Installed evaluation records per-item decisions separately from story findings, verifies evidence spans, measures elapsed time and confirms no scan-history writes. This fixture is a development contract, not an independent test set. See [fixture](../android-app/app/src/androidTest/assets/story-corpus.json), [release verification](test-results/story-release-summary.json) and [installed evaluation](test-results/story-evaluation.json).

Sequence rules cover the described playbooks and selected protective/quoted-language patterns. They can miss paraphrases, new schemes, cross-language transliteration and facts omitted from an image. OCR cannot reveal hidden HTML destinations. Offline scans cannot follow remote redirects or verify identities, UPI ownership, payments or recovery. Existing per-item warnings can occur on legitimate controls and are preserved; the evaluation reports them separately.

Samsung S24/A36, Android 16 and 16 KB runtime checks still require physical/device validation. Store release needs the owner's upload signing key. The installable development APK and unsigned release bundle are distinct deliverables.

Implementation: [controller](../android-app/core/src/main/java/com/sentinel/ai/core/story/StoryController.kt), [sequence engine](../android-app/core/src/main/java/com/sentinel/ai/core/story/StoryAnalyzer.kt), [encrypted vault](../android-app/core/src/main/java/com/sentinel/ai/core/story/StoryVault.kt), [screen](../android-app/ui/src/main/java/com/sentinel/ai/ui/screens/story/StoryScreen.kt). Original design: [plan](scam-story-mode-plan.md).
