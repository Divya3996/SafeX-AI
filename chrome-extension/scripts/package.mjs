import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import { createHash } from "node:crypto";
await fs.mkdir("release", { recursive: true });
execFileSync(
  "python3",
  [
    "-c",
    `from pathlib import Path
import zipfile
with zipfile.ZipFile('release/SafeX-AI-Chrome-1.0.0.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for p in sorted(Path('dist').rglob('*')):
  if p.is_file(): z.write(p,p.relative_to('dist'))
`,
  ],
  { stdio: "inherit" },
);
const data = await fs.readFile("release/SafeX-AI-Chrome-1.0.0.zip");
const result = {
  file: "SafeX-AI-Chrome-1.0.0.zip",
  bytes: data.length,
  sha256: createHash("sha256").update(data).digest("hex"),
};
await fs.writeFile(
  "release/checksum.json",
  JSON.stringify(result, null, 2) + "\n",
);
console.log(result);
