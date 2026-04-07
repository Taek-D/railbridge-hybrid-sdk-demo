# Phase 5: iOS Parity Demo - Research

**Researched:** 2026-04-08
**Domain:** iOS `WKWebView` parity for the existing Android hybrid SDK stabilization demo
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** The iOS demo must live under `ios/` as a new Xcode-openable project inside the repo.
- **D-02:** The iOS app is a sibling demo, not a repo split or Android project rewrite.
- **D-03:** The iOS app should stay lightweight and mirror the current Android demo shape rather than widen into a product-style app.
- **D-04:** Phase 5 is parity validation only, not production release hardening.
- **D-05:** The existing HTML/JS diagnostics page should be reused as much as possible.
- **D-06:** The public iOS bridge methods must mirror Android: `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`.
- **D-07:** The iOS result payload should preserve the same baseline fields and additive diagnostics metadata model already used on Android.
- **D-08:** Verification should focus on simulator execution and core smoke flows rather than a heavy iOS automation harness.
- **D-09:** Simulator smoke should prove the same high-value flows as Android: four-action flow, scenario switching, structured diagnostics visibility, and comparable success/error payloads.

### Planner Discretion
- Exact Xcode project name and folder structure under `ios/`
- Swift type layout for bridge, adapter, scenario, logging, and ownership seams
- Whether the bundled WebView asset is copied from Android or generated from the Android source on demand
- The smallest useful XCTest surface for pure Swift components

### Deferred Ideas (Out of Scope)
- Full XCTest/UI automation beyond smoke-level parity
- Shared Android/iOS abstraction layers
- Portfolio storytelling and artifact packaging, which belong to Phase 6
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| IOS-01 | A runnable iOS demo exists using `WKWebView + Swift bridge + mock adapter`. | Create an Xcode-openable iOS app under `ios/` with a lightweight shell, bundled diagnostics page, and a Swift bridge/adapter stack that can run in the simulator. |
| IOS-02 | The iOS bridge methods and log field model match the Android contract closely enough for side-by-side portfolio comparison. | Mirror Android method names, response field names, scenario preset names, request-timeline fields, and hardening states in pure Swift types, then verify on the simulator. |
</phase_requirements>

## Summary

Phase 5 should mirror the Android architecture, not abstract it away. The strongest parity story is: same operator-facing WebView page, same four business methods, same scenario names, same diagnostics/export shape, and the same request-ownership rules implemented in native Swift types behind `WKWebView`.

The main technical difference from Android is transport. Android can expose synchronous methods directly through `addJavascriptInterface`, but `WKWebView` uses message handlers. The safest parity strategy is to inject a `window.RailBridge` shim at document start so the page still calls `RailBridge.requestCharge(...)`, `RailBridge.getBalance(...)`, `RailBridge.getSdkStatus()`, and `RailBridge.reportError(...)`. That shim should forward messages to `window.webkit.messageHandlers.railBridge`. Native-to-JS delivery should stay aligned with Android by calling `window.onBridgeResult(...)` through `evaluateJavaScript(...)`.

The diagnostics helpers are the tricky part because the current page expects synchronous reads such as `RailBridge.getDiagnosticsSnapshot()`. The lowest-risk parity approach is to keep a JS-side cache string, update it from native whenever diagnostics state changes, and let the iOS-injected `RailBridge.getDiagnosticsSnapshot()` return the cached snapshot synchronously. That preserves the current page behavior without forcing Android contract churn.

**Primary recommendation:** Build `ios/RailBridgeIOS` as a lightweight app with a copied or synchronized diagnostics page in the app bundle, an injected `window.RailBridge` shim, pure Swift mirror types for adapter/scenario/logging/ownership, and simulator-first smoke verification.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Swift + Xcode-managed iOS toolchain | Current Xcode toolchain | Native iOS implementation | Required for a runnable parity demo |
| `WKWebView`, `WKUserContentController`, `WKUserScript`, `WKScriptMessageHandler` | iOS SDK | JS/native bridge transport | Standard iOS replacement for Android `JavascriptInterface` |
| `Foundation` + `Codable` or `JSONSerialization` | iOS SDK | Result payload and diagnostics JSON encoding | Keeps Android-shaped JSON contracts stable without extra dependencies |
| `DispatchQueue.main` + native timers | iOS SDK | JS callback delivery and timeout watchdogs | Mirrors Android main-thread and timeout ownership behavior |
| `XCTest` | Xcode standard | Pure Swift unit coverage for contract, scenario, and ownership seams | Lightest built-in test surface for parity work |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| SwiftUI app shell | iOS SDK | Minimal app entry and start screen | Good fit for a lightweight parity demo |
| `UIKit` wrappers around `WKWebView` | iOS SDK | WebView hosting and delegate control | Use where `WKWebView` lifecycle and delegates are clearer than a pure SwiftUI wrapper |
| `OSLog` | iOS SDK | Local debug traces during simulator smoke | Optional support during execution, not a parity requirement |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Injected `window.RailBridge` shim | Rewrite the HTML page around `window.webkit.messageHandlers` directly | More iOS-native, but breaks the “same page, same contract” parity story |
| JS-side cached diagnostics snapshot | `WKScriptMessageHandlerWithReply` or async promise-only helper APIs | Cleaner async model, but would force current page contract changes or a larger page fork |
| Bundled iOS copy of `index.html` seeded from Android | Move Android and iOS to a fully shared cross-platform web asset path | Better long-term deduplication, but risks disturbing the Android baseline during parity work |

## Architecture Patterns

### Recommended Project Structure
```text
ios/RailBridgeIOS/
|-- RailBridgeIOS.xcodeproj/
|-- RailBridgeIOS/
|   |-- RailBridgeIOSApp.swift
|   |-- MainView.swift
|   |-- WebViewDemoView.swift
|   |-- Bridge/
|   |   |-- IOSNativeBridge.swift
|   |   |-- BridgeResponseFactory.swift
|   |   `-- BridgeRequestCoordinator.swift
|   |-- SDK/
|   |   |-- RailPlusSdkAdapter.swift
|   |   |-- MockRailSdkAdapter.swift
|   |   |-- ScenarioPreset.swift
|   |   `-- ScenarioController.swift
|   |-- Logging/
|   |   |-- TimelineEvent.swift
|   |   |-- RequestTimeline.swift
|   |   `-- DiagnosticsSnapshot.swift
|   `-- Resources/
|       `-- webview/
|           `-- index.html
`-- RailBridgeIOSTests/
```

### Pattern 1: Injected `window.RailBridge` Shim
**What:** Add a `WKUserScript` at document start that defines a `window.RailBridge` object exposing the same public methods as Android and forwarding calls to a single script message handler.

**When to use:** For all JS-to-native entrypoints, including diagnostics-only helpers.

**Example shape:**
```javascript
window.__railBridgeDiagnosticsSnapshot = window.__railBridgeDiagnosticsSnapshot || "";
window.RailBridge = {
  requestCharge: (paramsJson) => window.webkit.messageHandlers.railBridge.postMessage({ method: "requestCharge", payload: paramsJson }),
  getBalance: (paramsJson) => window.webkit.messageHandlers.railBridge.postMessage({ method: "getBalance", payload: paramsJson }),
  getSdkStatus: () => window.webkit.messageHandlers.railBridge.postMessage({ method: "getSdkStatus" }),
  reportError: (errorJson) => window.webkit.messageHandlers.railBridge.postMessage({ method: "reportError", payload: errorJson }),
  setScenarioPreset: (preset) => window.webkit.messageHandlers.railBridge.postMessage({ method: "setScenarioPreset", payload: preset }),
  clearDiagnostics: () => window.webkit.messageHandlers.railBridge.postMessage({ method: "clearDiagnostics" }),
  getDiagnosticsSnapshot: () => window.__railBridgeDiagnosticsSnapshot,
  exportDiagnostics: () => window.__railBridgeDiagnosticsExport || window.__railBridgeDiagnosticsSnapshot
};
```

### Pattern 2: Native-Pushed Diagnostics Cache
**What:** Whenever native diagnostics state changes, push the serialized snapshot back into JS with `evaluateJavaScript(...)` so the page’s synchronous helper reads still work.

**When to use:** After scenario changes, timeline updates, timeout conversion, duplicate suppression, or clearing diagnostics.

**Example shape:**
```swift
let escaped = diagnosticsJson
    .replacingOccurrences(of: "\\", with: "\\\\")
    .replacingOccurrences(of: "'", with: "\\'")
let script = """
window.__railBridgeDiagnosticsSnapshot = '\(escaped)';
window.__railBridgeDiagnosticsExport = '\(escaped)';
window.dispatchEvent(new Event('bridgeReady'));
"""
webView.evaluateJavaScript(script, completionHandler: nil)
```

### Pattern 3: Pure Swift Mirror Types for Android Contracts
**What:** Mirror Android’s adapter, scenario, timeline, response, and request-ownership types in pure Swift rather than trying to share code across platforms.

**When to use:** For all parity-sensitive contracts that must stay comparable between Android and iOS.

**Examples to mirror exactly:**
- Scenario preset values: `normal`, `timeout`, `internal_error`, `callback_loss`, `duplicate_callback`, `retry_exhausted`
- Request ownership states: `pending`, `success`, `error`, `timed_out`, `abandoned`
- Diagnostics payload fields: `callbackId`, `correlationId`, `platform`, `stage`, `durationMs`, `scenario`, `vendorCode`, `retryable`, `resolvedByRetry`, `inFlightRequests`

### Anti-Patterns to Avoid
- Rewriting the page around an iOS-only API and letting the visible workflow drift from Android
- Treating iOS parity as storyboard-heavy or production-app UI work
- Hiding diagnostics-only behavior inside the page without a native source of truth
- Making the iOS mock adapter emit different scenario names or different terminal states than Android
- Skipping timeout/duplicate/teardown ownership on iOS and claiming parity anyway

## Common Pitfalls

### Pitfall 1: Assuming Android-style synchronous bridge returns exist on iOS
**What goes wrong:** The page calls `RailBridge.getDiagnosticsSnapshot()` and gets `undefined` or stale data because `WKScriptMessageHandler` is async.

**How to avoid:** Use an injected JS shim with a native-pushed diagnostics cache so synchronous page reads return the latest serialized snapshot.

### Pitfall 2: Letting the bundled HTML drift from Android
**What goes wrong:** Android and iOS become visually or behaviorally incomparable even though the native bridge names match.

**How to avoid:** Treat Android `index.html` as the source of truth and keep any iOS-specific edits confined to the injected shim or a narrowly scoped copy step.

### Pitfall 3: Bridging everything through view code
**What goes wrong:** Ownership, timeout, and diagnostics behavior become impossible to unit test.

**How to avoid:** Keep scenario, response, logging, and request ownership in pure Swift types with lightweight view/controller wrappers.

### Pitfall 4: Planning simulator verification without a macOS/Xcode path
**What goes wrong:** Execution later claims a “runnable iOS demo” without any realistic way to build or launch it from the current environment.

**How to avoid:** Plan explicit `xcodebuild` or Build iOS Apps plugin verification steps and record that execution requires a macOS/Xcode-capable environment.

## Open Questions Resolved for Planning

1. **Where the iOS app lives**
   - Locked: `ios/` subtree, new Xcode-openable project, no repo split.

2. **How close the page should stay to Android**
   - Locked: reuse the existing page as much as practical, with iOS transport differences handled through injected bridge/bootstrap code instead of a UI rewrite.

3. **How to preserve synchronous diagnostics reads**
   - Recommendation locked for planning: maintain a JS-side diagnostics cache that native updates whenever the snapshot changes.

4. **How deep verification should go**
   - Locked: simulator smoke and core parity checks, not a heavy UI automation phase.

## Environment Availability

| Dependency | Required By | Available Here | Version | Fallback |
|------------|------------|----------------|---------|----------|
| Xcode + iOS Simulator | Build and run `RailBridgeIOS` | no (current Windows workspace) | n/a | Execute on a macOS/Xcode-capable environment or via the Build iOS Apps plugin session |
| Swift toolchain | iOS source compilation | no (current Windows workspace) | n/a | Use Xcode-managed toolchain during execution |
| Git | Planning doc workflow | yes | current repo initialized | none |
| Existing Android diagnostics baseline | Contract parity reference | yes | current repo state | none |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | XCTest for pure Swift contract/seam tests plus manual iOS simulator smoke |
| Quick run command | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/BridgeResponseFactoryTests` |
| Full suite command | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16"` |
| Special constraint | Requires macOS with Xcode and a booted simulator; cannot be executed from the current Windows-only workspace |

### Requirement-to-Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| IOS-01 | Xcode project builds, launches, and loads the bundled diagnostics page in `WKWebView` | build + simulator smoke | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16"` | Wave 0 |
| IOS-02 | Public method names, scenario preset values, diagnostics fields, and timeout/duplicate handling match Android closely enough for side-by-side comparison | XCTest + simulator smoke | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/BridgeResponseFactoryTests -only-testing:RailBridgeIOSTests/BridgeRequestCoordinatorTests` | Wave 0 |

## Sources

### Primary
- `.planning/phases/05-ios-parity-demo/05-CONTEXT.md`
- `.planning/phases/04-android-bridge-hardening/04-VERIFICATION.md`
- `app/src/main/assets/webview/index.html`
- `app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java`
- `app/src/main/java/com/demo/railbridge/bridge/BridgeResponseFactory.java`
- `app/src/main/java/com/demo/railbridge/bridge/BridgeRequestCoordinator.java`
- `app/src/main/java/com/demo/railbridge/sdk/RailPlusSdkAdapter.java`
- `app/src/main/java/com/demo/railbridge/sdk/MockRailSdkAdapter.java`

### Secondary
- `README.md`
- `android-webview-bridge-demo-prompt.md`
- `Android Java WebView Bridge + SDK 에러 핸들링 데모 기획서 —  33ac1e7ae37981988f27c387eb2a3807.md`

## Metadata

**Confidence breakdown**
- Stack selection: HIGH
- Bridge transport strategy: MEDIUM
- Execution environment assumptions: HIGH

**Research date:** 2026-04-08
**Valid until:** 2026-05-08
