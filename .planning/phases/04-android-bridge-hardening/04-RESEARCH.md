# Phase 4: Android Bridge Hardening - Research

**Researched:** 2026-04-08
**Domain:** Android hybrid bridge request ownership, duplicate callback suppression, timeout watchdogs, and teardown-safe JS delivery
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (derived from ROADMAP.md and completed Phases 2-3)

### Locked Decisions
- **D-01:** The existing four bridge methods stay intact: `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`.
- **D-02:** Hardening happens in the Android bridge layer; the mock adapter keeps simulating duplicate callbacks and callback loss so those scenarios remain reproducible.
- **D-03:** The first accepted terminal callback wins for a request; later duplicate callbacks must be ignored instead of reaching JS twice.
- **D-04:** Hanging SDK-backed requests must eventually resolve to a timeout-style terminal outcome while the bridge is still alive.
- **D-05:** Teardown safety matters as much as retry safety; a destroyed bridge or closed screen must not receive late `evaluateJavascript` writes.
- **D-06:** In-flight ownership must be explicit enough to inspect from diagnostics rather than inferred only from flat logs.
- **D-07:** Existing additive metadata remains additive only; Phase 4 must not break the current `window.onBridgeResult(...)` consumer.
- **D-08:** The diagnostics panel added in Phase 3 remains the primary inspection path; do not invent a separate native debug screen.

### Planner Discretion
- Exact timeout duration for bridge-owned watchdogs
- Exact model name for the in-flight request registry or coordinator
- Whether in-flight observability is exposed as counts, request records, or both
- Exact stage names for ignored duplicate callbacks, timeout transitions, and teardown abandonment

### Deferred Ideas (Out of Scope)
- iOS parity work remains Phase 5
- Portfolio document packaging remains Phase 6
- Real vendor SDK integration and backend analytics remain future requirements
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| STAB-01 | Android bridge execution handles in-flight request tracking, duplicate callback suppression, timeout handling, and ignores late callbacks after teardown. | Add a bridge-owned request coordinator that tracks per-request state, decides whether a callback is still deliverable, schedules timeout transitions, and exposes observable in-flight state to diagnostics. |
</phase_requirements>

## Summary

Phase 4 should harden the existing Android bridge without undoing the adapter seam or diagnostics work from Phases 2 and 3. The bridge already has the right raw ingredients: deterministic duplicate and callback-loss scenarios come from `MockRailSdkAdapter`, request timelines already exist in `ErrorLogger`, and the WebView page already knows how to inspect diagnostics. What is still missing is ownership. Right now `NativeBridge` has no explicit model for "this request is still pending", "this callback already won", or "the bridge died before a late callback arrived".

The highest-value design move is to introduce a small Android-free coordinator for request lifecycle decisions and then let `NativeBridge` use it at the edges. That keeps the race-condition rules plain-JUnit-testable instead of pushing all hardening logic directly into `Handler`, `WebView`, and `RetryHandler` code. It also lets Phase 4 keep the current contract intact while making the diagnostics panel more truthful: pending work can be shown as explicitly in-flight, duplicate callbacks can be recorded as ignored rather than silently disappearing, and callback loss can turn into a visible timeout instead of hanging forever.

**Primary recommendation:** Add a pure-Java bridge request coordinator keyed by `correlationId`, wire it into `NativeBridge` for timeout ownership and teardown checks, record duplicate or late callback rejections into diagnostics, and expose current in-flight ownership in the existing diagnostics payload.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android WebView + `@JavascriptInterface` | API 26-34 (`minSdk 26`, `compileSdk 34`) | Existing JS/native transport and current demo entrypoint | Already locked by earlier phases; Phase 4 should harden it, not replace it |
| `Handler` / `Looper` | Platform API | Main-thread JS delivery and timeout scheduling | Already present in `NativeBridge` and `RetryHandler` |
| Existing `ErrorLogger` request timelines | Repo implementation | Persist timeout, duplicate-ignore, and teardown-ignore evidence | Keeps diagnostics additive and consistent with Phase 3 |
| Plain Java concurrent collections | JDK | In-flight request registry and terminal-state guards | Needed for race-safe request ownership without Android runtime coupling |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit | `4.13.2` | Deterministic tests for lifecycle-state transitions and duplicate suppression | Use for the coordinator and diagnostics payload logic |
| Existing emulator + WebView DevTools smoke path | Current local setup | Verify duplicate callback, timeout, and teardown behavior in the real page flow | Use after the unit tests and `assembleDebug` pass |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Bridge-owned coordinator + thin `NativeBridge` wiring | Direct state flags sprinkled across `NativeBridge` callbacks | Faster to type but harder to test and easier to regress |
| Existing diagnostics payload + in-flight extension | Separate native-only debug model | Would split observability across two places and weaken the portfolio story |
| Bridge-level timeout watchdog | Let callback loss stay indefinitely incomplete | Conflicts with Phase 4 success criteria that hanging requests should not remain unresolved forever |

## Architecture Patterns

### Recommended Project Structure
```text
app/src/main/java/com/demo/railbridge/
|-- bridge/
|   |-- NativeBridge.java                  # Runtime wiring, JS delivery, and diagnostics helper methods
|   |-- BridgeRequestCoordinator.java      # Android-free ownership and terminal-state decisions
|   `-- InFlightRequestRecord.java         # Diagnostics-facing pending request snapshot
|-- logging/
|   `-- ErrorLogger.java                   # Existing request timeline persistence with new hardening events
`-- sdk/
    `-- MockRailSdkAdapter.java            # Still emits duplicate callbacks and callback loss on purpose

app/src/test/java/com/demo/railbridge/bridge/
|-- BridgeRequestCoordinatorTest.java      # Plain-JUnit race and timeout rules
`-- NativeBridgeDiagnosticsTest.java       # Additive diagnostics payload and hardening evidence shape
```

### Pattern 1: Request Ownership Coordinator
**What:** Create a plain-Java coordinator that owns request registration, timeout deadlines, duplicate suppression, teardown abandonment, and an inspectable list of active requests.

**When to use:** For SDK-backed bridge actions such as `requestCharge` and `getBalance`.

**Example:**
```java
public final class BridgeRequestCoordinator {
    public RegisteredRequest begin(RequestSeed seed);
    public CompletionDecision acceptSuccess(String correlationId, int retryCount);
    public CompletionDecision acceptError(String correlationId, SdkErrorCode errorCode, int retryCount);
    public TimeoutDecision onTimeout(String correlationId);
    public TeardownSummary destroyAll();
    public List<InFlightRequestRecord> snapshot();
}
```

### Pattern 2: First-Terminal-Callback-Wins
**What:** Make success, error, and timeout all flow through a single terminal-state gate so only the first accepted terminal outcome reaches JS.

**When to use:** Always for duplicate callback scenarios and for racey combinations like timeout followed by a late SDK success.

**Why it matters:** The diagnostics panel should show that a duplicate happened, but the page should only receive one terminal result.

### Pattern 3: Timeout as a Bridge Outcome
**What:** Treat callback loss or hung SDK work as a bridge-owned timeout outcome rather than relying on the SDK or adapter to emit it.

**When to use:** For any request that begins and never receives an accepted terminal callback before the watchdog fires.

**Why it matters:** The adapter intentionally simulates callback loss. Phase 4 should turn that from "incomplete forever" into "timed out with evidence".

### Pattern 4: Teardown-Safe JS Delivery
**What:** Check both bridge liveness and request ownership before posting the JS result, and record ignored late callbacks as diagnostics evidence instead of silently dropping them.

**When to use:** For all delayed callbacks, including retry completions and duplicate success paths.

### Anti-Patterns to Avoid
- Letting duplicate callbacks call `window.onBridgeResult(...)` twice
- Cancelling the retry handler without also marking active requests as abandoned
- Recording timeout only in UI text without preserving a structured timeline event
- Moving hardening rules into the mock adapter instead of the bridge layer
- Making diagnostics truth depend on whether the WebView page is currently open

## Common Pitfalls

### Pitfall 1: Timeout fires after success and creates a second terminal outcome
**What goes wrong:** A request looks both successful and timed out.

**How to avoid:** Make timeout evaluation go through the same terminal-state gate as SDK callbacks so only the first accepted terminal event wins.

### Pitfall 2: Duplicate callbacks are ignored silently
**What goes wrong:** The behavior is safer, but support evidence gets weaker because the duplicate condition leaves no trace.

**How to avoid:** Record an explicit diagnostics stage such as `sdk_callback_ignored_duplicate` or equivalent when the coordinator rejects a terminal repeat.

### Pitfall 3: Destroyed bridge prevents evidence from being recorded
**What goes wrong:** The screen closes, late callbacks stop reaching JS, and there is no record explaining why.

**How to avoid:** Separate "record diagnostics" from "deliver JS result". Even when delivery is rejected, record the reason.

### Pitfall 4: In-flight visibility only exists in memory
**What goes wrong:** The diagnostics snapshot looks empty right when debugging a hung request.

**How to avoid:** Expose current in-flight request records through the existing diagnostics payload so the page can inspect pending state in real time.

### Pitfall 5: `getSdkStatus` and `reportError` get dragged into the heavy request-ownership path unnecessarily
**What goes wrong:** Bridge hardening becomes more invasive than needed.

**How to avoid:** Start by hardening the SDK-backed async flows (`requestCharge`, `getBalance`) and only add ownership to other actions if the wiring clearly benefits.

## Code Examples

### Main-thread cancellation pattern
```java
handler.removeCallbacksAndMessages(null);
```
Source: Android `Handler` API reference  
https://developer.android.com/reference/android/os/Handler

### JS result delivery guard
```java
webView.evaluateJavascript(
        "window.onBridgeResult(" + JSONObject.quote(resultJson) + ")",
        null
);
```
Source: Android `WebView.evaluateJavascript` API reference  
https://developer.android.com/reference/android/webkit/WebView

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java runtime | Gradle builds/tests | yes | `25.0.1` | Android Studio JBR 17 for AGP |
| Gradle wrapper | Build and tests | yes | `8.2` | none |
| Git | Planning doc commits | yes | `2.53.0.windows.1` | none |
| ASCII-path workspace copy | Reliable `testDebugUnitTest` execution | yes | `C:\codex-temp\railbridge-demo-copy` | Recreate copy if stale |
| Emulator verification path | Runtime bridge smoke checks | yes | existing local emulator flow | Android Studio Device Manager if adb path changes |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit `4.13.2` for coordinator and diagnostics contract tests plus manual Android emulator smoke |
| Quick run command | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest" --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"` |
| Full suite command | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` |
| Special constraint | Android unit tests must run from the ASCII-path copy because the main workspace path contains non-ASCII characters that break the AGP unit-test worker classpath |

### Requirement-to-Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| STAB-01 | Duplicate callbacks are suppressed after the first accepted terminal result | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest"` | Wave 0 |
| STAB-01 | Hung requests transition into a timeout outcome instead of remaining indefinitely unresolved | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest"` | Wave 0 |
| STAB-01 | Diagnostics payload exposes current in-flight ownership and ignored-late-callback evidence | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"` | existing file, expanded in Phase 4 |
| STAB-01 | Real WebView flow still works after bridge hardening | manual smoke | `:app:assembleDebug` plus emulator verification | Manual only |

## Sources

### Primary
- Local repo inspection of `NativeBridge`, `RetryHandler`, `MockRailSdkAdapter`, `ErrorLogger`, `NativeBridgeDiagnosticsTest`, and Phase 3 verification docs
- Android `Handler` API reference  
  https://developer.android.com/reference/android/os/Handler
- Android `WebView` API reference  
  https://developer.android.com/reference/android/webkit/WebView

### Secondary
- `.planning/ROADMAP.md`
- `.planning/REQUIREMENTS.md`
- `.planning/phases/03-logging-and-failure-simulation/03-VERIFICATION.md`

## Metadata

**Confidence breakdown**
- Standard stack: HIGH
- Architecture: MEDIUM
- Pitfalls: HIGH

**Research date:** 2026-04-08
**Valid until:** 2026-05-08
