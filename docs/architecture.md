# Architecture

`app` owns Android input routing, ML, OCR / QR, link and file checks and notification warnings. `agents` parses and deduplicates supported notifications. `core` defines the shared scan contract, scam signals, privacy preferences, event bus and Room history. `ui` renders Compose screens and permission / protection state. `services` schedules daily local history retention. `demo-sender` provides separate synthetic Android notifications.

Every active entry point calls `ScanRepository`. Manual text and notification analysis share message rules, the local text model and up to eight embedded URL analyses. Link analysis uses structural rules, the native model and a local reputation snapshot. Images decode locally before using text analysis. File analysis is bounded and never executes content.

For saved scans, `ThreatEventBus.emit` awaits the Room write before notifying subscribers; history no longer depends on a transient subscriber. Room stores the full structured result as well as searchable columns. Database version 2 migrates existing version 1 rows with an optional result JSON column; legacy rows use conservative default fields. Writes and cleanup are serialized, and rows are bounded to 1,000.

Scam Story Mode calls the repository's private text/link/QR methods and keeps its application-owned draft in RAM. Its bounded sequence engine in `core/story` attaches findings to original reviewed spans and recomputes after corrections, removal or reordered evidence. The `app/story` image bridge reuses the existing bounded extraction engines, serializes imports before allocating pixels and retains native pixel leases until extraction finishes. Generation guards reject late results after cancellation, lock or expiry. The secure Compose destination in `ui/screens/story` receives explicit handoffs through the controller; navigation intents carry a request flag.

Explicit case saves use AES-256-GCM snapshots in the internal no-backup directory, with an Android Keystore key and authenticated case identity. The case vault has separate save/open/delete controls, a 20-case cap and retention pruning. Redacted sharing is generated from trusted explanation templates, previewed, then handed to Android's chooser. [Story behavior and privacy bounds](scam-story-mode.md).

The application-scoped event observer posts warnings for suspicious notification results. There is no continuously running placeholder foreground service. Android owns the notification-listener lifecycle; UI shows listening, reconnecting, ready for manual scans, or paused based on actual permission / connection state.

No active scanner network provider is injected. The user can import an HTTP(S) URL list in Settings for offline matching. Absence of a match is reported as unknown, not safe.
