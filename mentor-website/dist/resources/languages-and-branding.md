# SafeX AI: languages, reading settings and brand

Choose **English**, **हिन्दी** or **ગુજરાતી** during setup or in **Settings → Language & reading**. Menus, scan results, explanations, next steps, errors, accessibility descriptions and warning notifications use the selected language. Original messages, sender names, filenames, QR payloads and web addresses remain unchanged. History stores canonical evidence and displays it in the current language; changing languages does not destroy past results.

The text-size slider ranges from **85% to 150%**, in 5% increments. The percentage previews while dragging; the new size applies when you release the slider, keeping the control steady under your finger. It applies across the app, including shared-content results and warning screens. It multiplies the phone's own font scale, preserving the user's system accessibility setting. Reset returns only the in-app multiplier to 100%. Language and size are saved locally and survive restart. Android 13+ also exposes the three languages in its per-app language settings through [LocaleManager](https://developer.android.com/guide/topics/resources/app-languages).

The public app name is **SafeX AI**. The existing `com.sentinel.ai` application ID and database remain stable so installed users retain their history and preferences. The [original logo and brand guide](../branding/README.md) describe the shield, X and intelligence-node mark and the matching teal/navy palette.

## Offline native-script support

Message rules inspect English, Hindi and Gujarati regardless of interface language. They cover credential requests, financial pressure, authority impersonation, prize/job/loan traps and UPI receipt scams. Negation checks distinguish common safety advice from malicious requests. The sample buttons load native-script fixtures for the selected language and visibly mark the result as synthetic.

Screenshot recognition uses bundled ML Kit Latin and Devanagari models plus Tesseract Gujarati OCR. All three scripts are checked independently of interface language. Gujarati is provided separately because [ML Kit's documented scripts](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages) do not include Gujarati. [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android) and [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast) support the bundled Gujarati model. Models are packaged with the APK, with no first-use network download. The Gujarati pack is copied only to the app's private files; analyzed images are released after recognition.

Gujarati asset: `app/src/main/assets/ocr/guj.traineddata`, SHA-256 `fa69658614b4946a9afae8853d67e0689838803dfa3d12c2e35ec53ee6f8df34`. It is licensed under Apache 2.0; the license is packaged beside the model. OCR depends on legible text, lighting, size and font. These functionality checks do not establish real-world scam accuracy in any language.

## Maintaining translations

Edit `localization/translations.tsv` (English | Hindi | Gujarati), then run:

```sh
python3 scripts/generate_localization.py
python3 scripts/generate_localization.py --check
```

The generator validates identical keys, nonempty translations and matching numbered placeholders, then writes three Android resource files and the canonical-evidence lookup index. UI components translate app copy; raw user content uses `localize = false`. New result messages should be added to the catalog when implementing features.

Verification includes JVM native-script scam/advice regressions and Android tests of real OCR, actual analysis results, language selection, text sizing, activity recreation, dynamic warning localization and unchanged URL placeholders. See [the validation report](validation.md) for results and screenshots from this build.
