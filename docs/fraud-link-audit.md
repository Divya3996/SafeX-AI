# SafeX AI fraud-link research and audit

Release under test: **1.3.0**. Research and verification date: **2026-10-08**.

SafeX checks URL text locally and explains warning signals. A warning means caution, not proof that a website is fraudulent. No detector can identify every scam from its URL alone. A clean URL can lead to a compromised website or a scam hosted on a legitimate service.

## Pattern list and tests

The [authored corpus](../models/url-pattern-corpus.json) contains 134 string-only examples across 21 families: 88 warning cases, 31 benign controls, 11 unsupported inputs and four explicit offline blind spots. Dangerous-looking examples use reserved domains or documentation addresses wherever possible. None was opened, resolved or downloaded. This corpus is a regression suite, not independently sampled fraud accuracy data.

| Family | What SafeX checks | Typical example or constraint |
| --- | --- | --- |
| Brand typos | Substitution, missing/repeated letters and adjacent transpositions with account context; digit confusables | `paypa1.example/login`, `payapl.example/login` |
| Brand affixes | Brand combined with support, account, KYC, refund or verification terms | `paypal-support.example` |
| Trusted domain prefix | The real domain after a misleading trusted-domain substring | `accounts.google.com.attacker.example` |
| Branded subdomain | Brand in an unrelated site's subdomain with a sensitive-action cue | `paypal.attacker.example/login` |
| Unicode homographs | A bounded Cyrillic/Greek confusable set, IDN normalization and undecodable Punycode | Similar-looking letters can conceal another owner; this is not the complete Unicode confusables database |
| User information | Text before `@` is not the destination host | `paypal.com@attacker.example` |
| Numeric hosts | Decimal, octal, hex and abbreviated IPv4 representations | `2130706433`, `0x7f000001`, `127.1`; normalize the actual destination |
| Redirect parameters | Visible cross-site destinations in `next`, `redirect`, `url` and related parameters | Relative or same-site redirects are benign controls |
| Layered redirects | Bounded percent decoding up to four layers | Avoid treating deeply encoded destination text as harmless |
| Base64 redirects | Bounded Base64/Base64url decoding in destination parameters | Decode only URL/app-handoff-shaped values, never execute them |
| Nested redirects | Visible embedded URLs in paths, query values and fragments | No HTTP redirect following or final-page inspection |
| Short links | Known URL-shortening hosts | Warn that the final destination is hidden; short links are not automatically fraud |
| Sensitive host cues | Distinct combinations of account, OTP, wallet, KYC or payment cues | A single ordinary login path is insufficient by itself |
| Hosted tenants | Public-suffix-aware ownership including private suffixes | Two unrelated `github.io` tenants are different sites |
| Executable downloads | Executable suffixes and document/program disguises | `.apk`, `.exe`, `.scr`, `invoice.pdf.js`; no claim of file antivirus |
| Brand outside host | Trusted brand URL text placed in path/query/fragment with account action | Documentation mentioning a brand is a benign control |
| Combined structure | HTTP, userinfo, suspicious TLD, unusual ports, host nesting and encoding together | Signals combine deterministically; HTTP alone means transport caution |
| Canonical evasion | Case, trailing dots, alternate IP forms, path/encoding preservation | Official host boundaries remain exact |
| Rejected ambiguity | Backslashes, invisible/bidi controls, impossible addresses, malformed ports and unsupported schemes | Reject explicitly rather than return a safe verdict |
| Benign controls | Official login pages, relative/same-site redirects, non-Latin hosts, ordinary PDFs and app scripts | Measure false warnings as well as attack coverage |
| Offline blind spots | Compromised clean hosts, opaque server redirects, hosted forms and device/OAuth authorization context | URL text alone cannot determine intent or page safety |

Phishing is also delivered as QR codes, APK installation links, fake bank/KYC requests, prize/refund offers, job/loan fees, investment promotions and wallet-connect requests. These are fraud purposes, not a unique set of URL shapes. The app combines extracted QR/screenshot/message content with URL analysis; a URL-only classifier cannot recognize every purpose.

## Gaps found and changes implemented

- Baseline rules warned on 29 of 88 authored warning cases. They also warned on five benign controls and accepted two invisible-control inputs.
- Brand matching missed deletions/transpositions, trusted-domain prefixes and selected Unicode confusables. Matching now uses registrable domains, official-host boundaries, context and a bounded confusable mapping.
- Numeric IP forms could disagree with browser interpretation. The parser now canonicalizes supported forms without DNS and rejects impossible/ambiguous forms.
- Redirect checks over-warned on same-site destinations and missed layered/Base64 destinations. Inspection now uses public-suffix ownership and bounded decoding.
- Known shorteners and executable link suffixes had no dedicated warning. Six new rules bring the inventory to 26, with explanations translated into English, Hindi and Gujarati.
- Same-site nested wrappers could hide a later absolute or protocol-relative cross-site destination. Overlapping string inspection now exposes those nested destinations without following them.
- Repeated same-site URLs could consume the destination search limit before a later external URL. Destination filtering now examines all parameters within the 8,192-character input bound before limiting external results.
- Messages with more than eight distinct links now explain that further destinations were not analyzed and raise caution.
- Malformed web links inside messages could be silently discarded. They now produce a caution explaining incomplete analysis. Unsupported executable QR/app-handoff content also receives caution.
- The original URL model lacked reproducible training provenance. It has been replaced with a compact, reproducibly trained URL-only neural network.
- The first retraining experiment scored well on historical data but over-warned on ordinary non-`www` links. That model was rejected. The final training includes domain-grouped synthetic host/path/query variations to reduce this source bias; historical metrics and synthetic robustness are reported separately.
- The trained model still over-warned in underrepresented contexts. Standalone model warnings now abstain for IDN/Punycode, exact official-brand hosts and structured `email` / `login_hint` query values. This is a coverage restriction, not a safety allowlist: all heuristic/snapshot evidence still applies, and the result labels limited model context. Clean scams in those contexts can still be missed.
- Model evidence can raise a warning, but cannot independently block a link or weaken existing rule/snapshot evidence. The risk index and classifier score are not calibrated probabilities of fraud.

Further synthetic subdomain variants and broad hosted-service abstention were evaluated and rejected because they reduced detection in the historical development benchmark. The shipped configuration keeps hosted links eligible for model warnings. A benign hosted page can still receive caution; this remaining false-warning gap is measured below.

## Training and provenance

The primary source is [UCI PhiUSIIL, Prasad and Chandra (2024)](https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset), licensed CC BY 4.0. It contains 235,795 historical records. SafeX extracts its own 15 Kotlin URL features; it does not use HTML, titles, page rank, remote reputation or the dataset's precomputed probability features. See [attribution](../models/URL-DATA-ATTRIBUTION.md).

Registrable-domain SHA-256 grouping assigns original records and their accepted synthetic variants to the same train, validation or test split. Different splits contain no overlapping ownership groups. Training-only means/scales and clipping feed a 15→24→12→1 neural network. The checkpoint and warning threshold use validation data only; the threshold limits model-only false positives separately in original and augmented validation data. The authored pattern corpus is never used for training. The historical test partition was inspected during candidate development, so it is a development benchmark rather than an untouched final evaluation. Fresh independent evaluation is still required.

Augmented URLs are mechanical text examples, not observations of actual live page safety. Domain-preserving `www`/root alternatives assume the same site owner; benign login, documentation, query and native-script paths have synthetic benign labels. Private-suffix ownership changes are excluded. These assumptions and the historical source bias limit any real-world performance claim.

## Reproduce

From the project root with Java 17, Android SDK 34 and Python with the pinned [training dependencies](../scripts/url-training-requirements.txt):

```sh
python3 scripts/prepare_url_dataset.py
python3 scripts/augment_url_dataset.py
python3 scripts/build_url_pattern_corpus.py
cd android-app
SAFEX_URL_DATASET="$PWD/../models/cache/urls.jsonl" \
SAFEX_URL_FEATURES="$PWD/../models/cache/features-updated.jsonl" \
SAFEX_URL_COVERAGE="$PWD/../models/cache/model-coverage.jsonl" \
./gradlew :app:testDebugUnitTest --tests '*UrlResearchExportTest' --rerun
SAFEX_URL_DATASET="$PWD/../models/cache/augmented-urls.jsonl" \
SAFEX_URL_FEATURES="$PWD/../models/cache/features-augmented.jsonl" \
./gradlew :app:testDebugUnitTest --tests '*UrlResearchExportTest' --rerun
cd ..
OPENBLAS_NUM_THREADS=2 python3 scripts/train_url_model.py
OPENBLAS_NUM_THREADS=2 python3 scripts/evaluate_url_pipeline.py
```

The archive checksum is pinned; changed upstream data requires a provenance review. Downloads contain data only. No dataset URL is visited. Raw source data, local dependencies and transient native fixtures remain ignored. Baseline replay is optional and uses preserved pre-update local caches; these are not training inputs.

To run native parity tests, copy the generated `models/cache/native-url-evaluation.json` into the ignored instrumentation asset path of the same name, build both debug APKs, install them and run the instrumentation suite in airplane mode. Production APK assets contain models/scalers/model metadata, not the dataset. See the final verification record below.

## Research references and remaining limits

- [Chromium IDN security](https://chromium.googlesource.com/chromium/src/+/main/docs/idn.md) supports the homograph threat model. SafeX uses a small audited mapping, not Chromium's complete browser policy.
- [WHATWG IPv4 parser](https://url.spec.whatwg.org/#ipv4-parser) documents alternate numeric representations; SafeX rejects unsupported parser ambiguities rather than silently navigating.
- [OWASP redirect guidance](https://cheatsheetseries.owasp.org/cheatsheets/Unvalidated_Redirects_and_Forwards_Cheat_Sheet.html) explains why destinations hidden in redirects need scrutiny.
- [Microsoft QR phishing research](https://www.microsoft.com/en-us/security/blog/2024/11/04/how-microsoft-defender-for-office-365-innovated-to-address-qr-code-phishing-attacks/) explains links delivered through images.
- [Microsoft OAuth redirection abuse](https://www.microsoft.com/en-us/security/blog/2026/03/02/oauth-redirection-abuse-enables-phishing-malware-delivery/) demonstrates why a legitimate authorization host cannot guarantee a trustworthy downstream flow.

Remaining gaps include fresh campaigns, unknown brands/confusables, clean compromised domains, dynamic pages, opaque redirect destinations, OAuth/device-code context, sender identity and poor OCR/QR quality. The installed app has no INTERNET permission, preserving privacy and offline operation while preventing live page/reputation verification. Fresh independent sources, domain/time-separated evaluations and physical-device testing are still needed before claiming production accuracy.


## Final verification: SafeX AI 1.3.0

| Check | Verified result |
| --- | --- |
| JVM suite | 452 passed, zero failures or skipped tests; includes three opt-in research exports |
| Installed Android suite | 14 passed in airplane mode on API 34; suite time 72.97 seconds |
| Android lint | Zero errors; 43 existing warnings remain |
| English / Hindi / Gujarati resources | 484 strings per language; keys, numbered placeholders and generated files match |
| Production APK | No INTERNET permission; no raw dataset or native parity fixtures packaged |
| Authored warning cases | 85 WARN, two BLOCK, one explicitly unsupported/rejected; no ALLOW |
| Authored benign controls | 30 ALLOW, one WARN, zero BLOCK |
| Authored unsupported cases | All 11 rejected |
| Documented offline blind spots | All four remain unflagged; these are deliberately not counted as successes |
| Native TFLite parity | All 256 feature/prediction fixtures pass; maximum score difference 0.0000008941 |
| Warm measured link-analysis duration | Median 27 ms, p95 51 ms, maximum 116 ms on this emulator |

The one benign caution is `benign-control-21`, an ordinary hosted-page URL. The rules alone stay below warning on all 31 benign controls, but the model still warns on this hosted shape. This is a measured false-warning gap, not complete accuracy. The native parity sample is the first 128 records from each class in the historical test partition, not a representative independent benchmark. Duration excludes asynchronous history writes and does not establish physical-phone latency.

The larger historical domain-separated **development benchmark** contains 35,344 original test rows: 15,279 labelled phishing and 20,065 labelled legitimate. A positive means WARN or BLOCK, not confirmed fraud. Model-only, augmented and whole-pipeline results are recorded separately in the [evaluation JSON](../models/url-evaluation.json).

| Complete pipeline on original historical rows | Before | After |
| --- | ---: | ---: |
| Phishing recall | 92.47% | 94.53% |
| Precision | 99.13% | 99.60% |
| False-warning rate on legitimate rows | 0.62% | 0.29% |
| Accuracy within this historical partition | 96.40% | 97.47% |
| Missed labelled phishing rows | 1,150 | 836 |
| Warned legitimate rows | 124 | 58 |

The final model was rebuilt twice with the same seed and inputs and produced the same hash. Extra synthetic subdomain augmentation was rejected after it reduced historical validation/development performance. The source remains biased and historical, and this partition was inspected during development. These numbers must not be presented as present-day accuracy or an untouched independent evaluation.

- [SafeX AI 1.3.0 debug APK](../android-app/app/build/outputs/apk/debug/SafeX-AI-1.3.0-debug.apk)
- [Machine-readable verification summary](test-results/url-upgrade-summary.json)
- [Full native test output](test-results/native-url-audit.txt)
- [Native pattern decisions](test-results/url-native-patterns.json)
- [Native parity and durations](test-results/url-native-parity.json)
- [Baseline rule audit](../models/url-pattern-baseline.json) and [updated rule audit](../models/url-pattern-updated.json)

APK SHA-256: `577677d80a81d08552259056f76847bda123e7ca556d66a6864dec59698c5f41`.

URL model SHA-256: `79c67a5bfd775d36141a345d5e3b49a253215c3d484fe3dc3eb4ddbd54f1aba1`.
