# RESEARCH SUMMARY

**Milestone:** `v0.2 포트폴리오 확장`
**Date:** 2026-04-06

## Existing Baseline

- The repo already demonstrates the Android half of a hybrid app: WebView UI, native bridge entrypoints, mock SDK calls, retry behavior, and local log persistence.
- The current baseline is credible as a runnable Android demo, but not yet as a full troubleshooting portfolio piece because there is no adapter seam, no deterministic scenario control, and no iOS parity.

## Key Findings

### 1. The strongest upgrade path is additive, not replacement-based

The Android demo is already validated, so the safest portfolio strategy is to preserve:

- current bridge method names
- current Android Studio build path
- current WebView demo flow

This lowers regression risk while still allowing deeper architecture to be layered in behind the UI.

### 2. The adapter seam is the most important architectural credibility boost

Right now the bridge depends directly on `MockRailSdk`. For portfolio positioning, an adapter seam matters because it demonstrates how a real legacy integration would isolate:

- vendor-specific result mapping
- retry eligibility
- timeout behavior
- duplicate or missing callbacks

This is the clearest way to show “closed SDK troubleshooting” thinking without needing the actual vendor binary.

### 3. Deterministic failure simulation matters more than more random failures

Random failure percentages are useful for a demo but weak for debugging evidence. Portfolio value increases when the project can force:

- timeout
- internal error
- callback never returned
- duplicate callback
- retry exhausted

That makes the project suitable for a debugging report and side-by-side platform comparison.

### 4. Structured tracing is what turns the demo into proof

The job-style use case is not just “handle errors” but “prove where failure happened.” That means the project needs structured request tracing across:

- JS entry
- native validation
- SDK execution start
- SDK callback
- JS callback delivery

Correlation IDs and exported logs are the minimum evidence model for that story.

### 5. iOS parity is required for the portfolio positioning goal

Because the target positioning includes hybrid troubleshooting on both native platforms, iOS cannot remain a document-only afterthought. A runnable `WKWebView + Swift bridge + mock adapter` sample is the cleanest parity story.

## Recommended Stack Direction

- Keep Android in Java and preserve the current module
- Add an Android `RailPlusSdkAdapter` interface plus scenario config types
- Add an iOS demo in Swift using `WKWebView` and a matching adapter protocol
- Keep logging local/exportable first; treat Crashlytics as an integration hook, not a configured backend

## Watch Outs

- Do not replace the current bridge response format outright; expand it additively
- Do not let iOS work block Android hardening; keep platform parity as a later phase
- Do not imply real vendor behavior that the mock cannot honestly support
- Do not bury the git gap; phase work should acknowledge that the repo is not yet under version control
