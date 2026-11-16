# Feature Tracker

Status definitions: **Done** = implemented and covered by a passing repeatable test; **Partial** = implemented subset or incomplete verification; **Not started** = no qualifying implementation/test. Infrastructure M1 (Gradle modules, Compose shell and CI) is Done after GitHub Actions run #3 passed. Product features remain individually tracked below.

| Feature | Module | Status | Test that proves it |
|---|---|---|---|
| HTML tokenizer | :engine-html | Partial | Bounded tokenizer fixtures; HTML5 conformance subset incomplete | HTML tokenizer fixtures |
| HTML tree builder | :engine-html | Partial | Bounded tree construction and DOM traversal fixtures; WHATWG modes incomplete | Tree-construction fixtures |
| DOM | :engine-html | Partial | Basic tree traversal and mutation tests | DOM traversal/mutation tests |
| CSS parser | :engine-css | Partial | Bounded rule/declaration parser fixtures | CSS syntax fixtures |
| CSS selectors | :engine-css | Partial | Compound and descendant selector fixtures; limited subset | Selector corpus |
| CSS cascade/computed style | :engine-css | Partial | Specificity/importance/inheritance fixtures; layout integration pending | Cascade golden fixtures |
| JavaScript interpreter | :engine-js | Partial | Primitive statement/expression interpreter and budget tests; not ECMAScript compliant | ECMAScript subset suite |
| DOM JS bindings | :engine-js | Not started | Host binding integration tests |
| Layout engine | :engine-render | Partial | Initial block/inline geometry tests; no full CSS formatting contexts | Geometry golden tests |
| Rendering/drawing | :engine-render | Partial | Android Canvas display-list backend; device pixel tests pending | Pixel/display-list golden tests |
| Hardware acceleration | :engine-render | Not started | Device benchmark + correctness |
| Networking HTTP | :engine-net | Partial | Transport implemented; local HTTPS/redirect integration tests pending |
| TLS validation | :engine-net | Partial | Platform TLS trust/hostname verification used; valid/invalid certificate integration tests pending |
| HTTPS-only | :engine-net/:privacy | Partial | Cleartext and HTTPS-downgrade rejection unit tests |
| DNS over HTTPS | :engine-net | Not started | Controlled DoH endpoint/leak tests |
| Page loading | :engine-net | Not started | Deterministic page-load integration |
| Cache management | :engine-net | Partial | Bounded in-memory GET cache implemented; cache-control/expiry integration tests pending |
| Resource management | :engine-net | Partial | Redirect/timeouts/response-size limits implemented; concurrency stress tests pending |
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
| Per-site isolated storage | :privacy/:storage | Partial | `OriginStorageManager` provides bounded memory-only local/session partitions keyed by normalized HTTPS scheme/host/port; DOM bindings and durable persistence remain pending | Cross-origin, IDN, quota and DOM integration tests |
| WebRTC leak control | :privacy | Not started | On-device ICE leak tests |
| Private browsing | :privacy/:storage | Partial | Separate in-memory private cookies/history/cache and cleanup paths exist; full tab/storage audit pending | Private-profile isolation and cleanup tests |
| Auto-clear history/cookies | :privacy/:storage | Not started | Deletion trigger tests |
| Cache control | :privacy/:engine-net | Not started | Clear/retain tests |
| Permission manager/dashboard | :privacy/:app | Partial | `SitePermissionManager` has bounded per-origin ASK/ALLOW/BLOCK decisions and fail-closed HTTPS/IDN origin parsing; Android runtime permission mediation, persistence and dashboard UI remain pending | Permission policy + runtime permission/UI tests |
| Tracking parameter removal | :privacy | Partial | utm_/click-id removal fixtures; provider-specific coverage pending | utm_ and fbclid fixtures |
| Referrer trimming | :privacy | Partial | Origin/same-origin/none policy fixtures | Request header tests |
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
| Bookmarks/history/search history | :storage | Partial | Normal bookmarks/history persist; private history is memory-only; search-history UX and full privacy tests pending | CRUD/persistence/privacy tests |
| Download manager | :app/:engine-net | Not started | Download state/file tests |
| QR scanner | :tools/:app | Not started | Camera permission/scan tests |
| Reader mode | :tools/:engine-html | Partial | Article extraction helper exists; user-facing reader workflow and fixtures pending | Article extraction fixtures |
| Translation | :tools | Not started | Provider adapter tests; key/network disclosure |
| Desktop mode | :privacy/:engine-net | Not started | UA/viewport tests |
| Share page | :app | Not started | Android share intent test |
| Search suggestions | :search | Not started | Provider response fixtures |
| Find in page | :tools/:engine-html | Not started | DOM text search tests |
| Zoom | :engine-render/:app | Not started | Layout scale tests |
| Offline pages | :tools/:storage | Not started | Save/reopen fixture tests |
| Android Keystore AES-256-GCM cipher primitive | :storage | Done | API 35 instrumentation passed five tests: round-trip, fresh IV, authenticated AAD/tamper rejection, malformed envelope and key deletion | Keystore instrumentation suite |
| Encrypted password manager | :storage | Not started | Keystore-backed credential CRUD, unlock policy and secure persistence tests |
| Compose UI | :app | Partial | Native Compose browser shell exists; broad UI coverage and accessibility tests pending | Compose UI tests |
| Dark/light/system theme | :app | Not started | Theme UI tests |
| Homepage/start page | :app/:storage | Not started | Preference persistence tests |
| Toolbar customization/position | :app | Not started | Compose state/UI tests |
| Search engine selection | :search/:app | Partial | Provider dropdown and HTTPS URL adapters exist; settings persistence and integration tests pending | URL template tests |
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
| SearchProvider interface | :search | Partial | HTTPS URL provider contract and encoding tests | Provider contract tests |
| DuckDuckGo provider | :search | Partial | HTTPS result URL fixture | Saved result fixture |
| Google provider | :search | Partial | HTTPS result URL fixture; no scraping | Legal result-page/API integration test |
| Brave Search provider | :search | Not started | Official API fixture; Needs API key where required |
| Bing provider | :search | Partial | HTTPS result URL fixture; no scraping | Official API/result-page fixture |
| Startpage provider | :search | Partial | HTTPS result URL fixture | Normal result-page integration fixture |
| Yahoo provider | :search | Not started | Result-page fixture |
| Ecosia provider | :search | Partial | HTTPS result URL fixture | Result-page fixture |
| Qwant provider | :search | Partial | HTTPS result URL fixture | Result-page fixture |
| Mojeek provider | :search | Partial | HTTPS result URL fixture | Result-page fixture |
| Yandex provider | :search | Partial | HTTPS result URL fixture | Result-page fixture |
| Wikipedia provider | :search | Partial | HTTPS search URL fixture; API integration pending | API fixture |
| YouTube provider | :search | Not started | Legal result-page/API fixture |
| User URL-template search | :search | Partial | Query encoding and HTTPS validation tests | Template encoding tests |
| Quick-switch mode | :search/:app | Not started | Provider selection tests |
| Merged meta-search | :search | Not started | Parallel timeout/dedup/RRF fixtures |
| One-tab-per-engine | :search/:app | Not started | Tab routing tests |
| Source badges | :search/:app | Not started | Result attribution UI tests |
| Meta-search warning/opt-in | :search/:app | Partial | Default-off setting model; UI disclosure pending | Default-off preference/UI test |
| Bang shortcuts | :search | Partial | !g, !ddg, !b, !w parser fixtures | !g, !ddg, !w parsing tests |
| Search timeout/rate-limit/offline | :search | Not started | Deterministic failure fixtures |
| GitHub Actions build | CI | Partial | Debug/release build, unit tests and lint have passed on earlier commits; latest emulator instrumentation run is failing and rerunning | Successful workflow run |
| Debug APK artifact | CI | Partial | Earlier debug APK artifacts uploaded; latest run upload is gated on emulator tests | APK artifact download/inspection |
| Release APK | CI | Partial | assembleRelease succeeds; release signing and artifact verification remain pending | Signed APK verification |
| Installation testing | CI/:app | Partial | API 35 emulator launched app and executed privacy test; test failed; API 26 run was skipped | Emulator install and launch |

| Private-site rules (user-defined always-private list) | :privacy/:storage/:app | Partial | Exact/subdomain in-memory matcher tests; persisted UI routing pending | Rule precedence and auto-private tab routing tests |
| Isolated private tab profile | :privacy/:storage | Not started | Storage inspection proves no history, cookies, cache, form data or download-list persistence |
| Private-tab close cleanup | :privacy/:storage | Not started | Close-tab storage inspection test |
| Panic button (close all private tabs + wipe) | :privacy/:storage/:app | Partial | Private-session cancellation and memory cleanup exist; multi-tab wipe verification pending | Panic action test verifies all ephemeral stores are empty |
| Private-tab PIN/biometric lock | :privacy/:app | Not started | Lock-state, biometric cancellation and lifecycle tests |
| FLAG_SECURE for private surfaces | :app | Partial | Window flag is wired to private-mode state; instrumentation assertion is being stabilized; screenshot/recents verification pending | Instrumented window flag and screenshot/recents behavior test |
| Neutral app icon/name option | :app | Not started | Launcher alias enable/disable instrumentation test; platform launcher caveats documented |
| Private notification redaction | :app/:privacy | Not started | Notification content inspection test |
| Private popup/pop-under and forced-redirect blocking | :adblock/:privacy/:engine-net | Not started | Navigation decision fixtures and redirect integration tests |
| Malvertising/fake-virus interstitial blocking | :adblock | Not started | Pinned malicious-pattern fixtures; false-positive tests |
| Auto-APK and fake-player-update download blocking | :adblock/:engine-net/:media | Not started | Download policy fixtures; explicit trusted-download exception tests |
| Deny site notification permission by default | :privacy/:app | Not started | Permission default and user-override tests |
| Video overlay ad filtering | :adblock/:engine-render/:media | Not started | Cosmetic rule fixtures on controlled test pages |
| Per-site autoplay toggle | :privacy/:media/:app | Not started | Per-site autoplay policy state tests |
| Private-mode fullscreen/landscape/seek/speed/PiP | :media/:app | Not started | Instrumented playback controls and lifecycle tests |
| Optional private-mode background audio | :media/:app | Not started | Audio focus and explicit opt-in service lifecycle test |
| Honest private-mode IP disclosure | :privacy/:app | Not started | UI copy test asserts IP visibility disclosure |
| DoH/VPN/Tor options and limitations | :privacy/:engine-net/:app | Not started | DNS/routing leak tests and limitation-copy assertions |
| Age-verification/access-control bypass | Not supported | Not started | Explicitly excluded; no bypass implementation or tests |

| YouTube network filter rules | :adblock/:engine-net | Not started | Pinned YouTube request fixtures; verify no breakage of playback |
| YouTube cosmetic filters (overlays, banners, sponsored cards, Shorts placements) | :adblock/:engine-render | Not started | DOM fixture selector tests plus real-page manual checks |
| YouTube scriptlet rule parser | :adblock | Not started | Parser fixtures for supported scriptlet syntax |
| YouTube player-response JSON pruning | :adblock/:engine-js | Not started | Synthetic fixture tests only; live YouTube response behavior not verified |
| YouTube ad-segment auto-skip/mute/speed-up | :adblock/:engine-js/:media | Not started | Not committed; evaluate feasibility and platform constraints before implementation |
| YouTube anti-adblock compatibility | :adblock/:engine-js | Not started | Benign compatibility tests; no anti-adblock evasion or access-control bypass |
| YouTube filter-list daily updates | :adblock | Not started | Scheduled update, signature/hash validation, rollback and stale-list tests |
| YouTube filter health warning and one-tap refresh | :adblock/:app | Not started | Stale/broken-list state and refresh UI tests |
| YouTube ad blocking on real pages | :adblock/:engine-js/:engine-render | Partial | Not verified on device; dated manual video-start, mid-roll, banner and Shorts checklist required |


## Pipelined M0-M11 implementation snapshot — 2026-11-16
- M0-M1: architecture, multi-module skeleton, Compose shell and CI verified earlier.
- M2: HTTPS-only transport, redirect controls, bounded cache and policy hooks remain Partial; real TLS/redirect/device integration remains pending.
- M3: bounded Adblock Plus network-rule subset parser is implemented and its latest parser CI run #21 passed. It is not full EasyList/AdGuard/uBO compatibility and does not provide YouTube page/cosmetic/scriptlet filtering.
- M4: URL tracking cleanup, referrer controls and exact/subdomain private-site matcher implemented as a partial policy layer. Persistence, cookie jar integration and automatic private-tab routing remain pending.
- M5: HTTPS result-page URL adapters, provider selection model, bang shortcuts and meta-search default-off setting implemented. Provider API integrations, result parsing/ranking, timeout/offline handling and user-facing disclosure UI remain pending.
- M6: bounded HTML tokenizer, simplified tree builder and minimal DOM traversal/mutation implemented. Not a WHATWG-complete HTML5 parser.
- M7: bounded CSS rule/declaration parser, compound/descendant selectors and basic cascade implemented. Pseudo selectors, child/sibling combinators, full CSS values and standards conformance remain unsupported.
- M8: initial block/inline layout and Android Canvas display-list renderer implemented. Device rendering, scrolling correctness, text shaping, wrapping, flex/grid and performance tests remain pending.
- M9: primitive JavaScript-like interpreter with statement/token/step/depth budgets implemented. It is not full ECMAScript and is not yet wired to DOM/page script execution.
- M10: memory-only private profile store, playback state controller and basic reader-mode extraction implemented. Durable bookmark/history store, Android Keystore, MediaCodec pipeline, PiP, subtitles and device tests remain pending.
- M11: Compose browser shell integrates HTTPS navigation, search provider selection, first-party HTML/CSS layout and Canvas drawing; private mode clears in-memory profile, uses FLAG_SECURE and shows IP disclosure. Tab persistence, app lock, neutral launcher identity, full settings/permissions UI and instrumented tests remain pending.
- Current verification is being driven by GitHub Actions. No local Android SDK/emulator is available; no physical-device tests are claimed.
- Consolidated CI run #24 passed on commit `5bcc64d6f0`: debug APK build, unit tests, Android lint and APK artifact upload all succeeded. This verifies the current source snapshot on CI, not full browser feature completeness.

## Full pipeline hardening — 2026-11-16
- M2: corrected HTTP 303 handling to preserve HEAD; added privacy-header recomputation and active-request cancellation regression tests. Real TLS/device integration remains pending.
- M3: filter target hosts now canonicalize IDN and trailing dots; request transport integration test added. Third-party classification remains a documented hostname approximation.
- M4: private-site rules normalize IDN. Private-mode transitions and panic clear cancel navigation and clear the in-memory cache/profile.
- M5: search-provider endpoint validation rejects userinfo/fragments; bang shortcuts now resolve case-insensitively.
- M6: DOM mutations reject self/ancestor cycles.
- M9: interpreter expression evaluation consumes execution budget and expression complexity is bounded.
- M10: private-profile stores now have explicit memory/count limits and zero replaced cache buffers; a file-backed durable normal bookmark/history store was added with persistence tests.
- M11: navigation generations stop stale requests from updating a new session; normal history and private history are routed to separate stores.
- M12: final clean release build, integration/security/performance audit, signed artifact and device compatibility checks remain outstanding.
- CI: historical run #24 passed, but all commits after `5bcc64d6f0` require their own successful Actions result before the latest source snapshot is verified.
## Latest pipelined milestone work — 2026-11-16
- M2: per-profile CookieManager-backed cookie stores, URI-scoped cookie attachment, Set-Cookie processing on redirect and final responses, cookie-strip policy integration, and cache bypass for cookie/authenticated requests. Real TLS/device integration remains pending.
- M3: bounded ABP subset remains injectable into the HTTP transport; full EasyList/AdGuard compatibility and a subresource scheduler remain pending.
- M4: persistent private-site host rules with IDN normalization, add/remove tests and automatic private routing; normal and private cookie jars are separate and private cookies are cleared on mode exit/panic.
- M8: computed styles are cached per DOM element; CSS rule and style-evaluation budgets are enforced; inline element width propagation corrected.
- M10: normal bookmarks/history use bounded file-backed storage; private history remains memory-only; private-profile collections have explicit size/count budgets.
- M11: native back/forward/refresh, bookmark save/open, normal/private history menus and always-private site control integrated. Full tab manager, app lock, permission UX and instrumented UI tests remain pending.
- Successful CI: run https://github.com/ishak6/fuck-ass-browser-/actions/runs/43763268460 passed build, unit tests, lint and APK upload on commit `a00ee7552a`; run https://github.com/ishak6/fuck-ass-browser-/actions/runs/43763237046 passed all four gates on commit `057f5a2393`.
- History ordering and private-routing helper changes passed CI on run https://github.com/ishak6/fuck-ass-browser-/actions/runs/43763492817 (commit `8f2be5713b`); build, unit tests, lint and APK upload succeeded.
- M12 final clean release, full integration/security/performance audit, signing readiness and device compatibility matrix remain outstanding.
## M12 validation infrastructure added
- Added Android instrumentation test that toggles private mode and verifies `FLAG_SECURE` is enabled/cleared.
- Android CI now provisions a headless API 35 Google APIs emulator and runs `connectedDebugAndroidTest` after build/unit/lint.
- Added CodeQL v4 workflow for Java/Kotlin with manual Gradle build mode and weekly scheduled scans.
- These new emulator and CodeQL jobs are pending their first completed run; no emulator or CodeQL success is claimed yet.

## M12 latest verification — 2026-11-16
- Commit `da32aca2fdc59ef182f61017935015681a08f2ab`: debug/release APK build, unit tests and Android lint passed; API 35 emulator instrumentation failed in `MainActivityPrivacyTest` while asserting the private-mode `FLAG_SECURE` transition. API 26 and APK upload were skipped because the API 35 test failed.
- CodeQL run https://github.com/ishak6/fuck-ass-browser-/actions/runs/43763967417 succeeded for that commit. A successful CodeQL workflow is not a claim that the app has no vulnerabilities; findings still require triage.
- Follow-up commit `49b2b86e160d762fc81713d21fa4aff6527750ac` updates the Compose instrumentation test to wait for the asynchronous window-flag transition before asserting it. Android CI and CodeQL reruns are in progress; no result is claimed yet.
- Emulator instrumentation is not physical-device validation. No physical Android device has been tested.


## Extended milestone execution — 2026-11-16
- M12 emulator regression: API 35 run `43813297437` timed out waiting for the `FLAG_SECURE` transition; the follow-up run `43814740542` reached the assertion but the flag was still unset after the Compose click. Debug/release builds, unit tests and lint passed; API 26 and APK upload were skipped. The toggle now updates the window flag synchronously in commit `d059ba7683292491e99d8b3657ab64bffb006ebb`; fresh emulator verification is pending.
- Test correction: Compose-idle assertions alone did not solve the issue; the UI click handler now applies `FLAG_SECURE` synchronously before changing Compose state. Do not mark this regression fixed until a new API 35 run passes.
- M13 started: added `AndroidKeyStoreCipher`, an Android Keystore-backed non-exportable AES-256-GCM key with authenticated associated data, random 96-bit IV envelope, 1 MiB plaintext/4 KiB AAD bounds and key deletion. Added emulator instrumentation coverage for round-trip, fresh IV, tamper/AAD rejection, malformed payload and key deletion.
- M13 status: Done for the Keystore AES-GCM primitive and its defined emulator test gate. The five `:storage:connectedDebugAndroidTest` cases passed on API 35 in run `43814740542`. The primitive is not yet wired into bookmark/history persistence or a password manager; no encrypted-at-rest claim is made for existing browser data.
- Extended roadmap M13–M49 is now recorded. M12 is the original core validation gate; M49 is the extended whole-project release gate.
- No physical Android device has been tested. Emulator CI results must be checked before claiming the new tests pass.
- M14 started in parallel with M13 CI: added a bounded per-origin permission policy for camera, microphone, location, notifications, JavaScript, popups and automatic downloads. Unknown sites default to ASK; malformed/non-HTTPS URLs fail closed; scheme/IDN host/effective port define the origin; in-memory entries are bounded and clearable. Persistent storage, Compose permission dashboard and Android runtime-permission mediation are still pending, so M14 is Partial.
- M15 started while M13/M14 CI runs: added a bounded, memory-only local/session key-value partition manager keyed by canonical HTTPS origin (scheme/host/port), with per-origin and aggregate byte quotas, entry/origin caps, explicit clear operations and cross-origin/IDN tests. It is not yet connected to DOM JavaScript APIs or durable storage; M15 remains Partial.


## Latest pipeline checkpoint — 2026-11-16
- Android CI run [43814740542](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43814740542): Debug/Release builds, unit tests and lint passed; the app privacy instrumentation failed because `FLAG_SECURE` was not set after the Compose click. The storage instrumentation task subsequently ran all five M13 Keystore tests successfully. API 26 and APK upload were skipped due to the app test failure.
- CodeQL run [43814740545](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43814740545) succeeded. Findings still require review; a successful scan is not a zero-vulnerability guarantee.
- Commit `d059ba7683292491e99d8b3657ab64bffb006ebb` changes the private-mode toggle to set/clear the window flag synchronously. New CI verification is required.

## Extended milestones M13-M15 — live implementation update (2026-11-16)
- M13: Android Keystore AES-256-GCM cipher primitive is implemented. API 35 storage instrumentation previously passed five tests (round-trip, fresh IV, AAD/tamper rejection, malformed envelope and key deletion). Encrypted bookmark/history migration and password vault are not implemented.
- M14: per-origin HTTPS permission policy, encrypted-at-rest EncryptedSitePermissionStore, and a Compose Site Permission panel are implemented. The panel exposes Ask/Allow/Block decisions per permission and disables edits in private mode. Camera/microphone/location Android runtime permission mediation is not wired because the current renderer exposes no such site capability; a site-level Allow never grants Android runtime access. Instrumentation smoke test was added. CI run https://github.com/ishak6/fuck-ass-browser-/actions/runs/43825090277 exposed a test-source API import error (assertExists); corrected to fetchSemanticsNode() in commit 18233083b779b1167067ff9b3be884eb78cc8347. Re-run is pending. M14 remains Partial.
- M15: bounded origin-partitioned local/session storage primitive is implemented with same-origin (scheme/host/port) isolation, IDN normalization, quotas and clearing. Added OriginStorageSession, a normalized-origin-bound host adapter with local/session areas, indexed key access and session cleanup on close; four lifecycle/isolation tests added. This is not yet wired to the JavaScript interpreter or DOM window.localStorage/sessionStorage, and storage is memory-only. CI runs https://github.com/ishak6/fuck-ass-browser-/actions/runs/43825301035 and https://github.com/ishak6/fuck-ass-browser-/actions/runs/43825324058 are in progress; unit tests have passed in those runs, while emulator/API 35 checks are still running.
- Most recent source commit at this update: 18233083b779b1167067ff9b3be884eb78cc8347. Do not mark M14/M15 Done until the latest CI, security review and remaining integration work pass.
- No physical-device validation has been performed. The current app remains a limited native renderer, not a modern-browser replacement.

## Extended milestones M14-M16 — pipeline continuation (2026-11-16)
- M14/M15 verification: Android CI run [43825706387](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43825706387) completed successfully on `9aa09075b2`; API 35 and API 26 emulator smoke tests and debug APK upload passed. CodeQL run [43825706474](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43825706474) passed. This validates the source snapshot and emulator smoke suite, not physical-device behavior. M14/M15 remain Partial because Android runtime permission mediation and JS/DOM storage persistence are not integrated.
- M16 implementation started: `CookieAccessPolicy` adds runtime modes ALLOW_ALL, SAME_ORIGIN_ONLY and BLOCK_ALL; SAME_ORIGIN_ONLY deliberately requires exact HTTPS scheme/host/effective-port equality and does not pretend to implement registrable-domain/PSL same-site logic. `BrowserHttpClient` checks policy before sending and accepting cookies. Added origin-scoped cookie removal, durable history clearing that preserves bookmarks, and Compose privacy/data controls for cookie modes, site/all-cookie deletion, cache clear and history clear.
- M16 tests added for cookie policy modes, fail-closed malformed/non-HTTPS origins, transport enforcement of BLOCK_ALL, origin-scoped cookie deletion, and durable history deletion preserving bookmarks. First CI run [43826615992](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43826615992) exposed one invalid CookieStore test fixture (missing explicit cookie domain/path); corrected in commit `b8951c7c97d45622c7229632a08f56fe049b4caf`. CI runs [43826782476](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43826782476) and [43826931831](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43826931831) are pending/running; M16 is not Done until the latest run passes all unit, lint, API 35/API 26 emulator and APK artifact steps.
- Cache limitation: requests using the profile cookie jar bypass the simple URL-only HTTP response cache to prevent cross-session or credential-bearing cache leakage. The Clear cache action empties any current in-memory cache entries; it does not enable persistent HTTP caching.
- Current latest implementation commit at this entry: `b8951c7c97d45622c7229632a08f56fe049b4caf`. Do not infer release readiness from successful compilation alone.

## Extended milestones M16-M17 — implementation status (2026-11-16)
- M16: cookie modes ALLOW_ALL, SAME_ORIGIN_ONLY and BLOCK_ALL are implemented in `CookieAccessPolicy`; exact scheme/host/effective-port checks are intentionally stricter than same-site. The HTTP transport enforces the mode for outgoing Cookie and incoming Set-Cookie on both redirect and final responses. Added origin-scoped cookie deletion, durable history deletion (bookmarks preserved), and Compose privacy/data controls. Cookie-backed requests intentionally bypass URL-only HTTP caching; cache clear only empties any in-memory entries and does not enable persistent cache.
- M16 regression found and fixed: initial tests exposed that cookie policy was not applied to redirect/final Set-Cookie paths and that a CookieStore fixture needed explicit domain/path. Commits `5c0d308d83` and `987bbb8f9f` contain the fixes. CI run [43827098777](https://github.com/ishak6/fuck-ass-browser-/actions/runs/43827098777) has passed build, unit tests and lint and is still running API 35 emulator tests; API 26 and artifact upload remain gated. M16 remains Partial pending latest full CI and UI verification.
- M17: added opt-in RFC 8484 DNS-message POST client in `engine-net` with HTTPS-only endpoint validation, platform TLS/hostname validation, redirect disabled, bounded timeouts/response bytes, A/AAAA parsing, transaction-ID and question/answer-owner validation, compression-pointer bounds and explicit error handling. Added deterministic fake-connection tests. Resolver hostname bootstrap still uses Android/system DNS; this is not a system-wide DNS replacement, is not yet wired into navigation, and has no user-facing opt-in UI. M17 remains Partial pending CI and integration.
- Latest M17 hardening commit: `bfe4aef9a02cf7dcd7885f488e154500f05e2f5a`. Do not mark M16 or M17 Done until their newest Android CI (including API 35/API 26 emulator and APK artifact) and CodeQL runs pass.
