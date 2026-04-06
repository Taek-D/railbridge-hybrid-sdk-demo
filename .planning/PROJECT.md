# PROJECT

**Project Name:** RailBridge Demo
**Repository Root:** `E:\프로젝트\위시켓\Android Java WebView Bridge + SDK 에러 핸들링 데모 — Qwen Code CLI 프롬프트`
**Last Updated:** 2026-04-06

## What This Is

Android Java + WebView bridge demo for simulating a hybrid transport-card SDK integration. The current codebase demonstrates:

- Android `JavascriptInterface` bridge methods for charge, balance, SDK status, and JS error reporting
- A mock SDK with retry handling and local error logging
- A runnable Android Studio project with verified emulator flow

The next milestone turns this from a single-platform demo into a portfolio-ready troubleshooting sample that better reflects hybrid app stabilization work.

## Core Value

Keep the existing Android demo stable while layering in architecture seams, richer diagnostics, repeatable failure simulation, and an iOS parity demo that prove legacy-troubleshooting capability.

## Current Context

- Android build currently succeeds with Gradle wrapper and Android Studio
- Existing Android demo flow is already manually verified on emulator
- Bridge methods currently depend directly on `MockRailSdk`
- Structured tracing, scenario control, and iOS parity are not implemented yet
- The workspace is not currently connected to a git repository, so GSD commit steps are blocked until git is initialized or the repo is attached to an existing remote

## Current Milestone: v0.2 포트폴리오 확장

**Goal:** Convert the current Android-only demo into a portfolio-ready hybrid SDK stabilization sample without breaking the existing Android demo flow.

**Target features:**
- Add an Android adapter seam and failure-scenario controls without removing current bridge actions
- Introduce correlation-based structured logging, exportable diagnostics, and bridge hardening
- Add a runnable iOS parity demo with the same bridge contract and mock SDK scenario model
- Document the architecture, debugging approach, and validation story as portfolio artifacts

## Active Requirements

- `BOOT-01` through `BOOT-02`
- `SDK-01` through `SDK-03`
- `BRG-01` through `BRG-02`
- `LOG-01` through `LOG-02`
- `STAB-01`
- `UI-01`
- `IOS-01` through `IOS-02`
- `DOC-01`

## Key Decisions

- Use a gradual extension strategy instead of replacing the current Android demo contract
- Keep existing Android bridge method names and baseline response fields stable
- Add metadata as optional response fields so the current WebView UI does not break
- Treat the mock SDK as a closed-SDK simulator via an adapter layer instead of coupling the bridge directly to `MockRailSdk`
- Keep Firebase / Crashlytics as a stub or integration point, not a real configured backend
- Build iOS as a runnable parity demo, not a production release target

## Constraints

- Existing Android `requestCharge`, `getBalance`, `getSdkStatus`, and `reportError` flows must keep working
- The repo currently has no iOS project scaffold
- The repo currently has no git metadata, so milestone commit automation cannot run yet
- Windows path contains non-ASCII characters, so Android tooling compatibility must stay intact

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition:**
1. Move validated requirements out of the active queue and record the phase reference.
2. Capture any new implementation constraints or edge cases discovered during execution.
3. Update decisions if the bridge contract or adapter strategy changes.
4. Adjust current context if the Android or iOS execution path changes.

**After each milestone:**
1. Review all requirements for carry-over vs completion.
2. Confirm the core value still reflects the portfolio goal.
3. Revisit out-of-scope items and decide whether they stay deferred.
4. Update current context to describe the new baseline state.
