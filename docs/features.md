# Features and coverage

| Feature | Working behavior | Scope |
| --- | --- | --- |
| Message check | Credential requests, authority impersonation, urgency, reward scams, job / loan fees, UPI and coercion signals plus local text classifier | English, Hindi and Gujarati rules; synthetic multilingual model prototype |
| Link check | 26 rules, native URL classifier, imported threat snapshot | URL structure only; no web page download or redirect resolution |
| Notification check | Supported messaging notification text uses the shared analyzer, deduplication and private warnings | Opt-in Android notification access; newest visible message; per-app switches |
| Screenshot | Bundled Latin, Hindi and Gujarati OCR then message / link check | Clear readable screenshots; images not saved |
| QR image | Bundled QR decoding then content check, including payment caution | Select an image; no live camera scanner |
| File | Metadata, PDF / executable signatures, APK disguise, unsafe archive paths and script entries | 10 MB input; 100 entries; 2 MB per entry / 8 MB expansion; explicitly partial at limits |
| History | Full saved results, reasons, search, warning filter, deletion | Private Room database; latest 1,000 records; configurable 7 / 30 / 90-day retention |
| UI | Dark / neon / system theme, shared result UI, accessible controls, optional setup | No permission needed for manual text and link checks |

Notifications can be hidden, truncated, redacted or suppressed by Android or the originating app. Android 15 can redact sensitive OTP notifications. See [Android’s platform behavior](https://developer.android.com/about/versions/15/behavior-changes-all#otp-redaction). SafeX AI does not read entire conversations, intercept every browser request, inspect call audio, continuously monitor the clipboard, or block other apps at the network layer. File checks are not a malware antivirus.

Share content to SafeX AI or use selected text when an app does not expose readable notifications. Web links route through SafeX AI only when Android sends the intent to it; verified app links and internal browser navigation can bypass it.

Language selection, translated guidance/notifications and a persistent 85–150% text-size control are available in setup and Settings. See [languages and brand](languages-and-branding.md).

## SafeX AI 1.4.0 additions

Live camera QR scanning, detailed UPI instruction review, actual-domain explanations, saved contextual reviews, incident-help checklists and a warning notification tune are implemented. See [practical features, controls and limitations](practical-features.md).
