import type {
  Language,
  Preferences,
} from "../../shared/security-engine/src/contracts";
import { el, button } from "./ui";
import COPY from "./guidance-copy.json";

export interface GuidanceState {
  version: number;
  introComplete: boolean;
  tourComplete: boolean;
}
export const DEFAULT_GUIDANCE: GuidanceState = {
  version: 1,
  introComplete: false,
  tourComplete: false,
};
export function guidanceText(value: string, language: Language): string {
  if (language === "en") return value;
  return (
    (COPY as Record<string, string[]>)[value]?.[language === "hi" ? 0 : 1] ??
    value
  );
}
export const EXTENSION_STEPS = [
  [
    "scan",
    '[data-guide="site"]',
    "Check the current page",
    "Scan this page after using the toolbar action. Optional site protection enables supported automatic checks. Private mail/chat and restricted pages use selected-text or paste fallback.",
  ],
  [
    "scan",
    "#scan-input",
    "Check an unexpected message",
    "Paste the complete message or link, then choose Analyze privately. Read the actual reason and coverage; clear does not mean verified safe.",
  ],
  [
    "scan",
    '[data-guide="context"]',
    "Add the context you know",
    "Use Add context when a request involves remote access, gift cards, a wallet phrase, an advance fee or secrecy. The extra caution is based on your report, not verified identity.",
  ],
  [
    "scan",
    '[data-guide="capture"]',
    "Capture only what you choose",
    "Capture the current visible tab, crop a region and review it. Choose an image to import instead. OCR and QR decoding run locally; check the recognized text before scanning.",
  ],
  [
    "scan",
    '[data-guide="images"]',
    "Review screenshot and QR content",
    "Choose a clear PNG, JPEG or WebP image. Select English, Hindi or Gujarati OCR. Review a decoded payment address and amount, then compare with expected details. Matching does not verify bank ownership.",
  ],
  [
    "scan",
    '[data-guide="samples"]',
    "Try a harmless example",
    "Synthetic samples run through the actual local detector. Compare a credential request and ordinary advice. No sample link needs to be opened.",
  ],
  [
    "history",
    "main",
    "Control saved reports",
    "Save is an explicit choice. History contains redacted local reports; unsaved session content expires after 15 minutes. Export and delete reports here. Local storage is not encrypted.",
  ],
  [
    "settings",
    '[data-guide="reading"]',
    "Make guidance easy to read",
    "Choose English, Hindi or Gujarati and adjust text size. Menus, warnings and this guide follow your choice immediately.",
  ],
  [
    "settings",
    '[data-guide="sound"]',
    "Test the warning tune",
    "Enable warning sound and use Test sound. Automatic alerts have a 60-second cooldown. Browser and device audio settings still control playback.",
  ],
  [
    "settings",
    '[data-guide="lists"]',
    "Review imported threat lists",
    "Imported reported entries warn; only explicitly labelled block entries can be blocked after you enable blocking. Lists expire after seven days. The model does not automatically create navigation blocks.",
  ],
  [
    "help",
    '[data-guide="situation"]',
    "Get help for what happened",
    "Choose whether you opened a link, shared a password or OTP, installed an app or sent money. The checklist is local and does not submit a complaint.",
  ],
  [
    "help",
    '[data-guide="contact"]',
    "Contact official help in India",
    "For financial cyber fraud, use 1930. For immediate danger, use 112. Copy the number to your phone or deliberately open a calling app. Desktop Chrome cannot guarantee that a calling app is installed.",
  ],
] as const;
const INTRO = [
  [
    "Welcome to SafeX AI",
    "Private security checks for messages, links, screenshots and QR content, right in your browser. Start with a quick introduction or skip to a manual scan.",
  ],
  [
    "Your language. Your reading size.",
    "Choose English, Hindi or Gujarati and a comfortable reading size. You can change both later in Settings.",
  ],
  [
    "Private checks, honest limits",
    "Your scan content is processed locally. OCR and model files are packaged for offline use. A clear result is not a safety guarantee. Site monitoring requires your consent.",
  ],
  [
    "You stay in control",
    "The tour highlights real controls. It does not scan, read your inbox, grant site access, enable sound, place a call or submit a report. You can close it and replay it anytime.",
  ],
] as const;
interface Hooks {
  preferences: () => Preferences;
  preference: (patch: Partial<Preferences>) => Promise<void>;
  persist: (
    patch: Partial<Pick<GuidanceState, "introComplete" | "tourComplete">>,
  ) => Promise<void>;
  navigate: (view: string) => void;
  announce: () => void;
}
export class Guidance {
  private intro: number | null = null;
  private step: number | null = null;
  private booted = false;
  private busy = false;
  private frame = 0;
  private dialog?: HTMLDialogElement;
  private renderKey = "";
  constructor(private hooks: Hooks) {
    addEventListener("resize", () => this.position());
    addEventListener("scroll", () => this.position(), true);
  }
  text(value: string) {
    return guidanceText(value, this.hooks.preferences().language);
  }
  boot(state: GuidanceState) {
    if (this.booted) return;
    this.booted = true;
    if (!state.introComplete) this.intro = 0;
  }
  start(index = 0) {
    this.intro = null;
    this.step = Math.max(0, Math.min(EXTENSION_STEPS.length - 1, index));
    this.hooks.navigate(EXTENSION_STEPS[this.step][0]);
  }
  private async finish(tour: boolean) {
    if (this.busy) return;
    this.busy = true;
    try {
      await this.hooks.persist({
        introComplete: true,
        ...(tour ? { tourComplete: true } : {}),
      });
      this.intro = null;
      this.step = null;
      this.dialog?.close();
      this.render();
    } catch {
      this.hooks.announce();
    } finally {
      this.busy = false;
    }
  }
  private help() {
    this.intro = null;
    this.step = null;
    this.dialog?.close();
    this.hooks.navigate("help");
  }
  render() {
    const preferences = this.hooks.preferences();
    const key = `${this.intro}/${this.step}/${preferences.language}/${preferences.fontSize}`;
    if (this.dialog?.isConnected && key === this.renderKey) {
      this.position();
      return;
    }
    this.renderKey = key;
    const focusedId = this.dialog?.contains(document.activeElement)
      ? (document.activeElement as HTMLElement).id
      : null;
    this.dialog?.close();
    this.dialog?.remove();
    this.dialog = undefined;
    if (this.intro === null && this.step === null) return;
    const dialog = el(
      "dialog",
      this.intro !== null ? "guide-intro" : "guide-tour",
    );
    dialog.id = "guide-dialog";
    dialog.setAttribute("aria-modal", "true");
    this.dialog = dialog;
    const card = el("section", "guide-card"),
      head = el("div", "guide-head"),
      logo = el("img", "logo");
    logo.src = "logo.svg";
    logo.alt = "SafeX AI";
    const title = el(
      "h2",
      "",
      this.text(
        this.intro !== null
          ? INTRO[this.intro][0]
          : EXTENSION_STEPS[this.step!][2],
      ),
    );
    title.id = "guide-title";
    dialog.setAttribute("aria-labelledby", title.id);
    head.append(
      logo,
      el(
        "span",
        "eyebrow",
        this.text(this.intro !== null ? "Getting started" : "Feature tour"),
      ),
    );
    card.append(
      head,
      el(
        "p",
        "muted small",
        `${(this.intro ?? this.step ?? 0) + 1} / ${this.intro !== null ? INTRO.length : EXTENSION_STEPS.length}`,
      ),
      title,
    );
    const content = el("div", "guide-body");
    content.append(
      el(
        "p",
        "",
        this.text(
          this.intro !== null
            ? INTRO[this.intro][1]
            : EXTENSION_STEPS[this.step!][3],
        ),
      ),
    );
    if (this.intro === 1) {
      const lab = el("label", "", this.text("Language")),
        select = el("select");
      select.id = "guide-language";
      for (const [v, name] of [
        ["en", "English"],
        ["hi", "हिन्दी"],
        ["gu", "ગુજરાતી"],
      ]) {
        const o = el("option", "", name);
        o.value = v;
        o.selected = v === this.hooks.preferences().language;
        select.append(o);
      }
      lab.htmlFor = select.id;
      select.onchange = () =>
        void this.hooks.preference({ language: select.value as Language });
      content.append(lab, select);
      const sizeLab = el("label", "", this.text("Text size")),
        size = el("select");
      size.id = "guide-size";
      sizeLab.htmlFor = size.id;
      for (const value of [85, 100, 125, 150]) {
        const o = el("option", "", value + "%");
        o.value = String(value);
        o.selected = value === this.hooks.preferences().fontSize;
        size.append(o);
      }
      size.onchange = () =>
        void this.hooks.preference({ fontSize: Number(size.value) });
      content.append(sizeLab, size);
    }
    card.append(content);
    const actions = el("div", "guide-actions");
    const skip = button(
      this.text(this.intro !== null ? "Skip introduction" : "Close tour"),
      () => this.finish(this.step !== null),
      "button ghost compact",
    );
    skip.id = "guide-skip";
    actions.append(skip);
    if ((this.intro ?? this.step ?? 0) > 0) {
      const back = button(
        this.text("Back"),
        () => {
          if (this.intro !== null) {
            this.intro--;
            this.render();
          } else {
            this.step = this.step! - 1;
            this.hooks.navigate(EXTENSION_STEPS[this.step!][0]);
          }
        },
        "button compact",
      );
      back.id = "guide-back";
      actions.append(back);
    }
    const last =
      this.intro !== null
        ? this.intro === INTRO.length - 1
        : this.step === EXTENSION_STEPS.length - 1;
    const next = button(
      this.text(
        last
          ? this.intro !== null
            ? "Take the feature tour"
            : "Finish"
          : "Next",
      ),
      async () => {
        if (this.busy) return;
        if (this.intro !== null) {
          if (last) {
            try {
              await this.hooks.persist({ introComplete: true });
              this.start();
            } catch {
              this.hooks.announce();
            }
          } else {
            this.intro++;
            this.render();
          }
        } else if (last) await this.finish(true);
        else {
          this.step = this.step! + 1;
          this.hooks.navigate(EXTENSION_STEPS[this.step!][0]);
        }
      },
      "button primary compact",
    );
    next.id = "guide-next";
    actions.append(next);
    card.append(actions);
    card.append(
      button(
        this.text("Need help after a scam?"),
        () => this.help(),
        "button ghost full",
      ),
    );
    if (this.step !== null) {
      const ring = el("div", "guide-ring");
      ring.setAttribute("aria-hidden", "true");
      dialog.append(ring);
    }
    dialog.append(card);
    document.body.append(dialog);
    dialog.addEventListener("cancel", (e) => {
      e.preventDefault();
      void this.finish(this.step !== null);
    });
    dialog.showModal();
    if (this.step !== null) {
      const target = document.querySelector<HTMLElement>(
        EXTENSION_STEPS[this.step][1],
      );
      if (target instanceof HTMLDetailsElement) target.open = true;
      target?.scrollIntoView({ block: "center", behavior: "instant" });
      this.position();
    }
    if (focusedId)
      document.getElementById(focusedId)?.focus({ preventScroll: true });
    else next.focus({ preventScroll: true });
  }
  private position() {
    cancelAnimationFrame(this.frame);
    this.frame = requestAnimationFrame(() => {
      if (this.step === null || !this.dialog) return;
      const target = document.querySelector<HTMLElement>(
          EXTENSION_STEPS[this.step][1],
        ),
        rect = target?.getBoundingClientRect(),
        ring = this.dialog.querySelector<HTMLElement>(".guide-ring"),
        card = this.dialog.querySelector<HTMLElement>(".guide-card");
      if (!ring || !card) return;
      if (rect && rect.width > 0 && rect.height > 0) {
        const top = Math.max(4, rect.top),
          bottom = Math.min(innerHeight - 4, rect.bottom);
        ring.style.left = Math.max(4, rect.left - 3) + "px";
        ring.style.top = top + "px";
        ring.style.width = Math.min(innerWidth - 8, rect.width + 6) + "px";
        ring.style.height = Math.max(0, bottom - top) + "px";
        ring.hidden = bottom <= top;
        card.style.top =
          rect.top + rect.height / 2 > innerHeight / 2 ? "12px" : "auto";
        card.style.bottom =
          rect.top + rect.height / 2 > innerHeight / 2 ? "auto" : "12px";
      } else {
        ring.hidden = true;
        card.style.bottom = "12px";
        card.style.top = "auto";
      }
    });
  }
}
