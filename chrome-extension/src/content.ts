import {
  LIMITS,
  type PageData,
  type Preferences,
  type Verdict,
} from "../../shared/security-engine/src/contracts";
import { t } from "./i18n";
import shieldSvg from "../../branding/safex-logo.svg";
const runtime = globalThis as typeof globalThis & {
  __safexInstalled?: boolean;
};
if (!runtime.__safexInstalled) {
  runtime.__safexInstalled = true;
  install();
}
function install() {
  let preferences: Preferences | undefined,
    root: HTMLDivElement | undefined,
    shadow: ShadowRoot | undefined,
    nav = 0,
    lastUrl = location.href,
    stopped = false,
    protecting = false,
    unsafeFormActions = new Set<string>();
  const documentKey = crypto.randomUUID();
  const excluded =
    'input,textarea,select,option,[contenteditable]:not([contenteditable="false"]),[role="textbox"],script,style,noscript,svg,canvas,iframe,[data-safex-root],[hidden],[aria-hidden="true"]';
  const msg = (message: unknown) =>
    chrome.runtime.sendMessage(message).catch(() => undefined);
  const visible = (el: Element) =>
    !el.closest(excluded) &&
    el.getClientRects().length > 0 &&
    getComputedStyle(el).visibility !== "hidden" &&
    getComputedStyle(el).opacity !== "0";
  function safeText(element: Element, limit: number) {
    if (!visible(element)) return "";
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
    let node: Node | null,
      text = "",
      visited = 0;
    while (
      (node = walker.nextNode()) &&
      ++visited <= 100 &&
      text.length < limit
    ) {
      const parent = node.parentElement;
      if (parent && visible(parent))
        text +=
          (text ? " " : "") +
          (node.textContent ?? "").trim().slice(0, limit - text.length);
    }
    return text.trim().slice(0, limit);
  }
  function extract(): PageData {
    if (location.href !== lastUrl) {
      lastUrl = location.href;
      nav++;
    }
    const privateCommunication =
      /^(mail\.google\.com|outlook\.(live|office)\.com|web\.whatsapp\.com|web\.telegram\.org|discord\.com)$/i.test(
        location.hostname,
      );
    if (privateCommunication)
      return {
        url: location.href,
        title: "",
        passages: [],
        links: [],
        forms: [],
        partial: true,
        inaccessibleFrames: document.querySelectorAll("iframe").length,
        pageKey: `${documentKey}:${nav}`,
        selectedOnly: true,
      };
    const groups = new Map<Element, string>(),
      walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let textCount = 0,
      nodes = 0,
      node: Node | null,
      partial = false;
    const started = performance.now();
    while (!privateCommunication && (node = walker.nextNode())) {
      if (++nodes > 5000 || performance.now() - started > 70) {
        partial = true;
        break;
      }
      const parent = node.parentElement,
        text = node.textContent?.trim();
      if (!parent || !text || !visible(parent)) continue;
      const block =
        parent.closest("p,li,h1,h2,h3,article,section,main") ?? parent;
      if (!groups.has(block) && groups.size >= LIMITS.passages) {
        partial = true;
        continue;
      }
      const room = LIMITS.text - textCount;
      if (room <= 0) {
        partial = true;
        break;
      }
      groups.set(
        block,
        ((groups.get(block) ?? "") + " " + text.slice(0, room)).trim(),
      );
      textCount += Math.min(room, text.length);
    }
    const anchors = document.querySelectorAll<HTMLAnchorElement>("a[href]"),
      forms = document.querySelectorAll<HTMLFormElement>("form");
    const links: PageData["links"] = [];
    let visitedLinks = 0;
    for (const a of anchors) {
      if (++visitedLinks > 300 || links.length >= LIMITS.links) {
        partial = true;
        break;
      }
      if (!visible(a) || !/^https?:/i.test(a.href)) continue;
      const context = a.closest("p,li,article") ?? a.parentElement;
      links.push({
        href: a.href.slice(0, 4096),
        text: privateCommunication ? "" : safeText(a, 300),
        context: privateCommunication
          ? ""
          : context
            ? safeText(context, 600)
            : "",
      });
    }
    const formData: PageData["forms"] = [];
    let visitedForms = 0;
    for (const form of forms) {
      if (++visitedForms > 60 || formData.length >= LIMITS.forms) {
        partial = true;
        break;
      }
      if (!visible(form)) continue;
      const fields = [
        ...form.querySelectorAll<HTMLInputElement>("input"),
      ].slice(0, 80);
      formData.push({
        action: (form.action || location.href).slice(0, 4096),
        method: form.method,
        password: fields.some((f) => f.type === "password"),
        otp: fields.some((f) => f.autocomplete === "one-time-code"),
        payment: fields.some((f) => /^cc-/.test(f.autocomplete)),
        labels: privateCommunication
          ? ""
          : [...form.querySelectorAll("label")]
              .slice(0, 15)
              .map((el) => safeText(el, 100))
              .join(" ")
              .slice(0, 300),
      });
    }
    return {
      url: location.href,
      title: privateCommunication ? "" : document.title.slice(0, 300),
      passages: [...groups.values()],
      links,
      forms: formData,
      partial: partial || privateCommunication,
      inaccessibleFrames: document.querySelectorAll("iframe").length,
      pageKey: `${documentKey}:${nav}`,
    };
  }
  function buildShield() {
    root?.remove();
    if (!preferences?.shield || stopped) return;
    root = document.createElement("div");
    root.dataset.safexRoot = "true";
    root.style.cssText =
      "position:fixed;right:20px;bottom:24px;z-index:2147483646";
    shadow = root.attachShadow({ mode: "open" });
    const style = document.createElement("style");
    style.textContent =
      ":host{all:initial}*{box-sizing:border-box}button{font:600 14px system-ui;cursor:pointer}button:focus-visible{outline:3px solid #35debc;outline-offset:4px}.shield{width:52px;height:52px;padding:6px;background:#08151d;border:1px solid #35debc66;border-radius:18px;box-shadow:0 6px 24px #0005;touch-action:none;color:#eff9f8}.shield svg{width:100%;height:100%}.warning{width:min(310px,calc(100vw - 30px));font:14px/1.5 system-ui;background:#122936;color:#eff9f8;padding:18px;border:1px solid #a5bfca55;border-radius:16px;box-shadow:0 10px 32px #0006;margin-bottom:12px}.warning strong{display:block;font-size:16px}.warning p{margin:8px 0 14px}.actions{display:flex;gap:8px;flex-wrap:wrap}.actions button{border:1px solid #a5bfca66;border-radius:9px;padding:9px 12px;background:#1b3544;color:#eff9f8}.actions .primary{background:#35debc;color:#08151d}.warning[hidden]{display:none}";
    style.textContent += `button,.warning{font-size:${(14 * preferences.fontSize) / 100}px}.warning{max-height:calc(100vh - 100px);overflow:auto}.warning strong{font-size:1.15em}`;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "shield";
    button.setAttribute(
      "aria-label",
      `SafeX AI · ${t("showDetails", preferences.language)}`,
    );
    button.title = "SafeX AI";
    const parsed = new DOMParser().parseFromString(shieldSvg, "image/svg+xml");
    parsed.documentElement.setAttribute("aria-hidden", "true");
    button.append(document.importNode(parsed.documentElement, true));
    let originX = 0,
      originY = 0,
      left = 0,
      top = 0,
      drag = false;
    button.addEventListener("pointerdown", (e) => {
      if (!e.isTrusted) return;
      originX = e.clientX;
      originY = e.clientY;
      const box = root!.getBoundingClientRect();
      left = box.left;
      top = box.top;
      drag = false;
      button.setPointerCapture(e.pointerId);
    });
    button.addEventListener("pointermove", (e) => {
      if (!button.hasPointerCapture(e.pointerId) || !root) return;
      if (Math.abs(e.clientX - originX) + Math.abs(e.clientY - originY) > 6)
        drag = true;
      if (drag) {
        root.style.right = "auto";
        root.style.bottom = "auto";
        root.style.left = `${Math.max(8, Math.min(innerWidth - root.offsetWidth - 8, left + e.clientX - originX))}px`;
        root.style.top = `${Math.max(8, Math.min(innerHeight - root.offsetHeight - 8, top + e.clientY - originY))}px`;
      }
    });
    button.addEventListener("click", (e) => {
      if (e.isTrusted && !drag) void msg({ type: "OPEN_PANEL" });
    });
    shadow.append(style, button);
    document.documentElement.append(root);
  }
  function warn(state: Verdict, continueAction?: () => void) {
    if (!shadow || !preferences || state === "clear" || state === "unknown")
      return;
    shadow.querySelector(".warning")?.remove();
    const box = document.createElement("section");
    box.className = "warning";
    box.setAttribute("role", "alert");
    const title = document.createElement("strong");
    title.textContent = t(state, preferences.language);
    const text = document.createElement("p");
    text.textContent = t(state + "Body", preferences.language);
    const actions = document.createElement("div");
    actions.className = "actions";
    const add = (key: string, handler: () => void, primary = false) => {
      const b = document.createElement("button");
      b.textContent = t(key, preferences!.language);
      b.className = primary ? "primary" : "";
      b.onclick = (e) => {
        if (e.isTrusted) handler();
      };
      actions.append(b);
    };
    add("showDetails", () => void msg({ type: "OPEN_PANEL" }), true);
    add("dismiss", () => box.remove());
    if (continueAction)
      add("continueOnce", () => {
        box.remove();
        continueAction();
      });
    box.append(title, text, actions);
    shadow.insertBefore(box, shadow.querySelector(".shield"));
    if (root?.style.left) {
      const bounds = root.getBoundingClientRect();
      root.style.left = `${Math.max(8, Math.min(innerWidth - root.offsetWidth - 8, bounds.left))}px`;
      root.style.top = `${Math.max(8, Math.min(innerHeight - root.offsetHeight - 8, bounds.top))}px`;
    }
  }
  let timer: ReturnType<typeof setTimeout> | undefined,
    lastScan = 0;
  function dirty() {
    if (stopped) return;
    if (timer) clearTimeout(timer);
    void msg({ type: "PAGE_DIRTY" });
    timer = setTimeout(
      () => {
        if (stopped) return;
        lastScan = Date.now();
        void msg({ type: "PAGE_AUTO", page: extract() });
      },
      Math.max(600, 3000 - (Date.now() - lastScan)),
    );
  }
  const observer = new MutationObserver((records) => {
    if (
      records.some(
        (r) =>
          !(
            r.target instanceof Element ? r.target : r.target.parentElement
          )?.closest("[data-safex-root],input,textarea,[contenteditable]"),
      )
    )
      dirty();
  });
  let bypass: string | undefined;
  document.addEventListener(
    "click",
    (e) => {
      if (
        stopped ||
        !protecting ||
        !preferences ||
        !e.isTrusted ||
        e.button !== 0 ||
        e.ctrlKey ||
        e.metaKey ||
        e.shiftKey ||
        e.altKey
      )
        return;
      const a = (e.target as Element)?.closest?.(
        "a[href]",
      ) as HTMLAnchorElement | null;
      if (
        !a ||
        a.closest(excluded) ||
        !/^https?:/.test(a.href) ||
        a.hasAttribute("download") ||
        (a.target && a.target !== "_self")
      )
        return;
      if (bypass === a.href) {
        bypass = undefined;
        return;
      }
      // Only intercept already-identified risky links; ordinary clicks retain their timing and behavior.
      if (a.dataset.safexRisk !== "caution" && a.dataset.safexRisk !== "high")
        return;
      e.preventDefault();
      e.stopImmediatePropagation();
      const destination = a.href;
      void msg({
        type: "CHECK_LINK",
        input: destination,
        text: safeText(a, 300),
        context: safeText(a.closest("p,li") ?? a, 600),
      }).then((reply) => {
        if (reply?.ok)
          warn(reply.result.verdict, () => {
            if (a.href !== destination) return;
            bypass = destination;
            a.click();
            bypass = undefined;
          });
        else warn(a.dataset.safexRisk === "high" ? "high" : "caution");
      });
    },
    true,
  );
  chrome.runtime.onMessage.addListener((request, _sender, respond) => {
    if (request.type === "PAGE_EXTRACT") {
      respond(extract());
      return;
    }
    if (request.type === "CONTENT_CONFIG") {
      preferences = request.preferences;
      stopped = !!request.stopped;
      protecting = request.automatic && !stopped;
      unsafeFormActions.clear();
      buildShield();
      observer.disconnect();
      if (timer) clearTimeout(timer);
      if (request.automatic && !stopped) {
        observer.observe(document.body, {
          subtree: true,
          childList: true,
          attributes: true,
          attributeFilter: ["href", "action", "method", "type"],
        });
        dirty();
      }
      respond({ ok: true });
      return;
    }
    if (request.type === "PAGE_WARNING") {
      unsafeFormActions = new Set(request.unsafeFormActions ?? []);
      shadow?.querySelector(".warning")?.remove();
      warn(request.verdict);
      for (const a of document.querySelectorAll<HTMLAnchorElement>("a[href]"))
        delete a.dataset.safexRisk;
      for (const link of request.links ?? []) {
        for (const a of document.querySelectorAll<HTMLAnchorElement>("a[href]"))
          if (a.href === link.url) a.dataset.safexRisk = link.verdict;
      }
      respond({ ok: true });
      return;
    }
  });
  document.addEventListener(
    "submit",
    (e) => {
      const form = e.target as HTMLFormElement;
      if (
        e.isTrusted &&
        protecting &&
        !stopped &&
        unsafeFormActions.has(form.action)
      ) {
        e.preventDefault();
        e.stopImmediatePropagation();
        warn("high");
      }
    },
    true,
  );
  addEventListener("popstate", dirty);
  addEventListener("hashchange", dirty);
  // Navigation changes are checked opportunistically without keeping the extension worker alive.
  const navigation = (
    globalThis as typeof globalThis & { navigation?: EventTarget }
  ).navigation;
  navigation?.addEventListener("navigatesuccess", dirty);
  void msg({ type: "CONTENT_READY" }).then((reply) => {
    if (reply?.ok) {
      preferences = reply.preferences;
      stopped = reply.stopped;
      protecting = reply.automatic && !stopped;
      buildShield();
      if (protecting) {
        observer.observe(document.body, {
          subtree: true,
          childList: true,
          attributes: true,
          attributeFilter: ["href", "action", "method", "type"],
        });
        dirty();
      }
    }
  });
}
