import type { Evidence, UrlModel, Verdict } from "./contracts";
export const BRANDS: Record<string, string[]> = {
  google: ["google.com", "google.co.in", "youtube.com", "gmail.com"],
  paypal: ["paypal.com", "paypal.me"],
  amazon: ["amazon.com", "amazon.in", "amazon.co.jp", "amazon.de"],
  apple: ["apple.com", "icloud.com"],
  instagram: ["instagram.com"],
  facebook: ["facebook.com", "fb.com"],
  whatsapp: ["whatsapp.com"],
  telegram: ["telegram.org", "t.me"],
  microsoft: [
    "microsoft.com",
    "microsoftonline.com",
    "office.com",
    "live.com",
    "outlook.com",
  ],
  netflix: ["netflix.com"],
  sbi: ["sbi.co.in", "onlinesbi.sbi"],
  hdfc: ["hdfcbank.com"],
  icici: ["icicibank.com"],
  paytm: ["paytm.com"],
  phonepe: ["phonepe.com"],
};
const TLDS = new Set(
  "live click top xyz online info vip fit gq cf tk ml ga work club buzz support security update verify download bid loan men win stream".split(
    " ",
  ),
);
const MODEL_WORDS = [
  "login",
  "verify",
  "secure",
  "account",
  "bank",
  "update",
  "crypto",
];
const SENSITIVE =
  "login verify verification account secure bank banking kyc otp password wallet connect refund gift claim reward update payment".split(
    " ",
  );
const EXTRAS = [...SENSITIVE, "support", "signin", "auth", "recovery", "help"];
const SOCIAL = [
  ...SENSITIVE,
  "aadhaar",
  "pan",
  "lottery",
  "ipl-ticket",
  "ipl",
  "ticket",
  "urgent",
  "signin",
  "free-gift",
  "cashback",
  "win-money",
];
const REDIRECTS = new Set(
  "redirect redirect_url redirect_uri return return_url next continue target destination dest goto out link url to".split(
    " ",
  ),
);
const SHORTENERS = new Set(
  "bit.ly tinyurl.com t.co cutt.ly is.gd ow.ly lnkd.in rebrand.ly tiny.cc rb.gy shorturl.at".split(
    " ",
  ),
);
export type DomainResolver = (host: string) => string;
export interface Parsed {
  url: URL;
  normalized: string;
  original: string;
  host: string;
  path: string;
  query: string;
  inferred: boolean;
  numeric: boolean;
  obfuscated: boolean;
}
export const official = (host: string, brand?: string): boolean =>
  (brand ? (BRANDS[brand] ?? []) : Object.values(BRANDS).flat()).some(
    (d) => host === d || host.endsWith(`.${d}`),
  );
const decode = (s: string): string => {
  try {
    return decodeURIComponent(s);
  } catch {
    return s;
  }
};
export function parseUrl(input: string, base?: string): Parsed | null {
  const original = input.trim();
  if (
    !original ||
    original.length > 4096 ||
    /[\u0000-\u0020\u007f\u200b-\u200f\u202a-\u202e\u2066-\u2069\ufeff\\]/u.test(
      original,
    )
  )
    return null;
  const inferred = !base && !/^[a-z][a-z\d+.-]*:/i.test(original);
  try {
    const value = inferred ? `https://${original}` : original;
    const url = new URL(value, base);
    if (!["http:", "https:"].includes(url.protocol) || !url.hostname)
      return null;
    url.hostname = url.hostname.toLowerCase().replace(/\.$/, "");
    const host = url.hostname;
    const numeric = /^\d+(?:\.\d+){3}$/.test(host);
    const authority =
      value
        .match(/^[a-z]+:\/\/([^/?#]+)/i)?.[1]
        ?.split("@")
        .at(-1)
        ?.replace(/:\d+$/, "") ?? host;
    const tail = value.match(/^[a-z]+:\/\/[^/?#]+(.*)$/i)?.[1];
    const normalized =
      tail !== undefined
        ? `${url.protocol}//${url.username ? `${url.username}${url.password ? ":" + url.password : ""}@` : ""}${host}${url.port ? ":" + url.port : ""}${tail}`
        : url.href;
    return {
      url,
      original,
      normalized,
      host,
      path: decode(
        tail === "" || tail?.startsWith("?") || tail?.startsWith("#")
          ? ""
          : url.pathname,
      ),
      query: url.search.slice(1),
      inferred,
      numeric,
      obfuscated: numeric && authority !== host,
    };
  } catch {
    return null;
  }
}
export function urlFeatures(p: Parsed): number[] {
  const u = p.normalized,
    host = p.host,
    suspicious = MODEL_WORDS.some((w) => u.toLowerCase().includes(w)),
    known = official(host);
  const impersonation = Math.min(
    1,
    (!known && Object.keys(BRANDS).some((b) => host.includes(b)) ? 0.5 : 0) +
      (suspicious ? 0.25 : 0) +
      (host.includes("-") ? 0.25 : 0),
  );
  return [
    u.length,
    host.length,
    +p.numeric,
    Math.max(0, host.split(".").length - 2),
    +(p.url.protocol === "https:"),
    +suspicious,
    (u.match(/[^\p{L}\p{N}]/gu)?.length ?? 0) / u.length,
    (u.match(/\p{Nd}/gu)?.length ?? 0) / u.length,
    +u.includes("@"),
    +TLDS.has(host.split(".").at(-1)!),
    impersonation,
    u.match(/-/g)?.length ?? 0,
    p.path.length + p.query.length,
    +known,
    (host.match(/[aeiou]/g)?.length ?? 0) / host.length,
  ].map(Math.fround);
}
export function predictUrl(features: number[], m: UrlModel): number {
  if (
    features.length !== 15 ||
    m.mean.length !== 15 ||
    m.scale.length !== 15 ||
    m.parameters.length !== 6
  )
    throw Error("modelInvalid");
  let state = features.map((n, i) =>
    Math.fround(
      Math.max(
        -m.clip,
        Math.min(m.clip, Math.fround(Math.fround(n - m.mean[i]) / m.scale[i])),
      ),
    ),
  );
  for (let layer = 0; layer < 3; layer++) {
    const weights = m.parameters[layer * 2] as number[][],
      bias = m.parameters[layer * 2 + 1] as number[];
    if (
      weights.length !== state.length ||
      weights.some((row) => row.length !== bias.length) ||
      !bias.length
    )
      throw Error("modelInvalid");
    state = bias.map((b, j) => {
      let sum = b;
      for (let i = 0; i < state.length; i++) sum += state[i] * weights[i][j];
      return Math.fround(layer === 2 ? sum : Math.max(0, sum));
    });
  }
  const prediction = Math.fround(1 / (1 + Math.exp(-state[0])));
  if (!Number.isFinite(prediction)) throw Error("modelInvalid");
  return prediction;
}
export function modelCovered(p: Parsed): boolean {
  return (
    !p.host.includes("xn--") &&
    !official(p.host) &&
    !(
      p.url.username === "" &&
      [...p.url.searchParams].some(
        ([k, v]) =>
          ["email", "login_hint"].includes(k.toLowerCase()) &&
          /^[a-z\d._%+-]+@[a-z\d.-]+\.[a-z]{2,63}$/i.test(v),
      )
    )
  );
}
const confusables: Record<string, string> = {
  а: "a",
  е: "e",
  о: "o",
  р: "p",
  с: "c",
  у: "y",
  х: "x",
  і: "i",
  ј: "j",
  ӏ: "l",
  ѕ: "s",
  α: "a",
  ο: "o",
  ρ: "p",
  ι: "i",
};
export function skeleton(s: string): string {
  return [
    ...s
      .toLowerCase()
      .normalize("NFKD")
      .replace(/\p{Mn}/gu, ""),
  ]
    .map((c) => confusables[c] ?? c)
    .join("");
}
export function near(a: string, b: string): boolean {
  if (a === b) return false;
  if (a.length > 64 || Math.abs(a.length - b.length) > 1) return false;
  const dp = Array.from({ length: a.length + 1 }, (_, i) =>
    Array.from({ length: b.length + 1 }, (_, j) =>
      i === 0 ? j : j === 0 ? i : 0,
    ),
  );
  for (let i = 1; i <= a.length; i++)
    for (let j = 1; j <= b.length; j++) {
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,
        dp[i][j - 1] + 1,
        dp[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1),
      );
      if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1])
        dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + 1);
    }
  return dp[a.length][b.length] <= 1;
}
export function urlRules(
  p: Parsed,
  domain: DomainResolver,
  unicodeHost: (host: string) => string,
): Evidence[] {
  const out: Evidence[] = [],
    h = p.host,
    u = p.normalized,
    label = skeleton(unicodeHost(domain(h)).split(".")[0]);
  const add = (hit: boolean, id: string, weight: number, detail?: string) => {
    if (hit) out.push({ id, weight, detail });
  };
  const unknownBrand = !official(h),
    credential = /login|signin|verify|password|account|otp|kyc|auth/i.test(
      p.path + p.query,
    );
  const hostWords = h.split(/[^a-z\d]+/),
    last = h.split(".").at(-1)!;
  add(TLDS.has(last), "suspicious_tld", 15);
  add(p.numeric || h.startsWith("["), "ip_address", 25);
  add(h.split(".").length - 1 > 3, "excessive_subdomains", 10);
  const entropy = (s: string) => {
    const counts = new Map<string, number>();
    for (const c of s) counts.set(c, (counts.get(c) ?? 0) + 1);
    return [...counts.values()].reduce(
      (v, n) => v - (n / s.length) * Math.log2(n / s.length),
      0,
    );
  };
  add(label.length > 16 && entropy(label) > 3.8, "random_hostname", 10);
  add(/--/.test(h) && !h.includes("xn--"), "repeated_hyphens", 10);
  add((h.match(/\d/g)?.length ?? 0) / h.length > 0.3, "excessive_digits", 10);
  add(h.includes("xn--"), "punycode", 15);
  add(u.length > 150, "excessive_length", 5);
  add(p.path.split("/").filter(Boolean).length > 4, "deep_nesting", 5);
  add((p.path.split("/").at(-1)?.length ?? 0) > 30, "long_filename", 5);
  add([...p.url.searchParams].length > 5, "excessive_query", 10);
  add((u.match(/%[\da-f]{2}/gi)?.length ?? 0) > 3, "encoded_chars", 10);
  add(
    [...p.url.searchParams.keys()].some((k) =>
      /^(utm_|fbclid$|gclid$|msclkid$)/i.test(k),
    ),
    "tracking_parameters",
    0,
  );
  const external: string[] = [];
  for (const [k, v] of p.url.searchParams)
    if (REDIRECTS.has(k.toLowerCase())) {
      let value = v;
      for (let n = 0; n < 3; n++) {
        const target = parseUrl(value);
        if (
          target &&
          domain(target.host) !== domain(h) &&
          /^https?:/i.test(value)
        ) {
          external.push(target.host);
          break;
        }
        value = decode(value);
      }
    }
  add(external.length > 0, "suspicious_redirect", 15, external[0]);
  let impersonated: string | undefined;
  for (const [brand, domains] of Object.entries(BRANDS)) {
    if (official(h, brand)) continue;
    const substitute =
      label.length === brand.length &&
      [...label].every(
        (c, i) =>
          c === brand[i] ||
          (
            { o: "0", i: "1l", l: "1i", e: "3", a: "4", s: "5", g: "9" }[
              brand[i]
            ] ?? ""
          ).includes(c),
      );
    if (
      domains.some((d) => h.includes(`${d}.`)) ||
      label === brand ||
      EXTRAS.some(
        (w) =>
          label.includes(brand + w) ||
          label.includes(w + brand) ||
          label.includes(brand + "-" + w) ||
          label.includes(w + "-" + brand),
      ) ||
      substitute ||
      (brand.length >= 5 &&
        near(label, brand) &&
        (credential || /\d/.test(label) || h.includes("xn--"))) ||
      (credential && hostWords.includes(brand))
    ) {
      impersonated = brand;
      break;
    }
  }
  add(!!impersonated, "brand_impersonation", 30, impersonated);
  const social =
    SOCIAL.find((w) => h.replace(/_/g, "-").includes(w)) ??
    SOCIAL.find((w) => u.toLowerCase().includes(w));
  add(!!social, "social_engineering", social && h.includes(social) ? 20 : 2);
  add(p.url.protocol === "http:" && !p.inferred, "insecure_http", 30);
  add(
    !!p.url.port && !["80", "443"].includes(p.url.port),
    "non_standard_port",
    10,
  );
  add(!!p.url.username || !!p.url.password, "userinfo_deception", 30);
  const embedded = decode(p.path)
    .match(/https?:\/\/[^\s?#]+/gi)
    ?.map((s) => parseUrl(s))
    .find((s) => s && domain(s.host) !== domain(h));
  add(!!embedded, "embedded_url", 30, embedded?.host);
  add(SHORTENERS.has(h.replace(/^www\./, "")), "shortened_destination", 30);
  add(p.obfuscated, "numeric_host_encoding", 30);
  add(
    /%25[\da-f]{2}/i.test(u) ||
      [...p.url.searchParams].some(
        ([k, v]) =>
          REDIRECTS.has(k) && /^(?:javascript:|intent:|data:)/i.test(v),
      ),
    "layered_destination",
    30,
  );
  add(
    /\.(apk|exe|msi|scr|bat|cmd|ps1|vbs|jar)$|\.(pdf|docx?|xlsx?|jpe?g|png|txt)[ .]+(?:exe|apk|scr|js|vbs|bat|cmd|ps1)$/i.test(
      p.path,
    ),
    "executable_download",
    30,
  );
  add(
    unknownBrand &&
      new Set(hostWords.filter((w) => SENSITIVE.includes(w))).size >= 2,
    "sensitive_host_cues",
    30,
  );
  add(
    unknownBrand &&
      credential &&
      Object.values(BRANDS)
        .flat()
        .some((d) => (p.path + p.query + p.url.hash).includes(d)),
    "brand_camouflage",
    30,
  );
  add(p.inferred, "inferred_scheme", 0);
  return out;
}
export function verdict(reasons: Evidence[]): Verdict {
  const score = reasons.reduce((n, r) => n + r.weight, 0);
  return score >= 65 ? "high" : score >= 30 ? "caution" : "clear";
}
