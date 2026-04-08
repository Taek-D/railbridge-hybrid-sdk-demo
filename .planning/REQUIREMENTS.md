# REQUIREMENTS

**Milestone:** `v0.2 portfolio expansion`
**Last Updated:** 2026-04-08

## In Scope

### Bootstrap

- [x] **BOOT-01**: The repo can store milestone planning state under `.planning/` with `PROJECT.md`, `STATE.md`, `REQUIREMENTS.md`, and `ROADMAP.md`.
- [x] **BOOT-02**: The planning docs explicitly record the git bootstrap path and decision history so later GSD automation can rely on a real repository state.

### Android SDK Seam

- [x] **SDK-01**: Android bridge calls go through a `RailPlusSdkAdapter` seam instead of depending directly on `MockRailSdk`.
- [x] **SDK-02**: The existing `MockRailSdk` remains available behind the adapter so the current Android demo flow keeps working.
- [x] **SDK-03**: The Android mock SDK can deterministically simulate timeout, internal error, callback loss, duplicate callback, and retry exhaustion scenarios.

### Bridge Contract

- [x] **BRG-01**: Android public bridge methods remain `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError`.
- [x] **BRG-02**: Bridge responses preserve current fields and add optional metadata fields for `callbackId`, `correlationId`, `platform`, `stage`, `durationMs`, `vendorCode`, and `retryable`.

### Logging

- [x] **LOG-01**: Each request is traceable across JS entry, native validation, SDK start, SDK callback, and JS callback using a shared `correlationId`.
- [x] **LOG-02**: Logs can be exported as structured JSON or saved so SDK issues can be demonstrated with evidence beyond on-screen text.

### Android Stability

- [x] **STAB-01**: Android bridge execution handles in-flight request tracking, duplicate callback suppression, timeout handling, and ignores late callbacks after teardown.

### WebView UI

- [x] **UI-01**: The current WebView demo screen keeps the existing actions and adds a scenario panel plus richer log inspection without removing current usability.

### iOS Parity

- [x] **IOS-01**: A runnable iOS demo exists using `WKWebView + Swift bridge + mock adapter`.
- [x] **IOS-02**: The iOS bridge methods and log field model match the Android contract closely enough for side-by-side portfolio comparison.

### Documentation

- [x] **DOC-01**: `README.md`, `ARCHITECTURE.md`, and `DEBUGGING_REPORT.md` explain the system shape, failure-analysis strategy, and validation story as portfolio artifacts.

## Future Requirements

- [ ] **FUT-01**: Replace the mock adapter with a real vendor SDK integration layer when SDK binaries and contracts are available.
- [ ] **FUT-02**: Wire structured events into a real Crashlytics or analytics backend with environment-specific configuration.
- [ ] **FUT-03**: Add automated regression coverage for Android and iOS bridge flows in CI.

## Out Of Scope

- Real payment processing or real transport-card balance updates
- Production release signing, App Store / Play Store delivery, or live deployment
- Full backend infrastructure for event ingestion
- Vendor-specific behavior claims that cannot be supported by an actual SDK binary

## Traceability

| Requirement | Phase |
|---|---|
| BOOT-01, BOOT-02 | 1. Milestone Bootstrap |
| SDK-01, SDK-02, BRG-01, BRG-02 | 2. Android Adapter Seam |
| SDK-03, LOG-01, LOG-02, UI-01 | 3. Logging and Failure Simulation |
| STAB-01 | 4. Android Bridge Hardening |
| IOS-01, IOS-02 | 5. iOS Parity Demo |
| DOC-01 | 6. Portfolio Documentation and Validation |
