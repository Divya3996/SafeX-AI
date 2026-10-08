# Fix Critical Bugs & Architectural Inconsistencies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve all identified critical bugs and architectural flaws across Sentinel AI's detection pipelines, UI rendering, event bus dispatch, background lifecycle, and reputation caching.

**Architecture:** Update `IntentScanRepository` to recompute risk levels, decisions, and UI models from ML-blended scores; reorder `ThreatEventBus` emission so Room and event subscribers persist post-ML results; fix link fallback forwarding in `IntentRouterActivity`; render informative indicator badges in `ThreatDetailsScreen`; eliminate cold-start blocking in `ThreatJournal`; harden contact matching; and introduce TTL caching for the OpenPhish reputation feed.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose Material 3, Android SDK 34, TensorFlow Lite, Coroutines/StateFlow, Hilt, Room.

---

## Global Constraints
- Target SDK 34, Minimum SDK 26.
- Maintain existing public interfaces and DI bindings wherever possible.
- Ensure all existing unit tests in `:app`, `:core`, `:agents`, and `:ui` continue to compile and pass.
- Preserve offline-first design: zero mandatory cloud dependencies.

## Review Focus
- **Decision Downgrade Prevention:** An ML blend with a lower score must never downgrade a `BLOCK` decision from a malicious reputation provider.
- **Link Forwarding Fallback:** Turning off click protection must never swallow a link; it must open in Chrome or the system browser chooser.
- **Indicator Badge Readability:** Threat details screen indicator chips must display the actual string description, not just a bare icon.
- **Background Cold-Start Safety:** Database restoration on app startup must never execute blocking I/O on the Android main thread.
- **Reputation Latency:** Repeated URL scans must read the OpenPhish feed from memory cache rather than re-downloading over HTTP each time.

---

### Task 1: Recalculate Risk Decision on ML Blending & Reorder Event Bus Emission

**Files:**
- Modify: `d:/hethon/Sentinel-AI/android-app/app/src/main/java/com/sentinel/ai/protection/intent/IntentScanRepository.kt`
- Modify: `d:/hethon/Sentinel-AI/android-app/app/src/main/java/com/sentinel/ai/protection/intent/IntentThreatAnalyzerImpl.kt`
- Test: `d:/hethon/Sentinel-AI/android-app/app/src/test/java/com/sentinel/ai/protection/intent/FunctionalBugRegressionTest.kt`

**Interfaces:**
- `IntentScanRepository`: receives `threatEventBus: ThreatEventBus`, updates `baseResult` with recalculated `riskLevel`, `decision`, `headline`, `recommendedAction`, and emits `ThreatEvent.LinkThreatDetected(mlResult)`.
- `IntentThreatAnalyzerImpl`: removes redundant pre-ML `threatEventBus.emit` for `UrlPayload`.

- [ ] **Step 1: Write test verifying ML score upgrades decision and emits to bus**
  In `FunctionalBugRegressionTest.kt`, add a test verifying that when an ML score elevates a previously `ALLOW` result to >= 30, the returned `ScanResult` has `decision == ProtectionDecision.WARN` and `recommendedAction == ProtectionAction.PROCEED_WITH_CAUTION`.

- [ ] **Step 2: Update `IntentThreatAnalyzerImpl.kt` to omit premature link threat emission**
  Remove `threatEventBus.emit(ThreatEvent.LinkThreatDetected(finalResult))` from `IntentThreatAnalyzerImpl.analyze(UrlPayload)` so it does not emit before the ML step.

- [ ] **Step 3: Update `IntentScanRepository.kt` to recalculate decision and emit final result**
  Inject `ThreatEventBus` into `IntentScanRepository`. In `applyMlScore`, map `combinedScore.toRiskLevel()`, recalculate `ProtectionDecision`, `defaultHeadline()`, and `defaultAction()`, preventing any downgrade if `baseResult.decision == BLOCK`. Emit `ThreatEvent.LinkThreatDetected(mlResult)` after ML inference in `scanLink()`.

- [ ] **Step 4: Run tests to verify they pass**
  Run `./gradlew.bat :app:testDebugUnitTest --tests "com.sentinel.ai.protection.intent.FunctionalBugRegressionTest"` and `./gradlew.bat :app:testDebugUnitTest --tests "com.sentinel.ai.protection.intent.IntentThreatAnalyzerImplTest"`.

---

### Task 2: Fix Fallback Link Forwarding when Click Protection is Disabled

**Files:**
- Modify: `d:/hethon/Sentinel-AI/android-app/app/src/main/java/com/sentinel/ai/protection/intent/IntentRouterActivity.kt`
- Test: `d:/hethon/Sentinel-AI/android-app/app/src/test/java/com/sentinel/ai/protection/intent/FunctionalBugRegressionTest.kt`

- [ ] **Step 1: Update `IntentRouterActivity.kt`**
  When `!FeatureManager.isClickEnabled()`, extract the payload. If it is a `UrlPayload`, call `BrowserLauncher().launch(this, payload.url)` before calling `finish()`.

- [ ] **Step 2: Add unit test in `FunctionalBugRegressionTest.kt`**
  Add a test verifying the fallback behavior logic.

---

### Task 3: Fix Threat Indicator Chips in `ThreatDetailsScreen.kt`

**Files:**
- Modify: `d:/hethon/Sentinel-AI/android-app/ui/src/main/java/com/sentinel/ai/ui/screens/threat/ThreatDetailsScreen.kt`

- [ ] **Step 1: Create `ThreatIndicatorBadge` composable in `ThreatDetailsScreen.kt`**
  Implement an indicator chip showing a colored dot with `riskColor(threat.riskLevel)` and a `Text(text = indicator, style = MaterialTheme.typography.labelLarge)`.

- [ ] **Step 2: Update `FlowRow` in `ThreatDetailsScreen.kt`**
  Replace the bare `ThreatLevelChip` with `ThreatIndicatorBadge(text = indicator, riskLevel = selectedThreat.riskLevel)`.

---

### Task 4: Asynchronous ThreatJournal Initialization & Safe Service Starting

**Files:**
- Modify: `d:/hethon/Sentinel-AI/android-app/core/src/main/java/com/sentinel/ai/core/event/ThreatJournal.kt`
- Modify: `d:/hethon/Sentinel-AI/android-app/app/src/main/java/com/sentinel/ai/SentinelApp.kt`
- Modify: `d:/hethon/Sentinel-AI/android-app/ui/src/main/java/com/sentinel/ai/ui/protection/ProtectionControl.kt`

- [ ] **Step 1: Make `ThreatJournal.initialize` asynchronous**
  Remove `runBlocking` from `restoreState()`. Launch `restoreState()` inside `persistenceScope.launch { ... }` so app startup is completely non-blocking.

- [ ] **Step 2: Guard `startService` calls**
  In `SentinelApp.onCreate()` and `ProtectionControl.startService()`, wrap `startService()` calls with `runCatching` to prevent background `IllegalStateException` crashes on Android 8.0+.

---

### Task 5: Improve Contact Name Matching in `SentinelNotificationListener.kt`

**Files:**
- Modify: `d:/hethon/Sentinel-AI/android-app/app/src/main/java/com/sentinel/ai/listeners/SentinelNotificationListener.kt`
- Test: `d:/hethon/Sentinel-AI/android-app/app/src/test/java/com/sentinel/ai/listeners/SentinelNotificationListenerTest.kt`

- [ ] **Step 1: Write test for contact name matching**
  Add tests asserting that a contact name "Al" does not match scammer sender "Bank Alert", while exact names and legitimate full name tokens match.

- [ ] **Step 2: Implement token-based name matching in `SentinelNotificationListener.kt`**
  Replace loose `(na.contains(nb) || nb.contains(na))` with token-level matching requiring exact word equality for tokens.

---

### Task 6: Add In-Memory TTL Cache for OpenPhish Reputation Feed

**Files:**
- Modify: `d:/hethon/Sentinel-AI/android-app/app/src/main/java/com/sentinel/ai/protection/intent/reputation/OpenPhishReputationProvider.kt`
- Test: `d:/hethon/Sentinel-AI/android-app/app/src/test/java/com/sentinel/ai/protection/intent/reputation/OpenPhishReputationProviderTest.kt`

- [ ] **Step 1: Add feed cache fields and TTL logic in `OpenPhishReputationProvider.kt`**
  Add `cachedFeedBody`, `cachedTimestamp`, and `CACHE_TTL_MS = 6 * 3600 * 1000L`. If the cached feed is valid, parse against it without making an HTTP network call.

- [ ] **Step 2: Verify with tests and build**
  Run `./gradlew.bat :app:testDebugUnitTest --tests "com.sentinel.ai.protection.intent.reputation.OpenPhishReputationProviderTest"` and compile the full debug APK.
