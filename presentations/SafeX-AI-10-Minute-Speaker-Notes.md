# SafeX AI — Code Carnival 3.0 presenter guide

Team: **Doclock** · ID: **M87V**  
Leader: **Divya Gohil** · Member: **Krupa**  
Presentation: **10 minutes, nine template slides**  
Prepared: **10 October 2026**

Use Presenter View for the notes embedded in the PowerPoint. The organizer's instruction slide is excluded from the final deck.


## 1. CODE CARNIVAL 3.0

**00:00–00:35 · Divya Gohil · 35 seconds**

Hello, we are team Doclock, M87V. I am Divya Gohil, the team leader, and Krupa is my teammate. Our project is SafeX AI: an on-device security assistant for phishing links, scam messages and suspicious communications. We have a working Android app and Chrome extension. Our goal is to help people pause, understand the evidence and choose a safer next action, without uploading their private messages or screenshots for AI inference.

**Supporting details:** Team name and ID are user supplied. Names supplied: Divya Gohil (leader), Krupa (member). 'On-device AI Security' is a descriptive theme based on the selected statement, not a verified organizer track code. No invented surname, contact information, registration value or submission deadline is used.

**Sources:** User-provided team details; Supplied Code Carnival 3.0 template


## 2. PROBLEM STATEMENT & UNDERSTANDING

**00:35–01:30 · Divya Gohil · 55 seconds**

The problem happens at the moment someone is asked to trust a message. A message may imitate a bank, promise a reward, demand an urgent payment or ask for an OTP. Those requests arrive through different apps, links and QR codes. A user needs understandable help before acting. We also considered local languages, comfortable reading sizes and poor connectivity. Some scams spread across several messages: an ordinary-looking reward promise may become suspicious only when a later message demands money to release it. Our design therefore combines individual checks with explicit, user-reviewed context.

**Supporting details:** No prevalence, financial-loss or adoption statistic is invented. Intended users and usability benefits are design goals, not a completed field study.

**Sources:** docs/features.md; docs/mentor-meeting-plan.md (historical design context)


## 3. PROPOSED SOLUTION

**01:30–02:25 · Divya Gohil · 55 seconds**

SafeX AI combines compact trained classifiers with security rules. The Android app accepts deliberate inputs and optional readable notifications; the Chrome extension adds authorized page and link context. The output includes the warning, reasons, coverage limits and next steps. We do not call a cloud chatbot. For screenshots and QR codes, local recognition prepares content the user can review. Scam Story Mode adds an evidence timeline with exact supporting passages. The interface and guidance are available in English, Hindi and Gujarati, with 85 to 150 percent text scaling.

**Supporting details:** Android ALLOW/WARN/BLOCK describes the local decision/opening policy. A model alone can add a warning, not independently block. A risk index is not a calibrated probability. A missing pattern/list entry does not certify safety.

**Sources:** README.md; docs/architecture.md; docs/scam-story-mode.md


## 4. KEY FEATURES / FUNCTIONALITY

**02:25–03:55 · Divya Gohil · 90 seconds**

There are four feature groups. First, Android can check text, links, screenshots, QR images, live camera QR and supported file structures. Notification analysis and warning sound are optional. Second, the floating shield lets the user give screen-capture consent, crop an area, correct recognized text and analyze privately. It does not silently capture other apps. Third, Chrome checks selected text, real link destinations and authorized pages. Optional site protection adds bounded rescans and warnings on supported risky clicks and form submissions. Fourth, Scam Story Mode connects reviewed evidence, while tours and incident-help guides make the product easier to use. Android has a 19-step highlighted tour and Chrome has 12 steps. Show one selected-message scan and its reasons here; use the longer floating or browser demonstration only if time permits.

**Supporting details:** Android also has explicit share/selected-text input, UPI address/amount comparison, disclosed context questions, local searchable history with 7/30/90-day retention, warning-sound test, and country-aware incident guidance. File inspection is bounded structure checking, not antivirus. Chrome also has pointer crop, local OCR/QR, redacted reports/export, incorrect-result feedback, themes, keyboard support, warning sound and opt-in exact URL blocking from explicitly labeled local list entries. Scam Story Mode is currently Android-only. Neither product continuously reads the clipboard, authenticates callers or intercepts all communications.

**Sources:** docs/features.md; docs/practical-features.md; docs/onboarding-and-release.md; chrome-extension/README.md; docs/chrome-extension-release.md


## 5. HOW THE SOLUTION WILL WORK

**03:55–05:35 · Divya Gohil · 100 seconds**

Every input follows the same idea: choose content, extract and review it when necessary, analyze locally, then explain the result. Text uses a logistic classifier and request-context rules; URLs use a structural neural network and URL rules. Chrome adds visible link and form metadata while excluding field values. In Android Scam Story Mode, add the two authored messages 'Your commission balance is 5000 rupees' and 'Transfer 1000 rupees to release the balance'. The installed fixture records no individual warning for either message, but the supported sequence connects the balance promise to the payment demand. Open the finding to show its exact supporting passages. Remove the payment item and the connection disappears. That is one controlled development example, not a general accuracy claim. Keep airplane mode enabled and do not open a fraud link or make a payment.

**Supporting details:** The Android sequence engine has three bounded playbooks: task/release fee, claimed authority plus pressure/demand, and support plus remote access and sensitive requests. Cases accept up to eight reviewed items. Individual warnings are retained. Cases without a sequence match say more context is needed. Drafts live in app-owned RAM, clear on screen lock and expire after two minutes away. Explicit case snapshots use AES-256-GCM with an Android Keystore key; exports are previewed and exclude raw contents, URLs and payment details. Ordinary Android history and Chrome report storage do not have that case-vault encryption guarantee.

**Sources:** docs/architecture.md; docs/scam-story-mode.md; docs/test-results/story-evaluation.json; docs/privacy.md


## 6. TECHNOLOGY / TECH STACK

**05:35–06:35 · Krupa · 60 seconds**

The app uses Kotlin, Compose, Hilt and Room; Chrome uses TypeScript and Manifest V3. The message model is logistic regression with word, bigram and character-trigram features. Its 5606 prepared messages are split into 3910 training, 810 validation and 886 evaluation examples. Observed English data is supplemented with authored Hindi and Gujarati examples. The URL neural network has 15 inputs and layers of 24, 12 and one output, exported to TensorFlow Lite/LiteRT. Its sources include PhiUSIIL, authored variants and a bounded reported-link dataset. ML Kit and Tesseract provide bundled OCR/QR recognition. Models are trained on the development computer and packaged for local inference; the phone does not automatically retrain on users' messages. Android requests no Internet permission.

**Supporting details:** Text feature vocabulary: 13605 terms; shipped JSON about 462 KB. URL model: 15 -> 24 -> 12 -> 1, 4016-byte TFLite asset. The research text threshold is 0.545 with action context; a guarded familiar-Latin path uses 0.915. URL warning threshold is about 0.9001. These are internal uncalibrated scores, not fraud probabilities. Campaign grouping and domain separation reduce split overlap. Model loading checks hashes, feature order and finite values; failure retains local rules. Training and model artifacts were promoted in 1.7 and not retrained for Story Mode.

**Sources:** docs/ml-model.md; models/fraud-text-evaluation.json; models/fraud-url-evaluation.json; models/FRAUD-DATA-ATTRIBUTION.md; android-app/app/src/main/assets/text-model-card.json; android-app/app/src/main/assets/url-model-card.json


## 7. CURRENT PROGRESS / WORK DONE

**06:35–07:50 · Krupa · 75 seconds**

We have installable Android 1.9.0 and an unpacked Chrome 1.1.0 release. The final Android build passed 593 active unit tests and 73 active offline Android checks, with zero lint errors and 108 warnings. Its tests used Android 14 on an emulator, not our Samsung phones. Chrome's recorded validation passed 431 unit checks and 43 browser scenarios. These counts establish functional behavior. Separately, the Android 1.7 historical English development comparison detected 71 of 78 scams, with one warning among 769 legitimate messages; it still missed seven scams. The models are unchanged in 1.9. We do not present that result as universal accuracy or as a browser benchmark. Physical S24/A36 testing, fresh independent multilingual evaluation and publisher release work remain open.

**Supporting details:** Four optional unit research-export checks and one optional native research-input check were skipped; the 593/73 numbers count passed active checks. Twenty authored Story contracts matched, including ten suspicious and ten legitimate comparisons. One paid-support control intentionally gets a review connection, while existing per-item warnings remain. This is not 100 percent detection accuracy. Text model-only historical English recall was 76/78 (97.4 percent); full app recall was 71/78 (91.0 percent). URL model-only historical recall was 93.31 percent with 0.40 percent false warnings; new-domain reported-feed recall was 68.02 percent, with no matched current benign set. Hindi/Gujarati independent real-world accuracy is not established. Shown warning screenshots are existing prior-release captures, cropped to the relevant region, not a claim of new 1.9 screenshots.

**Sources:** docs/test-results/story-release-summary.json; docs/test-results/chrome-extension-summary.json; docs/test-results/fraud-native-evaluation.json; docs/validation.md; docs/screenshots/scam-result.png; docs/screenshots/chrome-extension-scan-warning.png


## 8. FUTURE PLAN / NEXT STEPS

**07:50–08:55 · Krupa · 65 seconds**

Our immediate goal is a reliable end-to-end rehearsal on both Samsung phones and desktop Chrome, including capture consent, OCR review, sound and permission recovery. After the hackathon we plan a consented pilot and fresh independently labeled scam and legitimate examples. We will measure recall and false warnings by language and scam category before adding broader sequence coverage. Longer-term work includes signed publisher-controlled model updates, wider compatibility, store releases and privacy-preserving education or institutional pilots. These are planned steps, not shipped capabilities or existing partnerships.

**Supporting details:** Physical S24/A36, Android 16/16 KB runtime, TalkBack/manual accessibility and physical speaker checks remain. Publisher signing and store reviews remain. No dates, pilot participants, clinical/financial outcomes, active partnerships or automatic retraining are promised. Fresh evaluation must be kept separate from implementation and tuning.

**Sources:** docs/validation.md; docs/chrome-extension-release.md; docs/ml-model.md


## 9. GITHUB REPOSITORY LINK

**08:55–10:00 · Krupa · 65 seconds**

Our public repository is github.com/Divya3996/SafeX-AI. The source documentation covers setup, features, privacy and validation. The QR code opens our public jury demonstration library with labeled messages, URLs, QR codes and screenshots. The public website has older versioned downloads, so use the separately supplied Android 1.9.0 APK and Chrome 1.1.0 ZIP for this presentation. Our goal is security guidance that people can understand, use offline and read in their own language. The floating assistant supports phone workflows, Chrome adds browser context, and Scam Story Mode connects the clues users deliberately review. The next step is independent multilingual evaluation and physical-device rehearsal. Thank you; we welcome your questions.

**Supporting details:** Public GitHub API verified private=false and an accessible README on 10 October 2026. Contributor listing contained Divya3996 only. Current 1.9 changes exist locally and are not pushed by this presentation task. The public jury website returned HTTP 200 and SafeX-branded HTML on 10 October. Its library has 271 development fixtures; do not claim these are independent tests. Do not use its older app/extension version references as current release references.

**Sources:** https://github.com/Divya3996/SafeX-AI; https://safex-ai-jury-demo.cuddlyrobin1.chatgpt.site; docs/jury-demo.md; README.md
