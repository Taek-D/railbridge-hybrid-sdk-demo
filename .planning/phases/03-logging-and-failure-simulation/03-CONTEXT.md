# Phase 3: Logging and Failure Simulation - Context

**Gathered:** 2026-04-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Add deterministic failure controls and structured diagnostics to the current Android WebView demo without removing the existing four bridge actions. This phase covers reproducible mock SDK scenarios, correlation-based request tracing, structured log export, and an expanded diagnostics UI on the existing page. Async hardening beyond logging and scenario simulation remains a later phase.

</domain>

<decisions>
## Implementation Decisions

### Scenario control model
- **D-01:** Failure simulation uses a preset selector model rather than free-form toggles or scripts.
- **D-02:** The preset list should explicitly cover the support-relevant cases from the milestone requirements: `normal`, `timeout`, `internal error`, `callback loss`, `duplicate callback`, and `retry exhausted`.
- **D-03:** Scenario selection should be visible on the current WebView page as a dedicated diagnostics panel rather than a separate screen.
- **D-04:** The selected scenario acts as the active mock-SDK mode for subsequent SDK-backed requests until the user changes it.

### Structured tracing and evidence
- **D-05:** Logging is request-scoped around a shared `correlationId`, not just a flat event list.
- **D-06:** Each request timeline should capture the major stages needed for troubleshooting: `js_entry`, `native_validation`, `sdk_start`, `sdk_callback`, and `js_callback`.
- **D-07:** Structured evidence must include `stage`, `retryCount`, `durationMs`, `scenario`, `vendorCode`, `retryable`, and `resolvedByRetry` when applicable, because the portfolio goal is to demonstrate root-cause evidence for external SDK issues.
- **D-08:** Additive bridge metadata should continue to surface at runtime without replacing the baseline fields already used by the current HTML handler.

### Export and inspection workflow
- **D-09:** Logs should be exportable as structured JSON organized by request timeline, not only shown as human-readable on-screen entries.
- **D-10:** The export format should preserve enough detail to support a debugging report or incident handoff without requiring logcat replay.
- **D-11:** The current WebView page should gain a richer inspection surface, such as raw payload/detail viewing alongside the existing visible log list.

### UI delivery strategy
- **D-12:** Diagnostics remain on the existing WebView demo page instead of moving to a separate diagnostics route or native-only screen.
- **D-13:** The current four main action buttons remain in place; the new scenario and log tooling should feel like an overlayed troubleshooting layer, not a replacement flow.

### the agent's Discretion
- Exact naming and presentation of the scenario presets
- Whether export is implemented as direct file save, JSON text handoff, or both, as long as structured JSON evidence is preserved
- Exact layout of the detail viewer and how raw payloads are presented beside the current result log
- Internal event model shape beyond the required evidence fields, as long as `correlationId` remains the request anchor

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone requirements
- `.planning/PROJECT.md` - Portfolio goal, compatibility strategy, and current repo constraints
- `.planning/REQUIREMENTS.md` - In-scope requirements for `SDK-03`, `LOG-01`, `LOG-02`, and `UI-01`
- `.planning/ROADMAP.md` - Phase 3 goal, success criteria, and dependency chain
- `.planning/STATE.md` - Current milestone position after Phase 2 execution

### Prior phase decisions
- `.planning/phases/02-android-adapter-seam/02-CONTEXT.md` - Locked adapter seam, response compatibility rules, and phase-boundary decisions carried into Phase 3
- `.planning/phases/02-android-adapter-seam/02-01-SUMMARY.md` - Summary of the adapter seam artifacts already in place
- `.planning/phases/02-android-adapter-seam/02-02-SUMMARY.md` - Summary of the runtime bridge migration and verification baseline

### Android diagnostics baseline
- `app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java` - Request entrypoints, additive metadata behavior, and current request context usage
- `app/src/main/java/com/demo/railbridge/bridge/BridgeResponseFactory.java` - Current runtime payload construction and additive metadata path
- `app/src/main/java/com/demo/railbridge/sdk/MockRailSdkAdapter.java` - Current seam where deterministic scenario behavior can be introduced
- `app/src/main/java/com/demo/railbridge/retry/RetryHandler.java` - Existing retry orchestration and retry count semantics
- `app/src/main/java/com/demo/railbridge/logging/ErrorLogger.java` - Existing local log persistence and logging entrypoints
- `app/src/main/java/com/demo/railbridge/logging/LogEvent.java` - Current persisted log structure
- `app/src/main/assets/webview/index.html` - Current diagnostics page and UI surface that must be extended rather than replaced

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MockRailSdkAdapter` already forms the seam where deterministic scenario injection can happen without rewiring the bridge again.
- `NativeBridge.RequestContext` already generates a `correlationId` and captures elapsed time, which is the right anchor for Phase 3 request timelines.
- `BridgeResponseFactory` already centralizes additive response metadata and is the natural place to keep response compatibility stable while exposing richer diagnostics.
- `ErrorLogger` and `LogEvent` already provide local persistence and can be expanded into structured timeline storage/export instead of starting over.
- `index.html` already has a log container and baseline operator flow, so Phase 3 can extend it with scenario controls and raw detail views in place.

### Established Patterns
- The Android demo keeps its diagnostics on a single WebView page, so adding a panel beside the current flow is more consistent than creating a separate screen.
- The bridge contract already treats new fields as additive, so richer diagnostics should follow the same pattern.
- Retry remains bridge-owned, which means Phase 3 logging must observe retry attempts without moving retry policy into the adapter.

### Integration Points
- Scenario selection should connect to the adapter seam, not the old `MockRailSdk` directly.
- Structured timeline capture should connect at both bridge and logger layers so stage transitions and final outcomes share one request identifier.
- Export and raw detail inspection should connect to the current WebView diagnostics page rather than bypassing it.

</code_context>

<specifics>
## Specific Ideas

- The portfolio should read like a real hybrid-app troubleshooting tool, not a random failure simulator, so presets and logs should map to realistic support cases.
- The diagnostics experience should help prove external-SDK instability with evidence that could be pasted into a report.
- Keeping everything on the existing WebView page supports the “minimal code change, maximum stability” story from the target job post.

</specifics>

<deferred>
## Deferred Ideas

- In-flight request ownership, duplicate callback suppression, timeout guardians, and teardown-safe late callback handling belong to Phase 4
- iOS parity for the same scenario model belongs to Phase 5
- Real Crashlytics/backend event ingestion remains future work once the demo-level evidence model is stable

</deferred>

---

*Phase: 03-logging-and-failure-simulation*
*Context gathered: 2026-04-06*
