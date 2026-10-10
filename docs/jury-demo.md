# SafeX AI public jury demo

Live website: https://safex-ai-jury-demo.cuddlyrobin1.chatgpt.site

Public access: judges can open the link without signing in.

## Start here

1. Open the website once online and wait for **Offline demo assets ready**. Download the fixture pack and jury guide PDF.
2. Prepare the installed Android 1.7.0 app on Samsung S24 and A36, and the Chrome 1.0.0 developer-install extension. The website includes the Chrome ZIP; your Android APK remains in `releases/SafeX-AI-1.7.0-debug.apk`.
3. Use **Start the 5-minute demo** for the eight-stop script. Each example includes actual Android/Chrome scan steps, copy/share/download controls and optional recorded findings.
4. Rehearse screen permission, crop/OCR review, QR import/camera, notification access and sound on both phones before the jury round. The stored Android tests used an emulator, not these Samsung devices.

## Included

- 271 fixtures: 120 authored English/Hindi/Gujarati messages, 134 URL regressions, 13 real QR images, three synthetic screenshot PNGs and one harmless disguised-PDF fixture.
- Search, type/family/language filters, paired ordinary controls and transparent differences between Android/browser outcomes.
- Eight timed presenter steps, setup checklist, six optional feature demonstrations, recovery instructions and jury questions.
- English/Hindi/Gujarati guide and adjustable text. Original source payloads and audit notes retain their original language.
- Actual DOM labs for a brand/form conflict, visible-link/destination mismatch, ordinary control and an explicit imported blocking list.
- A printable five-page PDF, Chrome extension ZIP, fixture ZIP/TXT/JSON, checksums and evidence reports.
- Local browser preferences and an offline asset cache. Downloading the website initially requires a connection; SafeX AI inference runs locally.

## Verification

21 website and production-extension lab checks passed in an isolated Chrome profile on Linux. All 13 fixture QR images decoded exactly. Final publication checks also verified complete guide translations, mobile layouts at 130% reading size, final offline PDF/report/QR downloads and asset sizes.

These are functional checks, not independent real-world detection accuracy. Recorded findings stay labelled, unsupported QR actions remain visible, and no fraud destination was opened or resolved.

Local website source: `jury-demo-website/`. This has its own Sites source repository and `.openai/hosting.json`; app and extension source files were preserved.
