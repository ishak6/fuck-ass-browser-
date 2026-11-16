# Progress

## Current state
- Repository inspected and reset; default branch: main.
- Step 0: complete. Architecture, feature tracker, roadmap and progress documents committed.
- M1: complete. Android multi-module skeleton, native Compose shell and CI are committed.
- M1 verification: GitHub Actions run #3 succeeded; all 11 job steps passed (wrapper generation, debug APK, unit tests, lint, artifact upload).
- Debug APK artifact: `custom-browser-debug-apk`, artifact ID `14177260929`, approximately 9.0 MB.
- Current execution mode: pipelined M0-M12 implementation; see `MILESTONE_PIPELINE.md` and `PROGRESS.md`.
- Latest M2 hardening commit `00905c062f304b828a43aa82b3e23623a9669a19`; GitHub Actions run #13 passed all 11 steps (debug APK, unit tests, lint, artifact upload).
- Private-site support is partially implemented: persistent IDN-normalized host rules, automatic private routing, ephemeral private profile, Compose toggle, panic clear and FLAG_SECURE. App lock and instrumented privacy verification remain pending.
- M3 includes a bounded network-rule parser only. YouTube cosmetic/scriptlet filtering and real-device verification remain unimplemented.

## Finished
- Step 0 repository inspection and planning documentation.
- M1 multi-module Gradle skeleton and CI verification.

## M1 verification
- GitHub Actions run: https://github.com/ishak6/fuck-ass-browser-/actions/runs/43752678236
- Build debug APK: passed.
- Unit tests: passed.
- Android lint: passed.
- APK artifact upload: passed.

## M2 implementation in progress
- First-party HTTP transport using platform `HttpURLConnection`.
- Platform TLS trust and hostname verification; no permissive trust manager.
- HTTPS-only requests; HTTPS downgrade redirects blocked.
- Redirect limit, connect/read timeouts, response-size cap and bounded in-memory GET cache.
- Request-body size limit and restricted framing/header validation.
- Privacy policy and network filter policy are evaluated on each redirect destination.
- Cache excludes responses with Set-Cookie or Vary headers and honors conservative Cache-Control directives.
- Unit tests cover URL/scheme validation, request policy hooks, request bounds, restricted headers, closed lifecycle, configured limits, cross-origin credential stripping, redirect destination policy and redirect count.
- Real HTTPS/TLS certificate integration, real redirect-chain integration, cache expiry integration and Android device tests remain pending; fake-connection redirect tests pass, but M2 is not complete.

## Verification
- M2 compile/test CI run #7 passed: https://github.com/ishak6/fuck-ass-browser-/actions/runs/43754706014
- YouTube requirements documentation CI run #8 passed: https://github.com/ishak6/fuck-ass-browser-/actions/runs/43754936931
- Latest hardening change CI run #13 passed: https://github.com/ishak6/fuck-ass-browser-/actions/runs/43756511454 (all 11 steps passed).

## Known limitations/risks
- No local Android SDK/emulator is available in this tool session; rely on GitHub Actions and report this limitation.
- This is an HTTP transport, not yet a full browser loading/resource scheduler.
- Profile-scoped in-memory cookie jars, redirect/final Set-Cookie handling and cookie stripping are implemented; durable normal-cookie persistence, HTTP/2 prioritization, service workers, DNS-over-HTTPS and certificate UI remain pending.
- Full modern JavaScript, Widevine DRM, WebGL and broad modern-site compatibility cannot be promised.
- An initial first-party HTML/CSS/layout/Canvas rendering path is integrated; it remains a limited subset, not a production browser engine.
- YouTube manual verification (video-start ad, mid-roll, banner/sponsored card, Shorts): NOT RUN on 2026-11-16; no real-device test was performed.
- Scope boundary: no anti-adblock evasion, protected stream extraction, DRM/access-control bypass, age-verification bypass or claim that server-side inserted ads can be blocked. Direct-stream player alternative requires explicit user approval.

## Resume instructions
Read this file, FEATURES.md and ROADMAP.md. Continue the current milestone only. Never mark a feature Done without passing tests.


## Pipelined implementation status — 2026-11-16
- M0-M1: architecture and CI scaffold verified.
- M2-M3: network transport and bounded filter parser implemented as partial subsets; M2 real TLS/device integration and full filter-list compatibility are still pending.
- M4-M5: privacy URL/referrer policies and HTTPS search-provider URL adapters implemented with unit tests. Provider API/search-result integration is pending.
- M6-M8: bounded HTML tokenizer/tree builder/DOM, CSS subset parser/cascade, initial layout and Android Canvas backend implemented. Full standards conformance and device rendering are not claimed.
- M9: bounded primitive JS interpreter implemented; it is not full ECMAScript and is not connected to page scripts/DOM.
- M10: bounded ephemeral private storage, playback state controller, reader-mode extraction and a file-backed durable normal bookmark/history store are implemented with unit tests; Keystore and actual MediaCodec/PiP pipeline remain pending.
- M11: Compose shell includes back/forward/refresh, bookmarks, normal/private history menus, always-private site controls, navigation cancellation, private-mode cache/cookie reset, FLAG_SECURE and IP disclosure. Full tab manager, app lock, permission UX and instrumented tests remain pending.
- Consolidated CI run #24 passed on commit `5bcc64d6f0`. Newer pipeline changes are being tested in Actions; do not treat that older pass as validation of the newest commit. This is CI source-snapshot verification only, not device validation.


## Active full-pipeline hardening — 2026-11-16
- M2: fixed 303 redirect handling so HEAD remains HEAD; added redirect policy-header regression coverage and active-connection cancellation. Real TLS/device tests remain pending.
- M3: canonicalized IDN/trailing-dot filter target hosts and added transport integration coverage. Full EasyList/AdGuard/uBO syntax and YouTube cosmetic/scriptlet features remain unsupported.
- M4: private-site host rules normalize IDN; private-mode transitions and panic clear cancel active navigation and clear memory cache/profile.
- M5: search provider endpoints reject userinfo/fragments and bangs are case-insensitive.
- M6: DOM mutation rejects self/ancestor cycles.
- M9: interpreter charges expression evaluation against step budgets and caps expression complexity.
- M10: private-profile stores are bounded with replaced cache buffers zeroed; file-backed normal bookmarks/history persist atomically with tests.
- M11: navigation generations prevent stale requests from updating a new session; normal history and private history use separate storage paths.
- M12: final release/security/performance/device compatibility gate remains outstanding.


## Current CI evidence (rolling pipeline)
- Run https://github.com/ishak6/fuck-ass-browser-/actions/runs/43762730518 passed on commit `1404cc457452`: debug APK build, unit tests, Android lint and APK artifact upload all succeeded.
- Run 43763268460 passed build, unit tests, lint and APK upload on `a00ee7552a`; run 43763237046 passed the same gates on `057f5a2393`. Later history-order and private-routing helper tests are still running; those commits are not yet verified.
- No physical-device or emulator verification has been performed.

- M4/M11: `FilePrivateSiteRulesStore` persists host rules and the app automatically selects the private profile for matching sites; app-level routing unit tests have been added and are pending CI.

- M12 infrastructure: added an Android emulator instrumentation test for private-mode FLAG_SECURE and a CodeQL v4 Java/Kotlin workflow. First emulator and CodeQL results are pending; no success is claimed yet.


## Extended feature implementation
| Feature | Module | Status | Test that proves it |
|---|---|---|---|
| Android Keystore AES-256-GCM encryption primitive | :storage | Done | API 35 storage instrumentation passed: round-trip, fresh random IV, AAD/tamper rejection, malformed envelope and key deletion; existing bookmark/history file is not yet encrypted |
| Per-origin site permission policy | :privacy | Partial | Unit tests for default ASK, fail-closed URL parsing, IDN/origin normalization, per-permission decisions, bounded store and clearing; persistence/UI/runtime grants pending |
| Origin-partitioned local/session storage primitive | :storage/:privacy | Partial | Unit tests for same-origin sharing, cross-origin/port/scheme isolation, IDN normalization, separate local/session buckets, quota enforcement and clear operations; DOM/persistence integration pending |


## Extended milestone verification update — 2026-11-16
- M13 Keystore cipher primitive: API 35 `:storage:connectedDebugAndroidTest` completed all five instrumentation tests successfully in run [43814740542](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43814740542). The overall Android workflow still failed in the separate app privacy UI test; this does not invalidate the passing storage test task.
- M14 site permission policy and M15 origin-storage primitive remain Partial: current code and unit tests cover bounded policy/partition logic, but Android permission mediation, UI, DOM integration and persistence are not complete.
- Private-mode window flag regression remains open. A synchronous flag update was committed in `d059ba7683292491e99d8b3657ab64bffb006ebb`; its fresh emulator result is required before marking the regression fixed.
- No physical-device validation is claimed.
