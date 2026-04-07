# Phase 5: iOS Parity Demo - Context

**Gathered:** 2026-04-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Add a runnable iOS demo that mirrors the Android hybrid SDK stabilization sample with `WKWebView + Swift bridge + mock adapter`. This phase creates a parity-validation app, not a production iOS client. The goal is to demonstrate the same bridge contract, scenario-driven diagnostics story, and troubleshooting approach on a second native platform without disturbing the existing Android baseline.

</domain>

<decisions>
## Implementation Decisions

### iOS project placement
- **D-01:** Create the iOS demo under `ios/` as a new Xcode-openable project inside the same repository.
- **D-02:** Keep the iOS demo as a sibling platform sample, not a restructuring of the Android project or a repo split.

### Parity depth
- **D-03:** The iOS app should match the Android demo shape closely: a lightweight shell that leads into a WebView diagnostics screen rather than a broader product-style app.
- **D-04:** Phase 5 is parity validation only. It should prove the same architecture and debugging approach, not attempt production release hardening beyond what is needed to run and compare.

### Shared WebView and bridge contract
- **D-05:** Reuse the existing HTML/JS diagnostics page as much as possible so Android and iOS can be compared through the same operator-facing surface.
- **D-06:** The public iOS bridge methods should mirror Android: `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`.
- **D-07:** The iOS result payload should preserve the same baseline fields and additive diagnostics metadata model already established on Android.

### Verification strategy
- **D-08:** iOS verification should focus on simulator execution and core smoke coverage rather than introducing a heavy test harness in this phase.
- **D-09:** The parity smoke should prove the same high-value flows as Android: preserved four-action flow, deterministic scenario switching, structured diagnostics visibility, and comparable error/success payloads.

### the agent's Discretion
- Exact Xcode project naming under `ios/`
- Whether the iOS entry flow uses one or two view controllers before the `WKWebView` screen, as long as the resulting demo stays lightweight
- The smallest viable Swift type decomposition for bridge, adapter, scenario control, and diagnostics support
- How much HTML/JS is physically shared vs copied, as long as behavior stays aligned and future documentation can compare the platforms clearly

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone and phase framing
- `.planning/PROJECT.md` - Portfolio goal, platform-parity positioning, and repo-level constraints
- `.planning/REQUIREMENTS.md` - In-scope requirements for `IOS-01` and `IOS-02`
- `.planning/ROADMAP.md` - Phase 5 goal, success criteria, and follow-on documentation phase
- `.planning/STATE.md` - Current milestone baseline after Android hardening completed

### Prior phase decisions
- `.planning/phases/02-android-adapter-seam/02-CONTEXT.md` - Locked bridge contract and adapter seam rules that iOS should mirror
- `.planning/phases/03-logging-and-failure-simulation/03-CONTEXT.md` - Locked scenario, diagnostics, and export model that iOS should match
- `.planning/phases/03-logging-and-failure-simulation/03-VERIFICATION.md` - Verified Android diagnostics behaviors that parity should preserve conceptually
- `.planning/phases/04-android-bridge-hardening/04-01-SUMMARY.md` - Android hardening outcomes and ownership model to mirror on iOS
- `.planning/phases/04-android-bridge-hardening/04-VERIFICATION.md` - Runtime evidence for duplicate suppression, timeout conversion, and teardown-safe behavior

### Current Android implementation baseline
- `app/src/main/java/com/demo/railbridge/WebViewActivity.java` - Android WebView composition and bridge injection baseline
- `app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java` - Current bridge contract, diagnostics helpers, and hardening behavior
- `app/src/main/java/com/demo/railbridge/bridge/BridgeResponseFactory.java` - Baseline response JSON shape and additive metadata rules
- `app/src/main/java/com/demo/railbridge/bridge/BridgeRequestCoordinator.java` - Request ownership model to conceptually mirror on iOS
- `app/src/main/java/com/demo/railbridge/sdk/RailPlusSdkAdapter.java` - Adapter seam contract already proven on Android
- `app/src/main/java/com/demo/railbridge/sdk/MockRailSdkAdapter.java` - Deterministic scenario model already proven on Android
- `app/src/main/java/com/demo/railbridge/sdk/ScenarioPreset.java` - Canonical scenario list to mirror on iOS
- `app/src/main/java/com/demo/railbridge/logging/ErrorLogger.java` - Diagnostics persistence and export entrypoints to conceptually mirror on iOS
- `app/src/main/assets/webview/index.html` - Shared diagnostics page that iOS should reuse as much as possible

### Repo-level reference material
- `android-webview-bridge-demo-prompt.md` - Original scaffold brief and intended demo scope
- `Android Java WebView Bridge + SDK 에러 핸들링 데모 기획서 —  33ac1e7ae37981988f27c387eb2a3807.md` - Portfolio/job-target framing that explains why iOS parity matters

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/assets/webview/index.html` already provides the operator-facing diagnostics workflow, so iOS should reuse or closely mirror this page instead of inventing a second UI.
- `BridgeResponseFactory`, `ScenarioPreset`, and the diagnostics metadata model already define the contract iOS needs to match.
- `MockRailSdkAdapter`, `BridgeRequestCoordinator`, and the request timeline/logging flow form the clearest source of parity requirements even if the Swift implementation uses different type names.

### Established Patterns
- The demo favors additive evolution over replacement, so iOS should mirror Android behavior without forcing Android abstractions into a shared cross-platform layer.
- The current architecture uses a thin activity/controller layer that wires WebView + bridge + adapter, which is a good fit for a lightweight iOS sample with `UIViewController`/`WKWebView`.
- Diagnostics remain on the same page as the main actions, so iOS parity should keep that operator workflow intact.

### Integration Points
- A new `ios/` subtree is the clean integration point for Phase 5 and avoids disturbing the Android Gradle project.
- The iOS WebView bridge should align to the Android HTML page contract first, then mirror adapter, scenario, and diagnostics flow behind it.
- Simulator smoke should be the main execution checkpoint because the repo currently has no iOS project or shared cross-platform test harness.

</code_context>

<specifics>
## Specific Ideas

- Keep the iOS sample obviously comparable to Android, so a reviewer can inspect the same scenario and diagnostics flow on both platforms without re-learning the UI.
- Favor shared or near-shared HTML diagnostics content over separate Android/iOS demo pages, because parity is the selling point.
- Treat Phase 5 as evidence that the troubleshooting architecture is portable across native platforms, not as proof of production iOS release readiness.

</specifics>

<deferred>
## Deferred Ideas

- Full iOS automated UI testing or XCTest-based regression infrastructure belongs to future work once the parity demo exists
- Shared cross-platform abstraction layers between Android and iOS are out of scope for this phase
- Portfolio copywriting, architecture write-up, and debugging-report packaging belong to Phase 6

</deferred>

---

*Phase: 05-ios-parity-demo*
*Context gathered: 2026-04-08*
