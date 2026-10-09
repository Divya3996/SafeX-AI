import test from "node:test";
import assert from "node:assert/strict";
import {
  importReputation,
  redactUrl,
  savedReport,
} from "../../shared/security-engine/src/privacy";
import type { ScanResult } from "../../shared/security-engine/src/contracts";
test("saved/exported links remove credentials, query tokens and fragments", () =>
  assert.equal(
    redactUrl("https://alice:password@site.test/login?token=secret#code"),
    "https://site.test/login",
  ));
test("reputation imports are exact, bounded and expire", () => {
  const r = importReputation(
    "https://bad.test/\nhttps://bad.test/",
    "list.txt",
    100,
  );
  assert.equal(r.entries.length, 1);
  assert.equal(r.expiresAt, 100 + 7 * 86400000);
  assert.equal(r.entries[0].label, "reported");
});
test("blocking rejects executable and sensitive payloads", () => {
  assert.throws(() =>
    importReputation('[{"url":"javascript:alert(1)","label":"block"}]', "x"),
  );
  assert.throws(() =>
    importReputation(
      '[{"url":"https://a.test/?token=private","label":"block"}]',
      "x",
    ),
  );
  assert.throws(() => importReputation("https://a.test/\n".repeat(2001), "x"));
});
test("saving strips tab/navigation identity and payment identity", () => {
  const r = {
    id: "a",
    source: "qr",
    verdict: "caution",
    reasons: [],
    coverage: [],
    links: [],
    payments: [
      {
        payee: "alice@bank",
        name: "Alice",
        amount: "10",
        currency: "INR",
        issues: [],
      },
    ],
    checkedAt: 1,
    durationMs: 1,
    modelVersion: "x",
    partial: false,
    tabId: 1,
    pageKey: "private",
  } as ScanResult;
  const saved = savedReport(r);
  assert.equal(saved.tabId, undefined);
  assert.equal(saved.pageKey, undefined);
  assert.equal(saved.payments[0].payee, "[redacted]");
  assert.equal(saved.payments[0].name, "");
});
