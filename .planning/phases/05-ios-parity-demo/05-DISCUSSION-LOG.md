# Phase 5: iOS Parity Demo - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md; this log preserves the alternatives considered.

**Date:** 2026-04-08
**Phase:** 05-ios-parity-demo
**Areas discussed:** iOS project placement, parity depth, shared WebView/bridge contract, iOS verification depth

---

## iOS project placement

| Option | Description | Selected |
|--------|-------------|----------|
| `ios/` subtree | New Xcode-openable project under `ios/` inside the current repo | yes |
| Separate sibling demo folder | Keep iOS outside the main repo tree layout | |
| Defer scaffold | Keep Phase 5 conceptual only and postpone Xcode project creation | |

**User's choice:** Recommended defaults  
**Notes:** Locked to the recommended option: add a new iOS project under `ios/` so the repo reads as one portfolio artifact with parallel Android/iOS demos.

---

## parity depth

| Option | Description | Selected |
|--------|-------------|----------|
| Lightweight parity app | Match the current Android demo shape with a minimal shell leading to a WebView diagnostics screen | yes |
| Product-like demo app | Expand iOS into a broader multi-screen experience | |
| Library-only parity | Implement only underlying bridge/adapter pieces without a runnable UI shell | |

**User's choice:** Recommended defaults  
**Notes:** Locked to the recommended option: keep iOS as a lightweight runnable parity demo rather than widening the app scope.

---

## shared webview and bridge contract

| Option | Description | Selected |
|--------|-------------|----------|
| Reuse existing HTML/JS as much as possible | Keep the operator-facing diagnostics page aligned across Android and iOS | yes |
| Build separate iOS page | Recreate the UI natively or with platform-specific web assets | |
| Partial parity only | Reuse method names but allow wider page/contract drift | |

**User's choice:** Recommended defaults  
**Notes:** Locked to the recommended option: preserve the same four bridge actions and reuse the diagnostics page or behavior as closely as practical.

---

## iOS verification depth

| Option | Description | Selected |
|--------|-------------|----------|
| Simulator smoke verification | Focus on build/run plus core flow checks in the simulator | yes |
| Heavy automated harness | Add deeper XCTest/UI-test infrastructure in this phase | |
| Minimal compile-only verification | Stop at building without runtime parity checks | |

**User's choice:** Recommended defaults  
**Notes:** Locked to the recommended option: Phase 5 should verify runnable parity in the simulator without over-investing in test infrastructure yet.

---

## the agent's Discretion

- Exact Xcode project name and internal folder layout
- Swift type naming for bridge, adapter, scenario control, and diagnostics helpers
- Whether HTML assets are copied or referenced through a shared repo path, as long as behavior remains aligned

## Deferred Ideas

- XCTest/UI automation beyond smoke-level parity
- Shared Android/iOS abstraction layer work
- Portfolio packaging and narrative docs for the finished multi-platform story
