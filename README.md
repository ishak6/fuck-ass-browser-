# Custom Browser

Native Android browser project built with Kotlin, Jetpack Compose and first-party modular browser components.

## Current implementation

The app contains an experimental first-party HTTPS navigation and rendering path:
- bounded HTTP transport using Android platform TLS verification
- bounded HTML tokenizer/tree builder and DOM subset
- bounded CSS parser/cascade subset
- initial block/inline layout and Android Canvas display-list renderer
- primitive, isolated JavaScript-like interpreter (not full ECMAScript)
- network filter subset and local privacy policies
- private in-memory session state and durable normal bookmark/history storage primitives
- native Compose browser shell with private-mode disclosure and request cancellation

This is an early development browser, not a production-compatible replacement for Chromium. Complex websites, full modern JavaScript, service workers, flex/grid, WebGL, Widevine DRM and many media sites are not supported. Real-device rendering and media tests remain pending.

**Engine policy:** no Android WebView, GeckoView, Chromium or third-party browser rendering engine. No telemetry or mandatory account. No DRM, age-verification or access-control bypass.

See [ARCHITECTURE.md](ARCHITECTURE.md), [FEATURES.md](FEATURES.md), [ROADMAP.md](ROADMAP.md), [MILESTONE_PIPELINE.md](MILESTONE_PIPELINE.md) and [PROGRESS.md](PROGRESS.md).
