# RailBridge Hybrid SDK Stabilization Demo

Hybrid WebView transport-card SDK troubleshooting portfolio project. This repository simulates the kind of failure analysis and stabilization work that happens when a legacy app already has a vendor SDK wired in, but the team needs better evidence, safer bridge behavior, and platform parity before touching production code.

The demo now includes:

- Android `WebView + JavascriptInterface + Java` baseline
- Android Kotlin parity path for the same bridge-core contract
- iOS `WKWebView + Swift` parity implementation
- deterministic vendor-failure presets
- correlation-based timeline logging
- exportable diagnostics JSON
- timeout, duplicate-callback, and teardown hardening
- runtime verification evidence for both Android and iOS

## Why This Exists

This project was built to demonstrate how to approach a real hybrid-app stabilization engagement where:

- the business flow is already in production
- the vendor SDK is partially opaque
- failures are intermittent
- WebView and native code share responsibility
- the client needs evidence, not guesses

The codebase does not claim to be a real RailPlus integration. Instead, it uses a mock adapter layer to simulate the failure shapes that usually make closed-SDK troubleshooting hard:

- `timeout`
- `internal_error`
- `callback_loss`
- `duplicate_callback`
- `retry_exhausted`

## Repo Map

```text
app/
  src/main/java/com/demo/railbridge/
    bridge/
    logging/
    retry/
    sdk/
  src/main/assets/webview/index.html

ios/RailBridgeIOS/
  RailBridgeIOS/
    Bridge/
    SDK/
    Logging/
    Resources/webview/index.html
  RailBridgeIOSTests/

artifacts/ios-portfolio-shots/
.planning/
```

## Architecture At A Glance

The Android and iOS demos intentionally keep the same operator-facing shape:

1. WebView page triggers one of four business actions.
2. Native bridge generates a `correlationId` and records staged timeline events.
3. Bridge calls the adapter seam instead of talking directly to the mock SDK.
4. Adapter applies the active failure preset.
5. Bridge owns timeout, duplicate suppression, and teardown-safe delivery.
6. Result and diagnostics payloads return to the page with additive metadata.

Android now exposes the same shared diagnostics page through two launch paths:

- Java baseline demo
- Kotlin parity demo

See [ARCHITECTURE.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/ARCHITECTURE.md) for the full flow.

## What The Demo Proves

- How to keep a legacy WebView contract stable while refactoring native internals
- How to reproduce intermittent SDK failures on demand
- How to log enough evidence to argue whether the fault is in JS, the bridge, retry policy, or the SDK callback layer
- How to align Android and iOS around the same troubleshooting vocabulary
- How to harden bridge behavior without changing the visible business actions

## Bridge Contract

The preserved public bridge actions are:

- `requestCharge`
- `getBalance`
- `getSdkStatus`
- `reportError`

Base response fields remain stable:

- `status`
- `method`
- `data`
- `error`
- `retryCount`

Additive metadata fields:

- `callbackId`
- `correlationId`
- `platform`
- `stage`
- `durationMs`
- `scenario`
- `vendorCode`
- `retryable`
- `resolvedByRetry`

## Deterministic Presets

The current preset list is:

- `normal`
- `timeout`
- `internal_error`
- `callback_loss`
- `duplicate_callback`
- `retry_exhausted`

These are exposed in the shared diagnostics page so a reviewer can switch scenarios without changing source code.

## Android Run

Environment:

- Android Gradle Plugin `8.2.0`
- Gradle Wrapper `8.2`
- minSdk `26`
- compileSdk / targetSdk `34`

Recommended Android Studio flow:

1. Open the repository root in Android Studio.
2. Use the Gradle wrapper when prompted.
3. Set the Gradle JDK to `17+`.
4. Sync the project.
5. Run the `app` configuration.
6. Tap `Start Java demo` or `Start Kotlin parity demo`.
7. Exercise the four bridge actions and the diagnostics presets.

Required Android SDK packages:

- Android SDK Platform `34`
- Android SDK Build-Tools `34.0.0` or newer
- Android SDK Platform-Tools

CLI build example on Windows:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

Note:

- this workspace path contains non-ASCII characters
- Android unit tests were verified from an ASCII-path copy because AGP worker classpaths can fail under the original path

## iOS Run

Open the Xcode project:

```bash
open ios/RailBridgeIOS/RailBridgeIOS.xcodeproj
```

Recommended Xcode flow:

1. Select the `RailBridgeIOS` scheme.
2. Run on an iPhone simulator.
3. Tap `Start demo`.
4. Verify the shared diagnostics page loads.
5. Exercise the same four actions and scenario presets used on Android.

The iOS implementation intentionally mirrors the Android concepts rather than trying to look like a production app shell.

## Validation Evidence

Android evidence:

- Android Studio sync and `assembleDebug` succeeded
- emulator smoke confirmed preserved four-action flow
- `duplicate_callback` showed one visible result plus ignored-duplicate diagnostics
- `callback_loss` converted into timeout evidence

iOS evidence:

- Xcode runtime verification completed on macOS
- screenshots captured under [artifacts/ios-portfolio-shots](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots)

Supporting documents:

- [ARCHITECTURE.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/ARCHITECTURE.md)
- [DEBUGGING_REPORT.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/DEBUGGING_REPORT.md)
- [VALIDATION_REPORT.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/VALIDATION_REPORT.md)

## Current Limits

This repo is a troubleshooting demo, not a production payment app.

Important limits:

- the Android app still uses the Java implementation as the verified baseline while Kotlin parity is limited to the bridge-core path
- the SDK layer is simulated, not a real RailPlus binary
- no real payment processing, NFC, or card-state mutation occurs
- Crashlytics is treated as an integration point, not a live configured backend
- Android instrumentation coverage is still manual or `adb`-assisted

## Best Portfolio Framing

The strongest honest description is:

`A cross-platform hybrid WebView SDK stabilization demo that reproduces opaque vendor failure modes, preserves legacy bridge contracts, adds correlation-based diagnostics, and hardens async bridge ownership on Android and iOS.`

That framing is more defensible than claiming direct production RailPlus operations experience.
