---
phase: 05
slug: ios-parity-demo
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-08
---

# Phase 05 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | XCTest for pure Swift contract, scenario, and request-ownership tests plus manual iOS simulator smoke |
| **Config file** | `ios/RailBridgeIOS/RailBridgeIOS.xcodeproj/project.pbxproj` |
| **Quick run command** | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/BridgeResponseFactoryTests` |
| **Full suite command** | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16"` |
| **Estimated runtime** | Targeted loop: ~30-60 seconds; full suite plus simulator smoke: ~2-5 minutes |

**Constraint:** iOS build and simulator verification require a macOS/Xcode-capable environment. This Windows workspace can plan and edit the repo, but actual `xcodebuild` and simulator checks must run on a macOS machine or through the Build iOS Apps plugin environment.

---

## Sampling Rate

- **After 05-01 Task 1:** Run `xcodebuild build -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "generic/platform=iOS Simulator"`
- **After 05-01 Task 2:** Run `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/BridgeResponseFactoryTests`
- **After 05-02 Task 1:** Run `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/MockRailSdkAdapterTests -only-testing:RailBridgeIOSTests/BridgeRequestCoordinatorTests -only-testing:RailBridgeIOSTests/DiagnosticsSnapshotTests`
- **Before the final manual checkpoint:** Run `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16"`
- **Before `$gsd-verify-work`:** Full XCTest suite must be green and simulator smoke for the preserved four-action flow plus diagnostics parity must be recorded
- **Max feedback latency:** 5 minutes

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | IOS-01 | build | `xcodebuild build -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "generic/platform=iOS Simulator"` | no (Wave 0) | pending |
| 05-01-02 | 01 | 1 | IOS-01, IOS-02 | XCTest | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/BridgeResponseFactoryTests` | no (Wave 0) | pending |
| 05-02-01 | 02 | 2 | IOS-02 | XCTest | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16" -only-testing:RailBridgeIOSTests/MockRailSdkAdapterTests -only-testing:RailBridgeIOSTests/BridgeRequestCoordinatorTests -only-testing:RailBridgeIOSTests/DiagnosticsSnapshotTests` | no (Wave 0) | pending |
| 05-02-02 | 02 | 2 | IOS-01, IOS-02 | XCTest + simulator smoke gate | `xcodebuild test -project ios/RailBridgeIOS/RailBridgeIOS.xcodeproj -scheme RailBridgeIOS -destination "platform=iOS Simulator,name=iPhone 16"` | yes | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- Create an Xcode app target at `ios/RailBridgeIOS/RailBridgeIOS.xcodeproj` and a test target `RailBridgeIOSTests`.
- Add `RailBridgeIOSTests/BridgeResponseFactoryTests.swift` to lock the iOS response JSON shape to the Android field contract.
- Add `RailBridgeIOSTests/MockRailSdkAdapterTests.swift` for deterministic scenario preset behavior.
- Add `RailBridgeIOSTests/BridgeRequestCoordinatorTests.swift` for duplicate suppression, timeout single-fire, and teardown abandonment.
- Add `RailBridgeIOSTests/DiagnosticsSnapshotTests.swift` for structured diagnostics payload shape and `inFlightRequests` visibility.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| The iOS app launches and opens the demo page from a lightweight start screen | IOS-01 | Requires a running simulator and actual app navigation | Launch the app, tap `Start demo`, and confirm the WebView screen loads the diagnostics page |
| The original four action buttons remain present and usable on iOS | IOS-01, IOS-02 | Requires the real `WKWebView` page and injected bridge shim | Trigger `Request charge`, `Get balance`, `SDK status`, and `Report JS error` from the simulator |
| `duplicate_callback` yields one visible result plus ignored-duplicate diagnostics evidence | IOS-02 | Timing-sensitive interaction spanning native bridge, mock adapter, and page rendering | Select `duplicate_callback`, trigger `Request charge`, and confirm only one visible success appears while diagnostics show duplicate-ignore evidence |
| `callback_loss` times out instead of hanging forever and leaves no orphaned in-flight request | IOS-02 | Needs the real timeout watchdog and page update loop | Select `callback_loss`, trigger a request, wait for timeout, and confirm the page shows a timeout error and diagnostics clear pending ownership |
| Leaving the WebView screen during a pending request does not crash or deliver a stale callback to the closed screen | IOS-02 | Requires real lifecycle interaction with navigation or dismissal | Start a pending request, leave the screen before completion, wait past the timeout window, and confirm there is no crash or stale-screen callback |

---

## Validation Sign-Off

- [x] All tasks have an automated verify or an automated pre-checkpoint gate
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 gaps are explicit
- [x] No watch-mode flags
- [x] Feedback latency <= 5 minutes for task-level loops
- [x] `nyquist_compliant: true` set in frontmatter
- [x] macOS/Xcode dependency is explicit and not misrepresented as locally runnable in the current Windows workspace

**Approval:** ready for execution
