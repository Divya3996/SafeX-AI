import { chromium, expect } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
process.chdir(root);
const findings = [],
  errors = [],
  requests = [];
const copy = JSON.parse(await fs.readFile("src/guidance-copy.json", "utf8"));
let context;
async function check(name, fn) {
  try {
    await fn();
    findings.push({ name, passed: true });
    console.log("PASS " + name);
  } catch (e) {
    findings.push({ name, passed: false, error: String(e) });
    throw e;
  }
}
try {
  context = await chromium.launchPersistentContext("", {
    executablePath:
      process.env.SAFEX_CHROME_PATH ?? "/opt/google/chrome/google-chrome",
    headless: true,
    ignoreDefaultArgs: ["--disable-extensions"],
    args: ["--enable-unsafe-extension-debugging"],
    viewport: { width: 420, height: 850 },
  });
  context.on("request", (r) => {
    if (
      !r.url().startsWith("chrome-extension://") &&
      !r.url().startsWith("data:")
    )
      requests.push(r.url());
  });
  const browser = await context.browser().newBrowserCDPSession();
  const { id } = await browser.send("Extensions.loadUnpacked", {
    path: path.join(root, "dist"),
  });
  const page = await context.newPage();
  page.on("pageerror", (e) => errors.push(String(e)));
  const url = `chrome-extension://${id}/sidepanel.html`;
  const message = (type, patch) =>
    page.evaluate((m) => chrome.runtime.sendMessage(m), { type, patch });
  await context.setOffline(true);
  await page.goto(url);
  await check(
    "first launch shows a branded skippable introduction offline",
    async () => {
      await expect(page.locator("#guide-title")).toHaveText(
        "Welcome to SafeX AI",
      );
      await expect(page.locator("#guide-dialog img")).toHaveAttribute(
        "src",
        "logo.svg",
      );
      expect(
        (await page.evaluate(() => chrome.storage.local.get(null))).guidance,
      ).toBeUndefined();
      await page.locator("#guide-next").click();
    },
  );
  await check(
    "Hindi and Gujarati introduction update immediately at 150% reading size",
    async () => {
      for (const [language, index] of [
        ["hi", 0],
        ["gu", 1],
      ]) {
        await page.locator("#guide-language").selectOption(language);
        await expect(page.locator("#guide-title")).toHaveText(
          copy["Your language. Your reading size."][index],
        );
        await page.locator("#guide-size").selectOption("150");
        await expect
          .poll(() =>
            page.evaluate(() => getComputedStyle(document.body).fontSize),
          )
          .toBe("21px");
        await page.locator("#guide-next").scrollIntoViewIfNeeded();
        await expect(page.locator("#guide-next")).toBeInViewport();
        await page.screenshot({
          path: `test-results/introduction-${language}-150.png`,
        });
      }
      await page.locator("#guide-language").selectOption("en");
      await page.locator("#guide-size").selectOption("100");
      await page.locator("#guide-next").click();
      await page.locator("#guide-next").click();
      await page.locator("#guide-next").click();
    },
  );
  await check(
    "all 12 tour steps highlight actual controls and support Back",
    async () => {
      const targets = [
        '[data-guide="site"]',
        "#scan-input",
        '[data-guide="context"]',
        '[data-guide="capture"]',
        '[data-guide="images"]',
        '[data-guide="samples"]',
        "main",
        '[data-guide="reading"]',
        '[data-guide="sound"]',
        '[data-guide="lists"]',
        '[data-guide="situation"]',
        '[data-guide="contact"]',
      ];
      for (let i = 0; i < targets.length; i++) {
        await expect(page.locator("#guide-dialog .small")).toHaveText(
          `${i + 1} / 12`,
        );
        await expect(page.locator(targets[i])).toBeVisible();
        await expect(page.locator(".guide-ring")).toBeVisible();
        await expect
          .poll(async () => {
            const box = await page.locator(".guide-ring").boundingBox();
            return box && box.width > 10 && box.height > 10;
          })
          .toBeTruthy();
        if (i === 4) {
          await page.locator("#guide-back").click();
          await expect(page.locator("#guide-dialog .small")).toHaveText(
            "4 / 12",
          );
          await page.locator("#guide-next").click();
        }
        await page.locator("#guide-next").click();
      }
      await expect(page.locator("#guide-dialog")).toHaveCount(0);
      const local = await page.evaluate(() => chrome.storage.local.get(null));
      expect(local.guidance).toEqual({
        version: 1,
        introComplete: true,
        tourComplete: true,
      });
      expect(local.reports ?? []).toHaveLength(0);
      expect(local.preferences.sound).toBe(false);
    },
  );
  await check(
    "dismissed introduction persists and feature tour can be replayed and escaped",
    async () => {
      await page.reload();
      await expect(page.locator("main")).toBeVisible();
      await expect(page.locator("#guide-dialog")).toHaveCount(0);
      await page
        .getByRole("button", { name: "Learn SafeX AI", exact: true })
        .click();
      await page.locator("#replay-tour").click();
      await expect(page.locator("#guide-dialog")).toBeVisible();
      await page.keyboard.press("Escape");
      await expect(page.locator("#guide-dialog")).toHaveCount(0);
    },
  );
  await check(
    "all five scam checklists and their checkboxes work offline",
    async () => {
      await page.getByRole("button", { name: "Help", exact: true }).click();
      for (let i = 0; i < 5; i++) {
        await page.locator("#incident-type").selectOption(String(i));
        const checkbox = page.locator(".incident-checklist input").first();
        await checkbox.check();
        await expect(checkbox).toBeChecked();
      }
      await expect(page.locator(".incident-checklist")).toContainText(
        "Do not pay anyone who promises to recover your money.",
      );
    },
  );
  await check("helpline number copy works without placing a call", async () => {
    await page.bringToFront();
    await page.locator('[data-phone="1930"]').click();
    await expect(page.locator('[data-phone="1930"]')).toContainText(
      "Number copied.",
    );
    await context.grantPermissions(["clipboard-read"]);
    expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(
      "1930",
    );
  });
  await check(
    "1930 and 112 require deliberate confirmation without making any call",
    async () => {
      for (const number of ["1930", "112"]) {
        await page.locator(`[data-dial="${number}"]`).click();
        await expect(page.locator("dialog")).toContainText(number);
        await expect(page.locator("dialog a")).toHaveAttribute(
          "href",
          `tel:${number}`,
        );
        await page
          .locator("dialog")
          .getByRole("button", { name: "Cancel", exact: true })
          .click();
      }
      await expect(
        page.getByRole("link", {
          name: "Open official reporting website",
          exact: true,
        }),
      ).toHaveAttribute("href", "https://cybercrime.gov.in/");
      await page.locator("#incident-country").selectOption("OTHER");
      await expect(page.locator("[data-dial]")).toHaveCount(0);
      await expect(page.locator("main")).toContainText(
        "your country's emergency service",
      );
    },
  );
  await check(
    "help and replay guidance stay translated in Hindi and Gujarati",
    async () => {
      for (const [language, index] of [
        ["hi", 0],
        ["gu", 1],
      ]) {
        await message("PREFERENCES", { language, fontSize: 150 });
        await page.reload();
        await expect(
          page.getByRole("button", { name: copy.Help[index], exact: true }),
        ).toBeVisible();
        await page
          .getByRole("button", { name: copy.Help[index], exact: true })
          .click();
        await page.locator("#incident-country").selectOption("IN");
        await expect(page.locator("#incident-type option").first()).toHaveText(
          copy["I opened a link"][index],
        );
        await expect(page.locator("main")).toContainText(
          copy["Financial cyber fraud • 1930"][index],
        );
        await page
          .getByRole("button", {
            name: copy["Learn SafeX AI"][index],
            exact: true,
          })
          .click();
        await page.locator("#replay-tour").click();
        await expect(page.locator("#guide-title")).toHaveText(
          copy["Check the current page"][index],
        );
        await page.locator("#guide-skip").click();
      }
    },
  );
  await check(
    "guidance update rejects permission fields and wrong types",
    async () => {
      expect((await message("GUIDANCE", { sound: true })).ok).toBe(false);
      expect((await message("GUIDANCE", { introComplete: "yes" })).ok).toBe(
        false,
      );
      expect((await message("GUIDANCE", [])).ok).toBe(false);
      expect((await message("GET_STATE")).guidance.introComplete).toBe(true);
    },
  );
  await check(
    "urgent help can bypass onboarding and Skip persists after reopening",
    async () => {
      await message("PREFERENCES", { language: "en", fontSize: 100 });
      await message("GUIDANCE", { introComplete: false, tourComplete: false });
      await page.reload();
      await expect(page.locator("#guide-dialog")).toBeVisible();
      await page
        .locator("#guide-dialog")
        .getByRole("button", { name: "Need help after a scam?", exact: true })
        .click();
      await expect(page.locator("#incident-type")).toBeVisible();
      await page.reload();
      await page.locator("#guide-skip").click();
      await page.reload();
      await expect(page.locator("#guide-dialog")).toHaveCount(0);
      expect((await message("GET_STATE")).guidance.introComplete).toBe(true);
    },
  );
  await check(
    "no runtime errors or network requests during offline onboarding and help",
    async () => {
      expect(errors).toEqual([]);
      expect(requests).toEqual([]);
    },
  );
} finally {
  await fs.mkdir("test-results", { recursive: true });
  await fs.writeFile(
    "test-results/guidance-browser.json",
    JSON.stringify(
      { date: new Date().toISOString(), findings, errors, requests },
      null,
      2,
    ),
  );
  await context?.close();
}
