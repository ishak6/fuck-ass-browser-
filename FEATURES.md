# Feature Status

Status meanings:
- **Done**: implemented and covered by a repeatable passing test.
- **Partial**: some implementation exists, but acceptance criteria are incomplete or tests do not prove the full feature.
- **Not started**: no qualifying implementation/test exists.

The current repository is an initial GeckoView prototype. Because the project hard requirement prohibits GeckoView, that prototype does not count as implementation of the first-party browser engine. No feature below is currently marked Done.

| Feature | Module | Status | Test that proves it |
|---|---|---|---|
| HTML tokenizer | :engine-html | Not started | HTML tokenizer conformance fixtures |
| HTML tree builder | :engine-html | Not started | Tree construction fixtures |
| DOM nodes and mutation | :engine-html | Not started | DOM API unit tests |
| CSS tokenizer/parser | :engine-css | Not started | CSS syntax fixtures |
| CSS selectors | :engine-css | Not started | Selector matching tests |
| CSS cascade | :engine-css | Not started | Cascade/computed-style fixtures |
| JavaScript interpreter subset | :engine-js | Not started | ECMAScript subset conformance suite |
| DOM JavaScript bindings | :engine-js / :engine-html | Not started | DOM binding integration tests |
| Layout engine | :engine-render | Not started | Layout geometry golden tests |
| Paint/display list | :engine-render | Not started | Display-list golden tests |
| Canvas rendering | :engine-render | Not started | Pixel/golden rendering tests |
| Hardware acceleration | :engine-render | Not started | Device benchmark + rendering correctness |
| Hit testing and input | :engine-render | Not started | Pointer/scroll interaction tests |
| HTTP networking | :engine-net | Not started | Local HTTP server tests |
| TLS certificate validation | :engine-net | Not started | Valid/invalid certificate tests |
| HTTPS-only navigation | :engine-net / :privacy | Not started | HTTP upgrade/block tests |
| DNS resolution | :engine-net | Not started | Resolver tests |
| DNS over HTTPS | :engine-net | Not started | Controlled DoH endpoint tests |
| Redirect handling | :engine-net | Not started | Redirect policy tests |
| Page loading/resource scheduling | :engine-net | Not started | Deterministic page-load integration tests |
| HTTP cache management | :engine-net | Not started | Cache hit/expiry/invalidation tests |
| Resource limits/management | :engine-net | Not started | Size/concurrency/timeout tests |
| Website ad blocking | :adblock | Not started | Real filter-list URL fixtures |
| Video ad blocking | :adblock | Not started | Network rule fixtures; site-specific limitations documented |
| Popup blocking | :adblock | Not started | Popup/navigation policy tests |
| Redirect blocking | :adblock | Not started | Redirect rule fixtures |
| Tracker blocking | :adblock | Not started | Tracker URL fixtures |
| Cosmetic filtering | :adblock / :engine-render | Not started | Selector injection/removal DOM tests |
| Network request filtering | :adblock / :engine-net | Not started | Request decision integration tests |
| EasyList syntax support | :adblock | Not started | Pinned EasyList compatibility corpus |
| AdGuard syntax support | :adblock | Not started | Pinned AdGuard compatibility corpus |
| uBlock Origin syntax subset | :adblock | Not started | Explicitly scoped uBO fixture corpus |
| Custom filter lists | :adblock | Not started | Import/parse/persist tests |
| Custom filter rules | :adblock | Not started | Rule CRUD and matching tests |
| Filter rule compiler | :adblock | Not started | Compile equivalence/property tests |
| Automatic filter updates | :adblock | Not started | Mocked update server, integrity and rollback tests |
| Per-site AdBlock controls | :adblock / :app | Not started | Site policy persistence/UI tests |
| Whitelist/exceptions | :adblock | Not started | Exception precedence tests |
| Block counter | :adblock / :diagnostics | Not started | Deterministic counter tests |
| Rule manager UI | :app / :adblock | Not started | Compose UI tests |
| Cookie-consent banner rules | :adblock | Not started | Cosmetic filter fixture tests |
| Anti-adblock detection handling | :adblock | Not started | Benign compatibility tests; no access-control evasion |
| Anti-tracking policy | :privacy | Not started | Known tracker request policy tests |
| Fingerprinting protection policy | :privacy | Not started | Surface policy tests; no anonymity claim |
| Canvas noise | :privacy / :engine-render | Not started | Deterministic privacy-surface tests |
| WebGL noise/control | :privacy / :engine-render | Not started | Capability tests; WebGL may remain unsupported |
| Audio fingerprint surface control | :privacy / :media | Not started | Audio API exposure tests |
| Generic/reduced user-agent | :privacy / :engine-net | Not started | Header consistency tests |
| Cookie management | :privacy / :engine-net | Not started | Cookie policy unit/integration tests |
| Third-party cookie control | :privacy / :engine-net | Not started | Cross-origin cookie tests |
| Per-site isolated storage | :privacy / :storage | Not started | Origin isolation tests |
| HTTPS-only mode | :privacy / :engine-net | Not started | HTTP navigation tests |
| WebRTC leak controls | :privacy | Not started | ICE candidate leak tests on device |
| Private browsing | :privacy / :storage | Not started | Ephemeral profile persistence tests |
| Auto-clear history | :privacy / :storage | Not started | Triggered deletion tests |
| Auto-clear cookies | :privacy / :storage | Not started | Cookie deletion tests |
| Cache control | :privacy / :engine-net | Not started | Clear/retain cache tests |
| Website permission manager | :privacy / :app | Not started | Permission state and UI tests |
| Privacy dashboard | :app / :privacy | Not started | Compose state/UI tests |
| Tracking URL parameter removal | :privacy | Not started | utm_ and fbclid URL fixtures |
| Referrer trimming | :privacy / :engine-net | Not started | Request-header policy tests |
| Optional VPN integration | :privacy | Not started | Android VPN integration/leak tests; separate approval |
| Optional Tor integration | :privacy | Not started | Tor routing/leak tests; not Tor Browser equivalence |
| Video streaming | :media / :engine-net | Not started | Sample stream playback tests on supported devices |
| Fullscreen video | :media / :app | Not started | Instrumented fullscreen lifecycle test |
| Landscape playback | :media / :app | Not started | Orientation/device test |
| Play/pause | :media | Not started | Media state-machine tests |
| Seek controls | :media | Not started | Seek accuracy tests |
| Playback speed | :media | Not started | Supported-rate tests |
| Subtitle support | :media | Not started | WebVTT/SRT rendering fixtures |
| Picture-in-Picture | :media / :app | Not started | Android PiP instrumented test |
| Background playback | :media / :app | Not started | Audio focus/service lifecycle test |
| Hardware video decoding | :media | Not started | MediaCodec capability/device test |
| Permitted video downloads | :media / :engine-net | Not started | User-initiated download and permission tests |
| Supported DRM playback | :media | Not started | Authorized provider/device test; Widevine not guaranteed |
| Tabs | :app / :storage | Not started | Tab lifecycle/state tests |
| Tab manager | :app | Not started | Compose UI tests |
| Tab groups | :app / :storage | Not started | Group persistence tests |
| Bookmarks | :storage / :app | Not started | CRUD and persistence tests |
| Browsing history | :storage / :app | Not started | History persistence/clear tests |
| Search history | :storage / :app | Not started | Search record policy tests |
| Download manager | :app / :engine-net | Not started | Download state and file tests |
| QR scanner | :tools / :app | Not started | Camera permission + scan fixture test |
| Reader mode | :tools / :engine-html | Not started | Article extraction fixtures |
| Translation | :tools | Not started | Provider adapter tests; offline translation not assumed |
| Desktop mode | :privacy / :engine-net | Not started | User-agent/viewport behavior tests |
| Share page | :app | Not started | Android share intent test |
| Search suggestions | :app / :engine-net | Not started | Provider response parsing tests |
| Find in page | :tools / :engine-html | Not started | DOM text search tests |
| Page zoom | :engine-render / :app | Not started | Layout scale tests |
| Offline pages | :tools / :storage | Not started | Save/reopen offline fixture tests |
| Encrypted password manager | :storage | Not started | Keystore-backed encrypt/decrypt tests |
| Jetpack Compose UI | :app | Not started | Compose instrumented tests |
| Dark/light/system theme | :app | Not started | Compose theme tests |
| Custom homepage/start page | :app / :storage | Not started | Preference persistence tests |
| Custom toolbar and position | :app | Not started | Compose UI/state tests |
| Custom search engine | :app / :storage | Not started | Search URL template tests |
| Font size control | :app / :engine-render | Not started | Text scale rendering tests |
| Gesture controls | :app | Not started | Gesture dispatch tests |
| Homepage shortcuts | :app / :storage | Not started | Shortcut CRUD/UI tests |
| Network request inspector | :diagnostics / :engine-net | Not started | Redacted request event tests |
| Per-site privacy controls | :privacy / :app | Not started | Origin policy tests |
| Per-site JavaScript controls | :privacy / :engine-js | Not started | Script policy integration tests |
| Resource usage monitor | :diagnostics | Not started | Metrics collection unit tests |
| Crash recovery | :app / :diagnostics | Not started | Process/lifecycle recovery tests |
| Local backup and restore | :storage | Not started | Export/import integrity tests |
| Performance optimization | All engine modules | Not started | Baseline benchmark comparison |
| Camera permission control | :privacy / :app | Not started | Android permission tests |
| Microphone permission control | :privacy / :app | Not started | Android permission tests |
| Location permission control | :privacy / :app | Not started | Android permission tests |
| Notification permission control | :privacy / :app | Not started | Android permission tests |
| Clear browsing data | :storage / :privacy | Not started | Data deletion integration tests |
| Android Keystore password encryption | :storage | Not started | Keystore instrumented tests |
| Secure downloads | :app / :engine-net | Not started | Path validation and file integrity tests |
| Website security information | :app / :engine-net | Not started | TLS/origin display tests |
| Phishing/malware warnings | :privacy / :diagnostics | Not started | Signed/local threat-list fixture tests |
| App lock PIN/biometric | :app / :storage | Not started | Biometric prompt instrumented tests |
| One-tap clear all | :app / :storage | Not started | Full data-erasure integration test |
| GitHub Actions build | CI | Partial | Workflow run must pass on clean commit |
| Debug APK artifact | CI | Partial | Download artifact and verify APK exists |
| Release APK | CI / :app | Not started | Signed release build and signature verification |
| Installation testing | CI / :app | Not started | Install/launch test on emulator and physical device |
