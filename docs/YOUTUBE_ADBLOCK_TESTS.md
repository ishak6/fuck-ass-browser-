# YouTube Ad-Filtering Manual Test Checklist

**Last recorded test date:** 2026-11-16  
**Result:** NOT RUN — no real Android device or live YouTube page was available in this work session. No YouTube ad-blocking capability is claimed.

## Preconditions
- Run only after :engine-html, :engine-js, :engine-render and the relevant :adblock rules are integrated.
- Record device model, Android version, app commit, engine build, filter-set version and network conditions.
- Use a normal user-visible YouTube page. Do not bypass age gates, access controls, DRM or anti-adblock checks.

## Manual cases

| Case | Procedure | Expected observation | Result |
|---|---|---|---|
| Video-start ad | Open a video known to show a pre-roll ad | Record whether an ad appears, playback starts, or the page reports a compatibility issue | NOT RUN |
| Mid-roll ad | Play a sufficiently long video through a mid-roll point | Record whether the ad appears and whether playback resumes normally | NOT RUN |
| Banner / sponsored card | Inspect home, watch and search surfaces | Record visible banner/sponsored placements and any false-positive hiding | NOT RUN |
| Shorts | Open several Shorts and scroll between them | Record ad placements, overlays, playback and scrolling behavior | NOT RUN |

## Automated test requirements
- Rule parser: pinned fixtures for supported network, cosmetic and scriptlet syntax; malformed and unsupported syntax must fail safely.
- Scriptlet fixtures: deterministic synthetic JSON inputs/outputs, including absent keys, nested objects, arrays, malformed JSON and size limits. Do not treat fixture success as proof of live YouTube player compatibility.
- Filter updates: HTTPS fetch, integrity/parse validation, atomic activation, last-known-good rollback, stale-state and refresh tests.
- Regression: confirm ordinary video playback is not broken by a rule; record false positives and the exact rule responsible.

## Reporting
For every real-device run, append the date, device/build/filter-set versions, per-case PASS/FAIL/BLOCKED result and concise observations. Mark YouTube ad blocking **Partial** until the full matrix is run on a real device; server-side ad insertion may remain unfilterable.
