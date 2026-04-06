# 02-01 Summary

## What changed

- Added `RailPlusSdkAdapter` as the bridge-facing SDK seam.
- Added `BalanceSnapshot` and `SdkStatusSnapshot` so balance and status results cross the seam without exposing `MockRailSdk` nested types.
- Added `MockRailSdkAdapter` as the only wrapper around `MockRailSdk`.
- Added `BridgeResponseFactory` to centralize backward-compatible JSON payload generation with optional metadata fields.
- Added plain JUnit coverage for the adapter seam and response factory contract.

## Verification

- `:app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest"` passed on an ASCII temp copy of the repo.
- `:app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` passed on an ASCII temp copy of the repo.
- Direct JUnit execution against compiled classes in the main workspace also passed for both test classes.

## Notes

- The workspace path contains non-ASCII characters, and Gradle's Android unit-test worker resolves the project path with `?` substitution in this environment. The tests themselves are valid, but Gradle verification was run from `C:\codex-temp\railbridge-demo-copy` to avoid that path issue.
