import { chromium, expect } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";
import { createServer } from "node:http";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
process.chdir(root);
await fs.mkdir("test-results", { recursive: true });
const findings = [],
  errors = [],
  requests = [],
  warnings = [];
let ctx;
async function check(name, fn) {
  const start = Date.now();
  try {
    await fn();
    findings.push({ name, passed: true, durationMs: Date.now() - start });
    console.log(`PASS ${name}`);
  } catch (e) {
    findings.push({
      name,
      passed: false,
      error: String(e),
      durationMs: Date.now() - start,
    });
    console.log(`FAIL ${name}: ${String(e).slice(0, 400)}`);
    throw e;
  }
}
const server = createServer((req, res) => {
  res.setHeader("Content-Type", "text/html");
  res.end(
    '<!doctype html><html><head><title>Merchant sign in</title></head><body><h1>Merchant sign in</h1><p>Enter your password to sign in.</p><input type="password" value="PRIVATE_PASSWORD_DO_NOT_READ"><textarea>PRIVATE_MESSAGE_DO_NOT_READ</textarea><div contenteditable="true">PRIVATE_EDITOR_DO_NOT_READ</div><form action="/session" method="post"><label>Password</label><input type="password"><button>Sign in</button></form><a href="https://different.test/login">https://paypal.com</a><iframe src="about:blank"></iframe></body></html>',
  );
});
await new Promise((r) => server.listen(0, "127.0.0.1", r));
const origin = `http://127.0.0.1:${server.address().port}`;
try {
  ctx = await chromium.launchPersistentContext("", {
    channel: "chromium",
    headless: true,
    args: [
      `--disable-extensions-except=${path.join(root, "dist")}`,
      `--load-extension=${path.join(root, "dist")}`,
    ],
    viewport: { width: 420, height: 1050 },
  });
  ctx.on("page", (p) => {
    p.on("pageerror", (e) => errors.push(String(e)));
    p.on("console", (m) => {
      if (m.type() === "error") {
        if (
          /^Warning: Parameter not found: (classify_misfit_junk_penalty|merge_fragments_in_matrix)$/.test(
            m.text(),
          )
        )
          warnings.push(m.text());
        else errors.push(m.text());
      }
    });
  });
  ctx.on("request", (r) => {
    if (
      !r.url().startsWith("chrome-extension://") &&
      !r.url().startsWith(origin) &&
      !r.url().startsWith("data:")
    )
      requests.push(r.url());
  });
  let [sw] = ctx.serviceWorkers();
  if (!sw) sw = await ctx.waitForEvent("serviceworker");
  const id = sw.url().split("/")[2],
    panel = await ctx.newPage();
  await check(
    "production package installs and side panel renders",
    async () => {
      await panel.goto(`chrome-extension://${id}/sidepanel.html`);
      await expect(
        panel.getByRole("heading", { name: "Check before you click." }),
      ).toBeVisible();
      await expect(panel.locator("#scan-input")).toBeVisible();
    },
  );
  await check(
    "offline text inference completes with real bundled models",
    async () => {
      await ctx.setOffline(true);
      await panel
        .locator("#scan-input")
        .fill(
          "Urgently send your bank password and OTP to this support agent. PRIVATE_INPUT_NONCE",
        );
      await panel.locator("#analyze").click();
      await expect(panel.locator("#scan-result")).toContainText(
        "High-risk evidence",
      );
      const data = await panel.evaluate(() => chrome.storage.session.get(null));
      expect(JSON.stringify(data)).not.toContain("PRIVATE_INPUT_NONCE");
      expect(JSON.stringify(data)).toContain("fraud-text-research-v3");
    },
  );
  await check(
    "explicit save and history omit raw input and credentials",
    async () => {
      await panel
        .getByRole("button", { name: "Save redacted report", exact: true })
        .click();
      await panel.getByRole("button", { name: "History", exact: true }).click();
      await expect(panel.locator(".history-item")).toHaveCount(1);
      const local = await panel.evaluate(() => chrome.storage.local.get(null));
      expect(JSON.stringify(local)).not.toContain("PRIVATE_INPUT_NONCE");
    },
  );
  await check("settings, Gujarati text size and persistence", async () => {
    await panel.getByRole("button", { name: "Settings", exact: true }).click();
    await panel.locator("#language").selectOption("gu");
    await expect(
      panel.getByRole("heading", { name: "ભાષા અને દેખાવ" }),
    ).toBeVisible();
    await panel.locator("#font-size").fill("150");
    await panel.locator("#font-size").dispatchEvent("change");
    await expect
      .poll(() =>
        panel.evaluate(() => getComputedStyle(document.body).fontSize),
      )
      .toBe("21px");
    await panel.screenshot({
      path: "test-results/gujarati-settings-150.png",
      fullPage: true,
    });
    expect(
      await panel.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
    await panel.reload();
    await expect(panel.locator("#language")).toHaveValue("gu");
    await panel.locator("#language").selectOption("en");
  });
  await check("legitimate advice does not produce a scam warning", async () => {
    await panel.getByRole("button", { name: "Scan", exact: true }).click();
    await panel
      .locator("#scan-input")
      .fill(
        "Your parcel arrives this evening. Never share your password or OTP.",
      );
    await panel.locator("#analyze").click();
    await expect(panel.locator("#scan-result")).toContainText(
      "No strong warning signs found",
    );
  });
  await check(
    "optional context adds labeled caution and resets for the next message",
    async () => {
      await panel.locator("#scan-input").fill("Your meeting starts at noon");
      await panel.getByText("Add context (optional)", { exact: true }).click();
      await panel.locator("#context-remoteAccess").check();
      await panel.locator("#analyze").click();
      await expect(panel.locator("#scan-result")).toContainText(
        "You reported a sensitive request",
      );
      await panel
        .locator("#scan-input")
        .fill(
          "Your parcel arrives this evening. Never share your password or OTP.",
        );
      await expect(panel.locator("#context-remoteAccess")).not.toBeChecked();
      await panel.locator("#analyze").click();
      await expect(panel.locator("#scan-result")).toContainText(
        "No strong warning signs found",
      );
    },
  );
  await check("redacted export downloads a real JSON report", async () => {
    await panel
      .getByRole("button", { name: "Mark as incorrect", exact: true })
      .click();
    const feedback = await panel.evaluate(() =>
      chrome.storage.local.get("feedback"),
    );
    expect(feedback.feedback).toHaveLength(1);
    expect(JSON.stringify(feedback)).not.toContain("Your parcel");
    const exported = panel.waitForEvent("download");
    await panel
      .getByRole("button", { name: "Export redacted report", exact: true })
      .click();
    const file = await exported,
      report = JSON.parse(await fs.readFile(await file.path(), "utf8"));
    expect(report.application).toBe("SafeX AI");
    expect(JSON.stringify(report)).not.toContain("Your parcel");
    expect(report.report.pageKey).toBeUndefined();
    expect(report.feedback).toBe("incorrect");
  });
  await check("English, Hindi and Gujarati user-selected scan UI", async () => {
    for (const [lang, input, expected] of [
      [
        "hi",
        "अभी बैंक का पासवर्ड और ओटीपी एजेंट को भेजें।",
        "उच्च जोखिम के संकेत",
      ],
      [
        "gu",
        "હમણાં બેંકનો પાસવર્ડ અને ઓટીપી એજન્ટને મોકલો.",
        "ઊંચા જોખમના સંકેત",
      ],
    ]) {
      await panel
        .getByRole("button", {
          name: lang === "hi" ? "Settings" : "सेटिंग्स",
          exact: true,
        })
        .click();
      await panel.locator("#language").selectOption(lang);
      await panel
        .getByRole("button", {
          name: lang === "hi" ? "जाँच" : "તપાસ",
          exact: true,
        })
        .click();
      await panel.locator("#scan-input").fill(input);
      await panel.locator("#analyze").click();
      await expect(panel.locator("#scan-result")).toContainText(expected);
    }
    await panel.getByRole("button", { name: "સેટિંગ્સ", exact: true }).click();
    await panel.locator("#language").selectOption("en");
    await panel.locator("#font-size").fill("100");
    await panel.locator("#font-size").dispatchEvent("change");
    await panel.getByRole("button", { name: "Scan", exact: true }).click();
  });
  await check(
    "imported image, crop and English recognition work offline",
    async () => {
      const chooser = panel.waitForEvent("filechooser");
      await panel
        .getByRole("button", { name: "Choose an image", exact: true })
        .click();
      await (
        await chooser
      ).setFiles(path.join(root, "../demo-assets/synthetic-message.png"));
      await panel
        .getByRole("button", { name: "Use full image", exact: true })
        .click();
      await panel
        .getByRole("button", { name: "Read text & QR locally", exact: true })
        .click();
      await expect(panel.locator("#ocr-text")).toBeVisible({ timeout: 90000 });
      expect(
        (await panel.locator("#ocr-text").inputValue()).length,
      ).toBeGreaterThan(10);
      await panel.getByRole("button", { name: "Cancel", exact: true }).click();
    },
  );
  await check(
    "a real pointer crop and cancelled recognition recover into a fresh job",
    async () => {
      const choose = async () => {
        const file = panel.waitForEvent("filechooser");
        await panel
          .getByRole("button", { name: "Choose an image", exact: true })
          .click();
        await (
          await file
        ).setFiles(path.join(root, "../demo-assets/synthetic-message.png"));
      };
      await choose();
      const canvas = panel.locator("#crop-preview");
      await canvas.scrollIntoViewIfNeeded();
      const box = await canvas.boundingBox(),
        originalWidth = (
          await fs.readFile(
            path.join(root, "../demo-assets/synthetic-message.png"),
          )
        ).readUInt32BE(16);
      await panel.mouse.move(
        box.x + box.width * 0.02,
        box.y + box.height * 0.02,
      );
      await panel.mouse.down();
      await panel.mouse.move(
        box.x + box.width * 0.9,
        box.y + box.height * 0.9,
        { steps: 5 },
      );
      await panel.mouse.up();
      await panel
        .getByRole("button", { name: "Use this crop", exact: true })
        .click();
      expect(await canvas.evaluate((c) => c.width)).toBeLessThan(originalWidth);
      await panel
        .getByRole("button", { name: "Read text & QR locally", exact: true })
        .click();
      await panel.getByRole("button", { name: "Cancel", exact: true }).click();
      await expect(canvas).toHaveCount(0);
      await choose();
      await panel
        .getByRole("button", { name: "Use full image", exact: true })
        .click();
      await panel
        .getByRole("button", { name: "Read text & QR locally", exact: true })
        .click();
      await expect(panel.locator("#ocr-text")).toBeVisible({ timeout: 90000 });
      await panel.getByRole("button", { name: "Cancel", exact: true }).click();
    },
  );
  await check(
    "native Hindi recognition uses packaged offline assets",
    async () => {
      const chooser = panel.waitForEvent("filechooser");
      await panel
        .getByRole("button", { name: "Choose an image", exact: true })
        .click();
      await (
        await chooser
      ).setFiles(path.join(root, "../demo-assets/synthetic-message-hindi.png"));
      await panel
        .getByRole("button", { name: "Use full image", exact: true })
        .click();
      await panel.locator("#ocr-language").selectOption("hi");
      await panel
        .getByRole("button", { name: "Read text & QR locally", exact: true })
        .click();
      await expect(panel.locator("#ocr-text")).toBeVisible({ timeout: 90000 });
      expect(await panel.locator("#ocr-text").inputValue()).toMatch(
        /[\u0900-\u097f]/,
      );
      await panel.getByRole("button", { name: "Cancel", exact: true }).click();
    },
  );
  await check(
    "native Gujarati recognition uses packaged offline assets",
    async () => {
      const chooser = panel.waitForEvent("filechooser");
      await panel
        .getByRole("button", { name: "Choose an image", exact: true })
        .click();
      await (
        await chooser
      ).setFiles(
        path.join(root, "../demo-assets/synthetic-message-gujarati.png"),
      );
      await panel
        .getByRole("button", { name: "Use full image", exact: true })
        .click();
      await panel.locator("#ocr-language").selectOption("gu");
      await panel
        .getByRole("button", { name: "Read text & QR locally", exact: true })
        .click();
      await expect(panel.locator("#ocr-text")).toBeVisible({ timeout: 90000 });
      expect(await panel.locator("#ocr-text").inputValue()).toMatch(
        /[\u0a80-\u0aff]/,
      );
      await panel.getByRole("button", { name: "Cancel", exact: true }).click();
    },
  );
  await check(
    "QR extraction is separate and content can be scanned",
    async () => {
      const chooser = panel.waitForEvent("filechooser");
      await panel
        .getByRole("button", { name: "Choose an image", exact: true })
        .click();
      await (
        await chooser
      ).setFiles(path.join(root, "../demo-assets/synthetic-qr.png"));
      await panel
        .getByRole("button", { name: "Use full image", exact: true })
        .click();
      await panel.locator("#ocr-language").selectOption("en");
      await panel
        .getByRole("button", { name: "Read text & QR locally", exact: true })
        .click();
      await expect(
        panel
          .getByRole("button", { name: "Check QR content", exact: true })
          .first(),
      ).toBeVisible({ timeout: 90000 });
      await panel
        .getByRole("button", { name: "Check QR content", exact: true })
        .first()
        .click();
      await expect(panel.locator("#scan-result")).toBeVisible();
      await panel.getByRole("button", { name: "Cancel", exact: true }).click();
    },
  );
  await check("scan state and fonts reflow at narrow panel width", async () => {
    await panel.setViewportSize({ width: 320, height: 1000 });
    await panel
      .locator("#scan-input")
      .fill("Urgently send your bank password and OTP to this support agent.");
    await panel.locator("#analyze").click();
    await panel.screenshot({
      path: "test-results/scan-warning.png",
      fullPage: true,
    });
    expect(
      await panel.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
    await panel.evaluate(() =>
      chrome.runtime.sendMessage({
        type: "PREFERENCES",
        patch: { theme: "dark" },
      }),
    );
    await expect(panel.locator("html")).toHaveAttribute("data-theme", "dark");
    await panel.screenshot({
      path: "test-results/scan-warning-dark.png",
      fullPage: true,
    });
  });
  await check("actual extension requests stay local", async () => {
    expect(requests).toEqual([]);
  });
  await check("runtime has no page or CSP errors", async () => {
    expect(errors).toEqual([]);
  });
} finally {
  if (ctx) await ctx.close();
  await new Promise((r) => server.close(r));
  await fs.writeFile(
    "test-results/browser-summary.json",
    JSON.stringify(
      {
        browser: "Playwright Chromium 156",
        productionManifest: true,
        findings,
        consoleErrors: errors,
        recognitionLibraryWarnings: warnings,
        unexpectedRequests: requests,
      },
      null,
      2,
    ),
  );
}
