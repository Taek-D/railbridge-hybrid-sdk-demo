# Phase 3: Logging and Failure Simulation - Research

**Researched:** 2026-04-06
**Domain:** Android WebView bridge diagnostics, deterministic mock-SDK failure simulation, and structured JSON export
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Failure simulation uses a preset selector model rather than free-form toggles or scripts.
- **D-02:** The preset list explicitly covers `normal`, `timeout`, `internal error`, `callback loss`, `duplicate callback`, and `retry exhausted`.
- **D-03:** Scenario selection stays on the current WebView page as a dedicated diagnostics panel instead of a separate screen.
- **D-04:** The selected scenario remains active for subsequent SDK-backed requests until the user changes it.
- **D-05:** Logging is request-scoped around a shared `correlationId`, not just a flat event list.
- **D-06:** Each request timeline captures `js_entry`, `native_validation`, `sdk_start`, `sdk_callback`, and `js_callback`.
- **D-07:** Structured evidence includes `stage`, `retryCount`, `durationMs`, `scenario`, `vendorCode`, `retryable`, and `resolvedByRetry` when applicable.
- **D-08:** Additive bridge metadata continues to surface at runtime without replacing the baseline fields the current HTML handler already uses.
- **D-09:** Logs are exportable as structured JSON organized by request timeline.
- **D-10:** Export preserves enough detail to support debugging reports or incident handoff without replaying logcat.
- **D-11:** The WebView page gains a richer inspection surface, including raw payload or detail viewing alongside the visible result list.
- **D-12:** Diagnostics remain on the existing WebView demo page rather than moving to a separate route or native-only screen.
- **D-13:** The current four main action buttons remain in place and keep their current role.

### Planner Discretion
- Exact preset names exposed in the UI
- Whether export is JSON handoff only or also includes an optional native file save affordance
- Exact shape of the request timeline event model
- Exact diagnostics-only bridge helper method names

### Deferred Ideas (Out of Scope)
- In-flight request ownership, duplicate callback suppression, timeout guards, and teardown-safe late callback handling belong to Phase 4
- iOS parity for the same diagnostics model belongs to Phase 5
- Real Crashlytics or backend ingestion remains future work once the local evidence model is stable
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SDK-03 | The Android mock SDK can deterministically simulate timeout, internal error, callback loss, duplicate callback, and retry exhaustion scenarios. | Add an adapter-owned `ScenarioPreset` plus a small controller. Keep `retry exhausted` bridge-derived by repeatedly emitting retryable failures. |
| LOG-01 | Each request is traceable across JS entry, native validation, SDK start, SDK callback, and JS callback using a shared `correlationId`. | Add a request-timeline aggregate keyed by `correlationId` and append stage events from both bridge and retry/logger layers. |
| LOG-02 | Logs can be exported as structured JSON or saved so SDK issues can be demonstrated with evidence beyond on-screen text. | Keep a bounded persisted timeline store and expose JSON export to the WebView page. Native file save is optional, not baseline. |
| UI-01 | The current WebView demo screen keeps the existing actions and adds a scenario panel plus richer log inspection without removing current usability. | Extend the current `index.html` page instead of replacing it, and add diagnostics-specific helpers without changing the original four action buttons. |
</phase_requirements>

## Summary

Phase 3 should stay on the existing Android bridge transport and current WebView page. The main extension points are already present: `MockRailSdkAdapter` is where deterministic scenario behavior belongs, `NativeBridge.RequestContext` already creates a `correlationId`, `BridgeResponseFactory` already handles additive metadata, and `ErrorLogger` already provides bounded local persistence. The highest-value change is not another transport rewrite. It is converting the current flat error log into a request-timeline store and making incomplete requests observable even when a final callback never arrives.

Two planning implications matter most:
- `callback loss` and `duplicate callback` are observability scenarios in this phase, not hardening work. The logger must persist request start and intermediate stages independently of `window.onBridgeResult`, because `callback loss` intentionally never reaches that path.
- `retry exhausted` must remain bridge-derived because retry ownership is still in `RetryHandler`; the adapter should emit retryable vendor failures repeatedly and let the bridge derive the terminal code.

**Primary recommendation:** Keep `addJavascriptInterface`, add an adapter-level preset controller plus a persisted request-timeline model keyed by `correlationId`, expose diagnostics and export on the existing page, and treat native file save as optional instead of required.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android WebView + `@JavascriptInterface` | API 26-34 (`minSdk 26`, `compileSdk 34`) | Existing JS/native bridge transport | Already in the working app path and explicitly locked by the phase boundary |
| `Handler` / `Looper` + existing `RetryHandler` | Platform API | UI-thread JS delivery and retry orchestration | Matches the current bridge design and keeps Phase 4 hardening isolated |
| `SharedPreferences`-backed `ErrorLogger` | Existing repo implementation | Bounded local persistence for recent diagnostics | Zero-new-dependency path that is enough for a demo-sized ring buffer |
| `org.json` | `20231013` | Serialize bridge payloads, timeline events, and export blobs | Already present in the repo and aligned with the current result contract |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit | `4.13.2` | Deterministic unit coverage for preset mapping, timeline aggregation, and export JSON | Use for almost all Phase 3 behavior because current instrumentation coverage is minimal |
| AndroidX AppCompat | `1.6.1` | Activity host for WebView and optional export launcher | Only needed if native file save is added |
| Material Components | `1.11.0` | Optional native affordances if file export gets a native trigger | Not required for the baseline page-driven diagnostics |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Existing `addJavascriptInterface` transport | `WebMessagePort` / `addWebMessageListener` | More modern and origin-aware, but too much contract churn for an additive diagnostics phase |
| Bounded `SharedPreferences` timeline cache | DataStore | DataStore is modern, but a storage migration is unnecessary scope here |
| JSON handoff export on the current page | Native file save via `CreateDocument("application/json")` | Better artifact UX, but adds activity-owned URI handling and more test surface |

## Architecture Patterns

### Recommended Project Structure
```text
app/src/main/java/com/demo/railbridge/
|-- bridge/        # NativeBridge + additive bridge responses + diagnostics bridge methods
|-- sdk/           # MockRailSdkAdapter + ScenarioPreset + ScenarioController
|-- logging/       # RequestTimeline, TimelineEvent, export serializer, bounded persistence
|-- retry/         # Existing retry ownership remains here
`-- WebViewActivity.java   # Optional native export launcher if chosen

app/src/main/assets/webview/
`-- index.html     # Existing page plus diagnostics panel, detail viewer, export controls

app/src/test/java/com/demo/railbridge/
|-- sdk/           # Scenario mapping and adapter behavior tests
|-- logging/       # Timeline/export tests
`-- bridge/        # Metadata and diagnostics helper compatibility tests
```

### Pattern 1: Adapter-Owned Scenario Preset
**What:** Introduce a small preset model and a single active scenario controller read by `MockRailSdkAdapter` for every SDK-backed request.

**When to use:** For `requestCharge` and `getBalance`. `getSdkStatus` should surface the active preset but should not itself participate in failure simulation.

**Example:**
```java
public enum ScenarioPreset {
    NORMAL,
    TIMEOUT,
    INTERNAL_ERROR,
    CALLBACK_LOSS,
    DUPLICATE_CALLBACK,
    RETRY_EXHAUSTED
}

public final class ScenarioController {
    private final AtomicReference<ScenarioPreset> active = new AtomicReference<>(ScenarioPreset.NORMAL);

    public ScenarioPreset getActive() {
        return active.get();
    }

    public void setActive(ScenarioPreset preset) {
        active.set(preset == null ? ScenarioPreset.NORMAL : preset);
    }
}
```

### Pattern 2: Request Timeline Aggregate Keyed by `correlationId`
**What:** Replace flat log thinking with a request aggregate that contains summary fields plus an ordered list of stage events.

**When to use:** For every JS-triggered request starting at `js_entry`, including requests that never finish.

**Example:**
```java
public final class RequestTimeline {
    public final String correlationId;
    public final String method;
    public final String scenario;
    public final List<TimelineEvent> events;
    public boolean completed;
    public boolean resolvedByRetry;
    public Integer finalRetryCount;
    public String finalStatus;
}
```

### Pattern 3: Diagnostics as a Secondary Channel
**What:** Keep the existing result callback path for the four main actions, but provide a separate diagnostics fetch or export path for timeline inspection.

**When to use:** Always for the new diagnostics panel, especially for `callback loss`, because no final result ever reaches `window.onBridgeResult`.

**Example:**
```java
// Keep the existing four action methods unchanged.
// Add diagnostics-only helpers such as:
// - setScenarioPreset(String preset)
// - getDiagnosticsSnapshot()
// - clearDiagnostics()
// - exportDiagnostics()
```

### Anti-Patterns to Avoid
- Free-form failure toggles or a mini scenario DSL
- Adapter-generated `RETRY_EXHAUSTED`
- Logging only final outcomes
- Replacing the existing success or error JSON shape
- Replacing the single WebView page with a new screen

## Common Pitfalls

### Pitfall 1: `callback loss` disappears if you only log final callbacks
**What goes wrong:** The request looks like it vanished because the UI never receives a final bridge payload.

**How to avoid:** Persist `js_entry`, `native_validation`, and `sdk_start` immediately and let the diagnostics panel read native state independently of `window.onBridgeResult`.

### Pitfall 2: `internal error` accidentally collapses into `retry exhausted`
**What goes wrong:** Two distinct support cases end up with the same final observable outcome.

**How to avoid:** Lock scenario-to-error mapping in planning. If `internal error` must remain terminal and distinct, map it to a non-retryable bridge-visible outcome while preserving vendor/internal evidence in the timeline.

### Pitfall 3: Threading assumptions around the bridge are wrong
**What goes wrong:** Heavy JSON work or UI calls happen on the wrong thread and create race or ANR behavior.

**How to avoid:** Keep timeline mutation thread-safe, and confine JS callback delivery to the main thread as the repo already does.

### Pitfall 4: Export payloads become too large for a WebView bridge
**What goes wrong:** The page freezes or export becomes slow or unreliable.

**How to avoid:** Keep a bounded timeline ring, truncate raw payload previews, and export on demand rather than pushing every detail to the page continuously.

### Pitfall 5: Old `LogEvent` records break new timeline parsing
**What goes wrong:** Existing stored logs become unreadable or mixed-schema parsing fails.

**How to avoid:** Add a versioned JSON envelope or intentional migration path rather than silently replacing the stored format.

## Code Examples

### UI-thread JS callback delivery
```java
webView.post(() ->
        webView.evaluateJavascript(
                "window.onBridgeResult(" + JSONObject.quote(resultJson) + ")",
                null
        )
);
```
Source: Android `WebView.evaluateJavascript` API reference  
https://developer.android.com/reference/android/webkit/WebView

### Optional native file-save contract
```java
private final ActivityResultLauncher<String> exportLauncher =
        registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri == null) {
                        return;
                    }
                    // Write exported JSON to the returned Uri.
                }
        );
```
Source: AndroidX `ActivityResultContracts.CreateDocument` reference  
https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument

## Open Questions Resolved for Planning

1. **Export baseline**
   - Recommendation locked: JSON handoff on the existing page is required.
   - Native file save is optional and should only be added if Phase 3 capacity remains after the main diagnostics flow is working.

2. **`internal error` vs `retry exhausted`**
   - Recommendation locked: keep them distinct by making `retry exhausted` scenario repeatedly emit retryable timeout-style failures, while `internal error` maps to a non-retryable bridge-visible failure that still preserves vendor/internal evidence in timeline metadata.

3. **Diagnostics surface**
   - Recommendation locked: add narrowly scoped diagnostics-only bridge helpers instead of altering the original four action methods.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java runtime | Gradle builds/tests | yes | `25.0.1` | Use Android Studio JBR 17 when AGP 8.2 is run through Android Studio |
| Gradle wrapper | Build and unit tests | yes | `8.2` | none |
| Git | Doc commit workflow | yes | `2.53.0.windows.1` | none |
| ASCII-path workspace copy | Reliable `testDebugUnitTest` execution | yes | `C:\codex-temp\railbridge-demo-copy` | Recreate the copy if it goes stale |
| `adb` on `PATH` | CLI emulator smoke verification | no | n/a | Use explicit SDK platform-tools path or Android Studio Device Manager |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit `4.13.2` via Gradle `testDebugUnitTest` plus manual emulator smoke |
| Quick run command | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkScenarioTest" --tests "com.demo.railbridge.logging.RequestTimelineRepositoryTest"` |
| Full suite command | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` |
| Special constraint | Android unit tests must run from the ASCII-path copy because the main workspace path contains non-ASCII characters that break the AGP unit-test worker classpath in this environment |

### Requirement-to-Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SDK-03 | Each preset deterministically produces the intended adapter behavior | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkScenarioTest"` | Wave 0 |
| LOG-01 | Timelines capture all required stages under one `correlationId` | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.logging.RequestTimelineRepositoryTest"` | Wave 0 |
| LOG-02 | Export JSON includes required evidence fields and remains parseable | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.logging.DiagnosticsExportTest"` | Wave 0 |
| UI-01 | Existing buttons remain usable and diagnostics panel/detail viewer appear on the same page | manual smoke | Android Studio or emulator manual pass on the current WebView page | Manual only |

## Sources

### Primary
- Local repo inspection of `NativeBridge`, `BridgeResponseFactory`, `MockRailSdkAdapter`, `RetryHandler`, `ErrorLogger`, `LogEvent`, and `index.html`
- Android WebView native bridge guidance  
  https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge
- Android `WebView` API reference  
  https://developer.android.com/reference/android/webkit/WebView
- AndroidX `ActivityResultContracts.CreateDocument` reference  
  https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument
- Android `SharedPreferences` API reference  
  https://developer.android.com/reference/android/content/SharedPreferences

### Secondary
- `.planning/phases/03-logging-and-failure-simulation/03-CONTEXT.md`
- `.planning/STATE.md`

## Metadata

**Confidence breakdown**
- Standard stack: HIGH
- Architecture: MEDIUM
- Pitfalls: HIGH

**Research date:** 2026-04-06
**Valid until:** 2026-05-06
