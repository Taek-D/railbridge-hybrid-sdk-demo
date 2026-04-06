# 02-02 Summary

## What changed

- Migrated `NativeBridge` from direct `MockRailSdk` usage to `RailPlusSdkAdapter`.
- Kept the existing four `@JavascriptInterface` entrypoints:
  - `requestCharge`
  - `getBalance`
  - `getSdkStatus`
  - `reportError`
- Moved all runtime payload construction onto `BridgeResponseFactory`.
- Updated `WebViewActivity` to compose `MockRailSdkAdapter`, initialize it through the seam, inject it into `NativeBridge`, and shut it down through the seam.
- Added correlation-aware additive metadata to runtime payloads without removing the fields used by the current HTML page.

## Verification

- Static seam check passed:
  - `NativeBridge` no longer imports `MockRailSdk`
  - `NativeBridge` references `RailPlusSdkAdapter`, `BridgeResponseFactory`, and `retryHandler.execute`
  - `WebViewActivity` constructs `new MockRailSdkAdapter`, uses `sdkAdapter.initialize`, `sdkAdapter.shutdown`, keeps `addJavascriptInterface(nativeBridge, "RailBridge")`, and still loads `file:///android_asset/webview/index.html`
- `:app:assembleDebug` passed in the main workspace with Android Studio JBR 17.
- `:app:assembleDebug :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest" --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` passed on `C:\codex-temp\railbridge-demo-copy`.
- Emulator smoke on `emulator-5554` confirmed all four WebView actions still dispatch through the unchanged bridge surface:
  - `requestCharge called`
  - `getBalance called`
  - `getSdkStatus called`
  - `reportError called`

## Notes

- The manual smoke kept the existing WebView page and transport path. Verification used adb taps and app log inspection because the WebView DOM buttons are not exposed through the Android UI tree.
