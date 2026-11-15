# Architecture

## Product constraints
- Kotlin, native Android, Jetpack Compose, Gradle multi-module.
- Prohibited: Android WebView, GeckoView, Chromium, and all third-party browser/rendering engines.
- Browser, AdBlock, Privacy and Search are first-party modules with public interfaces and no circular Gradle dependencies.
- No telemetry, analytics, mandatory account or required cloud sync. Data is local-first.
- Android platform primitives are allowed: Canvas, networking/TLS, DNS APIs, Keystore, MediaCodec, permissions and biometrics.
- No feature is Done without implementation and a repeatable passing test.

## Modules and ownership
| Module | Responsibility | Dependency policy |
|---|---|---|
| :app | Compose UI, navigation, lifecycle, settings | Consumes public APIs only |
| :engine-html | HTML tokenizer, tree builder, DOM | Independent core |
| :engine-css | CSS parser, selectors, cascade, computed style | -> :engine-html |
| :engine-js | First-party ECMAScript interpreter subset and DOM bindings | -> :engine-html contracts only |
| :engine-render | Layout, display list, Canvas drawing, hit testing | -> :engine-html, :engine-css |
| :engine-net | HTTP(S), TLS, DoH, request pipeline, cache | -> :privacy, :adblock public policy APIs |
| :adblock | Rule parsing, compilation, matching, cosmetic rules | Independent policy engine; no :engine-net dependency |
| :privacy | URL, cookie, referrer, origin and permission policies | Independent policy engine; no :engine-net dependency |
| :search | SearchProvider contracts, provider implementations, merge/rank | Independent search module |
| :media | Android MediaCodec and playback integration | Platform media APIs only |
| :storage | Local history/bookmarks, encrypted credentials, settings | Android Keystore/platform storage |
| :tools | Reader mode, QR, translation adapters, find-in-page, offline pages | Narrow public contracts |
| :diagnostics | Local diagnostics, request inspector, resource metrics | No telemetry/cloud dependencies |

## Dependency direction
- :app -> all modules through public APIs.
- :engine-css -> :engine-html.
- :engine-js -> :engine-html.
- :engine-render -> :engine-html + :engine-css.
- :engine-net -> :privacy + :adblock interfaces. :privacy and :adblock must never depend on :engine-net.
- :tools -> :engine-html and :storage contracts only where needed.
- :search, :media, :storage, :diagnostics have no dependency on app.
- No cycles. If a future contract creates a cycle, move the smallest stable interface to a narrowly scoped contracts module after review.

## Request and rendering pipelines
1. Navigation input is parsed and normalized.
2. Privacy policy evaluates URL, referrer, cookie and origin rules.
3. AdBlock policy evaluates request context and returns allow/block/modify plus reason.
4. Network module applies TLS, redirect, timeout, DNS and cache rules.
5. Response bytes enter HTML tokenizer/tree builder and DOM.
6. CSS parsing/cascade computes styles.
7. Layout produces display-list commands.
8. Android Canvas backend draws the display list. UI receives immutable state/events.

## Security baseline
- HTTPS by default; cleartext disabled in production.
- Strict scheme and redirect validation; bounded response size, concurrency and timeouts.
- Page content is untrusted; no Android application privileges exposed to page scripts.
- Origin-scoped storage and ephemeral private profiles. Private profiles use separate in-memory/session-only cookie, cache, history, form and download state; private close/panic cleanup must be verified by storage inspection.
- User-configured private-site rules route matching destinations into isolated private tabs. Private UI uses FLAG_SECURE, redacted notifications and optional app-lock/neutral launcher identity. Platform launcher alias behavior may vary.
- Private mode does not hide the network address: destination sites still observe the user's IP unless a separately configured network privacy service changes routing. DoH protects DNS queries only; VPN/Tor have separate trust, availability and leak limitations. No anonymity or Tor Browser equivalence claim.
- Site notifications default to denied in private mode. Popup/redirect/download defenses are policy decisions; APK downloads and fake update prompts are blocked by default. Age-verification and other access-control bypass is explicitly out of scope.
- Secrets encrypted with Android Keystore; never log credentials or page content.
- DoH, WebRTC controls, fingerprinting mitigation and VPN/Tor require leak tests; no anonymity equivalence claims.
- DRM only through authorized platform/provider support. No DRM/access-control bypass.

## Feasibility and explicit limits
- A complete modern browser engine is a standards-scale, multi-year effort. This project will implement a scoped first-party engine and publish compatibility results.
- Full modern ECMAScript, complex CSS, service workers, WebAssembly, WebGL, accessibility parity and broad commercial-site compatibility are not assumed.
- Widevine requires licensed platform components and provider/device authorization; Kotlin code alone cannot provide it.
- Canvas is the initial rendering backend. GPU acceleration is not claimed until measured on devices.
- MediaCodec is a codec API, not a complete streaming/DRM stack.
- Search providers use legal official APIs or ordinary result pages. Providers requiring credentials are clearly marked Needs API key. Meta-search is opt-in and warns that queries are sent to every enabled provider.

## Verification
- Unit tests for parsers, matchers, policies, ranking and state machines.
- Fixture tests for HTML/CSS and filter syntax.
- Deterministic local HTTP/TLS test servers for redirects, cache and HTTPS-only behavior.
- Instrumented tests for Compose, permissions, Keystore, MediaCodec and lifecycle.
- Benchmarks for parsing, matching, layout and memory.
- Real-site checks are compatibility observations, not proof of standards compliance.
