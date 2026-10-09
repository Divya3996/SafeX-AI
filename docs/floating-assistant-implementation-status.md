# Floating assistant enhancement status

This document tracks the 17 findings from [the enhancement plan](floating-assistant-enhancement-plan.md). Engineering changes shipped in SafeX AI 1.6.0 and passed the recorded checks. External data, participants and physical-phone checks remain separate work.

| Finding | Implementation | Verification |
| --- | --- | --- |
| F01 | Stepwise Back, retained result input, Edit and rescan | Native Back/Edit state regressions passed |
| F02 | Explicit All/Selected/Edited modes, empty selection stays empty, retained edited draft | Native empty-selection and edited-draft retention passed |
| F03 | Independent OCR/QR outcomes preserve successful components | Core independent-failure aggregation and installed OCR passed |
| F04 | Engine coverage, native Gujarati quality, limited assessment | Coverage policy, native low-quality disclosure and QR scope checks passed |
| F05 | Shared bounded bare/defanged/Unicode/invisible/wrapped candidates and confirmation | Core candidate parsing and installed normalization fixtures passed |
| F06 | Full initial crop, original image retention, recrop, enlarged preview and pan | Crop geometry, captured-image review and original/recrop recovery passed |
| F07 | Application-owned RAM session, generation guards, expiry/lock cleanup and bitmap leases | Native recreation, stale jobs, lock cleanup, leases and pixel-release/draft regressions passed |
| F08 | Canonical vector logo in external shield, menu and header | Fresh native menu/header screenshots reviewed |
| F09 | Compact setup, expandable privacy, visible Enable | Three-language 150% setup/paste screenshots and workflows passed |
| F10 | Fixed primary actions and keyboard-aware safe insets | Actual large-font tap-to-result and visible Save actions passed |
| F11 | Primary routes, More controls, outside/Back dismissal, touch slop/cancel and insets | Outside/Back dismissal without source action passed; physical TalkBack remains unverified |
| F12 | Typed recoverable problems, busy Cancel, QR payload classification and real channel status | Capture/import recovery and typed QR caution passed; physical audio remains unverified |
| F13 | Nullable persisted origin/scope/edited/crop metadata and preserved detector coverage | New/old history JSON and native origin/edit provenance passed |
| F14 | Functional corpus, installed-app evaluation and authorized-data preparation tools | 55 authored contracts passed; independent representative dataset and retraining remain external work |
| F15 | Emulator capture/lifecycle matrix and S24/A36 rehearsal guide | API 34 full-display/landscape/denial checks passed; individual-app chooser and physical S24/A36, One UI, TalkBack and speaker remain unverified |
| F16 | Measured private analysis and extraction timings; canonical localization counts | Measured phase fields, 55-case analysis timings and 787 matched translation keys recorded |
| F17 | Distinct Resume/new-input actions and deliberate replacement; picker denial retains input | Native new-input cancellation, previous-review retention and notice identity passed |

[Installable debug APK](../releases/SafeX-AI-1.6.0-debug.apk) · [Verification record](test-results/floating-assistant-summary.json) · [Authored evaluation](test-results/floating-review-summary.json). Final checks: 518 active unit tests, 51 offline native tests, zero lint errors/54 warnings. No 100% detection or OEM certification is claimed.
