---
status: resolved
phase: 05-ios-parity-demo
source: [05-VERIFICATION.md]
started: 2026-04-08T02:22:09+09:00
updated: 2026-04-08T21:05:00+09:00
---

## Current Test

Resolved via macOS/Xcode runtime verification and captured portfolio screenshots.

## Tests

### 1. Launch parity app
expected: `RailBridgeIOS` builds and opens to the start screen, then navigates into the WebView demo when `Start demo` is tapped.
result: passed

### 2. Duplicate callback suppression
expected: `duplicate_callback` shows only one visible success while diagnostics include ignored-duplicate evidence.
result: passed

### 3. Callback loss timeout
expected: `callback_loss` times out after 5 seconds and leaves no lingering `inFlightRequests`.
result: passed

### 4. Teardown safety
expected: Leaving the WebView during a pending request causes no crash and no stale callback into the closed screen.
result: passed

### 5. Diagnostics export shape
expected: Export detail includes `schemaVersion`, `timelines`, and `inFlightRequests`.
result: passed

## Summary

total: 5
passed: 5
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

No remaining gaps.
