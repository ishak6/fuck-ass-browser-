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

## M12 core validation gate
- Validate the original M0–M11 engine and app foundation before extended feature work is considered releasable.
- This is not the final release gate for the extended roadmap.

## Extended milestones M13–M49
| Milestone | Scope | Exit criteria |
|---|---|---|
| M13 | Android Keystore AES-GCM encryption | Non-exportable key, authenticated encryption, tamper/AAD/key-loss tests on emulator |
| M14 | Site Permission Manager | Per-origin permission decisions, Android runtime permission integration and revoke tests |
| M15 | Per-origin Storage Isolation | Scheme/host/port partitioning with cross-origin isolation tests |
| M16 | Advanced Cookie & Cache Controls | Per-site cookie policy, cache controls and persistence/clear tests |
| M17 | DNS over HTTPS | Opt-in resolver, strict TLS validation, fallback disclosure and DNS leak tests |
| M18 | WebRTC leak protection/testing | Explicit no-WebRTC baseline plus ICE/network leak tests before any implementation claim |
| M19 | Advanced anti-tracking | Tested tracker policy and transparent breakage controls; no fingerprinting anonymity claims |
| M20 | PIN/Biometric App Lock | Android BiometricPrompt/credential flow, Keystore-backed verification and lockout tests |
| M21 | Private Session Cleanup & Verification | End-to-end data inventory, deletion tests and private-profile isolation verification |
| M22 | MediaCodec Playback | Authorized media pipeline with supported-codec device tests |
| M23 | Fullscreen, Seek, Speed | Playback controls and lifecycle/orientation tests |
| M24 | Subtitle Support | Bounded SRT/WebVTT parser and synchronization fixtures |
| M25 | Picture-in-Picture | Android PiP lifecycle and instrumentation tests |
| M26 | Background Audio | Audio focus/service lifecycle tests and visible user controls |
| M27 | Secure Download Manager | User-consented downloads, bounded sizes, safe filenames and integrity tests |
| M28 | Offline Page Save/Open | Bounded offline archive, origin/resource policy and reopen tests |
| M29 | Multiple Tabs & Tab Groups | Tab lifecycle, isolation, restore and state tests |
| M30 | Dark/Light/System Theme | Persisted theme preference and Compose UI tests |
| M31 | Homepage/Start Page Customization | Persisted start-page settings and rendering tests |
| M32 | Toolbar Customization | Persisted toolbar layout and UI tests |
| M33 | Find in Page | DOM text search, navigation and Unicode tests |
| M34 | Page Zoom | Layout scaling and overflow/accessibility tests |
| M35 | Desktop Mode | User-agent/viewport policy tests and compatibility disclosure |
| M36 | QR Scanner | Camera permission, scan validation and denial-path tests |
| M37 | Translation | Provider adapter, explicit query disclosure and credential handling |
| M38 | Android Share | Safe share intents and URI permission tests |
| M39 | Network Inspector | Redacted request event model and sensitive-header tests |
| M40 | Resource Monitor | Bounded metrics and lifecycle/performance tests |
| M41 | Local Backup/Restore | Encrypted export/import, integrity and recovery tests |
| M42 | Crash Recovery | Process-death recovery and corrupted-state handling tests |
| M43 | Official Search API Integration | Official provider adapters, credential disclosure and fixture tests |
| M44 | Search Suggestions | Provider suggestions with debounce, cancellation and privacy tests |
| M45 | Meta-search Opt-in | Default-off preference, query-sharing disclosure and opt-in tests |
| M46 | Search Source Attribution | Accurate provider/source labels and UI tests |
| M47 | Timeout/Retry Management | Bounded retries, cancellation and deterministic timeout tests |
| M48 | Rate-limit Handling | 429/backoff handling and per-provider rate-limit tests |
| M49 | Offline Search Handling + Final Release Gate | Offline-state UX and complete clean build/test/lint, security/performance/compatibility audit, APK/signing verification and dated device matrix for the entire M0–M49 scope |

## Per-milestone gate
- Implement only the current milestone.
- Add unit and relevant instrumented/integration tests.
- Run `./gradlew assembleDebug test lint`; fix and rerun until green.
- If no local SDK/emulator exists, use GitHub Actions and report the limitation; never invent output.
- Review null safety, lifecycle leaks, permissions, cleartext, injection, resource exhaustion, privacy and performance.
- Update status only with evidence. Commit and push. Verify the corresponding Actions run.
- Do not silently substitute WebView, GeckoView, Chromium or another engine.
