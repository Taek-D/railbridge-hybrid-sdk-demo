---
phase: 02
slug: android-adapter-seam
status: ready
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-06
---

# Phase 02 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Plain JUnit 4.13.2 for Android-free unit tests + AndroidX test runner 1.1.5 / Espresso 3.5.1 for optional runtime instrumentation |
| **Config file** | `app/build.gradle` |
| **Quick run command** | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest" --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` |
| **Full suite command** | `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest" --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` |
| **Estimated runtime** | Targeted task loop: ~15-30 seconds; build-plus-regression loop: ~30-45 seconds |

**Constraint:** The current repo test stack is plain JUnit plus Android instrumentation dependencies only. There is no Robolectric or equivalent JVM Android harness in `app/build.gradle`, so `NativeBridge`, `WebViewActivity`, and `RetryHandler` runtime behavior must not be promised as plain-JUnit tests unless a future phase explicitly adds that harness.

---

## Sampling Rate

- **After 02-01 Task 1:** Run `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest"`
- **After 02-01 Task 2:** Run `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"`
- **After 02-02 Task 1:** Run `powershell -NoProfile -Command "$nb = Get-Content 'app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java' -Raw; $wv = Get-Content 'app/src/main/java/com/demo/railbridge/WebViewActivity.java' -Raw; if ($nb -notmatch 'import\\s+com\\.demo\\.railbridge\\.sdk\\.MockRailSdk' -and $nb -match 'RailPlusSdkAdapter' -and $nb -match 'BridgeResponseFactory' -and $nb -match 'retryHandler\\.execute' -and $nb -match '@JavascriptInterface[\\s\\S]*requestCharge' -and $nb -match '@JavascriptInterface[\\s\\S]*getBalance' -and $nb -match '@JavascriptInterface[\\s\\S]*getSdkStatus' -and $nb -match '@JavascriptInterface[\\s\\S]*reportError' -and $wv -match 'new\\s+MockRailSdkAdapter' -and $wv -match 'sdkAdapter\\.initialize' -and $wv -match 'sdkAdapter\\.shutdown' -and $wv -match 'addJavascriptInterface\\(nativeBridge,\\s*\"RailBridge\"\\)' -and $wv -match 'file:///android_asset/webview/index.html') { exit 0 } else { exit 1 }"`
- **Before the manual checkpoint:** Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest" --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"`
- **Before `$gsd-verify-work`:** Full suite must be green and the manual four-button runtime smoke must be recorded
- **Max feedback latency:** 45 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | SDK-02 | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest"` | yes (created in task) | pending |
| 02-01-02 | 01 | 1 | BRG-02 | unit | `.\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` | yes (created in task) | pending |
| 02-02-01 | 02 | 2 | SDK-01, SDK-02, BRG-01, BRG-02 | static seam check | `powershell -NoProfile -Command "$nb = Get-Content 'app/src/main/java/com/demo/railbridge/bridge/NativeBridge.java' -Raw; $wv = Get-Content 'app/src/main/java/com/demo/railbridge/WebViewActivity.java' -Raw; if ($nb -notmatch 'import\\s+com\\.demo\\.railbridge\\.sdk\\.MockRailSdk' -and $nb -match 'RailPlusSdkAdapter' -and $nb -match 'BridgeResponseFactory' -and $nb -match 'retryHandler\\.execute' -and $nb -match '@JavascriptInterface[\\s\\S]*requestCharge' -and $nb -match '@JavascriptInterface[\\s\\S]*getBalance' -and $nb -match '@JavascriptInterface[\\s\\S]*getSdkStatus' -and $nb -match '@JavascriptInterface[\\s\\S]*reportError' -and $wv -match 'new\\s+MockRailSdkAdapter' -and $wv -match 'sdkAdapter\\.initialize' -and $wv -match 'sdkAdapter\\.shutdown' -and $wv -match 'addJavascriptInterface\\(nativeBridge,\\s*\"RailBridge\"\\)' -and $wv -match 'file:///android_asset/webview/index.html') { exit 0 } else { exit 1 }"` | yes | pending |
| 02-02-02 | 02 | 2 | SDK-01, SDK-02, BRG-01, BRG-02 | build + manual smoke gate | `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkAdapterTest" --tests "com.demo.railbridge.bridge.BridgeResponseFactoryTest"` | yes | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- None. All required plain-JUnit files are created inside the execution tasks that first need them, so no separate Wave 0 scaffold is required.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `NativeBridge` and `WebViewActivity` runtime wiring works after the adapter migration | SDK-01, SDK-02 | The classes depend on Android runtime types (`WebView`, `Handler`, `Looper`, `@JavascriptInterface`) and the repo does not include Robolectric | Build and run in Android Studio or emulator, open the WebView screen, and exercise the unchanged four-button flow |
| Existing four-button WebView flow still works | BRG-01 | `adb` is not currently available on `PATH` for scripted device smoke tests | Trigger `Request charge`, `Get balance`, `SDK status`, and `Report JS error`, and confirm logs still update |
| Additive metadata does not break the current JS page | BRG-02 | The current page contract is exercised best through the existing WebView screen | Trigger success and error flows and confirm the page still renders logs without JS exceptions |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or an automated pre-checkpoint gate
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 gaps eliminated by task-owned test creation
- [x] No watch-mode flags
- [x] Feedback latency <= 45 seconds for task-level loops
- [x] `nyquist_compliant: true` set in frontmatter
- [x] Android-runtime checks are explicitly manual or optional instrumentation, not misrepresented as plain-JUnit tests

**Approval:** ready for execution
