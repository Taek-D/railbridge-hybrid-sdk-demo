---
phase: 03-logging-and-failure-simulation
plan: 02
subsystem: ui
tags: [android, webview, diagnostics, bridge, junit, json]
requires:
  - phase: 03-logging-and-failure-simulation
    provides: deterministic scenario presets, request timelines, structured diagnostics export
provides:
  - bridge-side stage tracing with additive diagnostics metadata on live responses
  - diagnostics-only bridge helpers for preset control, snapshot access, export, and clear
  - in-page WebView diagnostics panel with timeline inspection and raw JSON export
  - plain JUnit coverage for diagnostics response compatibility
affects: [04-android-bridge-hardening, 05-ios-parity-demo, 06-portfolio-documentation-and-validation]
tech-stack:
  added: []
  patterns: [timeline-aware bridge metadata, diagnostics-only bridge helpers, additive webview diagnostics panel]
key-files:
  created: []
  modified:
    - app/src/main/java/com/demo/railbridge/bridge/BridgeResponseFactory.java
    - app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java
    - app/src/main/assets/webview/index.html
    - app/src/test/java/com/demo/railbridge/bridge/NativeBridgeDiagnosticsTest.java
key-decisions:
  - "Keep diagnostics additive by extending bridge metadata fields and adding separate diagnostics helper methods instead of altering the original four bridge actions."
  - "Render timeline inspection and export directly on the existing WebView page so incomplete callback-loss flows stay visible in the same operator workflow."
patterns-established:
  - "Bridge responses keep base status and payload fields stable while optional evidence fields carry scenario, vendor code, retryability, and duration."
  - "Diagnostics inspection is correlation-first: the page reads grouped timelines and selects raw detail by request rather than replaying flat logs."
requirements-completed: [LOG-01, LOG-02, UI-01]
duration: 14min
completed: 2026-04-06
---

# Phase 03 Plan 02: Logging and Failure Simulation Summary

**Live bridge diagnostics with stage-aware metadata, preset control helpers, and an in-page WebView timeline inspector for reproducible SDK failures**

## Performance

- **Duration:** 14 min
- **Started:** 2026-04-06T23:36:34+09:00
- **Completed:** 2026-04-06T23:50:38+09:00
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Added live bridge tracing for `js_entry`, `native_validation`, `sdk_start`, `sdk_callback`, and `js_callback` while preserving the original four `@JavascriptInterface` business actions.
- Extended response metadata additively so success and error payloads can surface `scenario`, `vendorCode`, `retryable`, `resolvedByRetry`, and `durationMs` without breaking the existing contract.
- Upgraded the existing WebView page with preset controls, correlation-grouped timeline inspection, incomplete-flow visibility, and JSON export on the same screen.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add bridge-stage tracing and diagnostics helper methods** - `90c702f` (`test`), `79b68cf` (`feat`)
2. **Task 2: Extend the existing WebView page with diagnostics UI and manually confirm runtime flow** - `c80a41f` (`feat`)

_Note: Task 1 used TDD, so it produced separate failing-test and implementation commits._

## Files Created/Modified
- `app/src/main/java/com/demo/railbridge/bridge/BridgeResponseFactory.java` - expands optional bridge metadata and supports post-build metadata attachment to retry-wrapped responses.
- `app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java` - records timeline stages, exposes diagnostics helpers, and builds correlation-aware diagnostics payloads for the page.
- `app/src/main/assets/webview/index.html` - preserves the four-button demo flow while adding preset selection, timeline browsing, raw detail viewing, export, and clear controls.
- `app/src/test/java/com/demo/railbridge/bridge/NativeBridgeDiagnosticsTest.java` - verifies additive diagnostics metadata and incomplete timeline payload compatibility in plain JUnit.

## Decisions Made
- Kept scenario control behind diagnostics-only bridge helpers so the original action methods remain unchanged for both runtime compatibility and later iOS parity work.
- Made the page consume diagnostics snapshots grouped by `correlationId` so callback-loss and retry-recovery cases can be inspected without depending on final JS callback delivery.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- `rg.exe` was not usable in this workspace, so repository searches fell back to PowerShell reads and targeted file loading.
- Replacing `index.html` in one patch hit a Windows command-length limit, so the page rewrite was applied in a smaller follow-up patch without changing scope.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 4 can now harden duplicate callbacks, teardown behavior, and late responses against a live diagnostics surface that already exposes incomplete and recovered flows.
- Phase 6 can reuse the exported JSON timelines and verified runtime scenarios as debugging-report evidence.

## Manual Verification

- Attached to the emulator WebView DOM through the WebView DevTools socket on `emulator-5554`.
- Confirmed the original four buttons remained present: `Request charge`, `Get balance`, `SDK status`, and `Report JS error`.
- Confirmed the diagnostics presets are available: `normal`, `timeout`, `internal_error`, `callback_loss`, `duplicate_callback`, and `retry_exhausted`.
- Verified timeout timelines finish with `finalStatus=success`, `resolvedByRetry=true`, `finalRetryCount=3`, and repeated `sdk_start`/`sdk_callback` stages before the final `js_callback`.
- Verified callback-loss timelines remain `incomplete` with `js_entry -> native_validation -> sdk_start` and no `js_callback`.
- Verified exported JSON uses `schemaVersion=1` and groups request evidence by `correlationId`.

## Self-Check

PASSED

---
*Phase: 03-logging-and-failure-simulation*
*Completed: 2026-04-06*
