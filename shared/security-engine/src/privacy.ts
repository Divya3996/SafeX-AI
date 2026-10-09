import type { Reputation, ScanResult, ThreatEntry } from "./contracts";
import { parseUrl } from "./url";
export function redactUrl(input: string): string {
  const parsed = parseUrl(input);
  if (!parsed) return "[redacted]";
  const u = parsed.url;
  u.username = "";
  u.password = "";
  u.search = "";
  u.hash = "";
  u.pathname = u.pathname
    .split("/")
    .map((s) => (s.length > 40 || /^[a-f\d]{20,}$/i.test(s) ? "redacted" : s))
    .join("/");
  return u.href;
}
export function savedReport(result: ScanResult): ScanResult {
  const report: ScanResult = {
    ...result,
    links: result.links.map((l) => ({
      ...l,
      url: redactUrl(l.url),
      reasons: l.reasons.map((r) => ({
        ...r,
        detail: r.detail
          ?.replace(/[\w.+-]+@[\w.-]+/g, "[redacted]")
          .slice(0, 100),
      })),
    })),
    payments: result.payments.map((p) => ({
      ...p,
      payee: "[redacted]",
      name: "",
      amount: "",
      matches: undefined,
    })),
    reasons: result.reasons.map((r) => ({
      ...r,
      detail: r.detail
        ?.replace(/[\w.+-]+@[\w.-]+/g, "[redacted]")
        .slice(0, 100),
    })),
  };
  delete report.tabId;
  delete report.pageKey;
  delete report.unsafeFormActions;
  return report;
}
export function importReputation(
  raw: string,
  name: string,
  now = Date.now(),
): Reputation {
  if (raw.length > 1_000_000) throw Error("importTooLarge");
  let rows: unknown[];
  if (raw.trim().startsWith("{") || raw.trim().startsWith("[")) {
    const data = JSON.parse(raw);
    rows = Array.isArray(data) ? data : data.entries;
  } else
    rows = raw
      .split(/\r?\n/)
      .map((s) => s.trim())
      .filter((s) => s && !s.startsWith("#"));
  if (!Array.isArray(rows) || rows.length > 2000) throw Error("importInvalid");
  const entries: ThreatEntry[] = [];
  for (const row of rows) {
    const value =
      typeof row === "string" ? row : (row as { url?: unknown })?.url;
    if (typeof value !== "string" || value.length > 1500)
      throw Error("importInvalid");
    const p = parseUrl(value);
    if (!p || p.url.username || p.url.password) throw Error("importInvalid");
    const label =
      typeof row === "object" &&
      row !== null &&
      (row as { label?: unknown }).label === "block"
        ? "block"
        : "reported";
    if (
      label === "block" &&
      (p.url.hash ||
        [...p.url.searchParams.keys()].some((k) =>
          /token|pass|secret|code|email|session|key|auth/i.test(k),
        ))
    )
      throw Error("importSensitive");
    entries.push({ url: p.url.href, label, addedAt: now });
  }
  if (entries.filter((e) => e.label === "block").length > 500)
    throw Error("importTooLarge");
  const unique = new Map(entries.map((e) => [e.url, e]));
  return {
    entries: [...unique.values()],
    importedAt: now,
    name: name.replace(/[^\p{L}\p{N} ._-]/gu, "").slice(0, 80),
    expiresAt: now + 7 * 86400000,
  };
}
