# SafeX AI Chrome extension — product and implementation plan

Prepared 9 October 2026. Status: proposed extension; the current Android app remains the implemented product. Chrome API and policy references were checked for this plan. Accuracy and performance numbers below are proposed release gates unless explicitly described as existing Android results.

## 1. Product goal and recommended first release

Build **SafeX AI**, a private browser security assistant that helps people check suspicious links, messages, login pages and payment requests before acting. Analysis runs on the user's computer using packaged models and rules. The assistant explains the evidence and offers practical next steps in English, हिन्दी and ગુજરાતી.

The strongest improvement over the Android screenshot workflow is access to a permitted webpage's actual link destinations, visible context and form metadata. Combining those signals should improve detection of disguised destinations and credential traps; that improvement must be measured before claiming it.

The first release should contain a trusted browser side panel, the existing SafeX shield, right-click scanning, local URL/message inference, bounded webpage analysis and optional protection on user-selected sites. Screenshot OCR, curated network blocking and specialized integrations follow once those foundations pass evaluation. This order provides a useful hackathon demonstration without making untested advanced features dependencies of the core scanner.

**Platform:** desktop Chrome on Windows, macOS and Linux. Use the existing Android app on the Samsung S24 and A36. Google's phone installation flow adds extensions to desktop Chrome; it does not provide a phone execution target. The browser shield lives inside supported webpages, whereas the Android floating assistant operates through Android's overlay/capture facilities. [Chrome extension installation help](https://support.google.com/chrome_webstore/answer/2664769?hl=en).

## 2. What the project already provides

| Existing asset | Extension reuse | Required work |
| --- | --- | --- |
| 26 URL rules, normalization and destination explanations | Detection behavior and evidence descriptions | Port to TypeScript; validate browser URL parsing, Unicode, IPv4 and encoding differences against Kotlin fixtures. |
| Text JSON classifier and multilingual action-context rules | Small local message inference; threat categories | Port features and warning policy, including coverage/abstention behavior. Evaluate browser messages separately. |
| 15-feature TensorFlow Lite URL model and scaler | Trained parameters and reference outputs | Export validated dense-layer parameters into a versioned browser artifact and implement bounded inference. Android's native TFLite runtime cannot simply run in Chrome. |
| Local threat-list import behavior | Offline reputation matching with explicit coverage | Add bounded browser imports, provenance, source date and expiration handling. An unmatched URL remains unverified. |
| Canonical explanations, incident guidance and UPI review | Understandable warnings and recovery assistance | Add browser-specific reason IDs, form context and selected payment details. |
| English/Hindi/Gujarati translation catalog | Existing copy and terminology | Generate extension catalogs and translate new browser strings; preserve user's manual language choice. |
| SafeX shield/X SVG and teal/navy palette | Consistent identity across phone and desktop | Export crisp toolbar/store icons and adapt layout to a narrow, resizable side panel. |
| Android OCR/QR implementations | Workflow, crop behavior and regression examples | Browser recognition requires separately packaged browser-compatible runtimes and language assets. Validate all three scripts before enabling it. |

Relevant local references: [architecture](architecture.md), [model card](ml-model.md), [fraud research](fraud-research-2026-10-09.md), [language/reading settings](languages-and-branding.md), [brand guide](../branding/README.md).

The installed Android pipeline detected **71 of 78 scam messages** with **one false warning on 769 legitimate messages** in an inspected historical English development comparison. That is useful regression evidence, not Chrome accuracy, contemporary fraud coverage or observed Hindi/Gujarati accuracy. The new extension needs its own untouched browser evaluation set.

## 3. Feature priorities

P0 is the hackathon-ready core. P1 adds protection after core validation. P2 expands inputs and maintenance.

| Priority | Feature | User benefit and intended behavior |
| --- | --- | --- |
| P0 | Scan current page | Check URL, visible text, link destinations and form metadata; show exactly what was assessed. |
| P0 | Right-click a link | Analyze the actual link target without opening it. Show destination domain and any visible deception. |
| P0 | Right-click selected text | Check a selected email, chat, job offer or payment message. Analyze the submitted selection rather than an entire inbox. |
| P0 | Paste text or link | Provide a reliable fallback on unsupported pages and when site access is declined. No clipboard-monitoring permission is needed. |
| P0 | Floating SafeX shield | Small, movable, dismissible control on the current authorized page; open the trusted panel for scanning and explanations. |
| P0 | Explain the warning | Show up to three strongest evidence points, actual destination, coverage and a recommended action. |
| P0 | Languages and reading controls | Complete English/Hindi/Gujarati UI, 85–150% text scaling, light/dark/system appearance and keyboard access. |
| P0 | Private review | Unsaved scans by default; explicit Save, Delete and redacted Export controls. |
| P1 | Protect selected sites | With optional site access, rescan meaningful changes and provide restrained link/login warnings. Clear permission and pause controls. |
| P1 | Destination preview and click warning | Compare displayed link text with resolved destination and action context before supported page clicks. Account for legitimate tracking and authentication redirects. |
| P1 | Login/form guard | Detect brand/domain conflicts and risky form destinations without reading passwords, OTPs, typed values or autofill. |
| P1 | Scam action checklist | Review remote-support installation, upfront fees, gift cards, wallet connection, urgency, secrecy and payment requests using visible evidence and optional context questions. |
| P1 | Curated known-threat blocking | Optional browser-enforced rules for verified, carefully scoped threats; reported-only entries raise caution. Explain exceptions and source freshness. |
| P1 | Warning tune and quiet mode | Optional short packaged sound, a user-triggered test, cooldown and mute controls. Visual warnings always remain available. |
| P2 | Screenshot crop + OCR | User invokes capture, crops/reviews it, edits recognized text and chooses what to scan; local English/Hindi/Gujarati recognition. |
| P2 | Image/QR payment review | Decode a deliberately selected image; review HTTP(S)/UPI payloads and expected payee/amount. Recipient identity stays unverified. |
| P2 | Selected-message adapters | Explicit adapters for supported webmail/chat layouts. Detect layout changes and fall back to selection/paste. No automatic whole-inbox ingestion. |
| P2 | Threat-data maintenance | Versioned, bounded, integrity-checked data imports; optional narrowly scoped updates with visible last-update status. |
| P2 | Local feedback and reports | Mark a result incorrect, correct the intended action and export a redacted local report for voluntary review. Feedback does not automatically retrain the model. |

The extension should not claim to authenticate email senders, inspect every wallet transaction, detect deepfake calls, prove a download is malware, or determine domain age offline. Such features need additional evidence and separately evaluated detectors.

## 4. User experience and complete flows

### First run

1. Choose English, हिन्दी or ગુજરાતી and preferred reading size; preview the result immediately.
2. Explain that analysis is local and manual scans are unsaved by default.
3. Offer **Scan when I ask** as the default. Explain optional **Protect this site** when the user enables it on a webpage.
4. Demonstrate with a clearly labeled synthetic example. Explain toolbar pinning and keyboard shortcuts without requiring either.

### Toolbar and side panel

Clicking the toolbar shield opens the browser side panel. Its first screen shows the current site's readable domain, permission/coverage status, **Scan page**, **Check text or link** and **Capture area** when available. Keep settings/history secondary. Avoid duplicate popup and side-panel navigation.

The Side Panel API supports extension-owned UI alongside the webpage; programmatic opening requires a user interaction. Plan compatibility around Chrome 116+ and feature-detect later APIs. Open the panel during the initiating gesture, then perform asynchronous scanning. [Side Panel API](https://developer.chrome.com/docs/extensions/reference/api/sidePanel).

### Link or message scan

Right-click → **Check with SafeX AI** → open the side panel → show the submitted input and source → display findings. Scanning never navigates to the suspect URL. Defanged or incomplete input shows the interpreted address for review before any optional opening action. Use a configurable shortcut for the same flow; test conflicts across operating systems.

### Page protection

**Protect this site** explains the required access, requests the selected origin and confirms when protection is active. A quiet shield/badge reflects the latest completed scan, not an optimistic loading state. Meaningful DOM/navigation changes invalidate outdated findings. Risky evidence produces one concise warning, with **Review details**, **Leave page** and **Dismiss**. Pause, dismiss and permission revocation are separate actions.

On denied or restricted access, show **Page content unavailable — paste text or a link to check it**. Do not show a reassuring result for unassessed content. A paused site displays that protection is paused. Returning to another tab must never display the previous tab's verdict as if it belongs to the current page.

### Screenshot and image scan

User action → capture only the visible active tab → local preview/crop → OCR/QR → editable selection → scan → result. The preview explains that the visible frame is temporarily captured before cropping. Discard the full frame as soon as the crop is accepted; cancellation clears capture data. Nothing is silently captured on a timer. Protect against tab/window changes between invocation and capture.

Chrome's capture API captures the visible active tab, not arbitrary other applications or a full desktop. Deliberately limit this feature to ordinary HTTP(S) pages, even though the API supports some additional sensitive targets with explicit access. [Tabs capture API](https://developer.chrome.com/docs/extensions/reference/api/tabs#method-captureVisibleTab).

### Result wording

| Display state | Meaning | Main action |
| --- | --- | --- |
| No strong warning signs found | Available checks found no strong evidence; safety is not established. | Show coverage and encourage independent verification of sensitive requests. |
| Needs checking | Ambiguous or suspicious evidence needs context. | Review destination, reason and a relevant context question. |
| High-risk evidence | Strong combined evidence or a qualified threat-list match. | Recommend leaving; explain any active blocking rule. |
| Unable to assess | Missing permission, invalid input or unavailable checks prevent a useful assessment. | Give a working alternative or retry path. |

Coverage is a separate field: URL only, selected text, permitted page content, OCR partial, model unavailable, or stale reputation data. Inference failure can leave rules available; it must not turn the scan into a safety claim. Do not label a raw model output as a percentage chance of fraud.

Example: **“Needs checking: this page claims to be your bank, but the link goes to a different domain and asks you to verify your password. Open your bank's app or use a bookmark you already trust.”** This combines observable evidence with an understandable action; a mismatched domain alone is not proof of fraud.

## 5. Detection design and accuracy improvements

Every entry point uses one browser analysis contract:

`input → bounded extraction → URL/text/context detectors → evidence policy → translated explanation + coverage + actions`

### Layer A — URL evidence

Port existing structural rules and the 15-feature model. Preserve raw and interpreted addresses. Identify registrable domains using a packaged public-suffix dataset including private tenant suffixes. Examine Unicode/punycode, brand lookalikes, deceptive subdomains, userinfo, unusual numeric hosts, visible nested destinations and bounded decoding.

Preserve case-sensitive path/query semantics. Reject unsupported schemes and malformed input with a clear explanation. Do not fetch links, expand opaque shorteners or follow redirect parameters to investigate them. An unresolved shortened destination stays unknown. Low-quality indicators such as HTTPS absence, a hyphen or a particular TLD must not independently establish fraud.

### Layer B — message evidence

Reuse identifier masking, Unicode word/bigram/character features and multilingual behavioral rules. Keep existing model coverage guards and separate action-context thresholds in the initial parity port. Analyze selected message-sized passages; the historical SMS classifier must not score an entire unrelated webpage as if it were an SMS.

Recognize payment/credential pressure, KYC/account threats, digital arrest, delivery fees, upfront job/loan fees, fake rewards, gift cards, investment promises and remote-support instructions. Include negation, educational advice and legitimate notification controls. Interface language does not determine the language being analyzed; mixed-language and transliterated messages need their own evaluation.

### Layer C — new browser context

Inspect the actual resolved anchor URL, displayed destination, nearby visible request and the page's origin. Inspect form `action`, method, field types and labels while excluding entered values. Correlate a claimed organization, sensitive request and destination; distinguish legitimate SSO, payment providers and multi-tenant services.

Page context can identify some scams on otherwise ordinary URLs, including compromised sites. A recognized official domain is evidence about destination spelling, never an exemption from content checks. External form actions are not inherently malicious. Form metadata cannot reveal every JavaScript submission target, hidden redirect or cross-origin frame.

Add a small page-context classifier only after acquiring labeled browser examples. Until then use tested, explicit context rules and describe their coverage. If a learned visual brand/logo detector is proposed later, evaluate it separately; branding alone does not authenticate a site.

### Layer D — local reputation

Use licensed, provenance-recorded data with exact URL/host scope and dates. A reported phishing entry is a caution signal, not automatically a verified block instruction. Avoid domain-wide blocking on shared hosting or formerly compromised legitimate sites. Expired entries require review; a miss does not establish safety.

### Layer E — evidence policy

Keep detectors independent so an unavailable model does not erase useful rules. Require stronger evidence for interruptions than for informational cautions. Models can raise warnings and do not independently create network-blocking rules. Present reason IDs, supporting snippets and coverage; generate explanations from local templates rather than an unvalidated cloud or large language model.

## 6. Training and independent evaluation

1. Freeze current Android artifacts and reserve their inspected examples for regression only.
2. Collect licensed or explicitly consented browser scam/legitimate data. Record source, labeling method, date, license and hashes. Reported URL feeds alone do not label page content or establish current precision.
3. Add verified page evidence for login traps, payment scams, support scams, OAuth/device-code abuse and deceptive links. Preserve examples as safe fixtures; acquire suspicious pages only through an isolated research workflow, not by opening them in the extension user's browser.
4. Add independently reviewed English, Hindi, Gujarati and mixed-language messages. Authored/translated examples improve functional coverage but are reported separately from observed-language evidence.
5. Include legitimate banks, delivery messages, government notices, job offers, charity appeals, authentication redirects, payment gateways, security education and shared-hosting pages. This is essential to avoid excessive warnings.
6. Deduplicate and group related campaigns, registrable domains/private tenants, templates and translated variants before splitting. Reserve a later-period holdout and, where feasible, unseen sources. Keep training, validation and evaluation separate.
7. Select thresholds on validation only. Compare rules alone, existing models alone and the full combined pipeline. Report misses, false warnings, precision/recall, abstention rate and confidence intervals by input, language and major scam category.
8. Promote only a passing, checksum-matched candidate; retain model cards, feature versions, dataset manifests and rollback artifacts. Production inference remains local. Training occurs on the development machine; user feedback is not silently uploaded or used for training.

### Proposed evaluation gates

| Area | Initial target | Evidence required |
| --- | --- | --- |
| Scam recall | At least 90% on the new representative browser evaluation set | Full production pipeline; include missed cases and confidence intervals. This is a target, not an existing result. |
| False warnings | At most 1% on representative legitimate controls | Separate controls for authentication, payments, education and multilingual messages. |
| Hard blocking | No false blocks in the release's legitimate regression suite | Enable only qualified list entries; measure block coverage separately. Passing a finite suite does not prove zero real-world false blocks. |
| Language claims | Observed results disclosed separately for English/Hindi/Gujarati | Aim initially for 1,000 legitimate and 200 scam examples per language where obtainable; insufficient data is explicitly labeled limited coverage. |
| Port correctness | All feature/policy reference fixtures agree; URL inference within documented floating-point tolerance | Kotlin/Python/browser comparison, including boundaries around thresholds. |
| Local latency | Warm URL check p95 ≤100 ms; bounded page check p95 ≤500 ms; cold check p95 ≤1 second | Declared reference laptop, real Chrome, extraction + inference + visible result measured separately. OCR has a separate benchmark. |
| Reliability | No stale-tab result, secret-field ingestion or crash across required browser flows | Browser tests, network/storage inspection and permission/lifecycle regression. |

Do not tune on the final holdout after looking at failures and continue calling it independent. Diagnose using development data, then reserve a fresh holdout for the next promotion. If a gate fails, revise the detector or release a clearly scoped manual/pilot experience; do not inflate accuracy or silently lower the gate.

## 7. Technical architecture

Use Manifest V3, TypeScript and a packaged UI build. React with locally compiled CSS is a practical UI option; the security engine stays framework-independent. Avoid dependence on experimental browser AI availability or first-run model downloads.

| Component | Responsibility |
| --- | --- |
| Service worker | Context menus, event routing, permissions, model initialization, bounded analysis, badges and session state. |
| Isolated content script | Extract approved visible text/link/form metadata, render the lightweight shield and observe bounded page changes. |
| Extension side panel | Trusted input/review/results, settings, history and OCR/image worker orchestration. |
| Shared engine package | Versioned normalization, features, rules, model inference, evidence contracts and locale-neutral reason IDs. |
| Local storage | Preferences and deliberately saved, redacted reports; no private scan synchronization. |
| Optional DNR rules | Browser-enforced matching of curated threats, with explicit activation and scoped exceptions. |
| Optional OCR worker | Packaged browser runtime and language data; process only a chosen image/crop. |

Proposed implementation paths, to be created during implementation:

```text
chrome-extension/
  manifest.json
  src/background/       src/content/       src/sidepanel/
  src/reading/          src/privacy/       src/recognition/
  public/icons/         public/models/     public/_locales/
  tests/                demo/              release/
shared/security-engine/
  src/url/              src/text/          src/context/
  src/policy/           src/contracts/     fixtures/
scripts/export_extension_artifacts.py
```

The browser and Android should share versioned model artifacts, evidence definitions and golden fixtures. Kotlin remains the Android implementation; TypeScript is the browser implementation. Do not describe two separate language implementations as literally the same executable code.

Chrome service workers can stop when idle and lose global variables. Use `chrome.storage.session` for bounded temporary results that must survive worker restart, expire them and clear them on tab/navigation changes. Reload models as needed; do not keep the worker alive with artificial polling. [Service-worker lifecycle](https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle).

Each scan carries a request ID, tab ID, document/navigation identity, source, feature/model versions, expiry and coverage. Reject late results after navigation. Bound message sizes and job queues. A practical starting budget is 12,000 text characters per passage, 512 model tokens, 100 inspected links and 20 forms per page; mark excess or skipped content as partial and allow explicit selection for deeper checks. Prioritize the clicked link and its context. Debounce mutation bursts and stop automatic work when paused or permission is revoked.

All JavaScript, model parameters and any WASM/runtime assets are packaged. No remote scripts or CDN inference dependencies. Chrome distinguishes executable remotely hosted code from data; even permitted data updates must be narrowly scoped and unable to execute code. Keep the initial release's inference artifacts bundled. [Remote hosted code policy](https://developer.chrome.com/docs/extensions/develop/migrate/remote-hosted-code).

Use a restrictive extension CSP; add packaged WASM support only when the recognition/runtime actually requires it. The initial URL/text inference can run as small TypeScript numerical code with validated exported parameters. [Extension CSP](https://developer.chrome.com/docs/extensions/reference/manifest/content-security-policy).

## 8. Permissions, privacy and extension security

### Permission design

| Capability | Permission approach |
| --- | --- |
| User-invoked page scan | `activeTab` + `scripting`. |
| Right-click input | `contextMenus`; consume only the selected text/link. |
| Side panel | `sidePanel`. |
| Settings/private session | `storage`; keyboard `commands` declared in the manifest. |
| Automatic protection | Optional HTTP(S) host permissions, requested for selected origins when enabled. |
| Known-threat blocking | Omit from the MVP manifest. A later build that supplies broad curated blocking declares required `declarativeNetRequest`; its rules remain user-controlled and initially disabled. |
| Background warning sound | Add `offscreen` only if a tested implementation needs a hidden audio document. |
| Feed maintenance | Prefer offline imports first; later optional access to a fixed update origin. |

Manual scanning gets temporary access through user invocation; automatic website monitoring needs separately granted site access. Do not require all-site content access, browser history, cookies, clipboard reading, debugger or download access for the core scanner. [activeTab](https://developer.chrome.com/docs/extensions/develop/concepts/activeTab), [permission declarations](https://developer.chrome.com/docs/extensions/develop/concepts/declare-permissions).

DNR blocking is an optional product feature, but `declarativeNetRequest` cannot be put in `optional_permissions`. Introduce its required permission only in a release that actually provides blocking, explain Chrome's install/update warning and offer an in-extension rules toggle. That permission can support allow/block rules without granting all-site DOM access. A site-limited alternative is `declarativeNetRequestWithHostAccess`, whose rules require appropriate granted host access; it cannot be described as global protection without that coverage. [Optional-permission restrictions](https://developer.chrome.com/docs/extensions/reference/api/permissions), [DNR permissions](https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest#permissions).

DNR provides declarative matching, not a synchronous AI decision for every arbitrary navigation. Most consumer MV3 extensions cannot use blocking `webRequest` listeners. Therefore separate supported page click/form warnings from curated browser-enforced rules; do not promise to stop every first visit, script navigation, middle-click, redirect or background request. The MVP remains functional without network-blocking permission. [DNR API](https://developer.chrome.com/docs/extensions/reference/api/declarativeNetRequest), [webRequest restrictions](https://developer.chrome.com/docs/extensions/reference/api/webRequest).

### Data handling

- Automatic extraction excludes input/textarea values, contenteditable regions, password/OTP fields, autofill, hidden text, cookies, page storage and unrelated private conversations. Selected/pasted content and screenshots can themselves contain sensitive data; process them locally and unsaved by default.
- Do not send user URLs, screenshots, messages, snippets or risk results to a server. Test extension-initiated network traffic separately from the visited webpage's traffic and Chrome's own update activity.
- Keep preferences local. Keep temporary scan state in memory/session storage with short expiry. Explicitly saved reports contain redacted evidence and exclude raw screenshots, credentials and token-like query values. Provide 7/30/90-day retention and Delete all.
- Do not use `storage.sync` for scan inputs or reports. Restrict storage access to trusted extension contexts and provide only necessary preferences/status to content scripts. Chrome session storage is memory-backed; local storage is not an encrypted vault and inherits browser-profile access protections. [Storage API](https://developer.chrome.com/docs/extensions/reference/api/storage).
- Incognito support is disabled for the first release; private review still means unsaved analysis in ordinary browsing. Any later incognito support requires explicit testing and separation of persistence.
- Optional updates retrieve a fixed data package, not user-specific lookups. Validate schema, size, version, integrity and provenance; use signed updates from a controlled publisher with rollback protection when that update system is introduced. A hash alone does not authenticate an untrusted publisher.
- Publish a clear extension privacy policy and accurate Web Store disclosures. Local-only handling still requires disclosure. [Chrome user-data requirements](https://developer.chrome.com/docs/webstore/program-policies/user-data-faq).

### Security boundaries

Content scripts have isolated JavaScript environments but share the page DOM. The page can alter, hide or imitate an in-page shield. Use the extension-owned side panel for authoritative explanations, saved reports, capture review and permission controls. Shadow DOM helps styling; it does not make an overlay impossible to spoof. [Content-script isolation](https://developer.chrome.com/docs/extensions/develop/concepts/content-scripts).

Treat all content-script messages as untrusted. Validate type, length, sender, tab/document association and current grants. Privileged operations such as screenshot capture, rule changes, exports and permission changes require a verified extension/user-action flow, never a generic page-supplied command. Render suspect content as text, not executable HTML; do not fetch a URL merely because a message contains it. Do not expose external messaging or unnecessary web-accessible resources. [Messaging security](https://developer.chrome.com/docs/extensions/develop/concepts/messaging#security-considerations).

## 9. Professional visual design

Reuse the original shield/X artwork, keeping it recognizable at 16/32/48/128-pixel exports. Use midnight `#08151D`, card surface `#122936`, teal `#35DEBC`, primary text `#EFF9F8` and secondary text `#A5BFCA`; use light-mode primary `#007A68`. Teal communicates brand/navigation. Warning states use distinct icons, wording and tested semantic colors.

The side panel uses a compact header, readable domain line, one primary action and a clear result card. Design responsively around roughly 320–480 px panel widths. Use restrained motion, reduced-motion support, visible keyboard focus, accessible labels and no endless flashing shield or repeated modal interruptions. The floating control must be dismissible, keyboard reachable and repositionable without covering important page controls.

Keep all controls, errors, warnings, reason explanations and accessibility strings translated. Use extension `_locales`/`default_locale` for Chrome-managed copy, plus a local rendering catalog for the user's independent in-extension language choice; `chrome.i18n.getMessage()` follows Chrome's locale and does not implement a manual language switch by itself. Rebuild context-menu labels when the chosen language changes. Preserve raw URLs/messages and store locale-neutral reason IDs. [Chrome internationalization](https://developer.chrome.com/docs/extensions/reference/api/i18n).

Reading size applies across the panel, settings, results and shield menu, preserving browser zoom. Check Gujarati/Hindi combining marks, line height and wrapping with bundled/licensed or appropriate system fonts. Target WCAG AA text contrast: 4.5:1 for normal text and 3:1 for qualifying large text. Test reflow at enlarged text and browser zoom; do not treat the palette alone as proof of accessibility. [W3C contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

Warning sound is optional and user-enabled, with a working test button and quiet mode. Start with panel-visible playback; validate any hidden-document audio path separately. Chrome's offscreen API has a specific audio-playback lifecycle, so create/close it deliberately rather than using it to keep scanning alive. [Offscreen API](https://developer.chrome.com/docs/extensions/reference/api/offscreen).

## 10. Milestones and completion criteria

| Milestone | Work | Complete when |
| --- | --- | --- |
| M1 — Engine portability | Browser contracts, Kotlin/Python fixtures, rules, model/scaler export, bounded inference and failure handling | Reference features/decisions match and no inference requires network access. |
| M2 — Useful extension | MV3 wiring, side panel, text/link/context menus, current-page scan, shield, localization and reading controls | A clean Chrome profile can install the unpacked build and complete all P0 flows offline on saved/local fixtures. |
| M3 — Browser-aware protection | Link/form context, optional site monitoring, mutation/navigation handling, pause/revoke and stale-result prevention | Supported authorized-page flows pass browser tests; legitimate SSO/payment controls remain usable. |
| M4 — Accuracy and release hardening | New labeled data, untouched holdout, threshold validation, security/privacy inspection and performance testing | Quality gates are measured and limits documented. Failed or untested features remain explicitly scoped. |
| M5 — Advanced inputs | Screenshot crop/edit, three-script OCR, image QR/UPI, sound and adapters | Each input completes capture-to-result/cancel flows, offline recognition works and coverage failures are visible. |
| M6 — Distribution | Curated blocking if qualified, attribution, privacy policy, screenshots, release ZIP, rollback and store submission | Release artifact loads with no remote code; required disclosures/permissions match behavior. Store approval remains an external review. |

For a close hackathon deadline, deliver M1–M4 with selected-site protection in warning mode and defer advanced input/network blocking. Benchmark the core before adding heavy OCR or additional models. Keep ordinary browser browsing usable if SafeX cannot assess a page; never replay form submissions or secrets while handling a warning.

## 11. Required end-to-end verification

- Link/text/page scans, genuine DOM destination mismatch and legitimate redirect controls; encoded URLs, IDNs, numeric hosts, unsupported schemes and private hosting boundaries.
- Selected Hindi/Gujarati and mixed-language messages; advice/negation; model vocabulary gaps; locale switching and 150% text with browser zoom.
- Permission accept/deny/revoke, restricted pages, unsupported frames, shield dismissal, site pause and recovery through paste/selection.
- SPA navigation, tab switching/closing, reload, duplicate requests, model startup/failure, extension update and forced service-worker termination. Verify request/document association throughout.
- Sensitive input/autofill/contenteditable exclusion; no raw secret logging; forged or oversized messages; malicious snippets rendered harmlessly; extension-initiated egress inspection.
- Crop/cancel/edit/retry, capture race with tab switches, empty OCR, partially recognized native scripts, oversized images and multiple QR codes when P2 is enabled.
- DNR exact URL/host scope, shared-hosting exclusions, stale entries, disabled rules and explicit exceptions when blocking is enabled. Inspect actual packaged rules rather than relying only on rule-generator tests.
- Latest stable Chrome and another supported version on declared desktop systems; keyboard/screen-reader checks and reference-laptop cold/warm performance. Android S24/A36 checks remain a separate app validation track.

Use safe local pages and synthetic messages for functional tests. Measure accuracy on independently labeled held-out examples. Publish both reports with the release; passing authored fixtures is not an accuracy percentage.

## 12. Hackathon demonstration and mentor goals

A four-minute demonstration should show:

1. Scan a selected synthetic delivery/KYC scam; reveal the real destination and payment/credential request.
2. Scan a realistic legitimate notice with similar vocabulary; show why it does not trigger the same strong warning.
3. Open a local fake login fixture; compare the claimed organization with the actual origin/form destination, then demonstrate the supported warning flow.
4. Change to Hindi/Gujarati and larger text; scan a native-script fixture.
5. Run the checks with extension network requests disabled and show private unsaved review. If advanced OCR has passed, demonstrate a chosen crop/QR briefly.

Every demonstration fixture is visibly labeled synthetic. Do not use a mock blocklist/model result as if it were a live verified threat. Keep an offline fallback recording and the tested release ZIP available.

Mentor-facing goals: a useful desktop companion to the Android app; evidence-based warnings at the point of action; no sensitive-data upload; accessible three-language UX; independently measured recall and false warnings; and a repeatable release process with known limitations.

**Recommended implementation starting point:** create the browser engine and parity fixtures, then build the side-panel/selection workflow. Once that works, add browser context and selected-site protection before spending time on screenshot OCR or broad blocking.
