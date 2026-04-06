# Phase 2: Android Adapter Seam - Research

**Researched:** 2026-04-06
**Domain:** Android WebView bridge decoupling and SDK adapter boundaries
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
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

### Deferred Ideas (OUT OF SCOPE)
- Deterministic scenario picker UI and structured diagnostics panel - Phase 3
- In-flight request tracking, duplicate callback suppression, and teardown-safe late callback handling - Phase 4
- Cross-platform parity concerns beyond Android adapter introduction - Phase 5
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SDK-01 | Android bridge calls go through a `RailPlusSdkAdapter` seam instead of depending directly on `MockRailSdk`. | Standard Stack, Architecture Patterns 1-2, Don't Hand-Roll #1 |
| SDK-02 | The existing `MockRailSdk` remains available behind the adapter so the current Android demo flow keeps working. | Architecture Patterns 1-2, Code Examples 1-2, Common Pitfalls 2 |
| BRG-01 | Android public bridge methods remain `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`. | Architecture Patterns 1 and 3, Common Pitfalls 1 and 4 |
| BRG-02 | Bridge responses preserve current fields and add optional metadata fields for `callbackId`, `correlationId`, `platform`, `stage`, `durationMs`, `vendorCode`, and `retryable`. | Architecture Pattern 3, Code Example 3, Common Pitfalls 2 |
</phase_requirements>

## Summary

Phase 2 should stay narrowly architectural: introduce a typed Android adapter seam between `NativeBridge` and `MockRailSdk`, keep `WebViewActivity` as the composition root, and leave the WebView page, method names, and visible 4-button flow unchanged. No new dependency is required. The cleanest implementation is to add a repo-local `RailPlusSdkAdapter` interface plus a `MockRailSdkAdapter` wrapper that converts `MockRailSdk` callbacks and status into bridge-safe types.

The highest-risk planning mistake is treating this as a transport rewrite. Android's current documentation recommends `addWebMessageListener` for new work, but this demo currently relies on `addJavascriptInterface`, local `file:///android_asset` content, and synchronous public entrypoints. Replacing the bridge mechanism in this phase would add contract churn and UI changes that are explicitly out of scope. Keep the existing bridge transport, but tighten its internals: one adapter seam, one response-building path, and no leaking of `MockRailSdk` nested types back into the bridge.

Current code inspection surfaced one important seam leak already: `ChargeResult` is top-level and bridge-safe, but balance currently returns `MockRailSdk.BalanceResult`, which would keep the bridge coupled to the concrete SDK even after an interface is added. Fix that in the seam design by introducing an adapter-owned balance/status model or by promoting balance data to an SDK-agnostic value object.

**Primary recommendation:** Add `RailPlusSdkAdapter` and `MockRailSdkAdapter` under the `sdk` layer, keep `RetryHandler` in `NativeBridge`, and centralize success/error/status JSON creation so additive metadata lands consistently without breaking the current JS page.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Android WebView + `@JavascriptInterface` | Android platform API 34, minSdk 26 | Preserve the current JS-to-native bridge contract | Already wired into the demo and explicitly required for Phase 2 compatibility |
| Android Gradle Plugin | 8.2.0 (repo-pinned) | Build system for the app | Already working in this repo; no upgrade is justified for a seam-only phase |
| Plain Java adapter interface | Repo-local | SDK seam between bridge and concrete mock SDK | Lowest-risk way to decouple the bridge without transport churn |
| `org.json.JSONObject` | Android platform | Parse params and build bridge payloads | Already used in `NativeBridge` and the WebView result handler expects JSON strings |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `RetryHandler` | Repo-local | Retry orchestration above the adapter | Keep for `requestCharge` and `getBalance` per D-06 |
| `MockRailSdk` | Repo-local (`1.2.3-mock` internal version) | Default concrete SDK behavior | Wrap it, do not rewrite it |
| JUnit | 4.13.2 (repo-pinned) | Fast JVM tests for adapter and response helpers | Use for seam/unit coverage that does not require a real `WebView` |
| AndroidX Test Runner | 1.1.5 (repo-pinned) | Instrumentation entrypoint | Use when emulator/device CLI is available |
| Espresso | 3.5.1 (repo-pinned) | UI smoke automation | Use later if terminal `adb` access is restored |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `addJavascriptInterface` for this phase | `WebViewCompat.addWebMessageListener` | Officially recommended for new apps, but it changes the bridge model and is a phase-boundary violation here |
| Repo-local adapter seam | Direct `MockRailSdk` usage in `NativeBridge` | Faster short term, but it fails `SDK-01` and keeps vendor swap impossible |
| `org.json` reuse | Gson/Moshi | Unnecessary dependency and response-shape churn for a seam-only phase |

**Installation:**
```bash
# No new dependencies are required for Phase 2.
.\gradlew.bat :app:assembleDebug
```

**Version verification:** This phase should use repo-pinned versions rather than introduce upgrades. Verified from `build.gradle` and `app/build.gradle` on 2026-04-06: AGP `8.2.0`, compile/target SDK `34`, AppCompat `1.6.1`, Material `1.11.0`, ConstraintLayout `2.1.4`, JUnit `4.13.2`, AndroidX test `1.1.5`, Espresso `3.5.1`.

## Architecture Patterns

### Recommended Project Structure
```text
app/src/main/java/com/demo/railbridge/
|-- bridge/                # JavascriptInterface entrypoints and JS callback posting
|-- sdk/                   # Mock SDK, adapter interface, wrapper, SDK-neutral result/status models
|-- retry/                 # Retry orchestration stays above the adapter in Phase 2
`-- logging/               # Error/event persistence
```

### Pattern 1: Activity As Composition Root
**What:** `WebViewActivity` should construct `MockRailSdk`, wrap it in `MockRailSdkAdapter`, pass the adapter into `NativeBridge`, and call adapter initialization during screen setup before loading the page.

**When to use:** Always. D-04 locks construction and initialization ownership in `WebViewActivity`.

**Example:**
```java
// Source: project-constrained pattern based on current WebViewActivity lifecycle and
// Android JS bridge guidance:
// https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge
MockRailSdk sdk = new MockRailSdk();
RailPlusSdkAdapter adapter = new MockRailSdkAdapter(sdk);

nativeBridge = new NativeBridge(webView, adapter, errorLogger);
webView.addJavascriptInterface(nativeBridge, "RailBridge");

adapter.initialize(new RailPlusSdkAdapter.Callback<Boolean>() {
    @Override
    public void onSuccess(Boolean ignored) {
        Log.d(TAG, "SDK initialized successfully");
    }

    @Override
    public void onError(SdkErrorCode errorCode) {
        Log.e(TAG, "SDK initialization failed: " + errorCode);
    }
});
```

### Pattern 2: Adapter Hides Concrete Callback and Model Types
**What:** `NativeBridge` should know only `RailPlusSdkAdapter`, `SdkErrorCode`, and SDK-neutral result models. It should never import `MockRailSdk` or `MockRailSdk.BalanceResult`.

**When to use:** For all bridge-to-SDK calls and status reads.

**Example:**
```java
// Source: project-constrained seam pattern derived from current NativeBridge call flow
public interface RailPlusSdkAdapter {
    void initialize(Callback<Boolean> callback);
    void requestCharge(String cardId, int amount, Callback<ChargeResult> callback);
    void getBalance(String cardId, Callback<BalanceSnapshot> callback);
    SdkStatusSnapshot getStatus();
    void shutdown();

    interface Callback<T> {
        void onSuccess(T result);
        void onError(SdkErrorCode errorCode);
    }
}
```

### Pattern 3: Single Response Builder For Compatibility Plus Metadata
**What:** Build one internal helper for base bridge responses so success, error, and status payloads all keep `status`, `method`, `data/error`, and `retryCount`, while additive metadata is applied consistently.

**When to use:** Any bridge response path that ends in `window.onBridgeResult(...)`.

**Example:**
```java
// Source: adapted to the current bridge contract and Android requirement that
// evaluateJavascript() runs on the UI thread:
// https://developer.android.com/reference/android/webkit/WebView
private JSONObject createBaseResponse(String method, int retryCount, long startedAtMs)
        throws JSONException {
    JSONObject response = new JSONObject();
    response.put("method", method);
    response.put("retryCount", retryCount);
    response.put("platform", "android");
    response.put("durationMs", System.currentTimeMillis() - startedAtMs);
    return response;
}
```

### Anti-Patterns to Avoid
- **Reflection-style bridge dispatcher:** Do not replace the four explicit `@JavascriptInterface` methods with a generic `invoke(method, payload)` API. It widens the exposed JS surface and violates D-07.
- **Concrete SDK types escaping the seam:** If `NativeBridge` still imports `MockRailSdk` or its nested result classes, the seam is cosmetic only.
- **Retry ownership moved into the adapter now:** D-06 explicitly keeps retries in the bridge layer for this phase.
- **Bridge transport rewrite:** Do not switch to `addWebMessageListener`, `postWebMessage`, or a new JS callback protocol in Phase 2.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| SDK boundary | A generic stringly typed dispatch map | A typed `RailPlusSdkAdapter` interface | The planner needs a credible future vendor seam, not another JSON router |
| Balance/status seam | SDK-specific nested result classes in bridge code | Adapter-owned immutable value objects | Otherwise the concrete SDK still leaks through the seam |
| Response serialization | Ad hoc string concatenation per method | Shared `JSONObject`-based response helper | Contract drift is already easy to introduce; metadata makes duplication worse |
| UI-thread hops | Per-callback manual thread juggling | Existing `mainHandler` for JS posting and existing `RetryHandler` | Android docs require `evaluateJavascript()` on the UI thread and `@JavascriptInterface` methods are not invoked there |
| Transport modernization | A new JS bridge mechanism in this phase | Keep `addJavascriptInterface` and current `window.onBridgeResult` | Phase 2 is about decoupling, not a protocol migration |

**Key insight:** The hard part here is not creating an interface. The hard part is keeping the seam real while preserving a legacy-style WebView contract that already has threading and compatibility constraints. Typed models and one response factory matter more than adding another abstraction layer.

## Common Pitfalls

### Pitfall 1: Assuming Bridge Methods Run On The UI Thread
**What goes wrong:** Planner moves parsing, logging, or UI-touching work into code paths that assume main-thread execution.

**Why it happens:** `addJavascriptInterface` looks simple, but Android's current docs state the system calls those methods on a background thread, while `evaluateJavascript()` must run on the UI thread.

**How to avoid:** Keep JS callback posting centralized through the existing `mainHandler`. Do not touch `WebView` directly from adapter callbacks or `@JavascriptInterface` entrypoints.

**Warning signs:** New code touches `webView`, `View`, or `Activity` state outside `postResultToJs(...)`.

### Pitfall 2: Contract Drift Between Success And Error Payloads
**What goes wrong:** Phase 2 adds metadata to success responses but forgets error/status paths, or removes fields the current HTML handler reads.

**Why it happens:** Current code already has asymmetry: success responses carry top-level `retryCount`, while the error UI currently reads `error.retryCount`.

**How to avoid:** Use one base response helper and keep nested error details for compatibility. If top-level `retryCount` is added to error responses, keep `error.retryCount` too.

**Warning signs:** Different methods build JSON independently or the planner describes metadata only for "successful calls".

### Pitfall 3: A Fake Seam That Still Depends On `MockRailSdk`
**What goes wrong:** `NativeBridge` stops taking a `MockRailSdk` constructor parameter but still imports `MockRailSdk.BalanceResult` or calls concrete methods in helper code.

**Why it happens:** Nested concrete result classes make the remaining coupling easy to miss.

**How to avoid:** Introduce adapter-owned balance/status models or promote them out of the concrete SDK package before rewiring `NativeBridge`.

**Warning signs:** `NativeBridge` still imports `MockRailSdk` after the refactor.

### Pitfall 4: Expanding The JavaScript Attack Surface
**What goes wrong:** Planner adds extra annotated helper methods or a generic bridge entrypoint because "it is only a demo".

**Why it happens:** `addJavascriptInterface` is easy to extend, but Android documents it as a legacy API with no origin-based access control and exposure across frames.

**How to avoid:** Keep only the four public bridge entrypoints annotated with `@JavascriptInterface`. Keep helpers private and unannotated.

**Warning signs:** New public methods appear in `NativeBridge` and are annotated for JavaScript access.

## Code Examples

Verified patterns from official sources and current repo constraints:

### Adapter Wrapper Around `MockRailSdk`
```java
// Source: adapted to current repo flow plus Android JS bridge guidance:
// https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge
public final class MockRailSdkAdapter implements RailPlusSdkAdapter {
    private final MockRailSdk sdk;

    public MockRailSdkAdapter(MockRailSdk sdk) {
        this.sdk = sdk;
    }

    @Override
    public void initialize(Callback<Boolean> callback) {
        sdk.initialize(new MockRailSdk.SdkCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(SdkErrorCode errorCode) {
                callback.onError(errorCode);
            }
        });
    }

    @Override
    public void requestCharge(String cardId, int amount, Callback<ChargeResult> callback) {
        sdk.charge(cardId, amount, new MockRailSdk.SdkCallback<ChargeResult>() {
            @Override
            public void onSuccess(ChargeResult result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(SdkErrorCode errorCode) {
                callback.onError(errorCode);
            }
        });
    }
}
```

### Bridge Retry Task Using Adapter Instead Of Concrete SDK
```java
// Source: adapted from current NativeBridge + RetryHandler shape in repo
retryHandler.execute(new RetryHandler.RetryTask() {
    @Override
    public void run(
            RetryHandler.OnSuccessCallback onSuccess,
            RetryHandler.OnErrorCallback onError
    ) {
        adapter.requestCharge(cardId, amount, new RailPlusSdkAdapter.Callback<ChargeResult>() {
            @Override
            public void onSuccess(ChargeResult result) {
                onSuccess.onResult(buildChargeSuccessJson(result, requestMeta));
            }

            @Override
            public void onError(SdkErrorCode errorCode) {
                onError.onError(errorCode);
            }
        });
    }

    @Override
    public String getMethodName() {
        return "requestCharge";
    }
}, retryCallback);
```

### UI-Safe JS Callback Posting
```java
// Source: Android WebView docs:
// https://developer.android.com/reference/android/webkit/WebView
private void postResultToJs(String resultJson) {
    mainHandler.post(() -> {
        if (destroyed) {
            return;
        }
        webView.evaluateJavascript(
                "window.onBridgeResult(" + JSONObject.quote(resultJson) + ")",
                null
        );
    });
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `addJavascriptInterface` as the default bridge choice | `addWebMessageListener` is the Android-recommended modern bridge API | Android docs updated by 2026-03-17 | Keep the legacy bridge only because Phase 2 is contract-preserving, not a transport rewrite |
| Bridge directly calling a concrete SDK | Bridge depends on an adapter interface with SDK-neutral models | Current engineering standard; required by `SDK-01` | Enables future vendor swap and narrower unit tests |

**Deprecated/outdated:**
- Treating `MockRailSdk.BalanceResult` as a bridge type: it keeps the concrete SDK visible across the seam.
- Adding extra `@JavascriptInterface` methods for convenience: Android explicitly treats this bridge style as legacy and lower-security.

## Open Questions

1. **Should status be modeled as a value object or built inline?**
   - What we know: D-01 requires a `getStatus` adapter operation, and D-05 keeps JSON construction inside `NativeBridge`.
   - What's unclear: whether `getStatus()` returns a small immutable `SdkStatusSnapshot` or a looser map/JSON object.
   - Recommendation: use a tiny value object (`initialized`, `version`) so the adapter stays typed and the bridge remains the only JSON builder.

2. **How should error `retryCount` be represented during the compatibility transition?**
   - What we know: requirements say `retryCount` remains intact, but current HTML reads `error.retryCount` while success payloads use top-level `retryCount`.
   - What's unclear: whether the planner wants Phase 2 to normalize the top-level field immediately.
   - Recommendation: preserve the nested `error.retryCount` field for compatibility and add top-level `retryCount` only as additive metadata.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | Gradle build and tests | yes | 25.0.1 | none |
| Gradle Wrapper | Build and test execution | yes | Gradle 8.2 | none |
| Android SDK toolchain | `assembleDebug` build validation | yes | SDK present via Gradle; exact CLI version not probed | Android Studio sync/build |
| `adb` | Terminal-driven emulator/instrumentation validation | no | none | Manual validation via Android Studio/emulator UI |

**Missing dependencies with no fallback:**
- None for Phase 2 implementation. The app builds successfully with `.\gradlew.bat :app:assembleDebug`.

**Missing dependencies with fallback:**
- `adb` is not on `PATH`, so terminal-driven device smoke tests are blocked. Use Android Studio/emulator UI for manual four-button validation.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + AndroidX instrumentation runner 1.1.5 + Espresso 3.5.1 |
| Config file | none - configuration is in `app/build.gradle`; `.planning/config.json` is absent so validation is treated as enabled |
| Quick run command | `.\gradlew.bat :app:testDebugUnitTest` |
| Full suite command | `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SDK-01 | `NativeBridge` depends on `RailPlusSdkAdapter`, not `MockRailSdk` | unit + code review gate | `.\gradlew.bat :app:testDebugUnitTest` | no - Wave 0 |
| SDK-02 | `MockRailSdkAdapter` preserves current mock behavior | unit | `.\gradlew.bat :app:testDebugUnitTest` | no - Wave 0 |
| BRG-01 | Public bridge methods remain unchanged and callable from the WebView page | manual smoke | `.\gradlew.bat :app:assembleDebug` | no - manual only |
| BRG-02 | Response payloads keep current fields and add metadata without breaking the page | unit for JSON helper + manual smoke | `.\gradlew.bat :app:testDebugUnitTest` | no - Wave 0 |

### Sampling Rate
- **Per task commit:** `.\gradlew.bat :app:testDebugUnitTest`
- **Per wave merge:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest`
- **Phase gate:** Build green plus manual validation of the existing four WebView actions in the emulator

### Wave 0 Gaps
- [ ] `app/src/test/java/com/demo/railbridge/sdk/MockRailSdkAdapterTest.java` - verifies adapter forwards success/error paths without leaking concrete types
- [ ] `app/src/test/java/com/demo/railbridge/bridge/BridgeResponseFactoryTest.java` - verifies success/error/status JSON compatibility plus additive metadata
- [ ] `app/src/test/java/com/demo/railbridge/bridge/NativeBridgeAdapterIntegrationTest.java` or equivalent seam-focused JVM test - verifies retry task wiring against a fake adapter
- [ ] Terminal device automation dependency: restore `adb` on `PATH` if CLI instrumentation or scripted emulator smoke tests are required

## Sources

### Primary (HIGH confidence)
- Repository inspection:
  - `app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java`
  - `app/src/main/java/com/demo/railbridge/WebViewActivity.java`
  - `app/src/main/java/com/demo/railbridge/sdk/MockRailSdk.java`
  - `app/src/main/java/com/demo/railbridge/retry/RetryHandler.java`
  - `app/src/main/assets/webview/index.html`
  - `app/build.gradle`
  - `gradle.properties`
- Android Developers, "Access native APIs with JavaScript bridge" - verified current bridge generations, threading, security, and recommendation status:
  - https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge
- Android Developers, `JavascriptInterface` API reference - verified annotation exposure rules:
  - https://developer.android.com/reference/android/webkit/JavascriptInterface
- Android Developers, `WebView.evaluateJavascript` reference - verified UI-thread requirement:
  - https://developer.android.com/reference/android/webkit/WebView

### Secondary (MEDIUM confidence)
- None. Critical claims were verified from the current repo and Android official docs.

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - mostly repo-pinned and intentionally avoids new dependencies
- Architecture: MEDIUM - adapter and response-helper recommendations are strongly supported by repo structure, but exact class naming/package placement remains discretionary
- Pitfalls: HIGH - directly supported by current code asymmetries and Android official bridge/threading/security docs

**Research date:** 2026-04-06
**Valid until:** 2026-05-06
