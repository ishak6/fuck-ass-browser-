# Fuck ass Browser

Android-first browser prototype using Mozilla GeckoView (not Android WebView).

## Current starter scope
- Native Android Activity hosting GeckoView.
- DuckDuckGo as the default search engine.
- Direct URL navigation and basic back/forward/reload/stop controls.
- GitHub Actions workflow to build a debug APK and upload it as an artifact.

## Planned work (not implemented yet)
- Ad and tracker blocking with maintained filter lists.
- Per-site privacy and permission controls.
- Local browsing-data controls and anti-fingerprinting configuration.
- User-controlled background media playback and video UX.
- Tabs, bookmarks, downloads, history and settings UI.

## Important limitations
This is an initial scaffold, not a finished or tested browser release. The ad blocker, Tor network routing, anti-fingerprinting hardening, and advanced media controls are not implemented in this commit. Website behavior, DRM, codecs, and device/network support can affect video playback. Normal website requests still contact those websites.

## Build
The GitHub Actions workflow at `.github/workflows/android-build.yml` runs `gradle assembleDebug` on pushes to `main` and manual dispatches. The resulting debug APK is uploaded as the `fuck-ass-browser-debug` artifact when the build succeeds.
