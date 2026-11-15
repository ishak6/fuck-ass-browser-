# Roadmap and Approval Gate

This is a proposal only. No milestone implementation is authorized until the owner approves this roadmap. After approval, complete one milestone at a time and stop for an explicit "continue".

## Reality check
A standards-complete browser engine is not a realistic short project. The deliverable will be a first-party, deliberately scoped browser with a published compatibility matrix. No Android WebView, GeckoView, Chromium, third-party rendering engine, or third-party JavaScript runtime will be introduced. If a requested feature cannot be implemented within these constraints, mark it Partial/Not started and request approval before changing constraints.

## Milestones and order

### M1 — Repository reset, modular skeleton, and CI
Scope:
- Remove the prohibited GeckoView dependency and implementation.
- Establish Kotlin/Android Gradle multi-module structure for all requested modules.
- Define public contracts and enforce one-way dependency graph.
- Native Compose shell with an explicit "engine not ready" state; do not display remote pages yet.
- GitHub Actions: assembleDebug, unit tests, lint, and upload debug APK.
- Add baseline unit test in each module and verify dependency graph.
Definition of done:
- No WebView/GeckoView/Chromium or third-party rendering/JS engine dependency in Gradle/source.
- All required modules exist and compile.
- ./gradlew assembleDebug test lint succeeds.
- CI run succeeds and debug APK artifact is present.
- Diff reviewed; FEATURES.md accurately updated.
Risk: Gradle plugin/Kotlin/Compose versions and Android SDK availability must be verified in CI.

### M2 — :engine-net foundation
Scope:
- Safe HTTP(S) request/response model, TLS validation, redirect policy, timeouts, bounded resource loading.
- HTTPS-only policy interface and origin model.
- In-memory/disk cache with explicit size/expiry policy.
- DNS resolver abstraction; investigate DoH with a deterministic test endpoint.
Definition of done:
- Local HTTP/TLS integration tests prove requests, invalid-cert rejection, redirects, cache behavior, and HTTPS-only policy.
- No cleartext by default; resource and redirect limits tested.
- DoH only marked Done after endpoint and leak-policy tests; otherwise Partial.

### M3 — :privacy policy foundation
Scope:
- URL tracking-parameter removal, referrer policy, cookie and third-party cookie decisions, origin-scoped storage contracts, private profile policy, permission decision model.
- Integrate policy hooks with :engine-net without circular dependencies.
Definition of done:
- Unit/integration tests prove policy decisions and origin isolation.
- Private data is not persisted; deletion tests pass.
- Fingerprinting resistance claims remain scoped to surfaces actually controlled.

### M4 — :adblock parser and matcher
Scope:
- First-party parser/compiler/matcher for a documented ABP-compatible subset.
- Network request filtering integrated through :engine-net policy interface.
- Custom rules, exceptions, per-site controls, block counter.
- Fixture testing against pinned EasyList/AdGuard/uBO samples; record unsupported syntax.
Definition of done:
- Rule parser and matcher tests pass, with false-positive/false-negative fixture report.
- Filter-list version/hash recorded.
- No claim of full uBlock Origin compatibility.
- Cosmetic filtering waits for a working DOM/selector engine.

### M5 — :engine-html tokenizer, tree builder, and DOM
Scope:
- HTML tokenizer and scoped tree builder.
- DOM node model, traversal, mutation, serialization.
- Explicitly scoped HTML conformance subset.
Definition of done:
- Parser fixture suite and malformed-input tests pass.
- Resource limits and adversarial nesting tests pass.
- Unsupported HTML features documented.

### M6 — :engine-css
Scope:
- CSS tokenizer/parser, basic selectors, specificity, cascade, computed style.
- Start with a documented CSS subset.
Definition of done:
- CSS parser and selector/cascade fixtures pass.
- Unsupported selectors/properties and inheritance behavior listed in compatibility matrix.

### M7 — :engine-render and first visible page
Scope:
- Block/inline layout subset, text wrapping, scrolling, hit testing, display list, Android Canvas drawing.
- Connect HTML/CSS pipeline to Compose host through a narrow rendering surface.
Definition of done:
- Deterministic layout and screenshot/golden tests pass.
- Simple local HTML pages render and interact on emulator/device.
- Performance/memory baseline recorded.
Risk: complex layout, fonts, SVG, transforms, compositing and modern CSS remain partial.

### M8 — :engine-js interpreter research spike (separate approval checkpoint)
Scope:
- Before implementation, produce ECMAScript scope, interpreter design, threat model, and effort estimate.
- If approved, implement a small first-party interpreter subset and safe DOM bindings.
Definition of done:
- Only declared syntax/features are tested and listed.
- Script isolation, execution budgets, recursion/heap limits and interruption tests pass.
- No compatibility claim beyond passing subset tests.
Risk: modern ECMAScript compatibility is very large; this milestone may need several sub-milestones and may remain Partial for a long time.

### M9 — :storage and browser tools
Scope:
- Local tabs metadata, bookmarks, history, search history, downloads metadata, encrypted credentials.
- Reader mode, find-in-page, offline snapshots, QR scanner, share, translation adapter.
Definition of done:
- CRUD, migration, backup/restore, encryption, private-mode erasure and UI tests pass.
- Translation provider/network disclosure is explicit; no mandatory cloud account.

### M10 — :media and UI completion
Scope:
- Android MediaCodec capability-based playback surface, controls, subtitles, PiP, background audio where supported.
- Compose tabs, toolbar, settings, privacy dashboard, per-site controls, theme and gestures.
Definition of done:
- Device/emulator media tests pass for supported formats and capabilities.
- PiP/background behavior verified on a physical device.
- DRM reported only for provider-authorized devices/content; Widevine not promised.

### M11 — diagnostics, hardening, and release
Scope:
- Local request inspector with secret/header redaction, resource monitor, crash recovery, phishing-list integration, app lock, one-tap data clear.
- Security, privacy, performance, accessibility and compatibility audit.
- Signed release workflow and installation tests.
Definition of done:
- ./gradlew assembleDebug test lint green.
- Release APK signed with owner-controlled key; signing secrets never committed.
- Emulator install/launch test and physical-device installation report.
- Published feature-by-feature compatibility and known-limitations report.
- No telemetry/analytics and no unresolved critical security findings.

## Per-milestone mandatory workflow
1. Implement only the approved milestone.
2. Add unit tests and relevant instrumented/integration tests.
3. Run ./gradlew assembleDebug test lint; inspect and fix every failure, then rerun.
4. If local Android SDK/emulator/device is unavailable, report that limitation and use CI; never fabricate test output.
5. Self-review diff for crashes, null safety, lifecycle leaks, permissions, cleartext, injection, resource exhaustion, privacy leaks and performance.
6. Update FEATURES.md with evidence-based statuses.
7. Commit with a descriptive message and report test logs/results, partial work, missing items and known bugs.
8. Stop. Do not begin the next milestone until the owner says "continue".

## Approval required
Please review this roadmap. No implementation milestone will begin until you explicitly approve it. You can approve as written or request changes to milestone scope/order.
