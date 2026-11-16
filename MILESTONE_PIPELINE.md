# Milestone Pipeline Execution

The execution plan covers M0 through M49 and is pipelined, not a single end-of-project test pass.

## Pipeline
1. Finish the implementation scope for the current milestone.
2. Commit the milestone so GitHub Actions starts immediately.
3. While that run executes, begin implementation of the next independent milestone on the latest main branch.
4. Inspect the earlier milestone's actual build/test/lint result and job logs.
5. If a prior run fails, fix the defect on the current branch, add or correct a regression test, and rerun CI. Do not mark a milestone verified until a successful run for the relevant commit is visible.
6. Keep FEATURES.md, PROGRESS.md and ROADMAP.md synchronized with implementation and evidence.
7. M12 is the original core-engine validation gate for M0–M11; it is not the final release gate for extended features.
8. M13–M48 are feature milestones. M49 combines offline-search handling with the final whole-project release gate: clean build/test/lint, integration/regression, security/performance/compatibility review, APK/signing verification and dated device matrix.

## Safety and quality gates
- Never knowingly build on a broken dependency; a failed earlier milestone is a blocking defect for dependent runtime behavior.
- Independent modules may be developed while an earlier CI run is executing, but dependent features must use stable interfaces and receive integration tests.
- Do not claim local emulator/device testing: this environment has no Android SDK/emulator. Use GitHub Actions for emulator/build/unit/lint checks and explicitly leave physical-device checks pending.
- Do not claim full HTML/CSS/JavaScript standards support, Widevine, broad site compatibility, or YouTube ad-block success without required evidence.
- Do not introduce WebView, GeckoView, Chromium, another browser engine, telemetry, mandatory accounts, hidden collection, DRM bypass or access-control evasion.
- Record commit SHA, Actions run URL, and actual conclusion for each verified milestone.
