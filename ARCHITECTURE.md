# Architecture

## Non-negotiable constraints
- Native Android application; no Android WebView, GeckoView, Chromium, or third-party browser/rendering engine.
- Browser, AdBlock, and Privacy are first-party modules with explicit APIs and one-way dependencies.
- No telemetry, analytics, mandatory account, or required cloud service. Persistent browsing data is local-first.
- No feature is marked Done without implementation and a repeatable test proving it.
- Android platform APIs may be used for primitives (Canvas, networking sockets/TLS, Keystore, MediaCodec, permissions, biometrics). They are not a substitute browser engine.
- No third-party JavaScript runtime or rendering engine unless the owner explicitly approves a deviation.

## Module responsibilities
| Gradle module | Responsibility | Must not depend on |
|---|---|---|
| :app | Jetpack Compose UI, navigation, lifecycle, user settings | Engine internals; depends on public module APIs |
| :engine-html | HTML tokenizer, tree builder, DOM model | CSS, JS, render, app |
| :engine-css | CSS tokenizer/parser, selectors, cascade, computed styles | render, JS, app |
| :engine-js | First-party ECMAScript interpreter subset and host bindings | app, render implementation |
| :engine-net | HTTP(S), TLS policy, DNS/DoH, request pipeline, cache, resource loading | app, UI |
| :privacy | Cookie/storage policy, URL/referrer policy, permissions, anti-tracking policy | app, renderer |
| :adblock | Filter parsing/compilation/matching, network and cosmetic rules | app, net implementation |
| :engine-render | Layout, paint/display list, Android Canvas backend, hit testing | app UI; consumes HTML/CSS public models |
| :media | Android MediaCodec integration, audio/video controls, subtitles, PiP integration | browser renderer internals |
| :storage | Local history, bookmarks, tabs metadata, encrypted credentials, settings | UI |
| :tools | Reader mode, find-in-page, QR integration, translation adapter, offline pages | app UI internals |
| :diagnostics | Local-only logs, request inspector, performance and crash diagnostics | telemetry/cloud services |

## Dependency direction
- :app -> public APIs from all feature modules.
- :engine-net -> :privacy API and :adblock request-decision API through interfaces.
- :engine-html -> no browser feature module.
- :engine-css -> :engine-html model API.
- :engine-js -> DOM interfaces from :engine-html; no renderer dependency.
- :engine-render -> :engine-html and :engine-css model APIs; optional narrow JS DOM event interface.
- :media -> Android platform APIs and narrow app/browser media contracts.
- :storage -> AndroidX/platform storage and Android Keystore; no network dependency.
- :tools -> narrow public APIs from HTML, storage, and net as needed.
- :diagnostics -> module event interfaces; local storage only.
- No circular Gradle project dependencies. Cross-module collaboration uses interfaces/events in the consuming layer or a small contracts module only if a real cycle is demonstrated.

## Data flow
- Network requests: normalize URL -> privacy policy -> adblock decision -> DNS/TLS/connect -> response/cache.
- HTML bytes -> tokenizer/tree builder -> DOM -> CSS cascade/computed style -> layout/display list -> Canvas.
- JavaScript runs only in a deliberately documented subset initially; DOM bindings use explicit interfaces.
- UI observes immutable state/events and never receives mutable internal engine objects.

## Security and privacy baseline
- HTTPS by default; cleartext disabled unless an explicit developer/test configuration requires a local test server.
- Validate schemes, redirects, MIME types, download destinations, and resource sizes.
- Do not execute untrusted page scripts with Android application privileges.
- Isolate origin-scoped cookies/storage; private sessions use ephemeral storage and are not written to history.
- Secrets use Android Keystore-backed encryption; never log credentials or page contents.
- Fingerprint randomization/noise is not claimed as strong anonymity. VPN/Tor is optional integration and does not make the browser Tor Browser-equivalent.
- DRM is supported only through platform/provider-authorized paths; no DRM or access-control circumvention.

## Engineering reality / explicit limitations
- A complete modern HTML/CSS/ECMAScript browser engine is a multi-year, standards-scale effort. This project will build a small first-party engine with a published compatibility matrix, not claim Chrome/Firefox compatibility.
- Modern JavaScript compatibility, complex CSS layout, accessibility, Web APIs, service workers, WebAssembly, WebGL, and broad site compatibility are not assumed.
- Widevine DRM depends on licensed platform components and provider/device policy; a custom renderer cannot implement Widevine merely by adding Kotlin code.
- Android Canvas drawing is a viable first milestone backend, but advanced GPU compositing and full hardware-accelerated browser rendering are later research items.
- MediaCodec is a codec API, not a complete streaming stack. Adaptive streaming, encrypted media, subtitles, and site player integration require separate compatibility work.
- DoH and VPN/Tor support must be evaluated against Android networking constraints and leak tests; do not promise system-wide routing from an ordinary app.

## Testing strategy
- Pure Kotlin unit tests for tokenizers, parsers, DOM, CSS, URL policy, filter matching, storage policy, and state machines.
- Golden/fixture tests for HTML/CSS and filter-list compatibility.
- Local deterministic HTTP/TLS test server tests for redirects, caching, HTTPS-only, DoH policy, and request interception.
- Instrumented tests for Android permissions, Keystore, MediaCodec availability, lifecycle, Compose UI, and APK launch.
- Benchmarks for parsing, matching, layout, memory, and page loading.
- Real-site tests are compatibility observations, not proof of general standards compliance.
