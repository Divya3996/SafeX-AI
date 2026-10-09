export type Language = "en" | "hi" | "gu";
export type Verdict = "clear" | "caution" | "high" | "unknown";
export type Source = "text" | "link" | "page" | "selection" | "image" | "qr";
export interface Evidence {
  id: string;
  weight: number;
  detail?: string;
}
export type ContextFlags = Partial<
  Record<
    | "remoteAccess"
    | "giftCards"
    | "walletConnection"
    | "advanceFee"
    | "secrecy",
    boolean
  >
>;
export interface LinkFinding {
  url: string;
  host: string;
  domain: string;
  verdict: Verdict;
  reasons: Evidence[];
  inferred: boolean;
}
export interface PaymentReview {
  payee: string;
  name: string;
  amount: string;
  currency: string;
  issues: string[];
  matches?: boolean;
}
export interface ScanResult {
  id: string;
  source: Source;
  verdict: Verdict;
  reasons: Evidence[];
  coverage: string[];
  links: LinkFinding[];
  payments: PaymentReview[];
  checkedAt: number;
  durationMs: number;
  modelVersion: string;
  partial: boolean;
  tabId?: number;
  pageKey?: string;
  domain?: string;
  counts?: { links: number; forms: number; passages: number };
  synthetic?: boolean;
  unsafeFormActions?: string[];
}
export interface AnchorData {
  href: string;
  text: string;
  context: string;
}
export interface FormData {
  action: string;
  method: string;
  password: boolean;
  otp: boolean;
  payment: boolean;
  labels: string;
}
export interface PageData {
  url: string;
  title: string;
  passages: string[];
  links: AnchorData[];
  forms: FormData[];
  partial: boolean;
  inaccessibleFrames: number;
  pageKey: string;
  selectedOnly?: boolean;
}
export interface ThreatEntry {
  url: string;
  label: "reported" | "block";
  addedAt: number;
}
export interface Reputation {
  entries: ThreatEntry[];
  importedAt: number;
  name: string;
  expiresAt: number;
}
export interface TextModel {
  version: string;
  feature_version: string;
  threshold: number;
  vocabulary: string[];
  weights: number[];
  intercept: number;
}
export interface UrlModel {
  version: string;
  threshold: number;
  mean: number[];
  scale: number[];
  clip: number;
  parameters: Array<number[][] | number[]>;
}
export interface Models {
  text: TextModel;
  url: UrlModel;
  contextFreeThreshold: number;
}
export interface Preferences {
  language: Language;
  fontSize: number;
  theme: "system" | "light" | "dark";
  sound: boolean;
  shield: boolean;
  retentionDays: 7 | 30 | 90;
  protectedOrigins: string[];
  pausedOrigins: string[];
  blocking: boolean;
}
export const DEFAULT_PREFERENCES: Preferences = {
  language: "en",
  fontSize: 100,
  theme: "system",
  sound: false,
  shield: true,
  retentionDays: 30,
  protectedOrigins: [],
  pausedOrigins: [],
  blocking: false,
};
export const LIMITS = {
  text: 12000,
  tokens: 512,
  links: 100,
  forms: 20,
  passages: 12,
  reports: 200,
  imports: 2000,
  imageBytes: 10 * 1024 * 1024,
  imagePixels: 16_000_000,
};
