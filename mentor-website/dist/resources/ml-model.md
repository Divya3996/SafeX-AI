# Model card

## URL classifier

The original `model.tflite` and `scaler.json` assets are retained. Native TensorFlow Lite inference takes 15 numeric URL features. No training dataset, independent benchmark or reproducible training script for this asset is present. Earlier documentation stated 238,000 training URLs without supporting artifacts; that claim is withdrawn.

A conservative contribution supplements structural rules. It cannot lower established local evidence or an existing BLOCK decision. Model loading failure falls back to rules and is disclosed in the result. The displayed risk index is **not a calibrated probability of a scam**.

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
