import { chromium, expect } from "@playwright/test";
import fs from "node:fs/promises";
import path from "node:path";
import { createServer } from "node:http";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
process.chdir(root);
const findings = [],
  errors = [];
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
    console.log(`FAIL ${name}: ${String(e).slice(0, 450)}`);
    throw e;
  }
}
const server = createServer((req, res) => {
  res.setHeader("Content-Type", "text/html");
  res.end(
    '<!doctype html><html><head><title>PayPal account sign in</title></head><body><h1>PayPal Account</h1><p>Sign in to verify your account.</p><input type="password" value="PRIVATE_PASSWORD_DO_NOT_READ"><textarea>PRIVATE_MESSAGE_DO_NOT_READ</textarea><div contenteditable="true">PRIVATE_EDITOR_DO_NOT_READ</div><form action="/session" method="post"><label>Password<textarea>PRIVATE_NESTED_LABEL_DO_NOT_READ</textarea></label><input type="password"><button>Sign in</button></form><a id="trap" href="https://different.test/login">https://paypal.com<span contenteditable="true">PRIVATE_NESTED_LINK_DO_NOT_READ</span></a><iframe src="about:blank"></iframe></body></html>',
  );
});
await new Promise((r) => server.listen(0, "127.0.0.1", r));
const origin = `http://127.0.0.1:${server.address().port}`;
try {
  ctx = await chromium.launchPersistentContext("", {
    executablePath:
      process.env.SAFEX_CHROME_PATH ?? "/opt/google/chrome/google-chrome",
    headless: process.env.SAFEX_HEADLESS !== "0",
    ignoreDefaultArgs: ["--disable-extensions"],
    args: ["--enable-unsafe-extension-debugging"],
    viewport: { width: 1000, height: 800 },
  });
  const browser = await ctx.browser().newBrowserCDPSession();
  const { id } = await browser.send("Extensions.loadUnpacked", {
    path: path.join(root, "dist"),
  });
  const panel = await ctx.newPage();
  await panel.goto(`chrome-extension://${id}/sidepanel.html`);
  await expect(panel.locator("#scan-input")).toBeVisible();
  await panel.evaluate(() => chrome.runtime.sendMessage({ type: "GET_STATE" }));
  const website = await ctx.newPage();
  website.on("pageerror", (e) => errors.push(String(e)));
  await website.goto(origin);
  const { targetInfos } = await browser.send("Target.getTargets", {
      filter: [{ type: "tab" }],
    }),
    targetInfo = targetInfos.find((t) => t.url === website.url());
  await check(
    "installed Chrome 154 loads production manifest and toolbar action",
    async () => {
      await Promise.race([
        browser.send("Extensions.triggerAction", {
          id,
          targetId: targetInfo.targetId,
        }),
        new Promise((_, reject) => {
          const timeout = setTimeout(
            () =>
              reject(Error("Toolbar action did not respond within 10 seconds")),
            10000,
          );
          timeout.unref();
        }),
      ]);
      await expect(website.locator("[data-safex-root]")).toBeVisible();
    },
  );
  await website.bringToFront();
  const request = (type, payload = {}) =>
    panel.evaluate((m) => chrome.runtime.sendMessage(m), { type, ...payload });
  await check("real activeTab grant enables bounded page scan", async () => {
    const r = await request("SCAN_PAGE");
    expect(r.ok).toBeTruthy();
    expect(r.result.verdict).toBe("high");
    expect(
      r.result.reasons.some((e) => e.id === "login_impersonation"),
    ).toBeTruthy();
    await expect(website.locator("[data-safex-root] .warning")).toBeVisible();
  });
  await check(
    "actual DOM extraction excludes passwords, textarea and editors",
    async () => {
      const data = await panel.evaluate(async () => {
        const [tab] = await chrome.tabs.query({
          active: true,
          currentWindow: true,
        });
        return chrome.tabs.sendMessage(tab.id, { type: "PAGE_EXTRACT" });
      });
      expect(JSON.stringify(data)).not.toContain("PRIVATE_");
      expect(data.forms[0].password).toBe(true);
      expect(data.inaccessibleFrames).toBe(1);
    },
  );
  await check(
    "screenshot capture is user-scoped and supports safe local crop",
    async () => {
      const r = await request("CAPTURE");
      expect(r.ok).toBeTruthy();
      expect(r.data).toMatch(/^data:image\/png;base64,/);
    },
  );
  await check(
    "untrusted webpage cannot call privileged report/capture commands",
    async () => {
      const result = await panel.evaluate(async () => {
        const [tab] = await chrome.tabs.query({
          active: true,
          currentWindow: true,
        });
        const [{ result }] = await chrome.scripting.executeScript({
          target: { tabId: tab.id },
          func: () => chrome.runtime.sendMessage({ type: "CAPTURE" }),
        });
        return result;
      });
      expect(result.ok).toBe(false);
    },
  );
  await check(
    "optional site monitoring requires persistent site authorization",
    async () => {
      const r = await request("SITE_ENABLE");
      expect(r.ok).toBe(false);
      expect(r.error).toBe("permissionDenied");
    },
  );
  await check(
    "service-worker termination preserves session result and preferences",
    async () => {
      const session = await ctx.newCDPSession(panel),
        versions = new Map();
      session.on(
        "ServiceWorker.workerVersionUpdated",
        ({ versions: updates }) =>
          updates.forEach((v) => versions.set(v.versionId, v)),
      );
      await session.send("ServiceWorker.enable");
      await expect
        .poll(() =>
          [...versions.values()].some(
            (v) => v.scriptURL === `chrome-extension://${id}/background.js`,
          ),
        )
        .toBe(true);
      const version = [...versions.values()].find(
          (v) =>
            v.scriptURL === `chrome-extension://${id}/background.js` &&
            v.runningStatus === "running",
        ),
        prior = await request("GET_STATE");
      expect(version).toBeTruthy();
      await session.send("ServiceWorker.stopWorker", {
        versionId: version.versionId,
      });
      const after = await request("GET_STATE");
      expect(after.ok).toBe(true);
      expect(after.preferences).toEqual(prior.preferences);
      expect(after.result?.id).toBe(prior.result?.id);
      await session.detach();
    },
  );
  await check("warning audio uses packaged offscreen playback", async () => {
    const r = await request("TEST_SOUND");
    expect(r.ok).toBeTruthy();
    const has = await panel.evaluate(() => chrome.offscreen.hasDocument());
    expect(has).toBe(true);
  });
  await check(
    "selected-site protection starts after an actual Chrome profile grant",
    async () => {
      const admin = await ctx.newPage();
      await admin.goto("chrome://extensions/");
      await admin.evaluate(
        ({ id, origin }) =>
          chrome.developerPrivate.addHostPermission(id, `${origin}/*`),
        { id, origin },
      );
      await admin.close();
      await website.bringToFront();
      expect(
        await panel.evaluate(
          (origin) => chrome.permissions.request({ origins: [`${origin}/*`] }),
          origin,
        ),
      ).toBe(true);
      const enabled = await request("SITE_ENABLE");
      expect(enabled.ok, JSON.stringify(enabled)).toBe(true);
      await expect
        .poll(async () => (await request("GET_STATE")).result?.verdict)
        .toBe("high");
    },
  );
  await check(
    "sensitive form submission is paused without reading the typed value",
    async () => {
      await website
        .getByRole("button", { name: "Sign in", exact: true })
        .click();
      expect(website.url()).toBe(`${origin}/`);
      await expect(website.locator("[data-safex-root] .warning")).toBeVisible();
      const local = await panel.evaluate(() => chrome.storage.local.get(null));
      expect(JSON.stringify(local)).not.toContain("PRIVATE_");
    },
  );
  await check(
    "click guard retains displayed-address evidence and a usable choice",
    async () => {
      await website.locator("#trap").click();
      await expect(
        website
          .locator("[data-safex-root]")
          .getByRole("button", { name: "Continue this click", exact: true }),
      ).toBeVisible();
      expect(website.url()).toBe(`${origin}/`);
      const r = (await request("GET_STATE")).result;
      expect(r.reasons.some((e) => e.id === "display_mismatch")).toBe(true);
      expect(r.verdict).not.toBe("clear");
      await website
        .locator("[data-safex-root]")
        .getByRole("button", { name: "Dismiss", exact: true })
        .click();
    },
  );
  await check(
    "pause, resume and revoke stop and restore automatic checks",
    async () => {
      const paused = await request("SITE_PAUSE");
      expect(paused.ok, JSON.stringify(paused)).toBe(true);
      await expect(website.locator("[data-safex-root]")).toHaveCount(0);
      expect((await request("GET_STATE")).result).toBeUndefined();
      const resumed = await request("SITE_PAUSE");
      expect(resumed.ok, JSON.stringify(resumed)).toBe(true);
      await expect
        .poll(async () => (await request("GET_STATE")).result?.verdict)
        .toBe("high");
      const revoked = await request("SITE_REVOKE");
      expect(revoked.ok, JSON.stringify(revoked)).toBe(true);
      expect(
        await panel.evaluate(
          (origin) => chrome.permissions.contains({ origins: [`${origin}/*`] }),
          origin,
        ),
      ).toBe(false);
      expect(
        await panel.evaluate(() =>
          chrome.scripting.getRegisteredContentScripts(),
        ),
      ).toHaveLength(0);
    },
  );
  await check(
    "exact imported blocking rules work and reported-only entries do not block",
    async () => {
      const imported = await request("IMPORT_LIST", {
        raw: JSON.stringify([
          { url: `${origin}/blocked`, label: "block" },
          { url: `${origin}/reported`, label: "reported" },
        ]),
        name: "Synthetic-local-test.json",
      });
      expect(imported.ok).toBe(true);
      const enabled = await request("PREFERENCES", {
        patch: { blocking: true },
      });
      expect(enabled.ok).toBe(true);
      const rules = await panel.evaluate(() =>
        chrome.declarativeNetRequest.getDynamicRules(),
      );
      expect(rules).toHaveLength(1);
      const blocked = await ctx.newPage();
      await expect(blocked.goto(`${origin}/blocked`)).rejects.toThrow(
        /ERR_BLOCKED_BY_CLIENT/,
      );
      await blocked.close();
      await website.bringToFront();
      await website.goto(`${origin}/reported`);
      expect(await website.title()).toContain("PayPal");
      await request("PREFERENCES", { patch: { blocking: false } });
      expect(
        await panel.evaluate(() =>
          chrome.declarativeNetRequest.getDynamicRules(),
        ),
      ).toHaveLength(0);
    },
  );
  await check("navigation invalidates the previous tab result", async () => {
    const state = await request("GET_STATE");
    expect(state.result).toBeUndefined();
  });
  await check(
    "restricted pages return usable fallback, never clear",
    async () => {
      await website.goto("chrome://settings/");
      const r = await request("SCAN_PAGE");
      expect(r.ok).toBe(false);
      expect(r.error).toBe("pageUnavailable");
    },
  );
  await check(
    "page extraction and warning code have no runtime exceptions",
    async () => expect(errors).toEqual([]),
  );
} finally {
  if (ctx) await ctx.close();
  await new Promise((r) => server.close(r));
  await fs.writeFile(
    "test-results/protection-summary.json",
    JSON.stringify(
      {
        browser: "Installed Google Chrome 154.0.8037.57",
        productionManifest: true,
        findings,
        runtimeErrors: errors,
      },
      null,
      2,
    ),
  );
}
