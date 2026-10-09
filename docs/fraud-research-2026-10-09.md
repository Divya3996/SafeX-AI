# SafeX AI fraud research and retraining — 9 October 2026

SafeX AI 1.7 bundles newly trained message and URL models. In the installed app's historical English development comparison, scam-message detection increased from **13/78 to 71/78**, with one warning on 769 legitimate controls in both versions. That comparison is measured on the actual private scan pipeline and is not an estimate of accuracy against all current fraud.

The collection covers named public sources and common fraud behaviors. No dataset contains every fraud type or every new attack. A source downloaded today may contain historical incidents, and a reported phishing URL is not an independently inspected page. All data processing and model training run on the development machine; the installed app continues to perform inference offline.

## Collected sources and access limits

| Source | Role | Label and freshness limits |
| --- | --- | --- |
| [Mishra and Soni SMS phishing dataset](https://data.mendeley.com/datasets/f45bkkt8pr/1), CC BY 4.0 | Observed English smishing and legitimate messages | Published in 2022. Ham and smishing are used; ambiguous spam labels are excluded. Source archive checksum is pinned. |
| [UCI SMS collection, Almeida and Hidalgo](https://archive.ics.uci.edu/dataset/228/sms+spam+collection), CC BY 4.0 | Additional legitimate SMS controls | Historical corpus; its spam label is not a fraud label. Ham overlaps the Mendeley collection and is deduplicated before partitioning. |
| [Phishing.Database](https://github.com/Phishing-Database/Phishing.Database), MIT | Reported phishing URL snapshot and bounded structural-model training sample | Retrieval time and SHA-256 are recorded. The feed is an archive without reliable incident timestamps for every row. Active describes reachability, not independently established intent. No destination is visited. |
| [UCI PhiUSIIL](https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset), CC BY 4.0 | Existing historical URL examples and regression controls | Historical single-source benchmark. Its prior test partition was inspected during earlier development and remains a regression comparison. |
| SafeX authored patterns | English, Hindi and Gujarati behavioral augmentation | Synthetic examples with explicit provenance; translated categories stay in the same data partition. They do not establish real-world native-language accuracy. |
| [CERT-In 2026 advisories](https://www.cert-in.org.in/s2cMainServlet?pageid=PUBADVLIST02&year=2026) | Current research context | Advisories are not automatically labeled message datasets. The 9 October payment/disbursement API advisory concerns institutional systems beyond this app's observable content. |
| [FTC consumer alerts](https://consumer.ftc.gov/consumer-alerts?field_cfg_topics_target_id%5B2270%5D=2270) | Behavioral research | Used to guide original examples, not copied wholesale into training data. |
| [URLhaus](https://urlhaus.abuse.ch/api/) | Candidate future malware URL source | Authentication key required; no key supplied. Not counted as collected or trained data. |
| [OpenPhish](https://www.openphish.com/kb.html) | Candidate future phishing source | Provider-specific training/data access terms require an appropriate offering. Not included in this training collection. |
| [SmishTank](https://smishtank.com/dataset) | Candidate future observed smishing source | Dataset endpoint unavailable during this check; repository did not expose a clear data license. Not included. |
| [Sting9](https://sting9.org/dataset) | Candidate future current scam messages | Access page says CC0 while its footer names ODC-BY-NC; excluded until the applicable license and verified-label access are clear. |

These exclusions do not block training on the sources already collected. They prevent unsupported data or labels from being silently added to the experiment.

## Fraud coverage checklist

| Behavior | Available app evidence | Remaining blind spot |
| --- | --- | --- |
| OTP/password theft, bank/KYC and account recovery impersonation | Message request context, model patterns and embedded links | Sender authenticity and account state cannot be verified offline. |
| Delivery/customs/redelivery fees | Message patterns and destination structure | An ordinary delivery notice is not fraud merely because it contains a link. |
| Traffic/toll/court and utility-payment pressure | Coercion, financial requests, QR/link review | A screenshot seal or case number does not authenticate an authority. |
| Reward expiry, prizes and lottery fees | Sensitive-action context and prize/payment patterns | Legitimate rewards need independent verification. |
| Upfront job/loan fees and task-withdrawal deposits | Message/model patterns and payment requests | New job vocabulary and ambiguous employment notices can be missed. |
| Guaranteed investment, crypto and wallet schemes | Financial context, visible links and payment review | Smart-contract behavior, investment legitimacy and wallet approvals are outside a URL-only model. |
| Digital arrest, secrecy and remote support | Coercion, authority and remote-access patterns | The app does not authenticate callers or listen to calls. |
| UPI refund/collect requests and QR phishing | Selected QR content, PIN-to-receive cues and payee review | A visible QR does not prove the recipient's identity. |
| Boss/gift-card and business impersonation | Sensitive code/payment requests and surrounding text | Organizational identity, invoice authenticity and bank-account changes need trusted verification. |
| Romance/emergency, charity and recovery-agent requests | Reviewed message context and authored behavioral augmentation | Multi-message trust building and external relationships are only partly visible. |
| Malicious APK/download lures | Visible install requests, link/file metadata and existing local rules | This update does not train an Android malware binary detector. |
| Brand lookalikes, defanged/Unicode links, nested visible destinations | Existing normalization confirmation, local rules and structural URL model | Opaque redirects and changed page contents require additional evidence. |
| Compromised legitimate sites, OAuth/device-code consent abuse and session theft | Limited warning evidence from visible content | Clean URL spelling cannot establish safety; the app cannot inspect remote page state offline. |
| Voice cloning, deepfakes and conversational grooming | Only text/screenshots the user deliberately submits | Audio/video authenticity and unseen conversation history are not modeled. |

Current FTC alerts specifically describe [traffic-notice QR phishing](https://consumer.ftc.gov/consumer-alerts/2026/04/text-about-traffic-violation-probably-scam), [reward-expiry link lures](https://consumer.ftc.gov/consumer-alerts/2026/04/got-text-about-expiring-reward-points-look-closer) and [boss gift-card impersonation](https://consumer.ftc.gov/consumer-alerts/2026/01/no-thats-not-your-boss-asking-you-buy-gift-cards). Authored examples represent these behaviors; they are not records of individual incidents.

## Training and promotion policy

Message features mask URL, email and numeric identifiers, preserve native-script combining marks, and add bounded word, bigram and character-trigram evidence. Kotlin and Python feature sets are compared for every research row. The vocabulary is learned from training examples only and capped at 16,000 terms. Exact masked duplicates and heuristic near-duplicate campaigns are grouped before a deterministic 70/15/15 split. Source overlap is removed; conflicting templates are quarantined. Near-duplicate grouping is not perfect campaign attribution.

Validation selects the message checkpoint and action-context warning threshold with a 1% model false-warning budget. Evaluation labels are not used for fitting or threshold selection. Promotion requires improved observed-English recall, sufficient positive examples, a positive paired recall-gain interval and a passing evaluation false-warning budget. Language and authored-category diagnostics are reported separately; category diagnostics containing training examples are not accuracy evidence. Rules and link checks continue independently.

Installed-app testing exposed a warning-eligibility gap: learned scam lures without explicit action requests were suppressed. The final research policy retains the **0.545 action-context threshold** and adds a **0.915 threshold** for a guarded, context-free WARN path. That second threshold is selected only from validation controls: a fixed .900–.995 grid, zero false warnings on all 741 validation negatives, maximum recall, and lower-threshold tie breaking. The guard requires four distinct non-identifier words, at least 95% ASCII Latin letters and 60% unigram vocabulary coverage. It is an out-of-vocabulary heuristic, not verified language identification. Kotlin and Python agree on all 5,606 guard decisions. Native-script messages retain their action-context path; the old synthetic model cannot use this new path. Neither path independently blocks content. [Warning policy](../models/fraud-text-warning-policy.json).

The URL candidate uses the exact existing 15 Kotlin features, the existing training-only scaler and domain-disjoint partitions. It learns from a bounded reported-feed sample alongside historical and explicitly synthetic URL examples. Its threshold is chosen on validation controls. Promotion requires better recall on reported domains absent from the historical dataset, no more than 1% model false warnings on historical/augmented controls, and no more than two percentage points of historical recall loss. Failure leaves the shipped URL artifact in place. Fresh independent benign URLs and page-level verification remain missing from this comparison.

Every candidate, source snapshot and evaluation receives a checksum. Raw data remains in the ignored local cache. Downloaded scripts, archive code and destinations are never executed or opened. Threshold metadata is consumed by the app rather than hard-coded. Model loading failures keep local rule analysis available.

The saved URL parameter arrays also reproduce both the baseline and candidate native TensorFlow Lite artifacts byte for byte. This prevents comparing a cached parameter set against metadata belonging to a different model. [Weight-integrity check](../models/fraud-url-weight-integrity.json).

The first candidates were promoted with `promote_fraud_models.py`, which verifies the evaluated artifacts and copies them without refitting. Follow-up action-context and protective-advice fixes use already-inspected development evaluation data. Their runtime results must be read as regression diagnostics, not a newly sealed independent test. The first model-only experiment and the later app policy comparison are recorded separately.

## Reproduce locally

Use the existing pinned environment in `scripts/url-training-requirements.txt`. The URL update also requires the historical Kotlin feature exports and baseline parameter cache described in [the earlier URL rebuild instructions](fraud-link-audit.md#reproduce). Preserve the original baseline artifacts before training a new version. The following commands run from the project root. The opt-in Gradle exports require the project's Java 17/Android SDK setup.

```sh
.url-training-venv/bin/python scripts/collect_fraud_training_data.py
.url-training-venv/bin/python scripts/prepare_fraud_messages.py
.url-training-venv/bin/python scripts/prepare_fraud_urls.py
cd android-app
SAFEX_MESSAGE_DATASET="$PWD/../models/cache/fraud-2026-10-09/messages.jsonl" \
SAFEX_MESSAGE_FEATURES="$PWD/../models/cache/fraud-2026-10-09/message-policy.jsonl" \
SAFEX_URL_DATASET="$PWD/../models/cache/fraud-2026-10-09/urls.jsonl" \
SAFEX_URL_FEATURES="$PWD/../models/cache/fraud-2026-10-09/url-features.jsonl" \
SAFEX_URL_COVERAGE="$PWD/../models/cache/fraud-2026-10-09/url-coverage.jsonl" \
./gradlew :app:testDebugUnitTest --tests '*TextResearchExportTest' --tests '*UrlResearchExportTest' --rerun
cd ..
OPENBLAS_NUM_THREADS=1 .url-training-venv/bin/python scripts/train_fraud_text_model.py
OPENBLAS_NUM_THREADS=1 .url-training-venv/bin/python scripts/train_fraud_url_model.py
```

The last two commands write candidates only. `--promote` applies the declared gates and copies a passing candidate into app assets; it does not skip evaluation or change the gates. Rebuild and verify the APK after promotion. `train_text_model.py` is the historical synthetic experiment and overwrites the bundled text asset; use the new pipeline for this release.

For an already-evaluated first candidate, use `OPENBLAS_NUM_THREADS=1 .url-training-venv/bin/python scripts/promote_fraud_models.py` to apply checked artifacts and refresh native parity fixtures without retraining. Export the current Kotlin message policy again, then run `scripts/calibrate_fraud_warning_policy.py` to reproduce the additional validation-selected warning policy. It verifies the unchanged first model and all Kotlin/Python vocabulary-guard decisions. The release also bundles source attribution and model metadata; include those files when distributing a rebuilt APK. Runtime-policy diagnostics use `scripts/evaluate_fraud_text_runtime.py`. `scripts/prepare_fraud_native_evaluation.py` prepares an ignored, transient observed-message file for installed-app evaluation; it must not be copied into APK assets. A future accuracy claim needs new representative evaluation data, kept separate from all inspected development examples.

These are dated research commands. Reproducing the exact published experiment requires its checksum-matched cached source snapshots and baseline artifacts; downloading a later mutable phishing feed creates a new experiment. Preserve old reports before training a new candidate. The production APK contains trained parameters and attribution, not the raw research messages or URL feed.

Collection provenance is in [the manifest](../models/fraud-data-manifest.json). Message/URL dataset summaries and evaluation reports are under `models/`; raw examples are excluded from Git and the APK. Device verification and the final release artifact are recorded separately after training.

## Installed-app improvement and remaining data gaps

The complete private scan pipeline improved from **13 to 71 detections out of 78 labeled scam messages** on the same 847-message historical development comparison. Both versions warned on one of 769 legitimate controls. The new result is **91.0% recall on that collection**, with seven missed scams; it does not mean 91% coverage of all fraud. [Actual installed-app outputs](test-results/fraud-native-evaluation.json) and [the 1.6 baseline](test-results/fraud-native-baseline-evaluation.json) preserve decisions by hashed case ID. No raw message text is published in those reports.

The training collection has 5,606 deduplicated messages, including 3,910 training examples. The reported-link snapshot has 789,054 lines; 40,000 host-sampled URLs form the bounded new URL dataset. Sampling and grouping reserve validation/evaluation rows rather than training on every downloaded item. All 120 authored fraud/legitimate contracts across the three languages and 55 floating-review contracts matched in the installed app. These examples include development and training content and are functional checks.

The next data collection should add independently verified contemporary scam and legitimate messages, including consented Hindi/Gujarati and mixed-language examples, and matched benign URLs from the same period and sources as the reported feed. Keep a new evaluation set untouched by implementation and threshold tuning. Existing voice/deepfake, hidden-redirect and malware-binary gaps need additional evidence and detectors; adding more URL examples alone cannot solve those modalities. Physical S24/A36 testing remains separate from model evaluation.
