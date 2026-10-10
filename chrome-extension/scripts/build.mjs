import { build } from "esbuild";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { createHash } from "node:crypto";
import sharp from "sharp";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), ".."),
  dist = path.join(root, "dist");
const guidanceCopy = JSON.parse(
  await fs.readFile(path.join(root, "src/guidance-copy.json"), "utf8"),
);
for (const [key, row] of Object.entries(guidanceCopy)) {
  if (row.length !== 2 || row.some((s) => !s.trim()))
    throw Error(`Incomplete guidance translation: ${key}`);
}
await fs.rm(dist, { recursive: true, force: true });
await fs.mkdir(dist, { recursive: true });
await fs.cp(path.join(root, "public"), dist, { recursive: true });
await fs.copyFile(
  path.join(root, "manifest.json"),
  path.join(dist, "manifest.json"),
);
await Promise.all(
  ["background", "content", "sidepanel", "offscreen"].map((name) =>
    build({
      entryPoints: [path.join(root, "src", `${name}.ts`)],
      bundle: true,
      outfile: path.join(dist, `${name}.js`),
      format: name === "content" || name === "offscreen" ? "iife" : "esm",
      platform: "browser",
      target: "chrome116",
      loader: { ".svg": "text" },
      legalComments: "linked",
      minify: true,
      metafile: true,
    }).then(async (result) => {
      await fs.mkdir(path.join(root, "test-results"), { recursive: true });
      await fs.writeFile(
        path.join(root, "test-results", `${name}-bundle.json`),
        JSON.stringify(result.metafile, null, 2),
      );
    }),
  ),
);
const temp = path.join(root, "test-results", "build-locales.mjs");
await build({
  entryPoints: [path.join(root, "src/i18n.ts")],
  bundle: true,
  outfile: temp,
  platform: "node",
  format: "esm",
});
const { COPY } = await import(pathToFileURL(temp));
for (const [i, language] of ["en", "hi", "gu"].entries()) {
  const messages = {};
  for (const [key, row] of Object.entries(COPY)) {
    if (row.length !== 3 || row.some((s) => !s.trim()))
      throw Error(`Incomplete translation: ${key}`);
    const placeholders = row.map((s) =>
      (s.match(/\{\d+\}/g) ?? []).sort().join(),
    );
    if (new Set(placeholders).size !== 1)
      throw Error(`Placeholder mismatch: ${key}`);
    messages[key] = { message: row[i] };
  }
  await fs.mkdir(path.join(dist, "_locales", language), { recursive: true });
  await fs.writeFile(
    path.join(dist, "_locales", language, "messages.json"),
    JSON.stringify(messages, null, 2),
  );
}
const logo = path.join(root, "../branding/safex-logo.svg");
await fs.copyFile(logo, path.join(dist, "logo.svg"));
await fs.mkdir(path.join(dist, "icons"), { recursive: true });
for (const size of [16, 32, 48, 128])
  await sharp(logo)
    .resize(size, size)
    .png()
    .toFile(path.join(dist, "icons", `icon-${size}.png`));
await fs.copyFile(
  path.join(root, "../android-app/core/src/main/res/raw/safex_warning.wav"),
  path.join(dist, "warning.wav"),
);
await fs.mkdir(path.join(dist, "recognition/core"), { recursive: true });
for (const name of await fs.readdir(
  path.join(root, "node_modules/tesseract.js-core"),
))
  if (name.endsWith(".wasm") || name.endsWith(".wasm.js"))
    await fs.copyFile(
      path.join(root, "node_modules/tesseract.js-core", name),
      path.join(dist, "recognition/core", name),
    );
await fs.copyFile(
  path.join(root, "node_modules/tesseract.js/dist/worker.min.js"),
  path.join(dist, "recognition/worker.min.js"),
);
await fs.copyFile(
  path.join(root, "node_modules/tesseract.js/dist/worker.min.js.LICENSE.txt"),
  path.join(dist, "recognition/worker.min.js.LICENSE.txt"),
);
const licenses = [];
await fs.mkdir(path.join(dist, "licenses"), { recursive: true });
for (const pkg of [
  "tldts",
  "tldts-core",
  "punycode",
  "jsqr",
  "tesseract.js",
  "tesseract.js-core",
  "bmp-js",
  "idb-keyval",
  "is-url",
  "zlibjs",
  "wasm-feature-detect",
  "regenerator-runtime",
]) {
  const dir = path.join(root, "node_modules", pkg);
  try {
    const data = JSON.parse(
      await fs.readFile(path.join(dir, "package.json"), "utf8"),
    );
    const candidates = (await fs.readdir(dir)).filter((n) =>
      /^licen[sc]e|^copying/i.test(n),
    );
    for (const name of candidates) {
      const output = `${pkg}-${name.replace(/[^a-z\d.-]/gi, "_")}`;
      await fs.copyFile(
        path.join(dir, name),
        path.join(dist, "licenses", output),
      );
      licenses.push({
        package: pkg,
        version: data.version,
        license: data.license,
        file: output,
      });
    }
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
  }
}
const escape = (s) =>
  s.replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
await fs.writeFile(
  path.join(dist, "licenses.html"),
  `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>SafeX AI — licenses</title><link rel="stylesheet" href="sidepanel.css"></head><body><main><section class="card"><h1>SafeX AI — third-party notices</h1><p>Libraries, OCR language assets and research model attribution.</p><ul>${licenses.map((l) => `<li><a href="licenses/${l.file}">${escape(l.package)} ${escape(l.version)} — ${escape(String(l.license))}</a></li>`).join("")}</ul><p><a href="recognition/lang/LICENSE">OCR language data — Apache 2.0</a></p><p><a href="models/security-model-attribution.txt">Security model sources and attribution</a></p><p><a href="licenses/Noto-SIL-OFL.txt">Noto language fonts — SIL Open Font License</a></p><p><a href="privacy.html">Privacy policy</a></p></section></main></body></html>`,
);
const hashes = {};
async function visit(dir) {
  for (const item of await fs.readdir(dir, { withFileTypes: true })) {
    const file = path.join(dir, item.name);
    if (item.isDirectory()) await visit(file);
    else
      hashes[path.relative(dist, file)] = createHash("sha256")
        .update(await fs.readFile(file))
        .digest("hex");
  }
}
await visit(dist);
await fs.writeFile(
  path.join(dist, "build-manifest.json"),
  JSON.stringify(
    {
      version: JSON.parse(
        await fs.readFile(path.join(root, "manifest.json"), "utf8"),
      ).version,
      files: hashes,
    },
    null,
    2,
  ),
);
console.log(
  `SafeX AI built: ${Object.keys(hashes).length} packaged files, ${Object.keys(COPY).length} strings per language.`,
);
