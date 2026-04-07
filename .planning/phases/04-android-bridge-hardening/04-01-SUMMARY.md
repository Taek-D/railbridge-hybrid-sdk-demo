# 04-01 Summary

## What changed

- Added `BridgeRequestCoordinator` as an Android-free ownership seam for pending bridge requests.
- Added `InFlightRequestRecord` so diagnostics can expose explicit pending request state without changing the existing business actions.
- Hardened `NativeBridge` so `requestCharge` and `getBalance` register ownership before SDK work starts, schedule a bridge-owned timeout watchdog, and suppress duplicate or late terminal callbacks.
- Extended diagnostics payloads to include `inFlightRequests` and additive hardening evidence such as timeout, duplicate-ignore, and abandoned-request stages.
- Expanded bridge diagnostics coverage with plain JUnit tests for coordinator behavior and additive in-flight diagnostics payload shape.

## Verification

- `:app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest" --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"` passed on the ASCII temp copy at `C:\codex-temp\railbridge-demo-copy`.
- `:app:testDebugUnitTest :app:assembleDebug` passed on `C:\codex-temp\railbridge-demo-copy`.
- Emulator smoke on `emulator-5554` confirmed:
  - `duplicate_callback` yields one visible `requestCharge` success and records `sdk_callback_ignored_duplicate` in diagnostics.
  - `callback_loss` now becomes a timeout error with `vendorCode=VENDOR_TIMEOUT`, `stage=timeout`, and no lingering in-flight request.
  - Navigating back to `MainActivity` during a pending `callback_loss` request produces no crash or stale-screen delivery after the watchdog interval.

## Notes

- Gradle Android unit tests still require the ASCII workspace copy because the primary workspace path contains non-ASCII characters that break the AGP unit-test worker classpath in this environment.
- Timeout teardown verification used the existing WebView DevTools socket to trigger the pending request, then `adb shell input keyevent 4` to leave `WebViewActivity` before the timeout window elapsed.
