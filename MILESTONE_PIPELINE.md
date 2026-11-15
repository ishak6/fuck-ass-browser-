# Milestone Pipeline Execution

The user-requested execution model covers M0 through M12 and is pipelined, not a single end-of-project test pass.

## Pipeline

1. Finish the implementation scope for the current milestone.
2. Commit the milestone so GitHub Actions starts immediately.
3. While that run executes, begin implementation of the next milestone on the latest main branch.
4. Inspect the earlier milestone's actual build/test/lint result and job logs.
5. If a prior run fails, fix the defect on the current branch, add or correct a regression test, and rerun CI. Do not mark the milestone verified until a successful run for the relevant commit is visible.
6. Continue through M11, keeping each milestone's implementation and evidence recorded in FEATURES.md and PROGRESS.md.
7. M12 is the final release gate: clean build/test/lint, integration and regression suite, security/performance/compatibility audit, APK artifact verification, signing readiness and dated device test matrix.

## Safety and quality gates

- Never knowingly build on a broken dependency; a failed earlier milestone is a blocking defect for dependent runtime behavior.
- Independent modules may be developed while an earlier CI run is running, but dependent features must use stable interfaces and receive integration tests.
- Do not claim local emulator/device testing: this environment has no Android SDK/emulator. Use GitHub Actions for build/unit/lint and explicitly leave device-only checks pending.
- Do not claim full HTML/CSS/JavaScript standards support, Widevine, broad site compatibility, or YouTube ad-block success without the required evidence.
- Do not introduce WebView, GeckoView, Chromium, another browser engine, telemetry, mandatory accounts, or hidden collection.
- Record commit SHA, Actions run URL, and actual conclusion for each verified milestone.
