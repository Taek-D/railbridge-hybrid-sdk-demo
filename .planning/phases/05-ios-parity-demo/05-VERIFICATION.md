---
phase: 05-ios-parity-demo
verified: 2026-04-08T02:22:09+09:00
status: human_needed
score: 3/4 must-haves verified
---

# Phase 05: iOS Parity Demo Verification Report

**Phase Goal:** Add a second runnable platform that demonstrates the same stabilization pattern with `WKWebView` and Swift.  
**Verified:** 2026-04-08T02:22:09+09:00  
**Status:** human_needed  
**Re-verification:** No - initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | An Xcode-openable iOS project exists in the repo. | VERIFIED | `ios/RailBridgeIOS/RailBridgeIOS.xcodeproj/project.pbxproj` and a shared `RailBridgeIOS.xcscheme` now exist with app and test targets. |
| 2 | The iOS demo exposes the same four bridge actions as Android. | VERIFIED | `IOSNativeBridge.swift` injects `window.RailBridge` with `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`, and the bundled HTML page is the Android-derived diagnostics page. |
| 3 | Scenario controls and structured log fields mirror the Android design closely enough for comparison. | VERIFIED | Swift mirror types now include the Android preset list, `schemaVersion`, `timelines`, `inFlightRequests`, additive metadata fields, timeout handling, duplicate suppression, and teardown abandonment. |
| 4 | The running iOS demo has been smoke-tested on a simulator for parity. | HUMAN NEEDED | The current workspace is Windows-only and does not provide `xcodebuild`, `swift`, or an iOS simulator. Runtime parity still needs a macOS/Xcode smoke pass. |

**Score:** 3/4 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `ios/RailBridgeIOS/RailBridgeIOS.xcodeproj/project.pbxproj` | Runnable iOS app target and test target | VERIFIED | Contains `PRODUCT_BUNDLE_IDENTIFIER = com.demo.railbridge.iosdemo;`, `IPHONEOS_DEPLOYMENT_TARGET = 16.0;`, app target, and test target. |
| `ios/RailBridgeIOS/RailBridgeIOS/MainView.swift` | Lightweight `Start demo` entry flow | VERIFIED | Provides the parity entry screen and navigation into the WebView demo. |
| `ios/RailBridgeIOS/RailBridgeIOS/WebViewDemoView.swift` | `WKWebView` host for the bundled diagnostics page | VERIFIED | Loads the bundled `index.html` from the app bundle and keeps bridge lifetime scoped to the screen. |
| `ios/RailBridgeIOS/RailBridgeIOS/Bridge/IOSNativeBridge.swift` | Scenario-aware bridge parity and diagnostics cache wiring | VERIFIED | Injects the JS shim, updates diagnostics caches, owns timeout/duplicate rules, and abandons pending requests on teardown. |
| `ios/RailBridgeIOS/RailBridgeIOSTests/*.swift` | Pure Swift parity coverage | VERIFIED | Covers bridge response payloads, callback loss, duplicate callbacks, timeout single-fire, and diagnostics payload fields. |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Project shell presence | PowerShell static checks on project, views, bridge, and HTML | All required strings and files were found | PASS |
| iOS build tooling availability | `Get-Command swift`, `Get-Command xcodebuild` | Both unavailable in this environment | BLOCKED |
| Simulator smoke | Xcode / iOS simulator run | Not executable from this Windows workspace | HUMAN NEEDED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| `IOS-01` | `05-01` | Add a runnable iOS parity demo app structure under `ios/` | SATISFIED (static) | App target, scheme, start screen, WebView host, and bundled page all exist. |
| `IOS-02` | `05-02` | Mirror Android bridge methods and diagnostics/logging contracts on iOS | SATISFIED (static) | The iOS bridge, response payloads, diagnostics payloads, scenario presets, and request ownership states match the Android vocabulary. |

## Human Verification Required

1. Build and run `ios/RailBridgeIOS/RailBridgeIOS.xcodeproj` in Xcode on macOS.
2. Tap `Start demo` and confirm the same four buttons appear in the WebView.
3. Verify `duplicate_callback` produces one visible success and ignored-duplicate diagnostics evidence.
4. Verify `callback_loss` produces a timeout after 5 seconds with no lingering `inFlightRequests`.
5. Verify leaving the WebView screen during a pending request does not crash or deliver stale callbacks.
6. Verify the exported detail still includes `schemaVersion`, `timelines`, and `inFlightRequests`.

## Gaps Summary

No structural gap was found in the code added for Phase 5. The remaining gap is environment-specific runtime validation on macOS/Xcode.

---

_Verified: 2026-04-08T02:22:09+09:00_  
_Verifier: Codex (gsd-execute-phase)_
