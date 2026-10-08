# SafeX AI mentor website

A static, responsive website containing the complete mentor meeting plan, real app screenshots and downloadable validation material. It uses the existing SafeX AI logo and brand colors. No account, analytics, external fonts or runtime dependencies are added to the page.

The canonical website text is `source/meeting-plan.md`. To regenerate the HTML after editing it:

```sh
python3 build.py
```

Serve `dist/` with any static server, for example `python3 -m http.server 4173 --directory dist`. The page includes section navigation, a mobile navigation drawer, reading progress and a print stylesheet. Supporting Markdown/JSON files are downloadable from `dist/resources/`.

Android APKs remain in the main Android project; they are not hosted by this document website. The page identifies them as project artifacts rather than showing nonfunctional download links.

Sites publishing configuration lives in `.openai/hosting.json`. The website has a separate source repository so publishing does not commit or upload unrelated Android project work.
