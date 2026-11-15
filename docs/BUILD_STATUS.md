# Build status for v0.3.0

## Current source behavior

- Native Android UI has Home, Setup, Schedule, Imam and More tabs, with system/light/dark appearance, an original mosque vector mark, and color-coded status states.
- Home shows a countdown to Iqama using the bundled Tarfaya Adhan calendar for 2026 and the supplied offsets: Fajer +23 min, Doher +15 min, Aser +15 min, Maghrib +10 min and Isha +15 min.
- Silence starts 30 seconds before Iqama. Restore is 10 minutes after Iqama by default and configurable from 1–30 minutes. The saved ringer mode is restored exactly.
- Setup offers Wi-Fi refresh and a fresh current-location refresh. Current-position refresh does not move the geofence. Separate controls configure the current-location test zone or Tarfaya zone. Both selected SSID presence and geofence presence are required.
- Imam tab shows device-local session/release counters and a local End Salat action. Connected attendees are unavailable because no backend is deployed or connected; remote control remains future functionality.

## Build verification

- Version: `0.3.0` (`versionCode` 3).
- Android build tool versions: AGP 8.10.1, Gradle 8.11.1, JDK 17, compileSdk 36.
- Local machine limitation: current environment has no detected Java/Gradle/Android SDK installation, so the edited v0.3 source has not been compiled locally.
- Previously verified v0.2 CI APK is **not** evidence that the current v0.3 source builds. A successful CI run and downloaded APK for this commit are required before claiming a v0.3 APK is built.

## Required device validation

Test on a Samsung Galaxy S23 Ultra: Normal/Vibrate/Silent profile restoration; Notification Policy Access denial or revocation; process death and reboot; geofence transitions while the UI is closed; Wi-Fi scan throttling; Doze and battery restrictions; and the local Imam release. Verify location permissions and the mosque's geofence center/radius on site. The current location/Adhan calendar and prayer offsets should be confirmed by the mosque before operational use.
