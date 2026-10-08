# Model card

## URL classifier

The original unverifiable URL model has been replaced by `url-phiusiil-augmented-v2`: a 15→24→12→1 neural network exported as native TensorFlow Lite. Its inputs come from the exact production Kotlin feature extractor. Training uses the [licensed UCI PhiUSIIL dataset](https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset) plus explicitly synthetic host/path/query variants. No HTML, title, dataset probability columns or remote reputation is used. See [data attribution](../models/URL-DATA-ATTRIBUTION.md).

Registrable domains, including private public-suffix tenants, do not overlap across train/validation/test splits. Synthetic variants stay with their source site's split. Means/scales use training data only, with inputs clipped to ±8 standard deviations. The checkpoint and model warning threshold use validation data only. [Training/evaluation report](../models/url-evaluation.json) records counts, threshold, metrics, model hash and limitations. [Fraud-link audit](fraud-link-audit.md) lists patterns, before/after gaps and Android verification.

The first retraining experiment was rejected after ordinary benign controls exposed non-`www`/path bias despite high historical recall. Host/path/query augmentation reduces this failure; it is synthetic robustness training, not evidence that those pages were visited or verified safe. The benchmark remains historical and single-source, with substantial transport/source bias. Its test partition was inspected during development while comparing candidates, so it is not an untouched final evaluation. Fresh independent, time-separated and language/category-specific evaluations remain necessary.

For underrepresented IDN, exact official-brand and structured email-query contexts, the model abstains from standalone warnings. Rules and imported local threat evidence remain active; limited context is disclosed. This restriction can miss clean scams in those contexts. The raw model benchmark and the complete pipeline including abstention are separate report fields.

The configured model threshold raises the risk index to at least 35 (WARN). It cannot independently BLOCK, lower established local evidence or weaken an existing BLOCK decision. Native loading checks model metadata hash, float tensor shapes, scaler feature order and positive finite scales. Loading failure retains rules and is disclosed. Neither the risk index nor the classifier output is a calibrated probability of fraud. Rebuild instructions and pinned dependencies are in the fraud-link audit.

## Text classifier

`text-model.json` is a binary logistic classifier using unique Unicode word unigrams and bigrams. Unicode combining marks are preserved in Hindi and Gujarati words. NFKC normalization and lowercase preprocessing are shared with text rules. Training uses deterministic gradient descent with L2 regularization.

- Training: 100 authored synthetic English, Hindi, Gujarati and Hinglish examples.
- Validation: 36 authored synthetic examples, used to select warning threshold 0.60. This is not independent test data or a real-world benchmark.
- Model-only validation: TP 11, FP 0, FN 7, TN 18; recall 61.1% on these 18 synthetic scam examples. Zero false positives on 18 synthetic benign examples does not establish real-world precision.
- Runtime: a positive model score raises the risk index to at least 35. It does not independently BLOCK. Rules and embedded links can raise severity further.
- Rebuild: `python3 scripts/train_text_model.py` (requires NumPy); examples and report are in `models/`.
- Artifact SHA-256: `6fd873b8c8569a06dbfa1bdc16668fe9545b690231adc3cb0cac2efb075dfd9f`.

Before claiming accuracy or production readiness, collect consented representative data, define independent train / validation / test splits, measure false positives and recall by language and scam category, calibrate thresholds, and evaluate device latency and model updates. Hard-coded cues and a small synthetic model can miss new attacks and misunderstand legitimate messages.

## OCR and QR

ML Kit Latin and Devanagari models and Tesseract's Gujarati language pack are bundled in the APK for offline recognition. All three scripts are checked regardless of interface language. QR decoding uses a bundled ML Kit model and is limited to QR codes from selected images. Blurred, rotated or low-resolution content may be missed. See [language support and model attribution](languages-and-branding.md).
