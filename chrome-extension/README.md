# SafeX AI for Chrome

Private browser checks for suspicious links, messages, login pages and deliberately selected images. Version 1.1.0 uses Manifest V3, local rules and packaged research models. English, हिन्दी and ગુજરાતી are selectable in the interface.

## Install the built extension

1. Open **chrome://extensions** in desktop Chrome and enable **Developer mode**.
2. Click **Load unpacked** and select this project's **chrome-extension/dist** folder. Select the folder containing the generated manifest, rather than the source folder.
3. Pin **SafeX AI** from Chrome's Extensions menu. Open an ordinary website, then click the SafeX toolbar icon to open the side panel and floating shield.
4. Start with **Analyze privately**, **Scan this page**, or the SafeX right-click menus for a link or selected text. On protected browser pages, use paste or a selected image instead.

The release ZIP contains the same generated files. Extract it into a permanent folder and load that folder. Keep it in place; unpacked installations refer to those files. This release has not been submitted to the Chrome Web Store.

## Available features

- Local text/link checks, actual destination explanations, IDN/lookalike and encoded-address checks, bounded page inspection and sensitive-form metadata checks.
- Native Chrome right-click actions for links and selected messages, plus **Alt+Shift+S** for a page check.
- Movable SafeX shield using the original shield/X artwork. The trusted side panel displays the detailed result.
- Optional protection for a chosen site, rescans after relevant changes, pause/resume and permission removal.
- Warnings before supported clicks on already identified risky links and submissions to identified risky sensitive forms. Link review preserves displayed-address mismatches. Forms are never replayed by SafeX.
- Optional questions about remote access, gift cards, wallet recovery, upfront fees and secrecy. Answers are clearly labeled as user supplied; they add caution without claiming that the sender was authenticated.
- Deliberate screenshot capture, pointer cropping, local English/Hindi/Gujarati OCR, editable recognized text, image QR decoding and UPI address/amount comparison.
- English/Hindi/Gujarati UI, 85–150% text size, light/dark/system themes, keyboard focus and reduced-motion support.
- Optional packaged warning sound, a sound test and automatic-warning cooldown.
- Explicitly saved redacted reports, deletion, 7/30/90-day retention, JSON export and private incorrect-result feedback.
- Bounded text/JSON threat-list imports. Reported entries warn; exact entries explicitly labeled **block** create browser blocking rules only after the user enables blocking. Lists expire after seven days.

## Privacy and scope

There is no account, API key, telemetry, cloud inference or remote reputation lookup. Model, worker, WASM, language and font assets are packaged. Runtime CSP restricts extension connections to its own origin. Ordinary websites still make their own requests.

Page inspection excludes input values, textareas, editable content, cookies and website storage. Recognized inbox/chat hosts are limited to address inspection: their messages, links and forms are excluded from page extraction. Select or paste the message you want checked. Layout-specific Gmail/WhatsApp adapters are not included.

Unsaved findings live in restricted Chrome session storage for up to 15 minutes; navigation removes the corresponding page result. Temporary findings can include the inspected URLs. Original messages, screenshot pixels and OCR drafts are not persisted. Explicit report saving/export removes URL credentials, query values and fragments, document/tab identifiers and payment identity. Remaining report metadata is stored locally, not encrypted. Imported lists intentionally retain their exact entries. Read the packaged [privacy policy](public/privacy.html).

Manual page access follows a user interaction through Chrome's activeTab permission. Automatic monitoring requires a separate site grant and can be paused or revoked. Chrome's required blocking permission is declared at install time, although blocking is initially off. Remote updates, browser-wide AI navigation blocking and automatic model retraining are not implemented.

Warnings can be wrong and scams can be missed. A clear result says **No strong warning signs found**, not that the destination is certified safe. Short links are not followed; inaccessible frames, unseen messages, every navigation technique, recipient identity and downloaded file contents are outside coverage. OCR can misread text; review the editable result. Independent browser recall and false-positive rates have not been established.

## Build and verify

Use Node.js 22 or newer (validated with Node 24), npm and Python 3 for ZIP packaging.

```sh
cd chrome-extension
npm ci --ignore-scripts
npm run check
npm test
npm run build
npx playwright install chromium
npm run test:browser
npm run package
```

The protection suite uses installed Linux Google Chrome by default. Set `SAFEX_CHROME_PATH` to another Chrome executable with the experimental CDP Extensions API available. It always creates a fresh temporary profile and does not modify your normal Chrome profile. Tests preapprove one synthetic localhost permission in that isolated profile, then exercise the real optional-permission API; they do not click a human permission dialog. `SAFEX_HEADLESS=0` runs that suite with a visible window.

`dist/` is the unpacked extension. `release/SafeX-AI-Chrome-1.1.0.zip` and `release/checksum.json` are the distribution artifacts. Generated folders, node_modules and temporary test profiles are ignored by Git. The packaged model and OCR exports are checked in; ordinary builds do not need research datasets or a training environment.

Model exports are generated by `../scripts/export_extension_artifacts.py`. It verifies that the available URL weight archive reproduces the shipped Android TFLite bytes before exporting the browser's numerical parameters. The browser engine is under `../shared/security-engine`; its adapted page policy is separate from Android's notification policy.

## Rehearse the hackathon demo

```sh
npm run demo
```

Open the printed localhost address and follow the visibly labeled synthetic scenarios. Demonstrate a scam message and legitimate advice, the fake login page and optional site protection, then Hindi/Gujarati, larger text and a selected image from `../demo-assets`. Disable connectivity after loading the local page to demonstrate local analysis. Saved/exported reports can show what was inspected without exposing the original message.

See [the release and validation report](../docs/chrome-extension-release.md) and [the original implementation plan](../docs/chrome-extension-plan.md). Store publication, independently labeled fresh-data evaluation, signed publisher updates and webmail layout adapters remain follow-up work.
