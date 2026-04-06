# Phase 2: Android Adapter Seam - Context

**Gathered:** 2026-04-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Decouple the Android WebView bridge from the concrete mock SDK without breaking the existing Android demo flow. This phase only introduces the adapter seam and additive bridge metadata. Deterministic failure controls, richer diagnostics UI, and deeper async hardening belong to later phases.

</domain>

<decisions>
## Implementation Decisions

### Adapter boundary
- **D-01:** Introduce an Android `RailPlusSdkAdapter` interface with only the operations the bridge already needs: `initialize`, `requestCharge`, `getBalance`, `getStatus`, and `shutdown`.
- **D-02:** Keep `MockRailSdk` in the codebase and place a `MockRailSdkAdapter` wrapper in front of it instead of rewriting the SDK class itself.
- **D-03:** Treat the adapter as the future vendor seam. Vendor-specific mapping, timeout policy, and scenario behavior can move behind this seam in later phases.

### Bridge ownership
- **D-04:** `WebViewActivity` remains responsible for constructing the concrete adapter and performing SDK initialization during screen setup.
- **D-05:** `NativeBridge` stops depending on `MockRailSdk` directly and only coordinates parameter parsing, retry execution, adapter invocation, and JS callback response construction.
- **D-06:** `RetryHandler` stays in the bridge layer for this phase. Retry ownership is not moved into the adapter yet.

### Contract compatibility
- **D-07:** Android public bridge method names remain `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`.
- **D-08:** Existing response fields `status`, `method`, `data`, `error`, and `retryCount` remain intact so the current WebView result handler keeps working.
- **D-09:** New metadata fields are additive only. Phase 2 may attach optional top-level fields such as `callbackId`, `correlationId`, `platform`, `stage`, and `durationMs`, plus optional error fields such as `vendorCode` and `retryable`.

### Phase boundary enforcement
- **D-10:** Scenario selection UI is not added in this phase. Only the model seam needed for later scenario control may be introduced, such as `ScenarioConfig` types or placeholder config objects.
- **D-11:** WebView UI behavior and the current 4-button flow remain unchanged in this phase.

### the agent's Discretion
- Naming and package placement of the Android adapter types
- Whether SDK status is modeled as a small value object or built inline from adapter data
- Exact helper method layout for additive metadata response building

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone requirements
- `.planning/PROJECT.md` - Milestone goal, compatibility strategy, and repo-level constraints
- `.planning/REQUIREMENTS.md` - In-scope requirements for `SDK-01`, `SDK-02`, `BRG-01`, and `BRG-02`
- `.planning/ROADMAP.md` - Phase 2 goal, dependency chain, success criteria, and plan slots

### Android integration baseline
- `app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java` - Current direct bridge-to-SDK coupling and response JSON shape
- `app/src/main/java/com/demo/railbridge/WebViewActivity.java` - Current SDK construction, initialization, and bridge injection flow
- `app/src/main/java/com/demo/railbridge/sdk/MockRailSdk.java` - Existing mock SDK behavior that must be wrapped rather than replaced
- `app/src/main/java/com/demo/railbridge/retry/RetryHandler.java` - Existing retry ownership and callback contract
- `app/src/main/assets/webview/index.html` - Current JS client expectations and response compatibility surface

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NativeBridge` already centralizes JSON parsing, response creation, and JS callback posting, so the adapter seam should slot into its existing orchestration role.
- `RetryHandler` already wraps async SDK calls behind `RetryTask` and should remain reusable for adapter-backed calls.
- `MockRailSdk` already exposes clear async callback entrypoints for initialize, charge, and balance operations, making it a good candidate for a wrapper instead of a rewrite.

### Established Patterns
- SDK work currently enters from `WebViewActivity`, which creates dependencies and injects the bridge into the `WebView`.
- Bridge responses are JSON strings posted through `evaluateJavascript("window.onBridgeResult(...)")`, so response shape changes must remain backward compatible.
- Error logging is handled outside the SDK class through `ErrorLogger`, which supports keeping adapter logic focused on SDK concerns.

### Integration Points
- `WebViewActivity` is the right place to swap concrete SDK construction for adapter construction.
- `NativeBridge` is the right place to replace direct `MockRailSdk` references with the adapter interface.
- The current WebView page should not need any behavior changes for this phase if additive metadata is optional.

</code_context>

<specifics>
## Specific Ideas

- Preserve the existing Android demo as the compatibility baseline and layer the seam underneath it.
- Use the adapter introduction to make future vendor-SDK replacement credible without claiming real vendor behavior yet.
- Defer all visible scenario controls and richer diagnostics UI until Phase 3.

</specifics>

<deferred>
## Deferred Ideas

- Deterministic scenario picker UI and structured diagnostics panel - Phase 3
- In-flight request tracking, duplicate callback suppression, and teardown-safe late callback handling - Phase 4
- Cross-platform parity concerns beyond Android adapter introduction - Phase 5

</deferred>

---

*Phase: 02-android-adapter-seam*
*Context gathered: 2026-04-06*
