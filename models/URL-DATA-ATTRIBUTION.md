# URL model research data

The SafeX AI URL classifier is trained from **PhiUSIIL Phishing URL (Website)** by Arvind Prasad and Shalini Chandra (2024), distributed by the UCI Machine Learning Repository as dataset 967.

- Dataset: https://archive.ics.uci.edu/dataset/967/phiusiil+phishing+url+dataset
- Associated paper: https://doi.org/10.1016/j.cose.2023.103545
- License: Creative Commons Attribution 4.0 International — https://creativecommons.org/licenses/by/4.0/
- Downloaded archive SHA-256: `0a639fd03aea6308c5b1c10c92aa23c2ce1505447a9137271865cd0badc9a59a`
- Original labels: 1 legitimate, 0 phishing. Training remaps 1 to phishing.

Changes: SafeX extracts its own 15 URL-only production features, groups related registrable domains (including private public-suffix tenants) into the same split, trains a compact neural network and exports a small offline TensorFlow Lite model. HTML, page titles, reputation data and dataset-derived URL/TLD probability features are not used. SafeX also creates mechanical www/root alternatives and benign path/query variants with synthetic labels. Variants that change public-suffix ownership are excluded, and accepted variants stay with the original domain split. These labels assume same-owner host variants and benign path usage; they do not establish the safety of live pages. The training scaler is fit on training rows only. Threshold and checkpoint selection use validation rows; the held-out test rows are not used for fitting or threshold selection.

Raw downloaded URLs stay in the ignored `models/cache/` directory and do not ship with the app. No destination in the dataset is fetched, resolved or opened. Temporary native test inputs are also ignored and are not production assets. The historical test partition was inspected during candidate development; it is a development benchmark, not an untouched final evaluation. The separately authored reserved-domain pattern corpus is regression data, not independent accuracy evidence or model training data.

Historical dataset results cannot establish present-day accuracy, representative notification performance or protection against every phishing technique. Results and dataset limitations are in `url-evaluation.json` and `../docs/fraud-link-audit.md`.
