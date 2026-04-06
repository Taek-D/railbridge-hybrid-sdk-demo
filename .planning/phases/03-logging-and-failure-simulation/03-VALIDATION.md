---
phase: 03
slug: logging-and-failure-simulation
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-06
---

# Phase 03 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Plain JUnit 4.13.2 for deterministic scenario, timeline, and export tests plus manual Android emulator smoke |
| **Config file** | `app/build.gradle` |
| **Quick run command** | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkScenarioTest" --tests "com.demo.railbridge.logging.RequestTimelineRepositoryTest"` |
| **Full suite command** | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` |
| **Estimated runtime** | Targeted loop: ~20-40 seconds; full gate: ~45-75 seconds |

**Constraint:** Android unit tests must run from `C:\codex-temp\railbridge-demo-copy` in this environment because the main workspace path contains non-ASCII characters that break the AGP unit-test worker classpath. Runtime smoke verification remains manual or `adb`-assisted.

---

## Sampling Rate

- **After 03-01 Task 1:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkScenarioTest"`
- **After 03-01 Task 2:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.logging.RequestTimelineRepositoryTest" --tests "com.demo.railbridge.logging.DiagnosticsExportTest"`
- **After 03-02 Task 1:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"`
- **Before the manual checkpoint:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
- **Before `$gsd-verify-work`:** Full unit suite must be green and the manual diagnostics-panel smoke must be recorded
- **Max feedback latency:** 75 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | SDK-03 | unit | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.sdk.MockRailSdkScenarioTest"` | no (Wave 0) | pending |
| 03-01-02 | 01 | 1 | LOG-01, LOG-02 | unit | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.logging.RequestTimelineRepositoryTest" --tests "com.demo.railbridge.logging.DiagnosticsExportTest"` | no (Wave 0) | pending |
| 03-02-01 | 02 | 2 | LOG-01, LOG-02, UI-01 | unit | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"` | no (Wave 0) | pending |
| 03-02-02 | 02 | 2 | UI-01 | build + manual smoke gate | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` | yes | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- Create `app/src/test/java/com/demo/railbridge/sdk/MockRailSdkScenarioTest.java` for deterministic preset behavior, including timeout recovery, callback loss, duplicate callback, and retry-exhausted setup.
- Create `app/src/test/java/com/demo/railbridge/logging/RequestTimelineRepositoryTest.java` for stage ordering, correlation grouping, completion state, and bounded retention.
- Create `app/src/test/java/com/demo/railbridge/logging/DiagnosticsExportTest.java` for JSON schema, required evidence fields, and export stability.
- Create `app/src/test/java/com/demo/railbridge/bridge/NativeBridgeDiagnosticsTest.java` for additive metadata and diagnostics-helper compatibility without changing the original four action methods.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Existing four buttons still work on the same WebView page | UI-01 | Current repo has no stable instrumentation harness for the WebView flow | Launch the app, open the demo page, and trigger `Request charge`, `Get balance`, `SDK status`, and `Report JS error` |
| Scenario panel changes the active preset and affects subsequent SDK-backed requests | SDK-03, UI-01 | Requires real WebView interaction and visual confirmation | Switch presets on-page, then trigger `Request charge` or `Get balance` and verify the observed behavior matches the selected preset |
| Diagnostics detail and export surface reflect incomplete requests such as `callback loss` | LOG-01, LOG-02, UI-01 | The key value is visibility of stalled or incomplete flows in the running app | Trigger `callback loss`, refresh diagnostics, and verify the timeline still shows `js_entry`, `native_validation`, and `sdk_start` without a final JS callback |

---

## Validation Sign-Off

- [x] All tasks have an automated verify or an automated pre-checkpoint gate
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 gaps are explicit
- [x] No watch-mode flags
- [x] Feedback latency <= 75 seconds for task-level loops
- [x] `nyquist_compliant: true` set in frontmatter
- [x] Android-runtime checks are explicitly manual, not misrepresented as plain-JUnit tests

**Approval:** ready for execution
