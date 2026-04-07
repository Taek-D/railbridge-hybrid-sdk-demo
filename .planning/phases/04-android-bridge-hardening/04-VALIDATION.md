---
phase: 04
slug: android-bridge-hardening
status: ready
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-08
---

# Phase 04 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Plain JUnit 4.13.2 for request-ownership and diagnostics contract tests plus manual Android emulator smoke |
| **Config file** | `app/build.gradle` |
| **Quick run command** | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest" --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"` |
| **Full suite command** | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` |
| **Estimated runtime** | Targeted loop: ~20-45 seconds; full gate: ~45-80 seconds |

**Constraint:** Android unit tests must run from `C:\codex-temp\railbridge-demo-copy` in this environment because the main workspace path contains non-ASCII characters that break the AGP unit-test worker classpath. Runtime smoke verification remains emulator-assisted.

---

## Sampling Rate

- **After 04-01 Task 1:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest"`
- **After 04-01 Task 2:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest" --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"`
- **Before the manual checkpoint:** Run `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`
- **Before `$gsd-verify-work`:** Full unit suite must be green and the manual duplicate-callback / timeout / teardown smoke must be recorded
- **Max feedback latency:** 80 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | STAB-01 | unit | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest"` | no (Wave 0) | pending |
| 04-01-02 | 01 | 1 | STAB-01 | unit + build + manual smoke gate | `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest --tests "com.demo.railbridge.bridge.BridgeRequestCoordinatorTest" --tests "com.demo.railbridge.bridge.NativeBridgeDiagnosticsTest"` then `cd C:\codex-temp\railbridge-demo-copy && .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` | partly yes | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- Create `app/src/test/java/com/demo/railbridge/bridge/BridgeRequestCoordinatorTest.java` for duplicate suppression, timeout single-fire, teardown abandonment, and in-flight snapshot visibility.
- Expand `app/src/test/java/com/demo/railbridge/bridge/NativeBridgeDiagnosticsTest.java` or add an equivalent diagnostics-focused test to cover additive in-flight visibility and hardening evidence shape.
- Keep the automated core plain-JUnit-testable by moving race-condition rules into an Android-free coordinator instead of burying them entirely inside `WebView` and `Handler` code.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Existing four buttons still work from the same WebView page | STAB-01 | The repo still has no stable instrumentation harness for the real WebView action flow | Launch the app, open the demo page, and trigger `Request charge`, `Get balance`, `SDK status`, and `Report JS error` |
| `duplicate_callback` produces one user-visible result and a diagnostics record showing the later callback was ignored | STAB-01 | Requires the real bridge plus diagnostics UI and timing-sensitive behavior | Select `duplicate_callback`, trigger `Request charge`, and verify only one visible success arrives while diagnostics show duplicate suppression evidence |
| `callback_loss` eventually times out instead of hanging forever | STAB-01 | Timeout ownership spans real `Handler` timing and bridge delivery | Select `callback_loss`, trigger `Request charge` or `Get balance`, wait for the watchdog window, and verify a timeout error appears with matching diagnostics evidence |
| Closing or destroying the screen prevents late JS delivery while preserving diagnostic evidence | STAB-01 | Needs lifecycle interaction that plain JUnit cannot simulate credibly here | Start a pending request, leave or close the screen before completion, and verify there is no crash or stale JS callback into the dead page |

---

## Validation Sign-Off

- [x] All tasks have an automated verify or an automated pre-checkpoint gate
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 gaps are explicit
- [x] No watch-mode flags
- [x] Feedback latency <= 80 seconds for task-level loops
- [x] `nyquist_compliant: true` set in frontmatter
- [x] Runtime-only teardown behavior is explicitly manual, not misrepresented as plain-JUnit coverage

**Approval:** ready for execution
