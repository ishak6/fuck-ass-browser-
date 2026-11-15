# Progress

## Current state
- Repository was reset to an empty main-branch tree before this clean rebuild.
- Step 0 repository inspection: complete; default branch is `main`.
- Current milestone: M1 — skeleton, modules and CI.
- M1 implementation: not started at the time this file was drafted.
- Build/test evidence: none yet.
- Next: create multi-module Gradle project, native Compose shell, baseline module tests and GitHub Actions workflow.

## Finished
- None in the new build.

## Known limitations/risks
- No third-party rendering engine is allowed. A first-party standards-compatible browser is a very large engineering effort.
- Full modern JavaScript, Widevine DRM, WebGL and broad modern-site compatibility cannot be promised.
- GitHub Actions is the available build environment; local Android SDK/emulator access is not established.
- Gradle wrapper binary may need to be generated in a trusted Gradle environment; CI can install a pinned Gradle distribution directly until then.

## Resume instructions
Read this file, FEATURES.md and ROADMAP.md. Continue the current milestone only. Never mark a feature Done without passing tests.
