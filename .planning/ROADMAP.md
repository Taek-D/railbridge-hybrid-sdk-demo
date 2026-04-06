# ROADMAP

**Milestone:** `v0.2 portfolio expansion`
**Last Updated:** 2026-04-06

## Summary

- Phases: `6`
- Requirements mapped: `14 / 14`
- Coverage: `All in-scope requirements mapped exactly once`

## Phases

- [x] **Phase 1: Milestone Bootstrap** - Establish GSD planning state and record bootstrap constraints
- [x] **Phase 2: Android Adapter Seam** - Introduce the adapter seam and preserve the current bridge contract
- [ ] **Phase 3: Logging and Failure Simulation** - Add deterministic scenario control and structured diagnostic output
- [ ] **Phase 4: Android Bridge Hardening** - Make the bridge resilient to async race conditions and teardown timing
- [ ] **Phase 5: iOS Parity Demo** - Build a runnable iOS sample that mirrors the Android integration model
- [ ] **Phase 6: Portfolio Documentation and Validation** - Turn the implementation into a portfolio-ready story with validation evidence

| # | Phase | Goal | Requirements |
|---|---|---|---|
| 1 | Milestone Bootstrap | Establish GSD planning state and record bootstrap constraints | BOOT-01, BOOT-02 |
| 2 | Android Adapter Seam | Introduce the adapter seam and preserve the current bridge contract | SDK-01, SDK-02, BRG-01, BRG-02 |
| 3 | Logging and Failure Simulation | Add deterministic scenario control and structured diagnostic output | SDK-03, LOG-01, LOG-02, UI-01 |
| 4 | Android Bridge Hardening | Make the bridge resilient to async race conditions and teardown timing | STAB-01 |
| 5 | iOS Parity Demo | Build a runnable iOS sample that mirrors the Android integration model | IOS-01, IOS-02 |
| 6 | Portfolio Documentation and Validation | Turn the implementation into a portfolio-ready story with validation evidence | DOC-01 |

## Phase Details

### Phase 1: Milestone Bootstrap

**Goal:** Create the planning baseline for this repo and make bootstrap gaps explicit before code execution starts.

**Depends on:** Nothing (first phase)
**Requirements:** [BOOT-01, BOOT-02]
**Success criteria:**
1. `.planning/` contains the milestone planning documents needed for ongoing GSD work.
2. The absence of git metadata is documented as an execution prerequisite rather than ignored.
3. The repo is described as an Android hybrid SDK stabilization demo with an iOS parity milestone.
**Plans:** 1 plan

Plans:
- [x] 01-01: Verify git/bootstrap constraints and normalize planning artifacts for GSD tooling

### Phase 2: Android Adapter Seam

**Goal:** Decouple the bridge from the concrete mock SDK while preserving the current Android demo behavior and public bridge entrypoints.

**Depends on:** Phase 1
**Requirements:** [SDK-01, SDK-02, BRG-01, BRG-02]
**Success criteria:**
1. `NativeBridge` uses an adapter interface rather than a direct `MockRailSdk` dependency.
2. Existing Android buttons still call the same bridge methods and still work.
3. New metadata fields are additive and do not break the current JS result handler.
4. Mock behavior remains the default adapter implementation.
**Plans:** 2 plans

Plans:
- [x] 02-01: Define Android adapter contracts and wrap MockRailSdk without changing user-visible actions
- [x] 02-02: Move NativeBridge to adapter-based execution and additive metadata responses

### Phase 3: Logging and Failure Simulation

**Goal:** Make failures reproducible and observable with structured diagnostics suitable for debugging reports.

**Depends on:** Phase 2
**Requirements:** [SDK-03, LOG-01, LOG-02, UI-01]
**Success criteria:**
1. Failure scenarios can be selected intentionally instead of relying only on randomness.
2. A single request can be followed across bridge and SDK stages via `correlationId`.
3. Logs can be exported or saved in a structured form.
4. The WebView UI still supports the current demo actions and also exposes scenario controls.
**Plans:** TBD

Plans:
- [ ] 03-01: Add deterministic failure scenario configuration and bridge-stage tracing
- [ ] 03-02: Extend WebView diagnostics UI and structured log export

### Phase 4: Android Bridge Hardening

**Goal:** Prevent common hybrid-bridge race conditions from corrupting the demo flow or misleading the diagnostic output.

**Depends on:** Phase 3
**Requirements:** [STAB-01]
**Success criteria:**
1. Duplicate callbacks are ignored after the first accepted result.
2. Timed-out requests do not keep hanging indefinitely.
3. Requests no longer try to write back into a destroyed bridge or closed screen.
4. In-flight request state is explicit and observable for debugging.
**Plans:** TBD

Plans:
- [ ] 04-01: Add in-flight request, timeout, and teardown safety to Android bridge execution

### Phase 5: iOS Parity Demo

**Goal:** Add a second runnable platform that demonstrates the same stabilization pattern with `WKWebView` and Swift.

**Depends on:** Phase 4
**Requirements:** [IOS-01, IOS-02]
**Success criteria:**
1. An Xcode-openable iOS project exists in the repo.
2. The iOS demo exposes the same four bridge actions as Android.
3. Scenario controls and structured log fields mirror the Android design closely enough for comparison.
4. The iOS demo is positioned as parity validation, not as a production deployment target.
**Plans:** TBD

Plans:
- [ ] 05-01: Scaffold iOS parity demo and align bridge methods with Android
- [ ] 05-02: Mirror failure scenarios and logging contracts on iOS

### Phase 6: Portfolio Documentation and Validation

**Goal:** Package the work as a credible troubleshooting portfolio artifact instead of a raw code sample.

**Depends on:** Phase 5
**Requirements:** [DOC-01]
**Success criteria:**
1. `README.md` explains the problem framing, architecture, and how to run both demos.
2. `ARCHITECTURE.md` describes the bridge, adapter, retry, and logging flow.
3. `DEBUGGING_REPORT.md` shows how scenario evidence supports root-cause analysis.
4. Validation notes cover both Android and iOS flows and call out remaining limits honestly.
**Plans:** TBD

Plans:
- [ ] 06-01: Write architecture and debugging documentation
- [ ] 06-02: Record validation evidence and portfolio positioning notes

## Progress

| Phase | Plans Complete | Status | Completed |
|---|---|---|---|
| 1. Milestone Bootstrap | 1/1 | Complete | 2026-04-06 |
| 2. Android Adapter Seam | 2/2 | Complete | 2026-04-06 |
| 3. Logging and Failure Simulation | 0/2 | Not started | - |
| 4. Android Bridge Hardening | 0/1 | Not started | - |
| 5. iOS Parity Demo | 0/2 | Not started | - |
| 6. Portfolio Documentation and Validation | 0/2 | Not started | - |

## Next Up

Recommended next command: `$gsd-discuss-phase 3`
