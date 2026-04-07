---
status: partial
phase: 05-ios-parity-demo
source: [05-VERIFICATION.md]
started: 2026-04-08T02:22:09+09:00
updated: 2026-04-08T02:22:09+09:00
---

## Current Test

Awaiting iOS simulator verification on a macOS/Xcode machine.

## Tests

### 1. Launch parity app
expected: `RailBridgeIOS` builds and opens to the start screen, then navigates into the WebView demo when `Start demo` is tapped.
result: pending

### 2. Duplicate callback suppression
expected: `duplicate_callback` shows only one visible success while diagnostics include ignored-duplicate evidence.
result: pending

### 3. Callback loss timeout
expected: `callback_loss` times out after 5 seconds and leaves no lingering `inFlightRequests`.
result: pending

### 4. Teardown safety
expected: Leaving the WebView during a pending request causes no crash and no stale callback into the closed screen.
result: pending

### 5. Diagnostics export shape
expected: Export detail includes `schemaVersion`, `timelines`, and `inFlightRequests`.
result: pending

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps

None recorded yet.
