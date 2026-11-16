# Roadmap

The project is executed in dependency order. Each milestone must build, test, pass lint, receive a diff/security review, update FEATURES.md and PROGRESS.md, and be committed before the next begins.

| Milestone | Scope | Definition of Done |
|---|---|---|
| M0 | Repository audit, requirements, module/dependency architecture, milestone plan and CI foundation | Repository tree and dependency direction reviewed; no prohibited rendering engine or telemetry; baseline CI workflow exists |
| M1 | Multi-module skeleton, native Compose shell, lifecycle and debug APK | All modules compile; no prohibited engine dependency; unit tests, lint and assembleDebug pass in CI; APK artifact exists |
| M2 | :engine-net HTTP/TLS, HTTPS-only, redirect limits, cache, DoH research | Local deterministic integration tests prove TLS rejection, redirects, cache, HTTPS-only; DoH only marked Done after leak/endpoint tests |
| M3 | :adblock parser, compiler, matcher, benchmark, YouTube filter contracts | Pinned EasyList/AdGuard/uBO subset fixtures; YouTube request/cosmetic/scriptlet fixtures; scheduled update integrity/rollback tests; warning/refresh state tests; unsupported syntax documented; request policy API integration. Real YouTube success is not claimed without a dated device checklist. |
| M4 | :privacy URL cleaning, cookies, referrer, origin isolation, permissions, private-site policy contracts | Unit/integration tests prove URL/header/cookie policy and origin isolation |
| M5 | :search providers, modes, meta-search and ranking | Saved provider fixtures; timeout/rate-limit/offline tests; merge opt-in; no scraping against terms |
| M6 | :engine-html tokenizer, tree builder and DOM | HTML fixture/conformance subset, malformed/adversarial input and resource-limit tests |
| M7 | :engine-css parser, selectors, cascade, computed style | CSS syntax, selector, specificity and cascade fixtures |
| M8 | :engine-render layout, Canvas drawing and app page surface | Golden layout/render tests; local pages render and scroll on device; performance baseline |
| M9 | :engine-js first-party interpreter subset and DOM bindings | Published ECMAScript subset coverage; execution budget/isolation tests; no broad compatibility claim |
| M10 | :media, :storage, :tools, ephemeral private profile plus durable normal bookmarks/history | Device tests for supported codecs/PiP; Keystore tests; CRUD/backup/offline/tool tests |
| M11 | Browser UX, navigation lifecycle, tabs, settings, diagnostics, private-site UX (rules, panic, lock, FLAG_SECURE, redaction), advanced and security controls | Compose/instrumented tests, redaction, clear-data and permission tests; no telemetry |
| M12 | Original core-engine validation gate | Core M0–M11 build/test/lint, integration/security/performance review and baseline APK verification; does not release the extended M13–M49 feature set |

## M12 final release gate\n- Full clean build/test/lint and integration suite on the final commit.\n- Security review: transport/TLS, redirects, storage boundaries, private-session cleanup, input/resource limits, permission handling and dependency audit.\n- Performance review: tokenizer/parser/layout budgets, large documents, memory use, navigation cancellation and scroll/render timing.\n- Release artifact/signing verification and install/launch test on emulator/device.\n- Publish a real-site pass/fail matrix with unsupported features and dated manual test evidence.\n\n## Per-milestone gate
- Implement only the current milestone.
- Add unit and relevant instrumented/integration tests.
- Run `./gradlew assembleDebug test lint`; fix and rerun until green.
- If no local SDK/emulator exists, use GitHub Actions and report the limitation; never invent output.
- Review null safety, lifecycle leaks, permissions, cleartext, injection, resource exhaustion, privacy and performance.
- Update status only with evidence. Commit and push. Verify the corresponding Actions run.
- Do not silently substitute WebView, GeckoView, Chromium or another engine.
