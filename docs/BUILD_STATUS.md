# Build status for v0.2.0

- Browser demo: multi-page Home / Setup / Schedule / Admin navigation, Tarfaya address and mapped coordinates, adjustable geofence radius, SSID list and simulation. The dependency-free smoke test `node tests/demo_logic_smoke.js` passes all six scenarios. GitHub Actions also passed this test.
- Android source: native multi-page UI, Tarfaya geofence centre (33.90066, -5.54761), saved adjustable 50–400 m radius, current-location coffee-shop geofence, explicit SSID selection, one-shot Wi-Fi scan scheduled at Iqama − 60 seconds, fixed L=30 seconds, adjustable D default 10 minutes, saved local sound-profile restoration, and local Imam release.
- Recovery: saved audio state is committed before silencing; a geofence exit restores outside the UI, and a boot receiver retries restoration after restart. Android policy access still must be granted for an actual restore.
- Gradle compatibility: AGP 8.10.1, Gradle 8.11.1, JDK 17 and compileSdk 36. The checked-in bootstrap scripts verify the Gradle distribution SHA-256 before extracting it.
- Prayer data: supplied annual PDF retained as reference only; it does not contain Iqama values.

## Verified CI build

- Workflow run: [Android debug build #2](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43762601281), commit `5498d81557d578ee24485ee41dd2861fc01a9213` — **success**.
- Passing steps: Android SDK setup, browser logic smoke test, Android unit tests (`testDebugUnitTest`), debug APK build (`assembleDebug`), and artifact upload.
- Artifact: `mosque-auto-silent-debug-apk`, GitHub artifact ID `14181211831` (ZIP size 3,622,942 bytes; retained through 2026-11-29).
- Retrieved APK: `android/app/build/outputs/apk/debug/app-debug.apk` (4,016,366 bytes). Verified it is a readable APK archive with 434 entries including `AndroidManifest.xml`, `classes.dex`, and `resources.arsc`.
- APK SHA-256: `e097df26d16f73cf8ad0c4da548c77cf062ea2d5b11d88749f444b497fb5f4f7`.
- The APK is a local ignored build artifact and is not committed to source control.

## Environment and remaining verification

The local execution environment has no Java runtime, Android SDK, Android build tools, Gradle installation or cached Android dependencies, so the Android build was verified in CI rather than locally. Opening the demo HTML directly in the browser was blocked by the browser's `file:` URL policy; the CI and local Node smoke harnesses passed.

Run native coffee-shop checks on a Samsung Galaxy S23 Ultra, including Normal/Vibrate/Silent restoration, Notification Policy Access revocation, reboot/process death, geofence exit while the UI is closed, Wi-Fi scan throttling, and Doze/battery restrictions. Scheduled activation still depends on the prototype process and scan timing. Verify background execution behavior before treating this as a reliable unattended service.

## Important

Android permissions cannot all be granted silently at installation. Wi-Fi/location permissions are runtime permissions; background location and Notification Policy Access require separate user approval in system settings. The app explains and requests each permission in context.

The Tarfaya coordinates are an approximate mapped centre, not a surveyed mosque boundary. The geofence radius is configurable; verify the centre and tune radius on-site before a public pilot.
