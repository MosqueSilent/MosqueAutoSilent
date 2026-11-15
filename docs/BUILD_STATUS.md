# Build status for v0.3.0

## Implemented in source

- Native Android UI has Home, Setup, Schedule, Imam and More tabs, with system/light/dark appearance, an original mosque vector mark, and color-coded status states.
- Home shows a countdown to Iqama using the bundled Tarfaya Adhan calendar for 2026 and the supplied offsets: Fajer +23 min, Doher +15, Aser +15, Maghrib +10 and Isha +15.
- Silence starts 30 seconds before Iqama. Restore is 10 minutes after Iqama by default and configurable from 1–30 minutes. The saved ringer mode is restored exactly.
- Setup offers Wi-Fi refresh and a fresh current-location refresh. Current-position refresh does not move the geofence. Separate controls configure the current-location test zone or Tarfaya zone. Both selected SSID presence and geofence presence are required.
- Imam tab shows device-local session/release counters and a local End Salat action. Connected attendees are unavailable because no backend is deployed or connected; remote control remains future functionality.

## Verified GitHub Actions build

- Source commit: `0999d0159398bd71ec804808fdf178c4752b32ac` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/0999d0159398bd71ec804808fdf178c4752b32ac).
- Workflow run: [Android debug build #5](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43774523789) — **success**.
- Passing steps: Android SDK setup, browser smoke test, Android unit tests, debug APK build and artifact upload.
- Artifact: `mosque-auto-silent-debug-apk`, GitHub artifact ID `14186110307`, expires 2026-11-29.
- Retrieved APK: `artifacts/app-debug.apk` (4,047,149 bytes). Verified as a readable APK ZIP containing `AndroidManifest.xml`, `classes.dex` and `resources.arsc` among 439 entries.
- APK SHA-256: `926af914958edafa3ff79e95b169c12f45729b48afc1ab023998e8d57ab00f99`.
- The downloaded APK and artifact ZIP are local workspace artifacts and are excluded from Git.
- Local Android build was not available: the environment has no detected Java/Gradle/Android SDK installation and could not download Gradle. The v0.3 APK was built and verified in GitHub Actions.

## Required physical-device validation

Test on a Samsung Galaxy S23 Ultra: Normal/Vibrate/Silent profile restoration; Notification Policy Access denial or revocation; process death and reboot; geofence transitions while the UI is closed; Wi-Fi scan throttling; Doze and battery restrictions; and local Imam release. Verify location permissions and the mosque's geofence center/radius on site. Confirm the 2026 Adhan calendar and the supplied offsets with the mosque before operational use.
