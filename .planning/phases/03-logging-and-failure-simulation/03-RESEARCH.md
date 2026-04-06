# Phase 3: Logging and Failure Simulation - Research

**Researched:** 2026-04-06
**Domain:** Android WebView bridge diagnostics, deterministic mock-SDK failure simulation, and structured JSON export
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

### Claude's Discretion
- Exact naming and presentation of the scenario presets
- Whether export is implemented as direct file save, JSON text handoff, or both, as long as structured JSON evidence is preserved
- Exact layout of the detail viewer and how raw payloads are presented beside the current result log
- Internal event model shape beyond the required evidence fields, as long as `correlationId` remains the request anchor

### Deferred Ideas (OUT OF SCOPE)
- In-flight request ownership, duplicate callback suppression, timeout guardians, and teardown-safe late callback handling belong to Phase 4
- iOS parity for the same scenario model belongs to Phase 5
- Real Crashlytics/backend event ingestion remains future work once the demo-level evidence model is stable
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SDK-03 | The Android mock SDK can deterministically simulate timeout, internal error, callback loss, duplicate callback, and retry exhaustion scenarios. | Use an adapter-owned `ScenarioPreset` + controller. Simulate `retry exhausted` by repeatedly emitting retryable errors and letting `RetryHandler` derive the terminal code. |
| LOG-01 | Each request is traceable across JS entry, native validation, SDK start, SDK callback, and JS callback using a shared `correlationId`. | Add a request-timeline aggregate keyed by `correlationId` and append stage events from both bridge and retry/logger layers. |
| LOG-02 | Logs can be exported as structured JSON or saved so SDK issues can be demonstrated with evidence beyond on-screen text. | Keep a bounded persisted timeline store and expose JSON export to the WebView page; add native file-save only if the plan explicitly needs one-tap artifact creation. |
| UI-01 | The current WebView demo screen keeps the existing actions and adds a scenario panel plus richer log inspection without removing current usability. | Keep the four action buttons unchanged and add a diagnostics panel plus detail/export view in the same `index.html` page. |
</phase_requirements>

## Summary

Phase 3 should stay on the existing Android bridge transport and current WebView page. The right implementation seam is already present: `MockRailSdkAdapter` is where deterministic scenario behavior belongs, `NativeBridge.RequestContext` already creates a `correlationId`, `BridgeResponseFactory` already carries additive metadata, and `ErrorLogger` already persists bounded local diagnostics. The missing piece is not another architecture rewrite. It is converting the current flat error log into a request-timeline store and making partial timelines observable even when a callback never arrives.

Two planning implications matter. First, `callback loss` and `duplicate callback` are observability features in Phase 3, not hardening features. The logger must record request start and intermediate stages independently of `window.onBridgeResult`, because `callback loss` intentionally never reaches that path. Second, `retry exhausted` must remain bridge-derived because retry ownership is still in `RetryHandler`; the adapter should emit retryable vendor failures repeatedly and let the bridge convert the final outcome to `RETRY_EXHAUSTED`.

The lowest-risk export path is JSON handoff to the existing page with copy/detail viewing. Native file save via `ActivityResultContracts.CreateDocument("application/json")` is viable and officially supported, but it adds activity-owned launcher wiring and is optional unless the planner decides the portfolio needs one-tap file creation.

**Primary recommendation:** Keep `addJavascriptInterface`, add an adapter-level preset controller plus a persisted request-timeline model keyed by `correlationId`, expose diagnostics/export on the existing page, and treat native file save as optional rather than the baseline requirement.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android WebView + `@JavascriptInterface` | Android API 26-34 (`minSdk 26`, `compileSdk 34`) | Existing JS/native bridge transport | Already in production path for this demo; changing transport now adds avoidable compatibility and origin-scope work. |
| Android `Handler` / `Looper` + existing `RetryHandler` | Platform API | UI-thread JS delivery and retry orchestration | Matches the current bridge design and keeps Phase 4 hardening concerns isolated. |
| `ErrorLogger` backed by `SharedPreferences` | Existing repo implementation | Bounded local persistence for recent diagnostics | Zero new dependency path; sufficient for a demo-sized ring buffer if the JSON schema is versioned and capped. |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `org.json:json` | `20231013` | Serialize bridge payloads, timeline events, and export blobs | Use for additive JSON fields and export generation because the repo already depends on it. |
| `androidx.appcompat:appcompat` | `1.6.1` | `AppCompatActivity` host for WebView and optional activity result launcher | Use current activity stack; no migration is needed for Phase 3. |
| `com.google.android.material:material` | `1.11.0` | If the native side later needs minimal save/share affordances | Optional only; the current page-level diagnostics live in HTML/CSS. |
| JUnit | `4.13.2` | Deterministic unit coverage for preset mapping, timeline aggregation, and export JSON | Use for almost all Phase 3 behavior because current instrumentation coverage is absent. |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Keep current `addJavascriptInterface` bridge | Migrate to `WebMessagePort` / `addWebMessageListener` | More modern and origin-aware, but adds file-origin constraints and larger compatibility risk for a phase that is supposed to be additive. |
| Keep bounded `SharedPreferences` timeline cache | Migrate to DataStore now | DataStore is the modern recommendation, but a storage migration is unnecessary scope for a 50-entry diagnostics ring. |
| JSON handoff export on the current page | Native file save with `CreateDocument` | Better artifact UX, but requires activity-owned URI handling and more test surface. |

**Installation:**
```bash
# Baseline Phase 3 needs no new dependency.
# Reuse the existing Android stack already declared in Gradle.
```

**Version verification:** No new package is required for the recommended baseline. Current repo versions were verified from local Gradle files on 2026-04-06: AGP `8.2.0`, Gradle `8.2`, AppCompat `1.6.1`, Material `1.11.0`, `org.json` `20231013`, and JUnit `4.13.2`.

## Architecture Patterns

### Recommended Project Structure
```text
app/src/main/java/com/demo/railbridge/
|-- bridge/        # NativeBridge + additive bridge responses + diagnostics bridge methods
|-- sdk/           # MockRailSdkAdapter + ScenarioPreset + ScenarioController
|-- logging/       # RequestTimeline, TimelineEvent, export serializer, bounded persistence
|-- retry/         # Existing retry ownership remains here
\-- WebViewActivity.java   # Optional native save/export launcher if chosen

app/src/main/assets/webview/
\-- index.html     # Existing page plus diagnostics panel, detail viewer, export controls

app/src/test/java/com/demo/railbridge/
|-- sdk/           # Scenario mapping and adapter behavior tests
|-- logging/       # Timeline/export tests
\-- bridge/        # Metadata and compatibility tests
```

### Pattern 1: Adapter-Owned Scenario Preset
**What:** Introduce a small enum-like preset model and a single active scenario controller read by `MockRailSdkAdapter` for every SDK-backed request.
**When to use:** For `requestCharge` and `getBalance` only; `getSdkStatus` should surface the active preset but should not itself participate in failure simulation.
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
**What:** Replace flat `LogEvent` thinking with a request aggregate that contains summary fields plus an ordered list of stage events.
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
**What:** Keep the existing result callback path for the four main actions, but provide a separate diagnostics fetch/export path for timeline inspection.
**When to use:** Always for the new diagnostics panel, especially for `callback loss`, because no final JS result exists in that scenario.
**Example:**
```java
// Keep the existing four action methods unchanged.
// Add dedicated diagnostics helpers only if needed:
// - getDiagnosticsSnapshot()
// - clearDiagnostics()
// - exportDiagnostics()
```

### Anti-Patterns to Avoid
- **Free-form failure toggles:** The context explicitly locks this phase to presets, not a mini scripting engine.
- **Adapter-generated `RETRY_EXHAUSTED`:** That bypasses the existing retry owner and weakens the evidence model.
- **Logging only final outcomes:** `callback loss` becomes invisible if `js_entry` and `sdk_start` are not persisted immediately.
- **Schema replacement in bridge results:** The current page still depends on `status`, `method`, `data`, `error`, and `retryCount`.
- **Replacing the single WebView page:** The user constraint is additive diagnostics on the current page, not a new route or native screen.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Failure simulation UX | Free-form toggle matrix or mini scenario DSL | A single `ScenarioPreset` selector | Deterministic presets are what the phase locks, and they are much easier to test. |
| Retry-exhausted outcome | Custom retry loop inside the adapter | Existing `RetryHandler` | Retry ownership is already bridge-owned and must remain there this phase. |
| Export artifact | Custom file-path picker or raw filesystem write | JSON handoff first, optional `CreateDocument("application/json")` second | Android already provides a supported document-creation contract without storage permissions. |
| Telemetry persistence | Room/SQLite or remote analytics backend | Existing bounded local logger + JSON export | This phase needs demo-grade evidence, not production telemetry infrastructure. |
| Bridge transport modernization | Full migration to `WebMessagePort` APIs | Existing `addJavascriptInterface` transport | The bridge is already live and Phase 3 is additive, not a transport rewrite. |

**Key insight:** The hard part in this phase is evidence modeling, not infrastructure. Reuse the existing transport, retry owner, and logger boundary; only add the missing deterministic preset and request-timeline layers.

## Common Pitfalls

### Pitfall 1: `callback loss` disappears if you only log final callbacks
**What goes wrong:** The request looks like it vanished because the UI never receives a final bridge payload.
**Why it happens:** `callback loss` intentionally never reaches `sdk_callback` or `js_callback`.
**How to avoid:** Persist `js_entry`, `native_validation`, and `sdk_start` immediately and let the diagnostics panel read native state independently of `window.onBridgeResult`.
**Warning signs:** The visible log only shows "request started" text from JS with no corresponding native record.

### Pitfall 2: `internal error` accidentally turns into `retry exhausted`
**What goes wrong:** Two distinct scenarios collapse into the same final user-visible outcome.
**Why it happens:** `RetryHandler.isRetryable()` currently retries both `ERR_NETWORK_TIMEOUT` and `ERR_SDK_INTERNAL`.
**How to avoid:** Lock scenario-to-error mapping early. If `internal error` must be terminal and distinct, introduce a non-retryable vendor/internal mapping instead of reusing the retryable one blindly.
**Warning signs:** Both presets produce the same final code `9001`.

### Pitfall 3: Threading assumptions around the bridge are wrong
**What goes wrong:** Heavy JSON work or UI calls happen on the wrong thread and create race or ANR behavior.
**Why it happens:** Android documents that JavaScript interface calls occur on a private background thread, while `evaluateJavascript()` must run on the UI thread.
**How to avoid:** Keep timeline mutation thread-safe, and confine JS callback delivery to the main thread as the repo already does.
**Warning signs:** Intermittent missing UI updates, duplicate event ordering issues, or thread-related crashes after adding diagnostics helpers.

### Pitfall 4: Export payloads become too large for a WebView bridge
**What goes wrong:** The page freezes or export becomes slow/unreliable.
**Why it happens:** Large JSON strings moved through the bridge increase memory and UI-thread work.
**How to avoid:** Keep a bounded timeline ring, truncate raw payload previews, and export on demand rather than pushing every detail to the page continuously.
**Warning signs:** Export or detail rendering gets noticeably slower after repeated requests.

### Pitfall 5: Old `LogEvent` records break new timeline parsing
**What goes wrong:** Existing stored logs become unreadable or mixed-schema parsing fails.
**Why it happens:** `ErrorLogger` currently stores a flat `LogEvent` array in `SharedPreferences`.
**How to avoid:** Add a versioned JSON envelope or backward-compatible parser and clear or migrate the old key intentionally.
**Warning signs:** `Failed to parse logs` appears after the first run with the new model.

## Code Examples

Verified patterns from official sources:

### UI-Thread JS Callback Delivery
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

### Native File Save Contract for Optional JSON Export
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

private void startExport() {
    exportLauncher.launch("railbridge-diagnostics.json");
}
```
Source: AndroidX `ActivityResultContracts.CreateDocument` reference  
https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Randomized `MockRailSdk` behavior | Preset-driven deterministic adapter behavior | Phase 3 | Makes support cases reproducible and testable. |
| Flat error/event list | Request timelines grouped by `correlationId` | Phase 3 | Makes retry chains, stalls, and duplicate callbacks inspectable. |
| On-screen human-readable result text only | Structured JSON export plus raw detail viewer | Phase 3 | Creates portable evidence for debugging reports and handoff. |
| No diagnostics path for incomplete requests | Separate diagnostics fetch/export surface | Phase 3 | Makes `callback loss` visible without adding Phase 4 timeout hardening yet. |

**Deprecated/outdated:**
- `ActivityResultContracts.CreateDocument()` no-arg constructor: deprecated in favor of passing an explicit MIME type such as `"application/json"`.
- Treating `SharedPreferences` as a long-term telemetry store: Android recommends DataStore as the modern persistence approach, but Phase 3 should not expand into that migration unless scope changes.
- Full bridge transport replacement in this phase: technically possible, but misaligned with the locked additive-delivery strategy and the current `file:///android_asset` flow.

## Open Questions

1. **Is one-tap native file save required in Phase 3, or is JSON handoff/copy enough?**
   - What we know: LOG-02 is satisfied by structured JSON export, and Android officially supports `CreateDocument("application/json")` if needed.
   - What's unclear: Whether the portfolio outcome requires a native file artifact from the first pass.
   - Recommendation: Plan JSON handoff/detail viewing as required. Make native file save a second task only if capacity remains.

2. **How should `internal error` remain distinct from `retry exhausted` under the current retry policy?**
   - What we know: `ERR_SDK_INTERNAL` is currently retryable, which can collapse the two presets into the same outcome.
   - What's unclear: Whether to add a non-retryable vendor/internal code or to encode distinct vendor metadata while reusing an existing bridge code.
   - Recommendation: Resolve this in planning before task breakdown. Do not leave scenario-to-error mapping implicit.

3. **Will diagnostics helpers add new bridge methods, or will the existing page poll native state another way?**
   - What we know: `callback loss` requires a second diagnostics path because the normal result callback never fires.
   - What's unclear: Whether the team wants explicit helper methods like `exportDiagnostics()` or a pull-on-load snapshot method.
   - Recommendation: Add narrowly scoped diagnostics-only methods if needed; keep the original four action methods and buttons unchanged.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java runtime | Gradle builds/tests | Yes | `25.0.1` | Use Android Studio JBR 17 if AGP 8.2 behavior differs under CLI Java. |
| Gradle wrapper | Build and unit tests | Yes | `8.2` | - |
| Git | Doc commit workflow | Yes | `2.53.0.windows.1` | - |
| Android unit-test ASCII workspace copy | Reliable `testDebugUnitTest` execution | Yes | `C:\codex-temp\railbridge-demo-copy` present | Create a fresh ASCII-path copy if this one goes stale. |
| `adb` on PATH | CLI emulator/manual smoke verification | No | - | Use Android Studio Device Manager or add platform-tools to PATH. |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` env vars | CLI Android tooling discovery | No | - | Rely on Android Studio-managed SDK configuration. |

**Missing dependencies with no fallback:**
- None for Phase 3 implementation itself.

**Missing dependencies with fallback:**
- `adb` and Android SDK env vars are missing in this shell, so CLI-driven smoke verification is weaker than Android Studio-driven manual validation.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit `4.13.2` via Gradle `testDebugUnitTest` |
| Config file | none - Gradle default test task |
| Quick run command | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest" --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` |
| Full suite command | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SDK-03 | Each preset deterministically produces the intended adapter behavior, including retry-exhausted setup | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.*Scenario*"` | No - Wave 0 |
| LOG-01 | Timelines capture `js_entry`, `native_validation`, `sdk_start`, `sdk_callback`, and `js_callback` under one `correlationId` | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.logging.*"` | No - Wave 0 |
| LOG-02 | Structured export JSON contains required evidence fields and remains parseable | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.logging.*Export*"` | No - Wave 0 |
| UI-01 | Existing buttons remain usable and diagnostics panel/detail viewer appear on the same page | manual smoke | Android Studio/emulator manual pass on existing WebView page | No - Manual only |

### Sampling Rate
- **Per task commit:** targeted unit tests in the ASCII workspace copy
- **Per wave merge:** `:app:testDebugUnitTest :app:assembleDebug` from the ASCII workspace copy
- **Phase gate:** Full unit suite green plus one manual emulator smoke that exercises all four existing actions and one failure preset

### Wave 0 Gaps
- [ ] `app/src/test/java/com/demo/railbridge/sdk/MockRailSdkScenarioTest.java` - deterministic preset mapping and retry-exhaustion setup
- [ ] `app/src/test/java/com/demo/railbridge/logging/RequestTimelineRepositoryTest.java` - stage ordering, completion state, and correlation grouping
- [ ] `app/src/test/java/com/demo/railbridge/logging/DiagnosticsExportTest.java` - JSON schema, field presence, and bounded export size
- [ ] `app/src/test/java/com/demo/railbridge/bridge/NativeBridgeDiagnosticsTest.java` - additive metadata and diagnostics helper compatibility
- [ ] Manual verification checklist for the WebView diagnostics panel, because no instrumentation or CLI `adb` path is currently configured

## Sources

### Primary (HIGH confidence)
- Local repo inspection - current bridge, retry, logger, WebView UI, and Gradle/test baseline
- Android WebView JS bridge guidance — https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge
- Android `WebView` API reference — https://developer.android.com/reference/android/webkit/WebView
- AndroidX `ActivityResultContracts.CreateDocument` reference — https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.CreateDocument
- Android DataStore guide — https://developer.android.com/topic/libraries/architecture/datastore
- Android `SharedPreferences` API reference — https://developer.android.com/reference/android/content/SharedPreferences

### Secondary (MEDIUM confidence)
- `.planning/phases/03-logging-and-failure-simulation/03-CONTEXT.md` - locked decisions and phase boundaries
- `.planning/STATE.md` - non-ASCII-path testing constraint and previously verified Android Studio/JBR baseline

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - The recommended baseline reuses the current repo stack and official Android platform APIs rather than introducing speculative dependencies.
- Architecture: MEDIUM - The main patterns are strongly supported by local code and platform constraints, but diagnostics-helper method shape and scenario-to-error mapping still require a planning decision.
- Pitfalls: HIGH - The major risks are directly visible in current code or explicitly documented by Android platform references.

**Research date:** 2026-04-06
**Valid until:** 2026-05-06
