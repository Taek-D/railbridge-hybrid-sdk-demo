---
phase: 05-ios-parity-demo
plan: 01
subsystem: ios
tags: [ios, swift, wkwebview, parity, bridge, xcode]
requires:
  - phase: 05-ios-parity-demo
    provides: iOS shell, bundled diagnostics page, Android-aligned bridge contract
provides:
  - Xcode-openable `ios/RailBridgeIOS` scaffold with app and test targets
  - lightweight `Start demo` flow that mirrors the Android entry path
  - bundled diagnostics page seeded from the Android WebView asset
  - `window.RailBridge` shim and Android-shaped response payload factory on iOS
affects: [05-ios-parity-demo, 06-portfolio-documentation-and-validation]
tech-stack:
  added: [SwiftUI, WKWebView, XCTest]
  patterns: [injected bridge shim, parity page reuse, Android-aligned JSON payloads]
key-files:
  created:
    - ios/RailBridgeIOS/RailBridgeIOS.xcodeproj/project.pbxproj
    - ios/RailBridgeIOS/RailBridgeIOS.xcodeproj/xcshareddata/xcschemes/RailBridgeIOS.xcscheme
    - ios/RailBridgeIOS/RailBridgeIOS/RailBridgeIOSApp.swift
    - ios/RailBridgeIOS/RailBridgeIOS/MainView.swift
    - ios/RailBridgeIOS/RailBridgeIOS/WebViewDemoView.swift
    - ios/RailBridgeIOS/RailBridgeIOS/Bridge/IOSNativeBridge.swift
    - ios/RailBridgeIOS/RailBridgeIOS/Bridge/BridgeResponseFactory.swift
    - ios/RailBridgeIOS/RailBridgeIOS/SDK/RailPlusSdkAdapter.swift
    - ios/RailBridgeIOS/RailBridgeIOS/SDK/MockRailSdkAdapter.swift
    - ios/RailBridgeIOS/RailBridgeIOS/Models/ChargeResult.swift
    - ios/RailBridgeIOS/RailBridgeIOS/Models/BalanceSnapshot.swift
    - ios/RailBridgeIOS/RailBridgeIOS/Models/SdkStatusSnapshot.swift
    - ios/RailBridgeIOS/RailBridgeIOS/Resources/webview/index.html
    - ios/RailBridgeIOS/RailBridgeIOSTests/BridgeResponseFactoryTests.swift
requirements-completed: [IOS-01, IOS-02]
completed: 2026-04-08
---

# Phase 05 Plan 01 Summary

## What changed

- Added a new iOS parity demo under `ios/RailBridgeIOS` with an app target and a unit-test target.
- Mirrored the Android entry shape with a lightweight start screen and a `Start demo` route into a `WKWebView`.
- Reused the existing Android diagnostics page by bundling the same `index.html` into the iOS app resources.
- Injected a `window.RailBridge` shim at document start so the existing page can keep calling `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`.
- Added a Swift response factory and payload tests so iOS success and error JSON keeps the same field names as Android.

## Verification

- Static acceptance checks passed for:
  - `PRODUCT_BUNDLE_IDENTIFIER = com.demo.railbridge.iosdemo;`
  - `IPHONEOS_DEPLOYMENT_TARGET = 16.0;`
  - visible `Start demo` entrypoint
  - `WKWebView` host presence
  - `window.RailBridge` shim presence
  - bundled page containing `RailBridge Demo` and `Diagnostics panel`
- `xcodebuild` and `swift` are not installed in this Windows environment, so actual build/test execution is deferred to human verification on macOS/Xcode.

## Notes

- Added a shared Xcode scheme so `xcodebuild -scheme RailBridgeIOS` is possible once the repo is opened on macOS.
- The bundled iOS HTML is a direct copy of the Android diagnostics page to keep visual and behavioral parity anchored to the Android baseline.

## Self-Check

PARTIAL - scaffold and contract are implemented; real iOS build verification requires Xcode on macOS.
