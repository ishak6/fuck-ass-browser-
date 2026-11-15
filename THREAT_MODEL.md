# Browser Threat Model

Status: engineering threat model for the experimental first-party browser. This is not a certification or penetration-test report.

## Assets

- Normal-profile cookies, bookmarks and browsing history.
- Private-profile cookies, in-memory history, form data and cache.
- User-entered URLs and search queries.
- Rendered page state and downloaded response bytes.
- User-controlled private-site rules and filter lists.

## Trust boundaries

1. **Remote origin to network transport:** all response headers, redirect targets and response bodies are untrusted.
2. **Network transport to parsers:** HTML, CSS and script-like source may be malformed, oversized or adversarial.
3. **Parser/layout to Android UI:** display commands and text must remain bounded and must not execute arbitrary platform code.
4. **App to local storage:** normal-profile persistence is stored in the app-private files directory; private-profile state must not be copied into it.
5. **Normal profile to private profile:** cookies, caches, history and navigation state must not cross the boundary.

## Implemented controls

- HTTPS-only network transport with platform TLS trust and hostname verification; no permissive trust manager.
- Manual redirect handling, redirect count cap, HTTPS downgrade rejection and cross-origin Authorization/Cookie header stripping.
- Request/response byte limits, connect/read timeouts, supported-method and header validation.
- Per-profile in-memory cookie jars; cookie lookup is URI-scoped; cookie attachment can be disabled by privacy policy.
- URL-only cache is bypassed when a cookie jar, Authorization/Cookie header or request no-cache/no-store directive is present. Responses with Set-Cookie or Vary are not cached.
- Bounded ABP-subset filter parser; unsupported syntax is ignored rather than guessed.
- Bounded HTML/CSS/JS-subset processing, DOM cycle rejection, CSS rule/style-evaluation budgets and JS statement/token/step/depth/expression limits.
- Private mode uses separate network/cookie state, memory-only private history, cache/cookie clearing, navigation cancellation and FLAG_SECURE.
- Panic clear cancels active requests and clears private profile state.
- Persistent normal bookmark/history and private-site rules use bounded files in the app-private files directory.

## Residual risks and incomplete controls

- No full browser-origin security model, resource scheduler, CSP enforcement or subresource isolation yet.
- The primitive JavaScript interpreter is not full ECMAScript and is not connected to page scripts or the DOM.
- No complete HTML5/CSS standards implementation; complex markup and styles can render incorrectly.
- No durable encrypted normal-cookie store, Android Keystore integration, certificate inspection UI or site permission manager.
- No service workers, WebGL, Widevine DRM, complete download sandbox, or broad modern-site compatibility.
- Third-party filter classification is a hostname-suffix approximation, not a Public Suffix List/eTLD+1 implementation.
- Private mode does not hide the device IP address from destination sites or network operators.
- FLAG_SECURE reduces ordinary screenshots/recents exposure but is not a guarantee against a rooted or compromised device.
- File-backed stores are bounded and atomically replaced where supported, but do not yet have authenticated encryption or robust corruption-recovery UX.
- Android emulator instrumentation and physical-device tests are still pending CI completion; no physical-device audit is claimed.

## Explicit non-goals

- No stealth data collection, keylogging, credential interception, covert tracking or hidden persistence.
- No DRM, age-verification, paywall or other access-control bypass.
- No anti-adblock evasion or claim that server-side inserted ads can always be blocked.

## Required M12 audit evidence

- Successful clean build, unit tests, lint and emulator instrumentation.
- CodeQL Java/Kotlin analysis with findings triaged.
- Regression tests for redirects, cookies, cache isolation, private-mode cleanup, parser budgets and navigation cancellation.
- Dated manual device matrix covering launch/exit, browsing, back/forward, bookmarks/history, private mode, downloads and media limitations.
