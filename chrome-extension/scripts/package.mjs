import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import { createHash } from "node:crypto";
const { version } = JSON.parse(await fs.readFile("manifest.json", "utf8"));
const artifact = `SafeX-AI-Chrome-${version}.zip`;
await fs.mkdir("release", { recursive: true });
execFileSync(
  "python3",
  [
    "-c",
    `from pathlib import Path
import zipfile
with zipfile.ZipFile('release/${artifact}','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for p in sorted(Path('dist').rglob('*')):
  if p.is_file(): z.write(p,p.relative_to('dist'))
`,
  ],
  { stdio: "inherit" },
);
const data = await fs.readFile(`release/${artifact}`);
const result = {
  file: artifact,
  bytes: data.length,
  sha256: createHash("sha256").update(data).digest("hex"),
};
await fs.writeFile(
  "release/checksum.json",
  JSON.stringify(result, null, 2) + "\n",
);
console.log(result);
