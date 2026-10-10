import fs from "node:fs/promises";
import path from "node:path";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
process.chdir(root);
const read = async (file) => JSON.parse(await fs.readFile(file, "utf8"));
const ui = await read("test-results/browser-summary.json"),
  protection = await read("test-results/protection-summary.json"),
  guidance = await read("test-results/guidance-browser.json"),
  audit = await read("test-results/dependency-audit.json"),
  artifact = await read("release/checksum.json");
const unit = await fs.readFile("test-results/unit.txt", "utf8"),
  passed = Number(unit.match(/ℹ pass (\d+)/)?.[1]),
  failed = Number(unit.match(/ℹ fail (\d+)/)?.[1]);
if (
  !passed ||
  failed !== 0 ||
  [...ui.findings, ...protection.findings, ...guidance.findings].some(
    (f) => !f.passed,
  ) ||
  guidance.errors.length ||
  guidance.requests.length ||
  ui.consoleErrors.length ||
  ui.unexpectedRequests.length ||
  protection.runtimeErrors.length ||
  audit.metadata.vulnerabilities.total
)
  throw Error("Release checks are incomplete or failed.");
const sha = (bytes) => createHash("sha256").update(bytes).digest("hex");
if (
  sha(await fs.readFile(path.join("release", artifact.file))) !==
  artifact.sha256
)
  throw Error("Release ZIP checksum differs.");
const build = await read("dist/build-manifest.json");
for (const [file, expected] of Object.entries(build.files))
  if (sha(await fs.readFile(path.join("dist", file))) !== expected)
    throw Error("Build file checksum differs: " + file);
const summary = {
  version: build.version,
  validatedAt: new Date().toISOString(),
  scope:
    "Functional development validation; no independent browser accuracy claim",
  unit: {
    passed,
    failed,
    skipped: Number(unit.match(/ℹ skipped (\d+)/)?.[1]),
  },
  browser: {
    passed: ui.findings.length + protection.findings.length,
    failed: 0,
    environments: [ui.browser, protection.browser],
    productionManifest: true,
    unexpectedExtensionRequests: ui.unexpectedRequests.length,
    runtimeErrors: 0,
    knownRecognitionLibraryWarnings: ui.recognitionLibraryWarnings,
  },
  dependencyVulnerabilities: audit.metadata.vulnerabilities.total,
  guidance: { passed: guidance.findings.length, failed: 0, offline: true },
  artifact,
  buildManifestSha256: sha(await fs.readFile("dist/build-manifest.json")),
  modelAssets: await read("public/models/manifest.json"),
  independentBrowserAccuracy: null,
  distribution: "Unpacked release ZIP; Chrome Web Store publication pending",
  limitations: [
    "Authored/reference fixtures do not establish fresh-world or native-language accuracy.",
    "Optional site grant was preapproved in an isolated test profile; human permission dialog interaction was not automated.",
    "No independent OS/device matrix or manual assistive-technology audit.",
    "No layout-specific webmail adapters, remote signed publisher updates or automatic retraining.",
  ],
};
const reports = path.join(root, "../docs/test-results"),
  screenshots = path.join(root, "../docs/screenshots");
await fs.mkdir(reports, { recursive: true });
await fs.mkdir(screenshots, { recursive: true });
await fs.writeFile(
  path.join(reports, "chrome-extension-summary.json"),
  JSON.stringify(summary, null, 2) + "\n",
);
for (const [source, destination] of [
  ["browser-summary.json", "chrome-extension-ui.json"],
  ["protection-summary.json", "chrome-extension-protection.json"],
  ["guidance-browser.json", "chrome-extension-guidance.json"],
  ["dependency-audit.json", "chrome-extension-dependencies.json"],
  ["unit.txt", "chrome-extension-unit.txt"],
])
  await fs.copyFile(
    path.join("test-results", source),
    path.join(reports, destination),
  );
for (const name of [
  "scan-warning",
  "gujarati-settings-150",
  "scan-warning-dark",
])
  await fs.copyFile(
    path.join("test-results", name + ".png"),
    path.join(screenshots, "chrome-extension-" + name + ".png"),
  );
console.log(
  "SafeX release validation recorded: " +
    passed +
    " unit checks and " +
    summary.browser.passed +
    " browser scenarios.",
);
