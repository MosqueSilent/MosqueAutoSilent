# Build status for v0.5.0

## Implemented

- Home, Setup, Schedule, Imam and More tabs with system/light/dark appearance and Arabic/English selection in a flag-marked dropdown.
- Root layout applies system-bar and display-cutout insets. Scroll views have bottom space, and text/buttons preserve font padding and can wrap so Arabic text and final setup/schedule items have room.
- Home Wi-Fi and geofence cards have separate refresh icons. The geofence refresh re-registers the active current-location test zone or selected mosque zone.
- Wi-Fi scan results display every visible SSID with estimated percentage and RSSI in dBm. The percentage is derived from the Android scan RSSI; Android may return cached or throttled results.
- Background is an original low-contrast vector inspired by the supplied mosque photo's arches, tiled columns, zellij geometry and carpet colors. The image itself is not included in source.
- Local mosque profiles remain selectable. Tarfaya is the only profile with a bundled 2026 Adhan calendar; other profiles do not receive scheduled timing until their own prayer data is added.
- Google dynamic translation is not enabled. Current English and Arabic are local; no Cloud API key, billable translation service or translation model is included.

## Verified GitHub Actions build

- Build source commit: `f50ae202a4baeed07199032a8dce7972cd3a2401` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/f50ae202a4baeed07199032a8dce7972cd3a2401).
- Workflow run: [Android debug build #7](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43829560433) — **success**.
- Browser smoke test, Android unit tests, `assembleDebug` and artifact upload all passed.
- Artifact: `mosque-auto-silent-debug-apk`, GitHub artifact ID `14208491167`, expires 2026-11-30.
- Downloaded APK: `artifacts/app-debug.apk` (4,064,647 bytes), verified as a ZIP containing `AndroidManifest.xml`, `classes.dex` and `resources.arsc` among 442 entries.
- APK SHA-256: `7eebe3fde14f6799405d7375712229cf0207e6720111fcd3bcd2b7b001cc260a`.
- Retrieved APK and artifact ZIP are local workspace files ignored by Git.
- The local shell lacks JDK/Android SDK and cannot download Gradle. CI built the v0.5.0 source.

## Still requires Samsung Galaxy S23 Ultra validation

Check the clock/status-bar spacing and bottom scroll reachability on the device; Arabic text and RTL layout; both theme variants of the wallpaper; home Wi-Fi/geofence refresh actions; RSSI labels against Android Wi-Fi settings; Normal/Vibrate/Silent restore behavior; Notification Policy Access denial/revocation; process death/reboot; background geofence transitions; Wi-Fi throttling; Doze and battery restrictions; and local Imam release. Verify mosque coordinates, the 2026 Tarfaya calendar and offsets on site. Add and verify each other mosque's own calendar before enabling scheduled timing there.
