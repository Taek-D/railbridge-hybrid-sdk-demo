---
phase: 03-logging-and-failure-simulation
plan: 01
subsystem: logging
tags: [android, webview, diagnostics, junit, sdk, json]
requires:
  - phase: 02-android-adapter-seam
    provides: adapter seam, bridge metadata path, retry ownership
provides:
  - deterministic scenario preset control behind the SDK adapter seam
  - bounded request timelines keyed by correlationId
  - structured diagnostics snapshot and JSON export APIs with legacy log migration
  - plain JUnit coverage for scenario behavior and timeline/export contracts
affects: [03-02, 04-android-bridge-hardening, 05-ios-parity-demo]
tech-stack:
  added: []
  patterns: [scenario-controller seam, timeline-first diagnostics persistence, legacy log migration]
key-files:
  created:
    - app/src/main/java/com/demo/railbridge/sdk/ScenarioPreset.java
    - app/src/main/java/com/demo/railbridge/sdk/ScenarioController.java
    - app/src/main/java/com/demo/railbridge/sdk/ScenarioOutcome.java
    - app/src/main/java/com/demo/railbridge/logging/TimelineEvent.java
    - app/src/main/java/com/demo/railbridge/logging/RequestTimeline.java
    - app/src/main/java/com/demo/railbridge/logging/DiagnosticsSnapshot.java
    - app/src/test/java/com/demo/railbridge/sdk/MockRailSdkScenarioTest.java
    - app/src/test/java/com/demo/railbridge/logging/RequestTimelineRepositoryTest.java
    - app/src/test/java/com/demo/railbridge/logging/DiagnosticsExportTest.java
  modified:
    - app/src/main/java/com/demo/railbridge/sdk/MockRailSdkAdapter.java
    - app/src/main/java/com/demo/railbridge/sdk/SdkErrorCode.java
    - app/src/main/java/com/demo/railbridge/logging/ErrorLogger.java
    - app/src/main/java/com/demo/railbridge/logging/LogEvent.java
key-decisions:
  - "Keep deterministic failure control behind a standalone ScenarioController so the bridge can consume it later without coupling to MockRailSdk."
  - "Move ErrorLogger to a request-timeline store while preserving its existing public log entrypoints and migrating older flat persisted data on read."
  - "Add ERR_VENDOR_INTERNAL so the internal-error scenario stays bridge-visible and non-retryable instead of collapsing into retry exhaustion."
patterns-established:
  - "Scenario presets map to adapter outcomes, not bridge rewrites."
  - "Diagnostics persistence is timeline-first, with legacy flat logs converted into legacy_log timeline events."
requirements-completed: [SDK-03, LOG-01, LOG-02]
duration: 11min
completed: 2026-04-06
---

# Phase 03 Plan 01: Deterministic Diagnostics Backend Summary

**Deterministic SDK scenario presets with correlation-based request timelines and structured diagnostics export behind the existing Android bridge seam**

## Performance

- **Duration:** 11 min
- **Started:** 2026-04-06T14:15:46Z
- **Completed:** 2026-04-06T14:26:45Z
- **Tasks:** 2
- **Files modified:** 13

## Accomplishments
- Added deterministic preset control for normal, timeout, internal error, callback loss, duplicate callback, and retry-exhausted setup without changing the adapter interface.
- Replaced flat error-log persistence with bounded request timelines keyed by `correlationId`, including incomplete request visibility and structured JSON export.
- Added plain JUnit coverage for scenario behavior, timeline grouping and retention, legacy flat-log migration, and export schema fields.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add deterministic scenario types and mock adapter behavior** - `a95813d` (`test`), `395fd6c` (`feat`)
2. **Task 2: Replace flat logs with request timelines and structured export** - `8091d5a` (`test`), `895076b` (`feat`)

_Note: TDD tasks produced separate failing-test and implementation commits._

## Files Created/Modified
- `app/src/main/java/com/demo/railbridge/sdk/ScenarioPreset.java` - closed preset list for deterministic failure simulation.
- `app/src/main/java/com/demo/railbridge/sdk/ScenarioController.java` - active preset holder that future bridge/UI code can read and update without SDK coupling.
- `app/src/main/java/com/demo/railbridge/sdk/ScenarioOutcome.java` - adapter-facing scenario metadata for retryability, vendor evidence, callback loss, and duplicate callback behavior.
- `app/src/main/java/com/demo/railbridge/sdk/MockRailSdkAdapter.java` - deterministic preset-driven adapter behavior layered on the existing SDK seam.
- `app/src/main/java/com/demo/railbridge/sdk/SdkErrorCode.java` - dedicated non-retryable vendor internal error code for scenario correctness.
- `app/src/main/java/com/demo/railbridge/logging/TimelineEvent.java` - per-stage event model with retry, duration, vendor, and completion metadata.
- `app/src/main/java/com/demo/railbridge/logging/RequestTimeline.java` - correlation-scoped request aggregate with ordered events and final status fields.
- `app/src/main/java/com/demo/railbridge/logging/DiagnosticsSnapshot.java` - stable JSON export/snapshot envelope for downstream bridge and UI work.
- `app/src/main/java/com/demo/railbridge/logging/ErrorLogger.java` - timeline-backed persistence, legacy migration, export APIs, and test seams for storage/time/platform logging.
- `app/src/main/java/com/demo/railbridge/logging/LogEvent.java` - legacy compatibility getters plus timeline-to-log conversion.
- `app/src/test/java/com/demo/railbridge/sdk/MockRailSdkScenarioTest.java` - deterministic preset and retry semantics coverage.
- `app/src/test/java/com/demo/railbridge/logging/RequestTimelineRepositoryTest.java` - grouping, incomplete visibility, retention, and legacy migration coverage.
- `app/src/test/java/com/demo/railbridge/logging/DiagnosticsExportTest.java` - structured export schema and legacy export parseability coverage.

## Decisions Made
- Used a standalone `ScenarioController` rather than embedding preset state directly into `MockRailSdk`, so 03-02 can surface scenario controls without another transport rewrite.
- Kept `ErrorLogger` as the persistence boundary and added timeline/export methods there instead of creating a parallel diagnostics service.
- Preserved older stored flat logs by migrating them into `legacy_log` timeline events on read instead of failing or silently dropping them.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added a non-retryable internal error code for scenario correctness**
- **Found during:** Task 1 (Add deterministic scenario types and mock adapter behavior)
- **Issue:** `RetryHandler` already treats `ERR_SDK_INTERNAL` as retryable, so using it for the new `internal error` preset would incorrectly collapse that preset into retry exhaustion.
- **Fix:** Added `ERR_VENDOR_INTERNAL` and mapped the scenario outcome/adapter path to that dedicated non-retryable code while preserving vendor metadata.
- **Files modified:** `app/src/main/java/com/demo/railbridge/sdk/SdkErrorCode.java`, `app/src/main/java/com/demo/railbridge/sdk/ScenarioOutcome.java`, `app/src/main/java/com/demo/railbridge/sdk/MockRailSdkAdapter.java`, `app/src/test/java/com/demo/railbridge/sdk/MockRailSdkScenarioTest.java`
- **Verification:** `MockRailSdkScenarioTest`, `RequestTimelineRepositoryTest`, and `DiagnosticsExportTest` all pass in the ASCII-path copy.
- **Committed in:** `395fd6c`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** The deviation was required to preserve the plan’s internal-error vs retry-exhausted distinction. No scope creep beyond correctness.

## Issues Encountered
- `rg.exe` was not usable in this workspace, so repository search fell back to PowerShell file enumeration and `Select-String`.
- A transient Git commit race created an `index.lock` symptom during Task 2 bookkeeping; rerunning the commit serially resolved it without affecting repo state.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- 03-02 can now wire the existing bridge/page flow to `ScenarioController`, `ErrorLogger.recordEvent(...)`, `getDiagnosticsSnapshot()`, and `exportDiagnosticsJson()` without redefining the backend models.
- Phase 4 can harden duplicate callbacks, incomplete requests, and teardown-safe flows against the new correlation-based timeline substrate.

## Self-Check

PASSED

---
*Phase: 03-logging-and-failure-simulation*
*Completed: 2026-04-06*
