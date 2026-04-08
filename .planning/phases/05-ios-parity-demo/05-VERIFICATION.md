---
phase: 05-ios-parity-demo
verified: 2026-04-08T21:05:00+09:00
status: passed
score: 4/4 must-haves verified
---

# Phase 05: iOS Parity Demo Verification Report

**Phase Goal:** Add a second runnable platform that demonstrates the same stabilization pattern with `WKWebView` and Swift.  
**Verified:** 2026-04-08T21:05:00+09:00  
**Status:** passed  
**Re-verification:** Yes - runtime verification completed on macOS/Xcode

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | An Xcode-openable iOS project exists in the repo. | VERIFIED | `ios/RailBridgeIOS/RailBridgeIOS.xcodeproj/project.pbxproj` and a shared `RailBridgeIOS.xcscheme` now exist with app and test targets. |
| 2 | The iOS demo exposes the same four bridge actions as Android. | VERIFIED | `IOSNativeBridge.swift` injects `window.RailBridge` with `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`, and the bundled HTML page is the Android-derived diagnostics page. |
| 3 | Scenario controls and structured log fields mirror the Android design closely enough for comparison. | VERIFIED | Swift mirror types now include the Android preset list, `schemaVersion`, `timelines`, `inFlightRequests`, additive metadata fields, timeout handling, duplicate suppression, and teardown abandonment. |
| 4 | The running iOS demo has been smoke-tested on a simulator for parity. | VERIFIED | macOS/Xcode verification completed and produced captured evidence for home/start flow, bridge-connected demo screen, timeout recovery, callback-loss timeout evidence, duplicate callback suppression, and JS error acknowledgment under `artifacts/ios-portfolio-shots/`. |

**Score:** 4/4 truths verified

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
| Project shell presence | PowerShell static checks on project, views, bridge, and HTML | All required strings and files were found before handoff to macOS | PASS |
| Xcode runtime verification | macOS/Xcode build and simulator run | Completed on macOS after fixing test host/runtime wiring in `project.pbxproj` and automation hooks in `IOSNativeBridge.swift` | PASS |
| Portfolio evidence capture | iOS simulator screenshots | Captured six screenshots under `artifacts/ios-portfolio-shots/` covering start flow, bridge-connected demo, timeout recovery, callback-loss timeout, duplicate suppression, and JS error acknowledgment | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| `IOS-01` | `05-01` | Add a runnable iOS parity demo app structure under `ios/` | SATISFIED | App target, scheme, start screen, WebView host, and bundled page exist and were exercised in Xcode on macOS. |
| `IOS-02` | `05-02` | Mirror Android bridge methods and diagnostics/logging contracts on iOS | SATISFIED | The iOS bridge, response payloads, diagnostics payloads, scenario presets, and request ownership states match the Android vocabulary and were runtime-verified on simulator. |

## Runtime Verification Completed

The Phase 5 macOS/Xcode smoke pass covered:

1. Launching `RailBridgeIOS` and navigating from the home/start screen into the WebView demo.
2. Confirming the bridge-connected demo screen and preserved four-action flow.
3. Confirming retry recovery evidence for timeout behavior.
4. Confirming `callback_loss` transitions into timeout evidence rather than lingering in-flight state.
5. Confirming duplicate callback suppression with only one visible success plus diagnostics evidence.
6. Confirming JS error reporting and native acknowledgment on iOS.

## Gaps Summary

No Phase 5 gaps remain. The previous environment-specific runtime gap was closed on macOS/Xcode and supporting screenshot evidence is committed under `artifacts/ios-portfolio-shots/`.

---

_Verified: 2026-04-08T21:05:00+09:00_  
_Verifier: Codex (gsd-execute-phase)_
