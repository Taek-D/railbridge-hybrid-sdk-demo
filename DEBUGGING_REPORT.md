# Debugging Report

## Scope

This report explains how the demo reproduces and analyzes hybrid SDK failures in a way that resembles a real troubleshooting engagement. The goal is not to prove a real vendor defect. The goal is to show how the codebase isolates failure ownership and produces enough evidence to make a defensible root-cause argument.

## Investigation Method

Every request is analyzed along the same path:

1. Did the WebView actually call the native bridge?
2. Did native validation succeed?
3. Did the bridge hand off to the adapter/SDK layer?
4. Did the SDK callback arrive once, twice, or never?
5. Did retry recover the issue?
6. Was the final user-visible result produced by the SDK or by bridge timeout logic?

This is why the project records stage-based timelines instead of only a final error.

## Evidence Model

The main evidence unit is a single request timeline keyed by `correlationId`.

Important fields:

- `method`
- `scenario`
- `stage`
- `retryCount`
- `durationMs`
- `vendorCode`
- `retryable`
- `finalStatus`
- `resolvedByRetry`

Interpretation rule:

- if the timeline reaches `js_entry` and `native_validation`, the WebView and bridge are alive
- if it reaches `sdk_start`, the handoff left the bridge layer
- if no callback stage follows and a timeout stage appears, the bridge had to synthesize the terminal failure
- if `sdk_callback_ignored_duplicate` appears, the SDK tried to deliver multiple terminal outcomes
- if `bridge_abandoned` appears, lifecycle teardown interrupted the request safely

## Scenario Findings

### 1. Timeout With Retry Recovery

Scenario:

- preset `timeout`
- first attempts behave like vendor/network timeout
- retry eventually succeeds

Observed evidence:

- `sdk_start`
- `sdk_callback` with retryable vendor timeout semantics
- increased `retryCount`
- final `js_callback`
- `resolvedByRetry = true`

Interpretation:

- this is the strongest example of why retry policy belongs in the bridge-facing stabilization layer
- the user sees a success, but diagnostics still preserve the fact that instability happened underneath

Portfolio value:

- demonstrates graceful recovery instead of only failure capture

### 2. Callback Loss

Scenario:

- preset `callback_loss`
- backend emits no callback

Observed evidence:

- `js_entry`
- `native_validation`
- `sdk_start`
- no terminal SDK callback
- terminal `timeout`
- empty or drained `inFlightRequests` after timeout handling

Interpretation:

- the request made it out of the WebView and through native validation
- the missing terminal callback is not a JS-entry problem
- the bridge now protects the UX from indefinite hanging by converting the missing callback into a real timeout result

Portfolio value:

- demonstrates how to distinguish "SDK never answered" from "bridge never received the request"

### 3. Duplicate Callback

Scenario:

- preset `duplicate_callback`
- backend success callback fires twice

Observed evidence:

- visible user success only once
- diagnostics event `sdk_callback_ignored_duplicate`

Interpretation:

- the bridge now enforces single terminal ownership
- duplicate vendor callbacks no longer corrupt UI state or create double-delivery ambiguity

Portfolio value:

- shows defensive handling for one of the most common opaque-SDK bugs

### 4. Internal Error

Scenario:

- preset `internal_error`

Observed evidence:

- error payload carries `vendorCode = VENDOR_INTERNAL`
- retryability remains false

Interpretation:

- the bridge can classify and expose a vendor-style terminal failure without retrying blindly
- this keeps retry strategy explainable instead of magical

Portfolio value:

- useful for conversations where the client wants explicit evidence for "don't retry this class of issue"

### 5. Retry Exhausted

Scenario:

- preset `retry_exhausted`

Observed evidence:

- multiple retryable failures
- terminal failure after retry budget is spent
- final error result with retry history still visible

Interpretation:

- this is different from single-shot internal error
- the system records that recovery was attempted and failed

Portfolio value:

- demonstrates "retry with proof" instead of "retry and hope"

### 6. JS Error Reporting

Scenario:

- `reportError`

Observed evidence:

- JS-originated error reaches native logging
- native acknowledgment returns to the page

Interpretation:

- the bridge can collect JS-side evidence and correlate it with native-side events
- this closes part of the observability gap common in hybrid apps

Portfolio value:

- useful for tracking gaps where the SDK itself may not expose enough telemetry

## Android Validation Notes

Validated paths:

- Gradle wrapper build
- Android Studio sync and run
- Java baseline and Kotlin parity launch paths
- emulator smoke
- deterministic scenario behavior
- duplicate suppression
- callback-loss timeout conversion
- teardown safety

Important caveat:

- Android unit tests required an ASCII-path copy of the repository because the main Windows path contains non-ASCII characters that can break AGP test workers

This is an environment constraint, not a product logic defect.

## iOS Validation Notes

Validated paths:

- Xcode-openable project
- simulator runtime flow
- shared diagnostics page loading
- timeout retry recovery
- callback-loss timeout evidence
- duplicate suppression
- JS error acknowledgment

Recorded evidence:

- [01-home-start-screen.jpg](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots/01-home-start-screen.jpg)
- [02-demo-bridge-connected.jpg](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots/02-demo-bridge-connected.jpg)
- [03-timeout-retry-recovered.jpg](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots/03-timeout-retry-recovered.jpg)
- [04-callback-loss-timeout-evidence.jpg](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots/04-callback-loss-timeout-evidence.jpg)
- [05-duplicate-callback-suppressed.jpg](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots/05-duplicate-callback-suppressed.jpg)
- [06-js-error-report-ack.jpg](/E:/프로젝트/위시켓/Android%20Java%20WebView%20Bridge%20+%20SDK%20에러%20핸들링%20데모%20—%20Qwen%20Code%20CLI%20프롬프트/artifacts/ios-portfolio-shots/06-js-error-report-ack.jpg)

## What This Demonstrates Well

- preserving legacy bridge APIs while refactoring internals
- building deterministic repro cases for opaque SDK behavior
- separating vendor behavior from bridge behavior
- designing logs that are useful in client and vendor conversations
- keeping Android and iOS aligned around the same debugging language

## What This Does Not Prove

This repository does not prove:

- production RailPlus integration experience
- real NFC hardware flow handling
- real money movement
- live Crashlytics or analytics operations
- full Android Kotlin production migration

Those should be described as follow-up work, not implied as already solved.

## Recommended Client-Facing Positioning

The best honest summary is:

`Built a cross-platform hybrid WebView SDK troubleshooting demo that preserves legacy bridge entrypoints, reproduces opaque vendor failure shapes, hardens callback ownership, and exports correlation-based evidence for root-cause analysis.`

That statement is strong because every part of it is backed by code and validation artifacts in this repository.
