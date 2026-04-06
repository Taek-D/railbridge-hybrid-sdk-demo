# Phase 3: Logging and Failure Simulation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md; this log preserves the alternatives considered.

**Date:** 2026-04-06
**Phase:** 03-logging-and-failure-simulation
**Areas discussed:** Scenario control model, structured log/export model, diagnostics UI delivery, evidence field scope

---

## Scenario control model

| Option | Description | Selected |
|--------|-------------|----------|
| Preset selector | Named support-oriented scenarios such as timeout and callback loss | yes |
| Toggle matrix | Individual switches for each failure behavior | |
| Scenario scripting | Free-form scenario composition or mini scripts | |

**User's choice:** Preset selector  
**Notes:** Recommended as the most portfolio-appropriate model because it communicates realistic support cases clearly and keeps the current demo stable.

---

## Structured log and export model

| Option | Description | Selected |
|--------|-------------|----------|
| Request timeline + JSON export | `correlationId` groups bridge and SDK stages into a structured exportable record | yes |
| Flat event list | Append-only event list without request grouping | |
| Human log only | On-screen diagnostics without structured export | |

**User's choice:** Request timeline + JSON export  
**Notes:** Chosen because the target job values evidence that can prove external SDK issues and feed into a completion report.

---

## Diagnostics UI delivery

| Option | Description | Selected |
|--------|-------------|----------|
| Existing page + scenario panel + detail view | Extend the current WebView demo without moving the workflow elsewhere | yes |
| Separate diagnostics screen | Keep the current screen simple and move debugging tools elsewhere | |
| Minimal UI change | Keep only the current log and no scenario tooling | |

**User's choice:** Existing page + scenario panel + detail view  
**Notes:** Chosen to preserve the “hybrid app with minimal invasive change” story and make the tooling feel like a real stabilization pass.

---

## Evidence field scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full support evidence | Include `stage`, `retryCount`, `durationMs`, `scenario`, `vendorCode`, `retryable`, `resolvedByRetry` | yes |
| Minimal metadata | Record only correlation and status | |
| Summary-only export | Export only final outcome summaries | |

**User's choice:** Full support evidence  
**Notes:** Chosen because the target job explicitly asks for logs that can prove an external SDK issue and show retry-based recovery behavior.

---

## the agent's Discretion

- Exact scenario preset labels
- Exact JSON export envelope structure
- Exact diagnostics panel layout and detail-view presentation

## Deferred Ideas

- Async hardening and duplicate callback suppression stay in Phase 4
- iOS parity for the same diagnostics model stays in Phase 5
