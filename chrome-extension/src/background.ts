import { getDomain } from "tldts";
import punycode from "punycode/punycode.js";
import {
  SecurityEngine,
  contextReview,
} from "../../shared/security-engine/src/engine";
import {
  DEFAULT_PREFERENCES,
  LIMITS,
  type Preferences,
  type Models,
  type PageData,
  type Reputation,
  type ScanResult,
  type Source,
  type ContextFlags,
} from "../../shared/security-engine/src/contracts";
import {
  importReputation,
  savedReport,
} from "../../shared/security-engine/src/privacy";
import { t } from "./i18n";

const trusted = (sender: chrome.runtime.MessageSender) =>
  sender.id === chrome.runtime.id &&
  sender.url?.split(/[?#]/)[0] === chrome.runtime.getURL("sidepanel.html");
const web = (url?: string) => {
  try {
    const u = new URL(url ?? "");
    return /^https?:$/.test(u.protocol) ? u : null;
  } catch {
    return null;
  }
};
const domain = (host: string) =>
  getDomain(host, { allowPrivateDomains: true }) ?? host;
interface LocalData {
  preferences?: Preferences;
  reputation?: Reputation;
  reports?: ScanResult[];
  feedback?: {
    id: string;
    checkedAt: number;
    verdict: string;
    modelVersion: string;
  }[];
}
interface StoredResult {
  result: ScanResult;
  expires: number;
}
let enginePromise: Promise<SecurityEngine> | undefined;
async function engine() {
  return (enginePromise ??= loadEngine());
}
async function loadEngine() {
  try {
    const hashes = await (
      await fetch(chrome.runtime.getURL("models/manifest.json"))
    ).json();
    async function asset(name: string) {
      const data = await (
        await fetch(chrome.runtime.getURL(`models/${name}`))
      ).arrayBuffer();
      const actual = [
        ...new Uint8Array(await crypto.subtle.digest("SHA-256", data)),
      ]
        .map((n) => n.toString(16).padStart(2, "0"))
        .join("");
      if (actual !== hashes[name]) throw Error("modelInvalid");
      return JSON.parse(new TextDecoder().decode(data));
    }
    const [text, url, policy] = await Promise.all([
      asset("text-model.json"),
      asset("url-model.json"),
      asset("text-warning-policy.json"),
    ]);
    const models: Models = {
      text,
      url,
      contextFreeThreshold: policy.threshold,
    };
    return new SecurityEngine(models, domain, punycode.toUnicode);
  } catch {
    return new SecurityEngine(undefined, domain, punycode.toUnicode);
  }
}
let serial: Promise<unknown> = Promise.resolve();
function mutate<T>(task: () => Promise<T>): Promise<T> {
  const next = serial.then(task, task);
  serial = next.catch(() => undefined);
  return next;
}
async function preferences(): Promise<Preferences> {
  const data = await chrome.storage.local.get<LocalData>("preferences");
  return { ...DEFAULT_PREFERENCES, ...data.preferences };
}
async function reputation(): Promise<Reputation | undefined> {
  return (await chrome.storage.local.get<LocalData>("reputation")).reputation;
}
async function activeTab() {
  return (await chrome.tabs.query({ active: true, currentWindow: true }))[0];
}
const key = (id: number) => `result:${id}`;
async function storeResult(result: ScanResult, tabId: number) {
  result.tabId = tabId;
  await chrome.storage.session.set({
    [key(tabId)]: { result, expires: Date.now() + 15 * 60000 },
  });
  await chrome.action.setBadgeText({
    tabId,
    text:
      result.verdict === "high" ? "!" : result.verdict === "caution" ? "?" : "",
  });
  await chrome.action.setBadgeBackgroundColor({
    tabId,
    color: result.verdict === "high" ? "#CF3B4C" : "#AD6900",
  });
  return result;
}
async function lastResult(tabId?: number): Promise<ScanResult | undefined> {
  const id = tabId ?? (await activeTab())?.id;
  if (id === undefined) return;
  const data = (
    await chrome.storage.session.get<Record<string, StoredResult>>(key(id))
  )[key(id)];
  if (!data || data.expires < Date.now()) {
    await chrome.storage.session.remove(key(id));
    return;
  }
  return data.result;
}
async function configure(tabId: number) {
  const tab = await chrome.tabs.get(tabId),
    u = web(tab.url),
    p = await preferences(),
    granted = u
      ? await chrome.permissions.contains({ origins: [`${u.origin}/*`] })
      : false,
    automatic = !!u && granted && p.protectedOrigins.includes(u.origin),
    stopped = !u || p.pausedOrigins.includes(u.origin);
  try {
    await chrome.tabs.sendMessage(
      tabId,
      { type: "CONTENT_CONFIG", preferences: p, automatic, stopped },
      { frameId: 0 },
    );
  } catch {
    /* Page has not installed a content script. */
  }
}
async function inject(tabId: number) {
  await chrome.scripting.executeScript({
    target: { tabId, allFrames: false },
    files: ["content.js"],
  });
  await configure(tabId);
}
const latest = new Map<number, string>();
async function scanPage(tabId: number) {
  const tab = await chrome.tabs.get(tabId),
    u = web(tab.url);
  if (!u) throw Error("pageUnavailable");
  await inject(tabId);
  const token = crypto.randomUUID();
  latest.set(tabId, token);
  const page = (await chrome.tabs.sendMessage(
    tabId,
    { type: "PAGE_EXTRACT" },
    { frameId: 0 },
  )) as PageData;
  validatePage(page);
  if (page.url !== tab.url) throw Error("captureChanged");
  const result = (await engine()).page(page, await reputation());
  const now = await chrome.tabs.get(tabId);
  if (now.url !== page.url || latest.get(tabId) !== token)
    throw Error("captureChanged");
  await storeResult(result, tabId);
  await chrome.tabs
    .sendMessage(
      tabId,
      {
        type: "PAGE_WARNING",
        verdict: result.verdict,
        links: result.links,
        unsafeFormActions: result.unsafeFormActions ?? [],
      },
      { frameId: 0 },
    )
    .catch(() => undefined);
  return result;
}
function validatePage(value: unknown): asserts value is PageData {
  const p = value as PageData;
  if (
    !p ||
    typeof p.url !== "string" ||
    typeof p.pageKey !== "string" ||
    p.pageKey.length > 100 ||
    typeof p.title !== "string" ||
    p.title.length > 300 ||
    !Array.isArray(p.passages) ||
    p.passages.length > LIMITS.passages ||
    p.passages.some((s) => typeof s !== "string" || s.length > LIMITS.text) ||
    !Array.isArray(p.links) ||
    p.links.length > LIMITS.links ||
    p.links.some(
      (l) =>
        typeof l.href !== "string" ||
        l.href.length > 4096 ||
        typeof l.text !== "string" ||
        l.text.length > 300 ||
        typeof l.context !== "string" ||
        l.context.length > 600,
    ) ||
    !Array.isArray(p.forms) ||
    p.forms.length > LIMITS.forms ||
    p.forms.some(
      (f) =>
        typeof f.action !== "string" ||
        f.action.length > 4096 ||
        typeof f.password !== "boolean" ||
        typeof f.otp !== "boolean" ||
        typeof f.payment !== "boolean" ||
        typeof f.labels !== "string" ||
        f.labels.length > 300,
    )
  )
    throw Error("scanFailed");
}
async function menus() {
  const p = await preferences();
  await chrome.contextMenus.removeAll();
  chrome.contextMenus.create({
    id: "safex-link",
    title: `SafeX AI · ${t("scan", p.language)}`,
    contexts: ["link"],
  });
  chrome.contextMenus.create({
    id: "safex-text",
    title: `SafeX AI · ${t("scan", p.language)}`,
    contexts: ["selection"],
  });
}
async function registeredSites() {
  const p = await preferences(),
    registered = await chrome.scripting.getRegisteredContentScripts(),
    wanted: chrome.scripting.RegisteredContentScript[] = [];
  for (const [index, origin] of p.protectedOrigins.entries())
    if (await chrome.permissions.contains({ origins: [`${origin}/*`] }))
      wanted.push({
        id: `safex-site-${index}`,
        matches: [`${origin}/*`],
        js: ["content.js"],
        allFrames: false,
        runAt: "document_idle",
        persistAcrossSessions: true,
      });
  if (registered.length)
    await chrome.scripting.unregisterContentScripts({
      ids: registered.map((r) => r.id),
    });
  if (wanted.length) await chrome.scripting.registerContentScripts(wanted);
}
async function syncRules() {
  const p = await preferences(),
    list = await reputation(),
    rules = await chrome.declarativeNetRequest.getDynamicRules();
  const addRules: chrome.declarativeNetRequest.Rule[] =
    p.blocking && list && list.expiresAt > Date.now()
      ? list.entries
          .filter((e) => e.label === "block")
          .slice(0, 500)
          .map((entry, i) => ({
            id: 1000 + i,
            priority: 1,
            action: { type: chrome.declarativeNetRequest.RuleActionType.BLOCK },
            condition: {
              regexFilter: `^${entry.url.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`,
              isUrlFilterCaseSensitive: true,
              resourceTypes: [
                chrome.declarativeNetRequest.ResourceType.MAIN_FRAME,
              ],
            },
          }))
      : [];
  for (const rule of addRules) {
    const result = await chrome.declarativeNetRequest.isRegexSupported({
      regex: rule.condition.regexFilter!,
      isCaseSensitive: true,
    });
    if (!result.isSupported) throw Error("importInvalid");
  }
  await chrome.declarativeNetRequest.updateDynamicRules({
    removeRuleIds: rules.map((r) => r.id),
    addRules,
  });
}
async function cleanup() {
  await mutate(async () => {
    const p = await preferences(),
      local = await chrome.storage.local.get<LocalData>([
        "reports",
        "feedback",
      ]);
    const reports = (local.reports ?? [])
      .filter(
        (r: ScanResult) =>
          r.checkedAt > Date.now() - p.retentionDays * 86400000,
      )
      .slice(0, LIMITS.reports);
    await chrome.storage.local.set({
      reports,
      feedback: (local.feedback ?? [])
        .filter((f) => f.checkedAt > Date.now() - p.retentionDays * 86400000)
        .slice(0, LIMITS.reports),
    });
    await syncRules();
  });
  const sessions =
      await chrome.storage.session.get<Record<string, StoredResult | number>>(
        null,
      ),
    expired = Object.keys(sessions).filter((k) => {
      const value = sessions[k];
      return (
        k.startsWith("result:") &&
        typeof value === "object" &&
        value.expires < Date.now()
      );
    });
  if (expired.length) await chrome.storage.session.remove(expired);
}
async function initialize() {
  await chrome.storage.local.setAccessLevel({
    accessLevel: "TRUSTED_CONTEXTS",
  });
  await chrome.storage.session.setAccessLevel({
    accessLevel: "TRUSTED_CONTEXTS",
  });
  await chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: false });
  await chrome.alarms.create("safex-cleanup", { periodInMinutes: 1 });
  await menus();
  await registeredSites();
  await cleanup();
}
chrome.runtime.onInstalled.addListener(() => void initialize());
chrome.runtime.onStartup.addListener(() => void initialize());
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === "safex-cleanup") void cleanup().catch(() => undefined);
});
chrome.action.onClicked.addListener((tab) => {
  if (tab.id !== undefined) {
    void chrome.sidePanel.open({ tabId: tab.id }).catch(() => undefined);
    if (web(tab.url)) void inject(tab.id).catch(() => undefined);
  }
});
chrome.commands.onCommand.addListener((command, tab) => {
  if (command === "scan-page" && tab?.id !== undefined) {
    void chrome.sidePanel.open({ tabId: tab.id });
    void scanPage(tab.id).catch(() => undefined);
  }
});
chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (tab?.id === undefined) return;
  void chrome.sidePanel.open({ tabId: tab.id });
  const input =
    info.menuItemId === "safex-link" ? info.linkUrl : info.selectionText;
  if (!input) return;
  const source: Source =
    info.menuItemId === "safex-link" ? "link" : "selection";
  void Promise.all([engine(), reputation()]).then(([e, list]) =>
    storeResult(e.scan(input.slice(0, LIMITS.text), source, list), tab.id!),
  );
});
chrome.tabs.onUpdated.addListener((tabId, changes, tab) => {
  if (changes.status === "loading" || changes.url) {
    latest.delete(tabId);
    void chrome.storage.session.remove(key(tabId));
    void chrome.action.setBadgeText({ tabId, text: "" }).catch(() => undefined);
  }
  if (changes.status === "complete")
    void preferences()
      .then(async (p) => {
        const u = web(tab.url);
        if (u && p.protectedOrigins.includes(u.origin)) await inject(tabId);
      })
      .catch(() => undefined);
});
chrome.tabs.onRemoved.addListener((id) => {
  latest.delete(id);
  void chrome.storage.session.remove(key(id));
});
chrome.permissions.onRemoved.addListener(() => {
  void mutate(async () => {
    const p = await preferences(),
      origins: string[] = [];
    for (const origin of p.protectedOrigins)
      if (await chrome.permissions.contains({ origins: [`${origin}/*`] }))
        origins.push(origin);
    p.protectedOrigins = origins;
    p.pausedOrigins = p.pausedOrigins.filter((o) => origins.includes(o));
    await chrome.storage.local.set({ preferences: p });
    await registeredSites();
    const tabs = await chrome.tabs.query({});
    await Promise.all(
      tabs
        .filter((tab) => tab.id !== undefined)
        .map((tab) => configure(tab.id!).catch(() => undefined)),
    );
  }).catch(() => undefined);
});

async function sound(force = false) {
  const p = await preferences();
  if (!p.sound && !force) return;
  const stored = await chrome.storage.session.get<{ lastSound?: number }>(
    "lastSound",
  );
  if (!force && Date.now() - (stored.lastSound ?? 0) < 60000) return;
  await chrome.storage.session.set({ lastSound: Date.now() });
  if (!(await chrome.offscreen.hasDocument()))
    await chrome.offscreen.createDocument({
      url: "offscreen.html",
      reasons: [chrome.offscreen.Reason.AUDIO_PLAYBACK],
      justification: "Play the user-enabled packaged SafeX warning sound.",
    });
  const played = await chrome.runtime.sendMessage({ type: "PLAY_SOUND" });
  if (!played?.ok) throw Error("soundFailed");
}
async function contentMessage(
  message: Record<string, unknown>,
  sender: chrome.runtime.MessageSender,
) {
  if (
    sender.id !== chrome.runtime.id ||
    sender.frameId !== 0 ||
    sender.tab?.id === undefined ||
    !web(sender.tab.url)
  )
    throw Error("pageUnavailable");
  const id = sender.tab.id,
    u = web(sender.tab.url)!,
    p = await preferences(),
    authorized = await chrome.permissions.contains({
      origins: [`${u.origin}/*`],
    });
  const automatic =
    authorized &&
    p.protectedOrigins.includes(u.origin) &&
    !p.pausedOrigins.includes(u.origin);
  if (message.type === "CONTENT_READY")
    return {
      preferences: p,
      automatic,
      stopped: p.pausedOrigins.includes(u.origin),
    };
  if (message.type === "PAGE_DIRTY") {
    if (automatic) {
      latest.delete(id);
      await chrome.storage.session.remove(key(id));
    }
    return {};
  }
  if (message.type === "PAGE_AUTO") {
    if (!automatic) return {};
    validatePage(message.page);
    const page = message.page;
    if (page.url !== sender.tab.url) throw Error("captureChanged");
    const token = crypto.randomUUID();
    latest.set(id, token);
    const result = (await engine()).page(page, await reputation());
    const published = await mutate(async () => {
      const current = await chrome.tabs.get(id),
        after = await preferences();
      if (
        current.url !== page.url ||
        latest.get(id) !== token ||
        !after.protectedOrigins.includes(u.origin) ||
        after.pausedOrigins.includes(u.origin) ||
        !(await chrome.permissions.contains({ origins: [`${u.origin}/*`] }))
      )
        return false;
      await storeResult(result, id);
      await chrome.tabs
        .sendMessage(
          id,
          {
            type: "PAGE_WARNING",
            verdict: result.verdict,
            links: result.links,
            unsafeFormActions: result.unsafeFormActions ?? [],
          },
          { frameId: 0 },
        )
        .catch(() => undefined);
      return true;
    });
    if (published && result.verdict === "high")
      await sound().catch(() => undefined);
    return {};
  }
  if (message.type === "CHECK_LINK") {
    if (typeof message.input !== "string" || message.input.length > 4096)
      throw Error("scanFailed");
    if (
      typeof message.text !== "string" ||
      message.text.length > 300 ||
      typeof message.context !== "string" ||
      message.context.length > 600
    )
      throw Error("scanFailed");
    const result = (await engine()).previewLink(
      message.input,
      message.text,
      message.context,
      await reputation(),
    );
    await storeResult(result, id);
    return { result };
  }
  throw Error("pageUnavailable");
}
async function panelMessage(m: Record<string, unknown>) {
  const type = m.type;
  if (type === "GET_STATE") {
    const tab = await activeTab(),
      p = await preferences(),
      u = web(tab?.url),
      local = await chrome.storage.local.get<LocalData>([
        "reports",
        "feedback",
        "reputation",
      ]);
    return {
      preferences: p,
      tab:
        u && tab?.id !== undefined
          ? { id: tab.id, origin: u.origin, domain: domain(u.hostname) }
          : null,
      result: await lastResult(),
      reports: local.reports ?? [],
      feedback: local.feedback ?? [],
      reputation: local.reputation
        ? {
            name: local.reputation.name,
            importedAt: local.reputation.importedAt,
            expiresAt: local.reputation.expiresAt,
            count: local.reputation.entries.length,
            blockCount: local.reputation.entries.filter(
              (e: { label: string }) => e.label === "block",
            ).length,
          }
        : null,
    };
  }
  if (type === "SCAN_INPUT") {
    if (typeof m.input !== "string" || m.input.length > LIMITS.text)
      throw Error("emptyInput");
    const tab = await activeTab();
    if (tab?.id === undefined) throw Error("scanFailed");
    const source: Source = [
      "text",
      "link",
      "selection",
      "image",
      "qr",
    ].includes(String(m.source))
      ? (m.source as Source)
      : "text";
    const expected = m.expected as
      | { payee: string; amount: string }
      | undefined;
    if (
      expected &&
      (typeof expected.payee !== "string" ||
        expected.payee.length > 150 ||
        typeof expected.amount !== "string" ||
        expected.amount.length > 24)
    )
      throw Error("scanFailed");
    const context = m.context as ContextFlags | undefined;
    if (
      context &&
      (typeof context !== "object" ||
        Object.entries(context).some(
          ([k, v]) =>
            ![
              "remoteAccess",
              "giftCards",
              "walletConnection",
              "advanceFee",
              "secrecy",
            ].includes(k) || typeof v !== "boolean",
        ))
    )
      throw Error("scanFailed");
    const result = (await engine()).scan(
      m.input,
      source,
      await reputation(),
      expected,
    );
    if (context) contextReview(result, context);
    result.synthetic = m.synthetic === true;
    await storeResult(result, tab.id);
    return { result };
  }
  if (type === "SCAN_PAGE") {
    const tab = await activeTab();
    if (tab?.id === undefined) throw Error("pageUnavailable");
    return { result: await scanPage(tab.id) };
  }
  if (type === "CAPTURE") {
    const tab = await activeTab();
    if (tab?.id === undefined || !web(tab.url)) throw Error("pageUnavailable");
    const data = await chrome.tabs.captureVisibleTab(tab.windowId, {
      format: "png",
    });
    const after = await activeTab();
    if (after?.id !== tab.id || after.url !== tab.url)
      throw Error("captureChanged");
    return { data };
  }
  if (type === "PREFERENCES")
    return mutate(async () => {
      const p = await preferences(),
        patch = m.patch as Partial<Preferences>;
      if (!patch || typeof patch !== "object") throw Error("updateFailed");
      for (const key of Object.keys(patch))
        if (
          ![
            "language",
            "fontSize",
            "theme",
            "sound",
            "shield",
            "retentionDays",
            "blocking",
          ].includes(key)
        )
          throw Error("updateFailed");
      const next = { ...p, ...patch };
      if (
        !["en", "hi", "gu"].includes(next.language) ||
        ![7, 30, 90].includes(next.retentionDays) ||
        !["system", "light", "dark"].includes(next.theme) ||
        typeof next.fontSize !== "number" ||
        next.fontSize < 85 ||
        next.fontSize > 150 ||
        typeof next.sound !== "boolean" ||
        typeof next.shield !== "boolean" ||
        typeof next.blocking !== "boolean"
      )
        throw Error("updateFailed");
      await chrome.storage.local.set({ preferences: next });
      try {
        await syncRules();
      } catch {
        await chrome.storage.local.set({ preferences: p });
        throw Error("updateFailed");
      }
      if (p.language !== next.language) await menus();
      const tabs = await chrome.tabs.query({});
      for (const tab of tabs)
        if (tab.id !== undefined)
          await configure(tab.id).catch(() => undefined);
      return { preferences: next };
    });
  if (type === "SITE_ENABLE" || type === "SITE_PAUSE" || type === "SITE_REVOKE")
    return mutate(async () => {
      const tab = await activeTab(),
        u = web(tab?.url);
      if (!u || tab?.id === undefined) throw Error("pageUnavailable");
      const p = await preferences();
      if (type === "SITE_ENABLE") {
        if (
          !(await chrome.permissions.contains({ origins: [`${u.origin}/*`] }))
        )
          throw Error("permissionDenied");
        p.protectedOrigins = [...new Set([...p.protectedOrigins, u.origin])];
        p.pausedOrigins = p.pausedOrigins.filter((o) => o !== u.origin);
      } else if (type === "SITE_PAUSE")
        p.pausedOrigins = p.pausedOrigins.includes(u.origin)
          ? p.pausedOrigins.filter((o) => o !== u.origin)
          : [...p.pausedOrigins, u.origin];
      else {
        p.protectedOrigins = p.protectedOrigins.filter((o) => o !== u.origin);
        p.pausedOrigins = p.pausedOrigins.filter((o) => o !== u.origin);
        await chrome.permissions.remove({ origins: [`${u.origin}/*`] });
      }
      latest.delete(tab.id);
      await chrome.storage.session.remove(key(tab.id));
      await chrome.storage.local.set({ preferences: p });
      await registeredSites();
      await configure(tab.id);
      if (type === "SITE_ENABLE") await inject(tab.id);
      return { preferences: p };
    });
  if (type === "SAVE_REPORT" || type === "EXPORT_REPORT" || type === "FEEDBACK")
    return mutate(async () => {
      const state = await chrome.storage.local.get<LocalData>([
          "reports",
          "feedback",
        ]),
        reports: ScanResult[] = state.reports ?? [],
        result = reports.find((r) => r.id === m.id) ?? (await lastResult());
      if (!result || result.id !== m.id) throw Error("scanFailed");
      const report = savedReport(result);
      if (type === "SAVE_REPORT")
        await chrome.storage.local.set({
          reports: [report, ...reports.filter((r) => r.id !== report.id)].slice(
            0,
            LIMITS.reports,
          ),
        });
      if (type === "FEEDBACK")
        await chrome.storage.local.set({
          feedback: [
            {
              id: report.id,
              checkedAt: Date.now(),
              verdict: report.verdict,
              modelVersion: report.modelVersion,
            },
            ...(state.feedback ?? []).filter(
              (f: { id: string }) => f.id !== report.id,
            ),
          ].slice(0, LIMITS.reports),
        });
      return {
        report,
        feedback:
          type === "FEEDBACK" || state.feedback?.some((f) => f.id === report.id)
            ? "incorrect"
            : null,
      };
    });
  if (type === "DELETE_REPORTS")
    return mutate(async () => {
      const state = await chrome.storage.local.get<LocalData>([
        "reports",
        "feedback",
      ]);
      await chrome.storage.local.set({
        reports: m.id
          ? (state.reports ?? []).filter((r: ScanResult) => r.id !== m.id)
          : [],
        feedback: m.id
          ? (state.feedback ?? []).filter((f: { id: string }) => f.id !== m.id)
          : [],
      });
      return {};
    });
  if (type === "IMPORT_LIST")
    return mutate(async () => {
      if (typeof m.raw !== "string" || typeof m.name !== "string")
        throw Error("importInvalid");
      const list = importReputation(m.raw, m.name),
        prior = await reputation();
      await chrome.storage.local.set({ reputation: list });
      try {
        await syncRules();
      } catch {
        if (prior) await chrome.storage.local.set({ reputation: prior });
        else await chrome.storage.local.remove("reputation");
        throw Error("importInvalid");
      }
      return {};
    });
  if (type === "CLEAR_LIST")
    return mutate(async () => {
      await chrome.storage.local.remove("reputation");
      await syncRules();
      return {};
    });
  if (type === "TEST_SOUND") {
    await sound(true);
    return {};
  }
  if (type === "LEAVE_PAGE") {
    const tab = await activeTab();
    if (tab?.id !== undefined)
      await chrome.tabs.update(tab.id, { url: "chrome://newtab/" });
    return {};
  }
  throw Error("scanFailed");
}
const ERROR_KEYS = new Set([
  "scanFailed",
  "emptyInput",
  "pageUnavailable",
  "permissionDenied",
  "captureChanged",
  "importInvalid",
  "importTooLarge",
  "importSensitive",
  "updateFailed",
  "soundFailed",
]);
chrome.runtime.onMessage.addListener((message, sender, respond) => {
  if (
    !message ||
    typeof message.type !== "string" ||
    message.type === "PLAY_SOUND"
  )
    return;
  // Chrome's gesture check is applied immediately, before asynchronous work can consume it.
  if (
    message.type === "OPEN_PANEL" &&
    sender.id === chrome.runtime.id &&
    sender.tab?.id !== undefined &&
    sender.frameId === 0
  ) {
    chrome.sidePanel.open({ tabId: sender.tab.id }).then(
      () => respond({ ok: true }),
      () => respond({ ok: false, error: "pageUnavailable" }),
    );
    return true;
  }
  if (JSON.stringify(message).length > 1_050_000) {
    respond({ ok: false, error: "scanFailed" });
    return;
  }
  const work = trusted(sender)
    ? panelMessage(message)
    : contentMessage(message, sender);
  void work.then(
    (value) => respond({ ok: true, ...value }),
    (error) =>
      respond({
        ok: false,
        error: ERROR_KEYS.has(error?.message) ? error.message : "scanFailed",
      }),
  );
  return true;
});
