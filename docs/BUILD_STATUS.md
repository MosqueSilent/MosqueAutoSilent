# Build status for v0.6.0

## Implemented

- Home, Setup, Schedule, Imam and More tabs with system/light/dark appearance.
- English and the built-in Arabic phrasebook remain available immediately. The language picker includes native names and flag markers for ML Kit's supported languages. Additional languages use Google ML Kit on-device translation, download their selected language model over Wi-Fi, and show English while the model is unavailable. There is no Google Cloud Translation API integration or API key. Machine-generated UI translations have not been reviewed for prayer or safety-critical wording.
- Root layout applies system-bar and display-cutout insets. Scroll views have bottom space, and text/buttons preserve font padding and can wrap so Arabic text and final setup/schedule items have room.
- Home Wi-Fi and geofence cards have separate refresh icons. The geofence refresh re-registers the active current-location test zone or selected mosque zone.
- Wi-Fi scan results display every visible SSID with estimated percentage and RSSI in dBm. The percentage is derived from the Android scan RSSI; Android may return cached or throttled results.
- Background is an original low-contrast vector inspired by the supplied mosque photo's arches, tiled columns, zellij geometry and carpet colors. The image itself is not included in source.
- Local mosque profiles remain selectable. Tarfaya is the only profile with a bundled 2026 Adhan calendar; other profiles do not receive scheduled timing until their own prayer data is added.

## Verified GitHub Actions build

- Source commit: `eb9763be4a47bedbd000191d393cd9cc01cf4aa6` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/eb9763be4a47bedbd000191d393cd9cc01cf4aa6).
- Workflow run: [Android debug build #9](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43832795524) — **success**.
- Browser smoke tests, Android unit tests, `assembleDebug` and artifact upload all passed.
- Artifact: `mosque-auto-silent-debug-apk`, ID `14210250178`, uploaded 2026-11-16 and expires 2026-11-30. Download it from the Actions run above.
- The workflow built `android/app/build/outputs/apk/debug/app-debug.apk` and uploaded it in the artifact archive. Artifact archive SHA-256: `4365746653b183863a6a061fd1efca880a4c2f398bf873812296b25568cdbdec`.
- Local Android build was unavailable because the workspace could not download Gradle. GitHub Actions used Gradle 8.11.1 and Java 17 to build this commit.

## Still requires Samsung Galaxy S23 Ultra validation

Check the clock/status-bar spacing and bottom scroll reachability on the device; Arabic text and RTL layout; both theme variants of the wallpaper; home Wi-Fi/geofence refresh actions; RSSI labels against Android Wi-Fi settings; Normal/Vibrate/Silent restore behavior; Notification Policy Access denial/revocation; process death/reboot; background geofence transitions; Wi-Fi throttling; Doze and battery restrictions; and local Imam release. Verify mosque coordinates, the 2026 Tarfaya calendar and offsets on site. Add and verify each other mosque's own calendar before enabling scheduled timing there. Install the APK from the v0.6.0 Actions artifact to test the ML Kit model download and translation on-device.
