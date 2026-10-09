import {
  DEFAULT_PREFERENCES,
  type Preferences,
  type ScanResult,
  type Language,
  type ContextFlags,
  LIMITS,
} from "../../shared/security-engine/src/contracts";
import { COPY, t } from "./i18n";
import { el, icon, button, card } from "./ui";
import {
  decodeImage,
  dataUrlBlob,
  recognize,
  cancelRecognition,
} from "./recognition";
interface State {
  preferences: Preferences;
  tab: { id: number; origin: string; domain: string } | null;
  result?: ScanResult;
  reports: ScanResult[];
  feedback: { id: string }[];
  reputation: null | {
    name: string;
    importedAt: number;
    expiresAt: number;
    count: number;
    blockCount: number;
  };
}
let state: State = {
  preferences: DEFAULT_PREFERENCES,
  tab: null,
  reports: [],
  feedback: [],
  reputation: null,
};
let view = ["#settings", "#history"].includes(location.hash)
    ? location.hash.slice(1)
    : "scan",
  draft = "",
  synthetic = false,
  working = false,
  noticeTimer: ReturnType<typeof setTimeout> | undefined;
let image: ImageBitmap | undefined,
  cropped: HTMLCanvasElement | undefined,
  rect: { x: number; y: number; width: number; height: number } | undefined,
  recognized = "",
  qr: string[] = [],
  ocrLanguage: Language = "en",
  recognitionDone = false,
  recognitionIssue = false;
let expectedPayee = "",
  expectedAmount = "",
  reviewed: ScanResult | undefined;
let notice: { key: string; values: (string | number)[] } | undefined;
let workGeneration = 0;
let context: ContextFlags = {};
const app = document.getElementById("app")!,
  status = document.getElementById("status")!;
const text = (key: string, ...values: (string | number)[]) =>
  t(key, state.preferences.language, ...values);
async function request(type: string, payload: Record<string, unknown> = {}) {
  const response = await chrome.runtime.sendMessage({ type, ...payload });
  if (!response?.ok) throw Error(response?.error ?? "scanFailed");
  return response;
}
function announce(key: string, ...values: (string | number)[]) {
  notice = { key, values };
  status.textContent = text(key, ...values);
  if (noticeTimer) clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => {
    status.textContent = "";
    notice = undefined;
  }, 5000);
}
async function work(task: () => Promise<void>) {
  if (working) return;
  const current = ++workGeneration;
  working = true;
  render();
  try {
    await task();
  } catch (error) {
    const key = (error as Error).message;
    if (current === workGeneration && key !== "cancel")
      announce(COPY[key] ? key : "scanFailed");
  } finally {
    if (current === workGeneration) {
      working = false;
      render();
    }
  }
}
async function refresh() {
  const response = await request("GET_STATE");
  state = response;
  render();
}
function applyAppearance() {
  document.documentElement.lang = state.preferences.language;
  document.documentElement.dataset.theme = state.preferences.theme;
  document.documentElement.style.setProperty(
    "--scale",
    String(state.preferences.fontSize / 100),
  );
}
function render() {
  const openDetails = new Set(
    [...app.querySelectorAll<HTMLDetailsElement>("details[data-detail]")]
      .filter((d) => d.open)
      .map((d) => d.dataset.detail),
  );
  const focused = document.activeElement as
      | HTMLInputElement
      | HTMLTextAreaElement
      | null,
    id = focused?.id,
    selection =
      focused && "selectionStart" in focused ? focused.selectionStart : null;
  applyAppearance();
  if (notice) status.textContent = text(notice.key, ...notice.values);
  app.replaceChildren();
  const header = el("header", "header"),
    logo = el("img", "logo");
  logo.src = "logo.svg";
  logo.alt = "SafeX AI";
  const brand = el("div", "brand-copy");
  brand.append(el("h1", "", "SafeX AI"), el("p", "", text("tagline")));
  const badge = el("span", "top-badge");
  badge.append(icon("shield"));
  header.append(logo, brand, badge);
  const nav = el("nav", "nav");
  nav.setAttribute("aria-label", text("scan"));
  for (const name of ["scan", "history", "settings"]) {
    const b = button(
      text(name),
      () => {
        view = name;
        history.replaceState(null, "", `#${name}`);
        reviewed = undefined;
        render();
      },
      "",
      name,
    );
    if (view === name) b.setAttribute("aria-current", "page");
    nav.append(b);
  }
  const main = el("main");
  if (view === "scan") renderScan(main);
  else if (view === "history") renderHistory(main);
  else renderSettings(main);
  const footer = el("footer", "footer");
  footer.append(icon("lock"), el("span", "", text("privacyTitle")));
  app.append(header, nav, main, footer);
  app
    .querySelectorAll<HTMLDetailsElement>("details[data-detail]")
    .forEach((d) => (d.open = openDetails.has(d.dataset.detail)));
  if (working)
    main
      .querySelectorAll<HTMLButtonElement>("button:not([data-cancel])")
      .forEach((b) => (b.disabled = true));
  if (id) {
    const next = document.getElementById(id) as
      | HTMLInputElement
      | HTMLTextAreaElement
      | null;
    next?.focus({ preventScroll: true });
    if (selection !== null && next && ["text", "textarea"].includes(next.type))
      next.setSelectionRange(selection, selection);
  }
}
function field(
  label: string,
  value: string,
  change: (v: string) => void,
  id: string,
  placeholder = "",
  multiline = false,
) {
  const wrap = el("div", "field"),
    lab = el("label", "", text(label));
  lab.htmlFor = id;
  const input = multiline ? el("textarea") : el("input");
  input.id = id;
  input.value = value;
  input.placeholder = placeholder;
  input.addEventListener("input", () => change(input.value));
  wrap.append(lab, input);
  return wrap;
}
function select(
  label: string,
  value: string,
  options: [string, string][],
  change: (v: string) => void,
  id: string,
) {
  const wrap = el("div", "field"),
    lab = el("label", "", text(label));
  lab.htmlFor = id;
  const input = el("select");
  input.id = id;
  for (const [v, name] of options) {
    const opt = el("option", "", name);
    opt.value = v;
    opt.selected = value === v;
    input.append(opt);
  }
  input.onchange = () => change(input.value);
  wrap.append(lab, input);
  return wrap;
}
function pickFile(accept: string, handler: (file: File) => Promise<void>) {
  const input = el("input");
  input.type = "file";
  input.accept = accept;
  input.onchange = () => {
    const file = input.files?.[0];
    if (file) void work(() => handler(file));
  };
  input.click();
}
async function scanInput(
  input = draft,
  source = "text",
  expected?: { payee: string; amount: string },
) {
  if (!input.trim()) {
    announce("emptyInput");
    return;
  }
  await work(async () => {
    const r = await request("SCAN_INPUT", {
      input,
      source,
      synthetic,
      expected,
      context: source === "text" ? context : undefined,
    });
    state.result = r.result;
    reviewed = undefined;
  });
}
function renderScan(main: HTMLElement) {
  const hero = el("section", "hero"),
    eyebrow = el("p", "eyebrow");
  eyebrow.append(icon("shield"), el("span", "", text("ready")));
  hero.append(
    eyebrow,
    el("h2", "", text("hero")),
    el("p", "", text("heroBody")),
  );
  const chips = el("div", "chips");
  for (const [key, name] of [
    ["local", "shield"],
    ["private", "lock"],
  ]) {
    const c = el("span", "chip");
    c.append(icon(name), el("span", "", text(key)));
    chips.append(c);
  }
  hero.append(chips);
  main.append(hero);
  const site = card(text("protection"));
  site.append(el("p", "domain", state.tab?.domain ?? text("chooseWebsite")));
  const active =
      state.tab &&
      state.preferences.protectedOrigins.includes(state.tab.origin),
    paused =
      state.tab && state.preferences.pausedOrigins.includes(state.tab.origin);
  const statusLine = el("p", "status-line");
  if (active && !paused) statusLine.append(el("span", "status-dot"));
  statusLine.append(
    el("span", "", text(active ? (paused ? "paused" : "active") : "manual")),
  );
  site.append(statusLine);
  const actions = el("div", "grid-actions");
  actions.append(
    button(
      text("scanPage"),
      () =>
        work(async () => {
          const r = await request("SCAN_PAGE");
          state.result = r.result;
          reviewed = undefined;
        }),
      "button primary",
      "scan",
    ),
    button(
      text("capture"),
      () =>
        work(async () => {
          const r = await request("CAPTURE");
          await loadImage(dataUrlBlob(r.data));
        }),
      "button",
      "image",
    ),
  );
  if (!state.tab)
    actions
      .querySelectorAll<HTMLButtonElement>("button")
      .forEach((b) => (b.disabled = true));
  site.append(actions);
  if (state.tab) {
    if (active) {
      const row = el("div", "actions");
      row.append(
        button(
          text(paused ? "resume" : "pause"),
          () => siteAction("SITE_PAUSE"),
          "button ghost compact",
        ),
        button(
          text("revoke"),
          () => siteAction("SITE_REVOKE"),
          "button ghost compact",
        ),
      );
      site.append(row);
    } else
      site.append(
        button(
          text("protectSite"),
          async () => {
            // Request permission directly in the click gesture, then persist the selected site.
            try {
              const granted = await chrome.permissions.request({
                origins: [`${state.tab!.origin}/*`],
              });
              if (!granted) {
                announce("permissionDenied");
                return;
              }
              await siteAction("SITE_ENABLE");
            } catch {
              announce("permissionDenied");
            }
          },
          "button ghost full",
        ),
      );
  }
  site.append(
    el("p", "muted small", text(state.tab ? "siteHint" : "pageUnavailable")),
  );
  main.append(site);
  const check = card(text("pasteTitle"), text("pasteHint"));
  check.append(
    field(
      "inputLabel",
      draft,
      (v) => {
        draft = v;
        synthetic = false;
        context = {};
        document
          .querySelectorAll<HTMLInputElement>("[data-context]")
          .forEach((input) => (input.checked = false));
      },
      "scan-input",
      text("inputPlaceholder"),
      true,
    ),
  );
  const questions = el("details");
  questions.dataset.detail = "context";
  questions.append(
    el("summary", "", text("contextTitle")),
    el("p", "muted", text("contextHint")),
  );
  for (const flag of [
    "remoteAccess",
    "giftCards",
    "walletConnection",
    "advanceFee",
    "secrecy",
  ] as const) {
    const row = toggle(
      `question_${flag}`,
      "contextOptional",
      context[flag] === true,
      (v) => {
        context[flag] = v;
      },
      `context-${flag}`,
    );
    row.querySelector("input")!.dataset.context = flag;
    questions.append(row);
  }
  check.append(questions);
  const analyze = button(
    text(working ? "loading" : "analyze"),
    () => scanInput(),
    "button primary full",
    "scan",
  );
  analyze.id = "analyze";
  const primaryActions = el("div", "actions");
  primaryActions.append(analyze);
  check.append(primaryActions);
  const second = el("div", "actions");
  second.append(
    button(
      text("image"),
      () => pickFile("image/png,image/jpeg,image/webp", loadImage),
      "button ghost compact",
      "image",
    ),
  );
  check.append(second);
  const examples = el("details");
  examples.dataset.detail = "samples";
  examples.append(el("summary", "", text("samples")));
  const options = el("div", "actions");
  options.append(
    button(text("scamExample"), () => example(true), "button compact"),
    button(text("legitimateExample"), () => example(false), "button compact"),
  );
  examples.append(options);
  if (synthetic) examples.append(el("p", "synthetic", text("synthetic")));
  check.append(examples);
  main.append(check);
  if (image || cropped) renderImage(main);
  if (state.result) main.append(resultCard(state.result));
}
async function siteAction(type: string) {
  await work(async () => {
    await request(type);
    await refresh();
  });
}
function example(scam: boolean) {
  const lang = state.preferences.language;
  const data: Record<Language, [string, string]> = {
    en: [
      "KYC expires now. Urgently send your bank password and OTP to this support agent.",
      "Your parcel is scheduled to arrive this evening. Never share your password or OTP.",
    ],
    hi: [
      "केवाईसी बंद होगी। अभी बैंक का पासवर्ड और ओटीपी एजेंट को भेजें।",
      "आपका पार्सल आज शाम आएगा। अपना पासवर्ड या ओटीपी कभी साझा न करें।",
    ],
    gu: [
      "કેવાયસી બંધ થશે. હમણાં બેંકનો પાસવર્ડ અને ઓટીપી એજન્ટને મોકલો.",
      "તમારું પાર્સલ આજે સાંજે આવશે. તમારો પાસવર્ડ કે ઓટીપી ક્યારેય શેર ન કરો.",
    ],
  };
  draft = data[lang][scam ? 0 : 1];
  context = {};
  synthetic = true;
  render();
  document.getElementById("scan-input")?.focus();
}
function resultCard(result: ScanResult) {
  const c = el("section", `card result ${result.verdict}`);
  c.id = "scan-result";
  c.setAttribute("aria-label", text(result.verdict));
  const header = el("div", "result-header"),
    symbol = el("span", "result-symbol");
  symbol.append(
    icon(
      result.verdict === "clear"
        ? "check"
        : result.verdict === "unknown"
          ? "scan"
          : "alert",
    ),
  );
  const title = el("div");
  title.append(
    el("h2", "", text(result.verdict)),
    el(
      "p",
      "meta",
      `${new Date(result.checkedAt).toLocaleTimeString(state.preferences.language, { hour: "2-digit", minute: "2-digit" })} · ${result.durationMs} ms`,
    ),
  );
  header.append(symbol, title);
  c.append(header, el("p", "muted", text(result.verdict + "Body")));
  if (result.synthetic) c.append(el("p", "synthetic", text("synthetic")));
  const reasons = [...result.reasons].sort((a, b) => b.weight - a.weight);
  if (reasons.length) {
    c.append(el("h3", "", text("evidence")));
    const list = el("ul", "evidence");
    for (const reason of reasons.slice(0, 3)) {
      const li = el("li");
      li.append(icon("shield"));
      const content = el("div", "", text(reason.id));
      if (reason.detail) content.append(el("strong", "", reason.detail));
      li.append(content);
      list.append(li);
    }
    c.append(list);
    if (reasons.length > 3) {
      const more = el("details");
      more.dataset.detail = `more:${result.id}`;
      more.append(el("summary", "", text("more")));
      for (const reason of reasons.slice(3))
        more.append(el("p", "", text(reason.id)));
      c.append(more);
    }
  }
  if (result.links.length) {
    c.append(el("h3", "", text("destinations")));
    for (const link of result.links.slice(0, 4)) {
      const box = el("div", "destination");
      box.append(
        el("strong", "", link.host || text("invalid_url")),
        el("code", "", link.url),
      );
      c.append(box);
    }
  }
  for (const p of result.payments) {
    const dl = el("dl", "payment");
    for (const [key, value] of [
      ["payee", p.payee],
      ["payeeName", p.name],
      ["amount", p.amount ? `${p.currency} ${p.amount}` : "—"],
    ])
      if (value) dl.append(el("dt", "", text(key)), el("dd", "", value));
    if (p.matches) dl.append(el("p", "muted", text("matches")));
    c.append(dl);
  }
  const details = el("details");
  details.dataset.detail = `coverage:${result.id}`;
  details.append(el("summary", "", text("coverage")));
  for (const reason of result.coverage)
    details.append(el("p", "muted", text(reason)));
  if (result.counts)
    details.append(
      el(
        "p",
        "meta",
        `${result.counts.links} ${text("destinations")} · ${result.counts.passages} ${text("scan")}`,
      ),
    );
  details.append(el("p", "meta", result.modelVersion));
  c.append(details);
  if (result.verdict !== "clear") {
    const steps = el("details");
    steps.dataset.detail = `steps:${result.id}`;
    steps.append(el("summary", "", text("nextSteps")));
    const list = el("ul");
    for (const key of [
      "step_verify",
      "step_secrets",
      "step_access",
      "step_payment",
    ])
      list.append(el("li", "", text(key)));
    steps.append(list);
    c.append(steps);
  }
  const actions = el("div", "actions");
  actions.append(
    button(
      text("save"),
      () =>
        work(async () => {
          await request("SAVE_REPORT", { id: result.id });
          announce("saved");
          await refresh();
        }),
      "button compact",
      "file",
    ),
    button(
      text("export"),
      () =>
        work(async () => {
          const reply = await request("EXPORT_REPORT", { id: result.id });
          download(reply.report, reply.feedback);
        }),
      "button ghost compact",
    ),
  );
  if (result.source === "page" && result.verdict !== "clear")
    actions.append(
      button(
        text("leave"),
        () =>
          work(async () => {
            await request("LEAVE_PAGE");
          }),
        "button danger compact",
      ),
    );
  actions.append(
    button(
      text("feedback"),
      () =>
        work(async () => {
          await request("FEEDBACK", { id: result.id });
          announce("feedbackSaved");
        }),
      "button ghost compact",
    ),
  );
  c.append(actions);
  return c;
}
function download(report: ScanResult, feedback: "incorrect" | null = null) {
  const blob = new Blob(
      [
        JSON.stringify(
          {
            application: "SafeX AI",
            privacy: "Redacted local report",
            report,
            feedback,
          },
          null,
          2,
        ),
      ],
      { type: "application/json" },
    ),
    url = URL.createObjectURL(blob),
    a = el("a");
  a.href = url;
  a.download = `SafeX-AI-report-${new Date(report.checkedAt).toISOString().slice(0, 10)}.json`;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
function renderHistory(main: HTMLElement) {
  const c = card(text("history"), text("redacted"));
  if (!state.reports.length) {
    const empty = el("div", "empty");
    empty.append(icon("history"), el("p", "muted", text("historyEmpty")));
    c.append(empty);
  }
  for (const r of state.reports) {
    const item = el("article", "history-item");
    item.append(
      el("h3", "", text(r.verdict)),
      el("p", "muted", r.domain ?? r.links[0]?.host ?? text("scan")),
      el(
        "p",
        "muted small",
        new Date(r.checkedAt).toLocaleString(state.preferences.language),
      ),
    );
    const actions = el("div", "actions");
    actions.append(
      button(
        text("showDetails"),
        () => {
          reviewed = r;
          render();
        },
        "button compact",
      ),
      button(
        text("delete"),
        () =>
          work(async () => {
            await request("DELETE_REPORTS", { id: r.id });
            reviewed = undefined;
            await refresh();
          }),
        "button ghost compact danger",
      ),
    );
    item.append(actions);
    c.append(item);
  }
  if (state.reports.length)
    c.append(
      button(
        text("deleteAll"),
        () => {
          if (confirm(text("deleteConfirm")))
            void work(async () => {
              await request("DELETE_REPORTS");
              reviewed = undefined;
              await refresh();
            });
        },
        "button ghost danger full",
      ),
    );
  main.append(c);
  if (reviewed) main.append(resultCard(reviewed));
}
async function preference(patch: Partial<Preferences>) {
  await work(async () => {
    const r = await request("PREFERENCES", { patch });
    state.preferences = r.preferences;
  });
}
function toggle(
  label: string,
  hint: string,
  checked: boolean,
  change: (v: boolean) => void,
  id: string,
) {
  const row = el("div", "setting-row"),
    lab = el("label", "", text(label));
  lab.htmlFor = id;
  lab.append(el("span", "muted", text(hint)));
  const input = el("input");
  input.type = "checkbox";
  input.id = id;
  input.checked = checked;
  input.onchange = () => change(input.checked);
  row.append(lab, input);
  return row;
}
function renderSettings(main: HTMLElement) {
  const appearance = card(text("appearance"));
  appearance.append(
    select(
      "language",
      state.preferences.language,
      [
        ["en", "English"],
        ["hi", "हिन्दी"],
        ["gu", "ગુજરાતી"],
      ],
      (v) => void preference({ language: v as Language }),
      "language",
    ),
  );
  const reading = el("div", "field"),
    lab = el(
      "label",
      "",
      `${text("fontSize")} · ${state.preferences.fontSize}%`,
    );
  lab.htmlFor = "font-size";
  const slider = el("input");
  slider.type = "range";
  slider.id = "font-size";
  slider.min = "85";
  slider.max = "150";
  slider.step = "5";
  slider.value = String(state.preferences.fontSize);
  slider.oninput = () => {
    document.documentElement.style.setProperty(
      "--scale",
      String(Number(slider.value) / 100),
    );
    lab.textContent = `${text("fontSize")} · ${slider.value}%`;
  };
  slider.onchange = () => void preference({ fontSize: Number(slider.value) });
  reading.append(lab, slider);
  appearance.append(
    reading,
    select(
      "theme",
      state.preferences.theme,
      ["system", "light", "dark"].map((v) => [v, text(v)]),
      (v) => void preference({ theme: v as Preferences["theme"] }),
      "theme",
    ),
  );
  main.append(appearance);
  const protection = card(text("protection"));
  protection.append(
    toggle(
      "sound",
      "soundHint",
      state.preferences.sound,
      (v) => void preference({ sound: v }),
      "sound",
    ),
    button(
      text("testSound"),
      () =>
        work(async () => {
          await request("TEST_SOUND");
        }),
      "button ghost compact",
      "sound",
    ),
    toggle(
      "shield",
      "shieldHint",
      state.preferences.shield,
      (v) => void preference({ shield: v }),
      "shield",
    ),
    select(
      "retention",
      String(state.preferences.retentionDays),
      [7, 30, 90].map((n) => [String(n), text("days", n)]),
      (v) =>
        void preference({
          retentionDays: Number(v) as Preferences["retentionDays"],
        }),
      "retention",
    ),
  );
  main.append(protection);
  const list = card(text("threatData"), text("listHint"));
  if (state.reputation) {
    list.append(
      el("p", "domain", state.reputation.name),
      el(
        "p",
        "muted",
        text(
          "listStatus",
          state.reputation.count,
          new Date(state.reputation.importedAt).toLocaleDateString(
            state.preferences.language,
          ),
        ),
      ),
    );
    if (state.reputation.expiresAt < Date.now())
      list.append(el("p", "notice", text("expired")));
  } else list.append(el("p", "muted", text("noList")));
  const controls = el("div", "actions");
  controls.append(
    button(
      text("importList"),
      () =>
        pickFile(".json,.txt,text/plain,application/json", async (file) => {
          if (file.size > 1_000_000) throw Error("importTooLarge");
          await request("IMPORT_LIST", {
            raw: await file.text(),
            name: file.name,
          });
          await refresh();
        }),
      "button compact",
      "file",
    ),
  );
  if (state.reputation)
    controls.append(
      button(
        text("clearList"),
        () =>
          work(async () => {
            await request("CLEAR_LIST");
            await refresh();
          }),
        "button ghost compact",
      ),
    );
  list.append(
    controls,
    toggle(
      "blocking",
      "blockingHint",
      state.preferences.blocking,
      (v) => void preference({ blocking: v }),
      "blocking",
    ),
  );
  main.append(list);
  main.append(card(text("privacyTitle"), text("privacyBody")));
  const about = card(text("about"), text("aboutBody"));
  const link = el("a", "small", text("about"));
  link.href = "licenses.html";
  link.target = "_blank";
  link.rel = "noopener";
  about.append(link);
  main.append(about);
}
async function loadImage(blob: Blob) {
  await cancelRecognition();
  image?.close();
  image = await decodeImage(blob);
  cropped = undefined;
  rect = undefined;
  recognized = "";
  qr = [];
  recognitionDone = false;
  recognitionIssue = false;
  render();
}
function clearImage() {
  workGeneration++;
  void cancelRecognition();
  image?.close();
  image = undefined;
  cropped = undefined;
  rect = undefined;
  recognized = "";
  qr = [];
  recognitionDone = false;
  recognitionIssue = false;
  working = false;
  status.textContent = "";
  notice = undefined;
  render();
}
function renderImage(main: HTMLElement) {
  const c = card(text("imageTitle"), text("cropHint")),
    canvas = el("canvas", "image-preview");
  canvas.id = "crop-preview";
  const original = image;
  if (original) {
    const scale = Math.min(1, 800 / original.width);
    canvas.width = Math.round(original.width * scale);
    canvas.height = Math.round(original.height * scale);
    const ctx = canvas.getContext("2d")!;
    const paint = () => {
      ctx.drawImage(original, 0, 0, canvas.width, canvas.height);
      if (rect) {
        ctx.fillStyle = "#08151d80";
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.clearRect(
          rect.x * scale,
          rect.y * scale,
          rect.width * scale,
          rect.height * scale,
        );
        ctx.drawImage(
          original,
          rect.x,
          rect.y,
          rect.width,
          rect.height,
          rect.x * scale,
          rect.y * scale,
          rect.width * scale,
          rect.height * scale,
        );
        ctx.strokeStyle = "#35debc";
        ctx.lineWidth = 3;
        ctx.strokeRect(
          rect.x * scale,
          rect.y * scale,
          rect.width * scale,
          rect.height * scale,
        );
      }
    };
    paint();
    let start: { x: number; y: number } | undefined;
    const coordinate = (event: PointerEvent) => {
      const box = canvas.getBoundingClientRect();
      return {
        x: Math.max(
          0,
          Math.min(
            original.width,
            ((event.clientX - box.left) * original.width) / box.width,
          ),
        ),
        y: Math.max(
          0,
          Math.min(
            original.height,
            ((event.clientY - box.top) * original.height) / box.height,
          ),
        ),
      };
    };
    canvas.onpointerdown = (e) => {
      start = coordinate(e);
      canvas.setPointerCapture(e.pointerId);
    };
    canvas.onpointermove = (e) => {
      if (!start) return;
      const end = coordinate(e);
      rect = {
        x: Math.min(start.x, end.x),
        y: Math.min(start.y, end.y),
        width: Math.abs(end.x - start.x),
        height: Math.abs(end.y - start.y),
      };
      paint();
    };
    canvas.onpointerup = () => {
      start = undefined;
    };
    canvas.setAttribute("aria-label", text("cropHint"));
    c.append(canvas);
    const actions = el("div", "actions");
    const crop = async (full = false) => {
      const selected = full
        ? { x: 0, y: 0, width: original.width, height: original.height }
        : rect;
      if (!selected || selected.width < 8 || selected.height < 8) return;
      cropped = el("canvas");
      cropped.width = Math.round(selected.width);
      cropped.height = Math.round(selected.height);
      cropped
        .getContext("2d")!
        .drawImage(
          original,
          selected.x,
          selected.y,
          selected.width,
          selected.height,
          0,
          0,
          cropped.width,
          cropped.height,
        );
      image?.close();
      image = undefined;
      rect = undefined;
      render();
    };
    actions.append(
      button(text("crop"), () => crop(), "button primary compact"),
      button(text("fullImage"), () => crop(true), "button compact"),
    );
    c.append(actions);
  } else if (cropped) {
    canvas.width = cropped.width;
    canvas.height = cropped.height;
    canvas.getContext("2d")!.drawImage(cropped, 0, 0);
    c.append(
      canvas,
      select(
        "ocrLanguage",
        ocrLanguage,
        [
          ["en", "English"],
          ["hi", "हिन्दी"],
          ["gu", "ગુજરાતી"],
        ],
        (v) => {
          ocrLanguage = v as Language;
        },
        "ocr-language",
      ),
    );
    c.append(
      button(
        text("recognize"),
        () =>
          work(async () => {
            const current = cropped!;
            const results = await recognize(current, ocrLanguage, (p) => {
              if (cropped === current)
                status.textContent = text("ocrLoading", p);
            });
            if (cropped !== current) return;
            recognized = results.text;
            qr = results.qr;
            recognitionIssue = results.ocrFailed;
            recognitionDone = true;
            status.textContent = "";
            if (!recognized && !qr.length) announce("noText");
          }),
        "button primary full",
        "scan",
      ),
    );
    if (recognized) {
      c.append(
        field(
          "recognized",
          recognized,
          (v) => {
            recognized = v;
          },
          "ocr-text",
          "",
          true,
        ),
        button(
          text("analyze"),
          () => scanInput(recognized, "image"),
          "button primary full",
        ),
      );
    } else if (recognitionDone)
      c.append(
        el(
          "p",
          "muted",
          text(recognitionIssue ? "recognitionFailed" : "noText"),
        ),
      );
    if (qr.length) {
      c.append(el("h3", "", text("qrFound")));
      for (const value of qr) {
        const box = el("div", "destination");
        box.append(
          el("code", "", value),
          button(
            text("checkQr"),
            () => scanInput(value, "qr"),
            "button compact",
          ),
        );
        c.append(box);
      }
      if (qr.some((v) => v.startsWith("upi:"))) {
        c.append(
          field(
            "expectedPayee",
            expectedPayee,
            (v) => {
              expectedPayee = v;
            },
            "expected-payee",
          ),
          field(
            "expectedAmount",
            expectedAmount,
            (v) => {
              expectedAmount = v;
            },
            "expected-amount",
          ),
          button(
            text("compare"),
            () =>
              scanInput(qr.find((v) => v.startsWith("upi:"))!, "qr", {
                payee: expectedPayee,
                amount: expectedAmount,
              }),
            "button compact",
          ),
        );
      }
    } else if (recognitionDone) c.append(el("p", "muted small", text("noQr")));
  }
  const actions = el("div", "actions"),
    cancel = button(
      text("cancel"),
      clearImage,
      "button ghost compact",
      "close",
    );
  cancel.dataset.cancel = "true";
  actions.append(cancel);
  c.append(actions);
  main.append(c);
}
let refreshTimer: ReturnType<typeof setTimeout> | undefined;
chrome.storage.onChanged.addListener((_changes, area) => {
  if (area === "session" || area === "local") {
    if (refreshTimer) clearTimeout(refreshTimer);
    refreshTimer = setTimeout(() => {
      if (!working) void refresh().catch(() => undefined);
    }, 100);
  }
});
chrome.tabs.onActivated.addListener(() => {
  state.result = undefined;
  void refresh().catch(() => undefined);
});
window.addEventListener("pagehide", () => {
  void cancelRecognition();
  image?.close();
});
render();
void refresh().catch(() => announce("scanFailed"));
