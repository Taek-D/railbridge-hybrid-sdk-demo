# Validation Report

## Summary

This repository now has validation evidence for both platforms:

- Android build and emulator verification
- iOS Xcode simulator verification
- deterministic scenario validation
- diagnostics export validation

The validation story is intentionally honest: Android has stronger local automation in this workspace, while iOS runtime proof was completed on macOS/Xcode and synced back into the repository.

Android now has two demonstrable runtime paths:

- Java baseline flow
- Kotlin parity flow using the same shared WebView page and diagnostics contract

## Verification Matrix

| Area | Android | iOS | Evidence |
| --- | --- | --- | --- |
| Project opens and builds | PASS | PASS | Android Gradle wrapper build, Xcode runtime verification |
| Shared four bridge actions | PASS | PASS | `requestCharge`, `getBalance`, `getSdkStatus`, `reportError` remain available |
| Deterministic preset control | PASS | PASS | Shared preset vocabulary across Java and Swift adapters |
| Correlation-based timelines | PASS | PASS | Diagnostics payloads group by `correlationId` |
| Timeout handling | PASS | PASS | `callback_loss` transitions into timeout evidence |
| Duplicate suppression | PASS | PASS | duplicate callbacks do not double-deliver to UI |
| Teardown safety | PASS | PASS | Android runtime verified; iOS bridge owns abandonment path |
| Exportable diagnostics | PASS | PASS | structured JSON snapshots on both platforms |

## Android Evidence

Primary references:

- [03-VERIFICATION.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/.planning/phases/03-logging-and-failure-simulation/03-VERIFICATION.md)
- [04-VERIFICATION.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/.planning/phases/04-android-bridge-hardening/04-VERIFICATION.md)

Validated behaviors:

- Java and Kotlin Android launch paths both open the same diagnostics page
- scenario presets produce reproducible failures
- timeline export preserves `schemaVersion`, stage data, and completion metadata
- duplicate callbacks are suppressed
- callback loss turns into timeout evidence
- destroying the bridge abandons pending ownership cleanly

## iOS Evidence

Primary references:

- [05-VERIFICATION.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/.planning/phases/05-ios-parity-demo/05-VERIFICATION.md)
- [05-HUMAN-UAT.md](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/.planning/phases/05-ios-parity-demo/05-HUMAN-UAT.md)
- [ios-portfolio-shots](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots)

Validated behaviors:

- Xcode project opens and runs
- `Start demo` navigates into the shared diagnostics page
- timeout retry recovery is visible
- callback loss becomes explicit timeout evidence
- duplicate callback suppression works
- JS error reporting is acknowledged by native code

## Remaining Limits

- Android now includes a Kotlin parity path, but the production-strength baseline and most shared infrastructure still remain in Java
- no real vendor SDK binary is integrated
- no production metrics backend is configured
- no CI pipeline currently runs cross-platform runtime validation

## Readiness Conclusion

This repository is ready to be presented as:

- a hybrid SDK troubleshooting portfolio sample
- a bridge hardening and diagnostics design reference
- a cross-platform parity demo for explaining failure ownership

It is not yet ready to be presented as:

- a production transport-card implementation
- proof of real vendor fault in a live service
