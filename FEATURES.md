# Feature Tracker

Status definitions: **Done** = implemented and covered by a passing repeatable test; **Partial** = implemented subset or incomplete verification; **Not started** = no qualifying implementation/test. At initial reset, all product features are Not started.

| Feature | Module | Status | Test that proves it |
|---|---|---|---|
| HTML tokenizer | :engine-html | Not started | HTML tokenizer fixtures |
| HTML tree builder | :engine-html | Not started | Tree-construction fixtures |
| DOM | :engine-html | Not started | DOM traversal/mutation tests |
| CSS parser | :engine-css | Not started | CSS syntax fixtures |
| CSS selectors | :engine-css | Not started | Selector corpus |
| CSS cascade/computed style | :engine-css | Not started | Cascade golden fixtures |
| JavaScript interpreter | :engine-js | Not started | ECMAScript subset suite |
| DOM JS bindings | :engine-js | Not started | Host binding integration tests |
| Layout engine | :engine-render | Not started | Geometry golden tests |
| Rendering/drawing | :engine-render | Not started | Pixel/display-list golden tests |
| Hardware acceleration | :engine-render | Not started | Device benchmark + correctness |
| Networking HTTP | :engine-net | Not started | Local HTTP integration tests |
| TLS validation | :engine-net | Not started | Valid/invalid certificate tests |
| HTTPS-only | :engine-net/:privacy | Not started | HTTP upgrade/block tests |
| DNS over HTTPS | :engine-net | Not started | Controlled DoH endpoint/leak tests |
| Page loading | :engine-net | Not started | Deterministic page-load integration |
| Cache management | :engine-net | Not started | Hit/expiry/invalidation tests |
| Resource management | :engine-net | Not started | Size/concurrency/timeout tests |
| Website ad blocking | :adblock | Not started | Pinned filter URL fixtures |
| Video ad blocking | :adblock | Not started | Request-rule fixtures; site limits documented |
| Popup blocking | :adblock | Not started | Navigation policy tests |
| Redirect blocking | :adblock | Not started | Redirect-rule fixtures |
| Tracker blocking | :adblock | Not started | Tracker URL corpus |
| Cosmetic filtering | :adblock/:engine-render | Not started | DOM selector application tests |
| Network request filtering | :adblock/:engine-net | Not started | Request decision integration |
| EasyList rules | :adblock | Not started | Pinned EasyList compatibility corpus |
| AdGuard rules | :adblock | Not started | Pinned AdGuard compatibility corpus |
| uBlock Origin rules subset | :adblock | Not started | Explicit uBO syntax fixtures |
| Custom filter lists/rules | :adblock | Not started | Import/CRUD/matching tests |
| Automatic filter updates | :adblock | Not started | Update integrity/rollback tests |
| Per-site controls/whitelist | :adblock | Not started | Policy persistence/precedence tests |
| Block counter/rule manager | :adblock/:app | Not started | Counter and Compose UI tests |
| Cookie-consent blocking | :adblock | Not started | Cosmetic rule fixtures |
| Anti-adblock handling | :adblock | Not started | Benign compatibility tests; no access-control evasion |
| Anti-tracking | :privacy | Not started | Known tracker policy tests |
| Fingerprinting protection | :privacy | Not started | Surface policy tests; no anonymity claim |
| Canvas/WebGL/audio noise | :privacy/:engine-render/:media | Not started | Surface-specific deterministic tests |
| Generic user-agent | :privacy/:engine-net | Not started | Header consistency tests |
| Cookie/third-party cookie control | :privacy | Not started | Cross-origin cookie tests |
| Per-site isolated storage | :privacy/:storage | Not started | Origin isolation tests |
| WebRTC leak control | :privacy | Not started | On-device ICE leak tests |
| Private browsing | :privacy/:storage | Not started | Ephemeral persistence tests |
| Auto-clear history/cookies | :privacy/:storage | Not started | Deletion trigger tests |
| Cache control | :privacy/:engine-net | Not started | Clear/retain tests |
| Permission manager/dashboard | :privacy/:app | Not started | Permission state/UI tests |
| Tracking parameter removal | :privacy | Not started | utm_ and fbclid fixtures |
| Referrer trimming | :privacy | Not started | Request header tests |
| VPN/Tor integration | :privacy | Not started | Routing/leak tests; not Tor Browser equivalence |
| Smooth video streaming | :media | Not started | Supported stream device tests |
| Fullscreen/landscape | :media/:app | Not started | Instrumented orientation/fullscreen tests |
| Play/pause/seek/speed | :media | Not started | Playback state/seek/rate tests |
| Subtitles | :media | Not started | WebVTT/SRT fixtures |
| Picture-in-Picture | :media/:app | Not started | Android PiP instrumentation |
| Background playback | :media | Not started | Audio focus/service lifecycle test |
| Hardware video decoding | :media | Not started | MediaCodec device capability test |
| Permitted downloads | :media/:engine-net | Not started | User-initiated download integrity test |
| Supported DRM playback | :media | Not started | Authorized provider/device test; Widevine not guaranteed |
| Tabs/tab manager/groups | :app/:storage | Not started | State and persistence tests |
| Bookmarks/history/search history | :storage | Not started | CRUD/persistence/privacy tests |
| Download manager | :app/:engine-net | Not started | Download state/file tests |
| QR scanner | :tools/:app | Not started | Camera permission/scan tests |
| Reader mode | :tools/:engine-html | Not started | Article extraction fixtures |
| Translation | :tools | Not started | Provider adapter tests; key/network disclosure |
| Desktop mode | :privacy/:engine-net | Not started | UA/viewport tests |
| Share page | :app | Not started | Android share intent test |
| Search suggestions | :search | Not started | Provider response fixtures |
| Find in page | :tools/:engine-html | Not started | DOM text search tests |
| Zoom | :engine-render/:app | Not started | Layout scale tests |
| Offline pages | :tools/:storage | Not started | Save/reopen fixture tests |
| Encrypted password manager | :storage | Not started | Keystore encrypt/decrypt tests |
| Compose UI | :app | Not started | Compose UI tests |
| Dark/light/system theme | :app | Not started | Theme UI tests |
| Homepage/start page | :app/:storage | Not started | Preference persistence tests |
| Toolbar customization/position | :app | Not started | Compose state/UI tests |
| Search engine selection | :search/:app | Not started | URL template tests |
| Font size/gestures/shortcuts | :app/:engine-render | Not started | UI and rendering tests |
| Network request inspector | :diagnostics/:engine-net | Not started | Redacted request event tests |
| JS permission control | :privacy/:engine-js | Not started | Script policy integration |
| Resource monitor | :diagnostics | Not started | Metrics unit tests |
| Crash recovery | :app/:diagnostics | Not started | Lifecycle/process recovery tests |
| Local backup/restore | :storage | Not started | Export/import integrity tests |
| Camera/mic/location/notification controls | :privacy/:app | Not started | Android permission instrumentation |
| Clear browsing data/one-tap clear | :storage/:privacy | Not started | Full erasure integration test |
| Secure downloads | :engine-net/:app | Not started | Path validation/integrity tests |
| Website security information | :app/:engine-net | Not started | TLS/origin display tests |
| Phishing/malware warning | :privacy/:diagnostics | Not started | Threat-list fixture tests |
| App lock PIN/biometric | :app/:storage | Not started | Biometric instrumentation |
| SearchProvider interface | :search | Not started | Provider contract tests |
| DuckDuckGo provider | :search | Not started | Saved result fixture |
| Google provider | :search | Not started | Legal result-page/API integration test |
| Brave Search provider | :search | Not started | Official API fixture; Needs API key where required |
| Bing provider | :search | Not started | Official API/result-page fixture |
| Startpage provider | :search | Not started | Normal result-page integration fixture |
| Yahoo provider | :search | Not started | Result-page fixture |
| Ecosia provider | :search | Not started | Result-page fixture |
| Qwant provider | :search | Not started | Result-page fixture |
| Mojeek provider | :search | Not started | Result-page fixture |
| Yandex provider | :search | Not started | Result-page fixture |
| Wikipedia provider | :search | Not started | API fixture |
| YouTube provider | :search | Not started | Legal result-page/API fixture |
| User URL-template search | :search | Not started | Template encoding tests |
| Quick-switch mode | :search/:app | Not started | Provider selection tests |
| Merged meta-search | :search | Not started | Parallel timeout/dedup/RRF fixtures |
| One-tab-per-engine | :search/:app | Not started | Tab routing tests |
| Source badges | :search/:app | Not started | Result attribution UI tests |
| Meta-search warning/opt-in | :search/:app | Not started | Default-off preference/UI test |
| Bang shortcuts | :search | Not started | !g, !ddg, !w parsing tests |
| Search timeout/rate-limit/offline | :search | Not started | Deterministic failure fixtures |
| GitHub Actions build | CI | Not started | Successful workflow run |
| Debug APK artifact | CI | Not started | APK artifact download/inspection |
| Release APK | CI | Not started | Signed APK verification |
| Installation testing | CI/:app | Not started | Emulator install and launch |
