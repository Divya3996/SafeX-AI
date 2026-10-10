# Features and coverage

| Feature | Working behavior | Scope |
| --- | --- | --- |
| Message check | Credential requests, authority impersonation, urgency, reward scams, job / loan fees, UPI and coercion signals plus local text classifier | English/Hindi/Gujarati rules; historical observed English research model with authored multilingual augmentation |
| Link check | 26 rules, native URL classifier, imported threat snapshot | URL structure only; no web page download or redirect resolution |
| Notification check | Supported messaging notification text uses the shared analyzer, deduplication and private warnings | Opt-in Android notification access; newest visible message; per-app switches |
| Screenshot | Bundled Latin, Hindi and Gujarati OCR then message / link check | Clear readable screenshots; images not saved |
| QR image / camera | Bundled QR decoding then content check, including payment caution | Select an image or use the optional foreground camera scanner; nothing opens automatically |
| Floating assistant | Movable shield, one-frame authorized capture, crop, editable OCR, text/link/QR selection and private results | Optional overlay permission; fresh consent each capture; no hidden link targets or protected-screen bypass |
| Scam Story Mode | Reviewed evidence timeline, three local sequence playbooks, linked original passages, editing/reordering, encrypted snapshots and redacted exports | User-grouped situation; 8 items; bounded English/Hindi/Gujarati rules; concern is not a fraud probability |
| File | Metadata, PDF / executable signatures, APK disguise, unsafe archive paths and script entries | 10 MB input; 100 entries; 2 MB per entry / 8 MB expansion; explicitly partial at limits |
| History | Full saved results, reasons, search, warning filter, deletion | Private Room database; latest 1,000 records; configurable 7 / 30 / 90-day retention |
| UI | Dark / neon / system theme, shared result UI, accessible controls, optional setup | No permission needed for manual text and link checks |

Notifications can be hidden, truncated, redacted or suppressed by Android or the originating app. Android 15 can redact sensitive OTP notifications. See [Android’s platform behavior](https://developer.android.com/about/versions/15/behavior-changes-all#otp-redaction). SafeX AI does not read entire conversations, intercept every browser request, inspect call audio, continuously monitor the clipboard, or block other apps at the network layer. File checks are not a malware antivirus.

Share content to SafeX AI or use selected text when an app does not expose readable notifications. Web links route through SafeX AI only when Android sends the intent to it; verified app links and internal browser navigation can bypass it.

Language selection, translated guidance/notifications and a persistent 85–150% text-size control are available in setup and Settings. See [languages and brand](languages-and-branding.md).

## SafeX AI 1.4.0 additions

Live camera QR scanning, detailed UPI instruction review, actual-domain explanations, saved contextual reviews, incident-help checklists and a warning notification tune are implemented. See [practical features, controls and limitations](practical-features.md).

## SafeX AI 1.5.0 additions

An optional floating shield launches screen cropping, selected-content review, Paste and screenshot import. Floating results and context edits remain private until explicit Save. See [floating assistant behavior, privacy and limits](floating-assistant.md).

## SafeX AI 1.7.0 research update

The message and URL classifiers are retrained from licensed sources with deduplication, campaign/domain grouping, validation-selected thresholds and explicit promotion gates. The app includes a higher-threshold vocabulary guard for learned English scam lures without explicit action requests, expanded native-script credential/payment cues and protective-advice handling. Twenty authored fraud categories are checked in English, Hindi and Gujarati. See [the dated research report](fraud-research-2026-10-09.md) for collected data, actual results and remaining coverage gaps.

## SafeX AI 1.9.0

Scam Story Mode connects related evidence on the device while retaining individual warnings. Screenshots and result handoffs prepare review drafts rather than automatically grouping content. Saved cases use authenticated encryption; a redacted export is previewed before sharing. The highlighted Android tour now contains 19 steps. See [usage, demo, privacy and tested scope](scam-story-mode.md).
