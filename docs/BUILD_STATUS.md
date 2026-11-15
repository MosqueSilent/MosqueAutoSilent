# Build status for v0.2.0

- Browser demo: multi-page Home / Setup / Schedule / Admin navigation, Tarfaya address and mapped coordinates, adjustable geofence radius, SSID list and simulation. `node tests/demo_logic_smoke.js` passes all six scenarios.
- Android source: native multi-page UI, Tarfaya geofence centre (33.90066, -5.54761), saved adjustable 50–400 m radius, current-location coffee-shop geofence, explicit SSID selection, one-shot Wi-Fi scan scheduled at Iqama − 60 seconds, fixed L=30 seconds, adjustable D default 10 minutes, saved local sound-profile restoration, and local Imam release.
- Recovery: saved audio state is committed before silencing; a geofence exit restores outside the UI, and a boot receiver retries restoration after restart. Android policy access still must be granted for an actual restore.
- Gradle compatibility: AGP 8.10.1, Gradle 8.11.1, JDK 17 and compileSdk 36. The checked-in bootstrap scripts verify the Gradle distribution SHA-256 before extracting it.
- Prayer data: supplied annual PDF retained as reference only; it does not contain Iqama values.
- Browser test result: `node tests/demo_logic_smoke.js` — PASS, six scenarios.
- Android unit tests, debug APK and CI artifact: pending successful execution. No APK is claimed until the generated artifact is downloaded and verified.

## Environment limits

The local execution environment has no Java runtime, Android SDK, Android build tools, Gradle installation or cached Android dependencies. Direct shell access to GitHub is blocked. The GitHub Actions workflow runs `clean testDebugUnitTest assembleDebug` and uploads `app-debug.apk`; its result must be checked after the source is pushed.

## Verification still required

- GitHub Actions Android unit tests and debug APK artifact are pending a successful CI run.
- Run native coffee-shop checks on a Samsung Galaxy S23 Ultra, including Normal/Vibrate/Silent restoration, Notification Policy Access revocation, reboot/process death, geofence exit while the UI is closed, Wi-Fi scan throttling, and Doze/battery restrictions.
- Scheduled activation still depends on the prototype process and scan timing. Verify background execution behavior before treating this as a reliable unattended service.

## Important

Android permissions cannot all be granted silently at installation. Wi-Fi/location permissions are runtime permissions; background location and Notification Policy Access require separate user approval in system settings. The app explains and requests each permission in context.

The Tarfaya coordinates are an approximate mapped centre, not a surveyed mosque boundary. The geofence radius is configurable; verify the centre and tune radius on-site before a public pilot.
