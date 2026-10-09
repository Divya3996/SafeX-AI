import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import { getDomain } from "tldts";
import punycode from "punycode/punycode.js";
import {
  SecurityEngine,
  paymentReview,
  contextReview,
} from "../../shared/security-engine/src/engine";
import {
  TextClassifier,
  textFeatures,
} from "../../shared/security-engine/src/text";
import {
  parseUrl,
  urlFeatures,
  predictUrl,
} from "../../shared/security-engine/src/url";
import type {
  Models,
  PageData,
} from "../../shared/security-engine/src/contracts";
const read = (p: string) =>
  JSON.parse(fs.readFileSync(new URL(p, import.meta.url), "utf8"));
export const models: Models = {
  text: read("../public/models/text-model.json"),
  url: read("../public/models/url-model.json"),
  contextFreeThreshold: read("../public/models/text-warning-policy.json")
    .threshold,
};
const engine = new SecurityEngine(
  models,
  (h) => getDomain(h, { allowPrivateDomains: true }) ?? h,
  punycode.toUnicode,
);
const classifier = new TextClassifier(models.text);
for (const [i, c] of read("fixtures/text-model-parity.json").cases.entries())
  test(`text model matches research reference ${i}`, () =>
    assert.ok(Math.abs(classifier.predict(c.text) - c.predicted) < 1e-7));
for (const [i, c] of read("fixtures/url-vector-parity.json").cases.entries())
  test(`URL weights/scaler reproduce reference ${i}`, () =>
    assert.ok(
      Math.abs(predictUrl(c.features, models.url) - c.predicted) < 1e-5,
    ));
for (const c of read("fixtures/fraud-app-contracts.json").cases)
  test(`multilingual authored contract ${c.id}`, () => {
    const result = engine.scan(c.text);
    assert.equal(
      ["caution", "high"].includes(result.verdict),
      c.expected === "WARN",
      JSON.stringify(result),
    );
  });
test("Kotlin URL feature spelling and no implicit slash", () => {
  const p = parseUrl("https://www.sfnmjournal.com")!;
  assert.equal(p.normalized, "https://www.sfnmjournal.com");
  assert.deepEqual(urlFeatures(p), [
    27,
    19,
    0,
    1,
    1,
    0,
    Math.fround(5 / 27),
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    Math.fround(4 / 19),
  ]);
});
test("identifier masking and combining marks", () => {
  assert.deepEqual(
    textFeatures("Contact a@b.com 9876 at https://example.test"),
    textFeatures("Contact c@d.com 1234 at https://other.test"),
  );
  assert.ok(textFeatures("ગુજરાતી पासवर्ड").has("ગુજરાતી"));
  assert.ok(textFeatures("ગુજરાતી पासवर्ड").has("पासवर्ड"));
});
test("unsupported and encoded numeric URL inputs", () => {
  assert.equal(parseUrl("javascript:alert(1)"), null);
  assert.equal(parseUrl("https://a.test\\@b.test"), null);
  assert.equal(parseUrl("https://a.test\u202eb"), null);
  assert.equal(parseUrl("http://2130706433")!.host, "127.0.0.1");
});
test("Unicode lookalike is detected without treating all IDNs as fraud", () => {
  assert.ok(
    engine
      .link("https://раypal.com/login")
      .reasons.some((r) => r.id === "brand_impersonation"),
  );
});
const page = (changes: Partial<PageData> = {}): PageData => ({
  url: "https://merchant.test/",
  title: "Merchant sign in",
  pageKey: "nav-1",
  passages: ["Enter your password to sign in"],
  links: [],
  forms: [
    {
      action: "https://merchant.test/session",
      method: "post",
      password: true,
      otp: false,
      payment: false,
      labels: "Password",
    },
  ],
  partial: false,
  inaccessibleFrames: 0,
  ...changes,
});
test("normal same-origin login is not a credential sharing warning", () =>
  assert.equal(engine.page(page()).verdict, "clear"));
test("external SSO form alone is informational", () =>
  assert.equal(
    engine.page(
      page({
        forms: [
          {
            action: "https://login.microsoftonline.com/auth",
            method: "post",
            password: true,
            otp: false,
            payment: false,
            labels: "Sign in",
          },
        ],
      }),
    ).verdict,
    "clear",
  ));
test("brand impersonation with sensitive form raises high risk", () =>
  assert.equal(
    engine.page(page({ title: "PayPal Account sign in" })).verdict,
    "high",
  ));
test("actual DOM link destination mismatch is explained", () =>
  assert.ok(
    engine
      .page(
        page({
          passages: [],
          forms: [],
          links: [
            {
              href: "https://different.test/",
              text: "https://paypal.com",
              context: "Verify your account",
            },
          ],
        }),
      )
      .reasons.some((r) => r.id === "display_mismatch"),
  ));
test("click review preserves a mismatch even when the destination itself is ordinary", () => {
  const r = engine.previewLink(
    "https://different.test/",
    "https://paypal.com",
    "Verify your account",
  );
  assert.ok(r.reasons.some((e) => e.id === "display_mismatch"));
  assert.notEqual(r.verdict, "clear");
});
test("form guard identifies only the sensitive unsafe action", () => {
  const r = engine.page(
    page({
      forms: [
        {
          action: "http://merchant.test/session",
          method: "post",
          password: true,
          otp: false,
          payment: false,
          labels: "Password",
        },
        {
          action: "https://merchant.test/search",
          method: "get",
          password: false,
          otp: false,
          payment: false,
          labels: "Search",
        },
      ],
    }),
  );
  assert.deepEqual(r.unsafeFormActions, ["http://merchant.test/session"]);
});
test("missing page access never produces a clear assessment", () =>
  assert.equal(
    engine.page(page({ url: "chrome://settings/" })).verdict,
    "unknown",
  ));
test("unread private messages are explicitly unassessed", () => {
  const r = engine.page(
    page({
      url: "https://mail.google.com/",
      title: "",
      passages: [],
      links: [],
      forms: [],
      partial: true,
      selectedOnly: true,
    }),
  );
  assert.equal(r.verdict, "unknown");
  assert.ok(r.coverage.includes("private_messages"));
});
test("UPI mismatch and duplicate payees are explicit", () => {
  assert.equal(
    paymentReview("upi://pay?pa=alice@bank&am=10", {
      payee: "bob@bank",
      amount: "10",
    })!.matches,
    false,
  );
  assert.ok(
    paymentReview("upi://pay?pa=alice@bank&pa=bob@bank")!.issues.includes(
      "payment_invalid",
    ),
  );
});
test("rules continue when models fail", () => {
  const r = new SecurityEngine(undefined, (h) => h).scan(
    "Urgently send your password and OTP",
  );
  assert.equal(r.verdict, "high");
  assert.ok(r.coverage.includes("model_unavailable"));
});
test("user-supplied context is labeled and multiple answers alone stay caution", () => {
  const r = contextReview(engine.scan("Your meeting starts at noon"), {
    remoteAccess: true,
    giftCards: true,
    secrecy: true,
  });
  assert.equal(r.verdict, "caution");
  assert.ok(r.reasons.some((e) => e.id === "context_remoteAccess"));
  assert.ok(r.coverage.includes("context_coverage"));
});
