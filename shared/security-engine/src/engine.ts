import {
  LIMITS,
  type Models,
  type Source,
  type ScanResult,
  type Evidence,
  type PageData,
  type LinkFinding,
  type Reputation,
  type PaymentReview,
  type ContextFlags,
} from "./contracts";
import { TextClassifier, messageSignals } from "./text";
import {
  BRANDS,
  official,
  parseUrl,
  predictUrl,
  urlFeatures,
  urlRules,
  modelCovered,
  verdict,
  type DomainResolver,
} from "./url";

const rank = { clear: 0, unknown: 0, caution: 1, high: 2 };
export function contextReview(
  result: ScanResult,
  flags: ContextFlags,
): ScanResult {
  const reported = Object.entries(flags)
    .filter(([, selected]) => selected === true)
    .map(([id]) => ({ id: `context_${id}`, weight: 0 }));
  if (!reported.length) return result;
  const total = result.reasons.reduce((n, e) => n + e.weight, 0);
  result.reasons.push(
    { id: "reported_context", weight: Math.max(0, 35 - total) },
    ...reported,
  );
  result.verdict = verdict(result.reasons);
  result.coverage.push("context_coverage");
  return result;
}
export function extractLinks(text: string): string[] {
  const normalized = text
    .replace(/hxxps?:\/\//gi, (s) =>
      s.toLowerCase().startsWith("hxxps") ? "https://" : "http://",
    )
    .replace(/\[\.\]|\(\.\)/g, ".");
  return [
    ...new Set(
      (
        normalized.match(
          /(?:https?:\/\/|www\.)[^\s<>"'\u0964\u0965]+|\b(?:[a-z\d-]+\.)+[a-z]{2,63}(?:\/[^\s<>"']*)?/gi,
        ) ?? []
      ).map((s) => s.replace(/[.,;!?)\]}]+$/, "")),
    ),
  ].slice(0, 9);
}
export function paymentReview(
  input: string,
  expected?: { payee: string; amount: string },
): PaymentReview | null {
  try {
    const u = new URL(input.trim());
    if (u.protocol !== "upi:" || u.hostname !== "pay") return null;
    const q = u.searchParams,
      payee = q.get("pa") ?? "",
      name = q.get("pn") ?? "",
      amount = q.get("am") ?? "",
      currency = q.get("cu") ?? "INR",
      issues = ["payment_unverified"];
    if (
      !/^[\p{L}\p{N}._-]{2,}@[a-z\d.-]{2,}$/iu.test(payee) ||
      q.getAll("pa").length !== 1
    )
      issues.push("payment_invalid");
    if (
      amount &&
      (!/^\d+(?:\.\d{1,2})?$/.test(amount) ||
        Number(amount) <= 0 ||
        Number(amount) > 1e9 ||
        q.getAll("am").length !== 1)
    )
      issues.push("payment_invalid");
    if (currency !== "INR") issues.push("payment_invalid");
    let matches: boolean | undefined;
    if (expected && (expected.payee.trim() || expected.amount.trim())) {
      matches =
        (!expected.payee.trim() ||
          expected.payee.trim().toLowerCase() === payee.toLowerCase()) &&
        (!expected.amount.trim() ||
          (/^[\d]+(?:\.\d{1,2})?$/.test(expected.amount) &&
            Number(expected.amount) === Number(amount)));
      if (!matches) issues.push("payment_mismatch");
    }
    return {
      payee: payee.slice(0, 150),
      name: name.slice(0, 150),
      amount: amount.slice(0, 24),
      currency,
      issues,
      matches,
    };
  } catch {
    return null;
  }
}
export class SecurityEngine {
  private classifier?: TextClassifier;
  constructor(
    readonly models: Models | undefined,
    readonly domain: DomainResolver,
    readonly unicodeHost: (h: string) => string = (h) => h,
  ) {
    try {
      if (models) this.classifier = new TextClassifier(models.text);
    } catch {
      this.classifier = undefined;
    }
  }
  link(input: string, reputation?: Reputation): LinkFinding {
    const p = parseUrl(input);
    if (!p)
      return {
        url: input.slice(0, 4096),
        host: "",
        domain: "",
        verdict: "unknown",
        reasons: [{ id: "invalid_url", weight: 0 }],
        inferred: false,
      };
    const reasons = urlRules(p, this.domain, this.unicodeHost);
    if (this.models && modelCovered(p))
      try {
        if (
          predictUrl(urlFeatures(p), this.models.url) >=
          this.models.url.threshold
        )
          reasons.push({
            id: "url_model",
            weight: Math.max(0, 35 - reasons.reduce((n, r) => n + r.weight, 0)),
          });
      } catch {
        reasons.push({ id: "model_unavailable", weight: 0 });
      }
    if (reputation) {
      const match = reputation.entries.find(
        (e) => parseUrl(e.url)?.url.href === p.url.href,
      );
      if (match)
        reasons.push({
          id:
            reputation.expiresAt < Date.now()
              ? "reputation_stale"
              : match.label === "block"
                ? "user_block"
                : "reputation_reported",
          weight:
            reputation.expiresAt < Date.now()
              ? 0
              : match.label === "block"
                ? 80
                : 35,
        });
    }
    return {
      url: p.url.href,
      host: p.host,
      domain: this.domain(p.host),
      verdict: verdict(reasons),
      reasons,
      inferred: p.inferred,
    };
  }
  private contextualLink(
    input: string,
    text: string,
    context: string,
    reputation?: Reputation,
  ): LinkFinding {
    const finding = this.link(input, reputation),
      target = parseUrl(input);
    if (!target) return finding;
    const displayed = extractLinks(text)
      .map((t) => parseUrl(t))
      .find((t) => t !== null);
    if (displayed && this.domain(displayed.host) !== this.domain(target.host))
      finding.reasons.push({
        id: "display_mismatch",
        weight: 35,
        detail: target.host,
      });
    const brand = Object.keys(BRANDS).find((b) =>
      new RegExp(`\\b${b}\\b`, "i").test(text),
    );
    if (
      brand &&
      !official(target.host, brand) &&
      /login|sign.?in|verify|password|otp|kyc/i.test(context)
    )
      finding.reasons.push({ id: "brand_context", weight: 35, detail: brand });
    finding.verdict = verdict(finding.reasons);
    return finding;
  }
  previewLink(
    input: string,
    text: string,
    context: string,
    reputation?: Reputation,
  ): ScanResult {
    const r = this.scan(input, "link", reputation),
      finding = this.contextualLink(input, text, context, reputation);
    r.links = [finding];
    r.reasons = finding.reasons;
    r.verdict = finding.verdict;
    return r;
  }
  private base(source: Source): ScanResult {
    return {
      id: crypto.randomUUID(),
      source,
      verdict: "clear",
      reasons: [],
      coverage: [],
      links: [],
      payments: [],
      checkedAt: Date.now(),
      durationMs: 0,
      modelVersion: this.models
        ? `${this.models.text.version} / ${this.models.url.version}`
        : "rules",
      partial: false,
    };
  }
  text(
    input: string,
    source: Source = "text",
    reputation?: Reputation,
    pageMode = false,
  ): ScanResult {
    const start = performance.now(),
      result = this.base(source);
    if (!input.trim())
      return { ...result, verdict: "unknown", coverage: ["empty_input"] };
    const text = input.slice(0, LIMITS.text),
      links = extractLinks(text),
      signals = messageSignals(text, links.length > 0);
    result.partial = input.length > LIMITS.text || links.length > 8;
    result.links = links.slice(0, 8).map((u) => this.link(u, reputation));
    result.reasons = signals.reasons;
    // Generic instructions to enter credentials in a form are not evidence of credential sharing.
    if (
      pageMode &&
      !/\b(share|send|reveal|tell|provide|give).*\b(password|otp|pin|code)|\b(password|otp|pin|code).*\b(share|send|agent)\b/i.test(
        signals.active,
      )
    )
      result.reasons = result.reasons.filter(
        (r) => !["credential_request", "authority"].includes(r.id),
      );
    if (this.classifier && !pageMode) {
      const score = this.classifier.predict(signals.active);
      if (
        (score >= this.classifier.model.threshold && signals.eligible) ||
        (score >= (this.models?.contextFreeThreshold ?? 1) &&
          this.classifier.covered(signals.active))
      )
        result.reasons.push({
          id: "text_model",
          weight: Math.max(
            0,
            35 - result.reasons.reduce((n, r) => n + r.weight, 0),
          ),
        });
    }
    for (const link of result.links)
      if (rank[link.verdict] > 0)
        result.reasons.push({
          id: "embedded_link",
          weight: rank[link.verdict] === 2 ? 65 : 35,
          detail: link.host,
        });
    if (/^(javascript:|data:text\/html|intent:|file:)/i.test(text.trim()))
      result.reasons.push({ id: "unsafe_scheme", weight: 35 });
    const qr = (text.match(/upi:\/\/pay\?[^\s<>]+/gi) ?? [])
      .slice(0, 4)
      .map((s) => paymentReview(s))
      .filter((r): r is PaymentReview => r !== null);
    result.payments = qr;
    if (qr.length)
      result.reasons.push({ id: "payment_unverified", weight: 30 });
    if (result.links.some((l) => l.verdict === "unknown")) {
      result.partial = true;
      result.reasons.push({ id: "invalid_url", weight: 30 });
    }
    result.verdict = verdict(result.reasons);
    result.coverage = [
      "text_coverage",
      this.classifier ? "model_limited" : "model_unavailable",
      "identity_unverified",
    ];
    if (result.partial) result.coverage.push("partial_coverage");
    result.durationMs = Math.round(performance.now() - start);
    return result;
  }
  scan(
    input: string,
    source: Source = "text",
    reputation?: Reputation,
    expected?: { payee: string; amount: string },
  ): ScanResult {
    const start = performance.now();
    const payment = paymentReview(input, expected);
    if (payment) {
      const r = this.base("qr");
      r.payments = [payment];
      r.reasons = payment.issues.map((id) => ({
        id,
        weight: id === "payment_mismatch" ? 70 : 30,
      }));
      r.coverage = ["payment_coverage"];
      r.verdict = verdict(r.reasons);
      return r;
    }
    if (
      source === "link" ||
      /^(https?:\/\/|hxxps?:\/\/|www\.)\S+$/i.test(input.trim())
    ) {
      const r = this.base("link"),
        link = this.link(input.replace(/^hxxp/i, "http"), reputation);
      r.links = [link];
      r.reasons = link.reasons;
      r.verdict = link.verdict;
      r.domain = link.domain;
      r.coverage = [
        "url_coverage",
        this.models ? "model_limited" : "model_unavailable",
        "identity_unverified",
      ];
      r.durationMs = Math.round(performance.now() - start);
      return r;
    }
    return this.text(input, source, reputation);
  }
  page(page: PageData, reputation?: Reputation): ScanResult {
    const start = performance.now(),
      r = this.base("page"),
      origin = parseUrl(page.url);
    if (!origin)
      return { ...r, verdict: "unknown", coverage: ["page_unavailable"] };
    r.pageKey = page.pageKey;
    r.domain = this.domain(origin.host);
    r.partial = page.partial || page.inaccessibleFrames > 0;
    const main = this.link(page.url, reputation);
    r.links = [main];
    r.reasons = [...main.reasons];
    for (const passage of page.passages.slice(0, LIMITS.passages))
      r.reasons.push(
        ...this.text(passage, "page", reputation, true).reasons.filter(
          (e) => e.weight >= 30,
        ),
      );
    for (const anchor of page.links.slice(0, LIMITS.links)) {
      const target = parseUrl(anchor.href, page.url);
      if (!target) continue;
      const finding = this.contextualLink(
        target.url.href,
        anchor.text,
        anchor.context,
        reputation,
      );
      if (rank[finding.verdict] > 0) {
        r.links.push(finding);
        r.reasons.push(...finding.reasons.filter((e) => e.weight >= 30));
      }
    }
    const pageClaim = (
      page.title +
      " " +
      page.passages.slice(0, 2).join(" ")
    ).slice(0, 1800);
    const claimed = Object.keys(BRANDS).find((b) =>
      new RegExp(`\\b${b}\\b`, "i").test(pageClaim),
    );
    r.unsafeFormActions = [];
    for (const form of page.forms.slice(0, LIMITS.forms)) {
      const target = parseUrl(form.action || page.url, page.url);
      if ((form.password || form.otp || form.payment) && target) {
        const prior = r.reasons.length;
        if (target.url.protocol === "http:")
          r.reasons.push({ id: "insecure_form", weight: 65 });
        if (this.domain(target.host) !== this.domain(origin.host))
          r.reasons.push({
            id: "external_form",
            weight: 0,
            detail: target.host,
          });
        if (claimed && !official(origin.host, claimed))
          r.reasons.push({
            id: "login_impersonation",
            weight: 65,
            detail: claimed,
          });
        const action = this.link(target.url.href, reputation);
        if (rank[action.verdict] > 0)
          r.reasons.push({ id: "risky_form", weight: 35, detail: target.host });
        if (r.reasons.slice(prior).some((e) => e.weight >= 30))
          r.unsafeFormActions.push(target.url.href);
      }
    }
    const unique = new Map<string, Evidence>();
    for (const e of r.reasons)
      if (!unique.has(e.id) || unique.get(e.id)!.weight < e.weight)
        unique.set(e.id, e);
    r.reasons = [...unique.values()].sort((a, b) => b.weight - a.weight);
    r.links = r.links.slice(0, LIMITS.links + 1);
    r.verdict = verdict(r.reasons);
    r.coverage = [
      "page_coverage",
      "forms_metadata",
      "identity_unverified",
      "model_limited",
    ];
    if (page.selectedOnly) {
      r.coverage = [
        "url_coverage",
        "private_messages",
        "identity_unverified",
        "model_limited",
      ];
      if (r.verdict === "clear") r.verdict = "unknown";
    }
    if (r.partial) r.coverage.push("partial_coverage");
    if (page.inaccessibleFrames) r.coverage.push("frames_unavailable");
    r.counts = {
      links: page.links.length,
      forms: page.forms.length,
      passages: page.passages.length,
    };
    r.durationMs = Math.round(performance.now() - start);
    return r;
  }
}
