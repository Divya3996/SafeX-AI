# Privacy

Scans run on the device. The merged application explicitly removes INTERNET and ACCESS_NETWORK_STATE permissions, including permissions from dependencies. Runtime reputation uses only an on-device URL list. Dormant network provider implementations remain for their isolated unit tests and are not bound to the production scanner.

The user chooses shared content and files through Android. Notification access is optional; only supported, enabled apps are analyzed. The newest visible notification message is used. The assistant does not authenticate senders. Contact recognition does not lower threat severity.

Room stores scan content, explanations, evidence and timestamps privately. **Raw message text is retained in local history**, including screenshot-extracted text. Original screenshot / QR pixels and original files are not retained. Delete history in History or Settings; retention defaults to 30 days and also runs daily. Storage is bounded to the newest 1,000 records. Android private app storage protects access; no separate database encryption is claimed.

Cloud backup and device transfer are disabled for stored app data. Warning notifications use private visibility and a generic lock-screen public version. HTTP body logging is disabled and scan content is not logged by the active pipeline.

The optional demo sender is a separate app that posts synthetic local notifications. It has no network permission. It is recognized only in debug builds, and its results are labeled demo data.

## Floating assistant

The optional floating shortcut captures one authorized screen only after a fresh Android consent prompt. Authorized pixels are temporarily held in memory for cropping; selected pixels are processed by bundled OCR/QR models. Private results and contextual reviews are not stored until Save result. Back and operation cancellation retain the previous input where recovery is possible. Screen lock, explicit Close/Done and background expiry clear the private session. Activity recreation can resume the live RAM session; process death cannot restore it. Memory pressure releases pixels while retaining review text. No clipboard polling, accessibility access, screen-image files or cloud processing are used. See [the floating assistant documentation](floating-assistant-1.6.md) for bounds and platform limitations. Device-test screenshots contain authored fixtures and are separate from production behavior.

## Research model training

The 1.7 models were trained on the development machine using named licensed public datasets and authored examples. App history and users' private notifications are not collected for training. Source URLs are downloaded by development scripts; reported phishing destinations are not opened. Raw research collections stay in the ignored development cache and are excluded from the APK. Text model features mask numeric, email and URL identifiers. The installed app loads bundled parameters and runs inference locally, with no Internet permission or cloud training service. See [source provenance and licenses](../models/FRAUD-DATA-ATTRIBUTION.md).
