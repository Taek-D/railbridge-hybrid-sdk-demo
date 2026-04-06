# Phase 2: Android Adapter Seam - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `02-CONTEXT.md`.

**Date:** 2026-04-06
**Phase:** 02-android-adapter-seam
**Areas discussed:** adapter boundary, bridge ownership, contract compatibility

---

## Adapter Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Add `RailPlusSdkAdapter` as a small bridge-facing seam | Wrap the current mock SDK and preserve future vendor replacement options | yes |
| Keep direct `MockRailSdk` coupling | Lowest change count but weak for long-term architecture credibility | |
| Push adapter deeper into the SDK class only | Hides some behavior but keeps the bridge tightly bound to SDK details | |

**User's choice:** Keep the proposed small adapter seam in front of `MockRailSdk`.
**Notes:** The current Android demo must stay working; the seam is for gradual extension, not a rewrite.

---

## Bridge Ownership

| Option | Description | Selected |
|--------|-------------|----------|
| `WebViewActivity` assembles the adapter, `NativeBridge` orchestrates bridge calls | Preserves current activity setup role and keeps bridge focused on request handling | yes |
| Move all SDK lifecycle and construction into `NativeBridge` | Centralizes logic but increases bridge responsibility | |
| Introduce a larger service layer in this phase | Cleaner long term but expands scope beyond the adapter seam | |

**User's choice:** Keep construction in `WebViewActivity` and route bridge operations through the adapter.
**Notes:** This keeps the phase small and aligned with the existing Android wiring.

---

## Contract Compatibility

| Option | Description | Selected |
|--------|-------------|----------|
| Preserve current response fields and add metadata optionally | Keeps the current WebView handler working while enabling future diagnostics | yes |
| Replace the response schema with a new v2 object | Cleaner schema, but breaks the current demo flow | |
| Delay all metadata until a later phase | Lowest risk now, but weakens the adapter phase deliverable | |

**User's choice:** Keep the existing contract and append optional metadata fields only.
**Notes:** The current 4-button flow remains the regression baseline for this phase.

---

## the agent's Discretion

- Exact class names and package placement for the adapter layer
- Internal helper shape for optional metadata injection

## Deferred Ideas

- Scenario control UI and deterministic failure selection
- Structured log export and richer diagnostics surface
- Async race-condition hardening beyond what is required for the seam
