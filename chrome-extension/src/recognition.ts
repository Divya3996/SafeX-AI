import { createWorker, type Worker } from "tesseract.js";
import jsQR from "jsqr";
import {
  LIMITS,
  type Language,
} from "../../shared/security-engine/src/contracts";
export function dimensions(
  bytes: Uint8Array,
): { width: number; height: number } | null {
  const v = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  if (
    bytes.length >= 24 &&
    bytes[0] === 137 &&
    String.fromCharCode(...bytes.slice(1, 4)) === "PNG"
  )
    return { width: v.getUint32(16), height: v.getUint32(20) };
  if (bytes.length > 12 && bytes[0] === 255 && bytes[1] === 216) {
    let at = 2;
    while (at + 8 < bytes.length) {
      if (bytes[at] !== 255) return null;
      while (bytes[at] === 255) at++;
      const marker = bytes[at++];
      if (marker === 217 || marker === 218) break;
      if (marker === 1 || (marker >= 208 && marker <= 215)) continue;
      const length = v.getUint16(at);
      if (length < 2 || at + length > bytes.length) return null;
      if (
        [
          192, 193, 194, 195, 197, 198, 199, 201, 202, 203, 205, 206, 207,
        ].includes(marker)
      )
        return { width: v.getUint16(at + 5), height: v.getUint16(at + 3) };
      at += length;
    }
  }
  if (
    bytes.length >= 30 &&
    String.fromCharCode(...bytes.slice(0, 4)) === "RIFF" &&
    String.fromCharCode(...bytes.slice(8, 12)) === "WEBP"
  ) {
    const kind = String.fromCharCode(...bytes.slice(12, 16));
    if (kind === "VP8X")
      return {
        width: 1 + bytes[24] + (bytes[25] << 8) + (bytes[26] << 16),
        height: 1 + bytes[27] + (bytes[28] << 8) + (bytes[29] << 16),
      };
    if (
      kind === "VP8 " &&
      bytes[23] === 157 &&
      bytes[24] === 1 &&
      bytes[25] === 42
    )
      return {
        width: v.getUint16(26, true) & 16383,
        height: v.getUint16(28, true) & 16383,
      };
    if (kind === "VP8L" && bytes[20] === 47)
      return {
        width: 1 + bytes[21] + ((bytes[22] & 63) << 8),
        height:
          1 + (bytes[22] >> 6) + (bytes[23] << 2) + ((bytes[24] & 15) << 10),
      };
  }
  return null;
}
export async function decodeImage(blob: Blob): Promise<ImageBitmap> {
  if (blob.size > LIMITS.imageBytes) throw Error("imageTooLarge");
  const meta = dimensions(new Uint8Array(await blob.arrayBuffer()));
  if (!meta || meta.width < 1 || meta.height < 1) throw Error("imageInvalid");
  if (meta.width * meta.height > LIMITS.imagePixels)
    throw Error("imageTooLarge");
  return createImageBitmap(blob);
}
export function dataUrlBlob(data: string): Blob {
  if (!data.startsWith("data:image/png;base64,")) throw Error("imageInvalid");
  const b = atob(data.slice(data.indexOf(",") + 1));
  if (b.length > LIMITS.imageBytes) throw Error("imageTooLarge");
  return new Blob([Uint8Array.from(b, (c) => c.charCodeAt(0))], {
    type: "image/png",
  });
}
let worker: Worker | undefined,
  generation = 0;
export async function cancelRecognition() {
  generation++;
  const old = worker;
  worker = undefined;
  await old?.terminate().catch(() => undefined);
}
export async function recognize(
  canvas: HTMLCanvasElement,
  language: Language,
  progress: (percent: number) => void,
): Promise<{ text: string; qr: string[]; ocrFailed: boolean }> {
  const current = ++generation,
    old = worker;
  worker = undefined;
  await old?.terminate().catch(() => undefined);
  if (current !== generation) throw Error("cancel");
  let owned: Worker | undefined;
  const preview = document.createElement("canvas"),
    scale = Math.min(1, 1600 / Math.max(canvas.width, canvas.height));
  preview.width = Math.round(canvas.width * scale);
  preview.height = Math.round(canvas.height * scale);
  preview
    .getContext("2d")!
    .drawImage(canvas, 0, 0, preview.width, preview.height);
  const context = preview.getContext("2d", { willReadFrequently: true })!,
    pixels = context.getImageData(0, 0, preview.width, preview.height),
    qr: string[] = [];
  const data = new Uint8ClampedArray(pixels.data);
  for (let attempt = 0; attempt < 4; attempt++) {
    const code = jsQR(data, preview.width, preview.height, {
      inversionAttempts: "attemptBoth",
    });
    if (!code) break;
    if (!qr.includes(code.data)) qr.push(code.data.slice(0, LIMITS.text));
    const corners = [
      code.location.topLeftCorner,
      code.location.topRightCorner,
      code.location.bottomLeftCorner,
      code.location.bottomRightCorner,
    ];
    const minX = Math.max(
        0,
        Math.floor(Math.min(...corners.map((p) => p.x))) - 4,
      ),
      maxX = Math.min(
        preview.width,
        Math.ceil(Math.max(...corners.map((p) => p.x))) + 4,
      ),
      minY = Math.max(0, Math.floor(Math.min(...corners.map((p) => p.y))) - 4),
      maxY = Math.min(
        preview.height,
        Math.ceil(Math.max(...corners.map((p) => p.y))) + 4,
      );
    for (let y = minY; y < maxY; y++)
      for (let x = minX; x < maxX; x++) {
        const i = (y * preview.width + x) * 4;
        data[i] = data[i + 1] = data[i + 2] = data[i + 3] = 255;
      }
  }
  let deadline: ReturnType<typeof setTimeout> | undefined,
    timedOut = false;
  try {
    progress(0);
    const langs =
      language === "en" ? "eng" : language === "hi" ? "eng+hin" : "eng+guj";
    const task = (async () => {
      const created = await createWorker(langs, 1, {
        workerPath: chrome.runtime.getURL("recognition/worker.min.js"),
        corePath: chrome.runtime.getURL("recognition/core"),
        langPath: chrome.runtime.getURL("recognition/lang"),
        workerBlobURL: false,
        gzip: false,
        cacheMethod: "none",
        logger: (m) => {
          if (current === generation && m.status === "recognizing text")
            progress(Math.round(m.progress * 100));
        },
      });
      owned = created;
      if (current !== generation || timedOut) {
        await created.terminate().catch(() => undefined);
        throw Error("cancel");
      }
      worker = created;
      const { data } = await created.recognize(canvas.toDataURL("image/png"));
      if (current !== generation) throw Error("cancel");
      return {
        text: data.text.trim().slice(0, LIMITS.text),
        qr,
        ocrFailed: false,
      };
    })();
    return await Promise.race([
      task,
      new Promise<never>((_, reject) => {
        deadline = setTimeout(() => {
          timedOut = true;
          void owned?.terminate().catch(() => undefined);
          reject(Error("recognitionFailed"));
        }, 90000);
      }),
    ]);
  } catch {
    if (current !== generation) throw Error("cancel");
    if (qr.length) return { text: "", qr, ocrFailed: true };
    throw Error("recognitionFailed");
  } finally {
    if (deadline) clearTimeout(deadline);
    if (worker === owned) worker = undefined;
    await owned?.terminate().catch(() => undefined);
  }
}
