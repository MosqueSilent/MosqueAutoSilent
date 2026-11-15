# Mosque Auto-Silent

## Android prototype v0.3.0

Mosque Auto-Silent helps an attendee's Android phone silence its ringer during prayer. The current prototype uses a selected Wi-Fi SSID and an active geofence as two required presence signals. It derives Iqama from the checked-in Tarfaya Adhan calendar and the offsets supplied by the local Imam:

| Prayer | Iqama offset after Adhan |
| --- | ---: |
| Fajer | 23 minutes |
| Doher | 15 minutes |
| Aser | 15 minutes |
| Maghrib | 10 minutes |
| Isha | 15 minutes |

Silence begins 30 seconds before Iqama. The saved ringer profile is restored 10 minutes after Iqama by default; the duration is configurable from 1 to 30 minutes. The Imam control ends the prayer session on this phone only.

The Android interface includes a Home countdown, Setup controls for SSID and geofence/location refresh, Schedule and timing controls, a local Imam dashboard, and light/dark/system appearance settings. Status colors distinguish ready, waiting and active conditions. The original mosque mark and teal/green palette are inspired by the visual direction of MAWAQIT; this project is not affiliated with MAWAQIT.

## Components and boundaries

- `android/` is the native Android application. Wi-Fi scanning, geofence events, local audio control and local Imam release run on the device.
- `demo/` is a browser simulator. It does not scan Wi-Fi radios or alter Android audio settings.
- `backend/worker/` is an undeployed scaffold for possible future mosque-wide commands. It is not connected to the app. Attendee rosters and connected-device counts are therefore unavailable; the Imam screen states this limitation and shows only on-device counters.

## Build the Android debug APK

Requirements: JDK 17 and Android SDK Platform 36. The Gradle wrapper pins Gradle 8.11.1; the project uses Android Gradle Plugin 8.10.1.

Windows PowerShell:

```powershell
cd android
.\gradlew.bat assembleDebug
```

macOS/Linux:

```bash
cd android
./gradlew assembleDebug
```

Expected APK: `android/app/build/outputs/apk/debug/app-debug.apk`. CI runs the browser smoke test and Android unit tests, builds the debug APK, and uploads it as `mosque-auto-silent-debug-apk`. See [docs/BUILD_STATUS.md](docs/BUILD_STATUS.md) for the latest verified build and artifact details.

## Android prototype behavior and limits

- Select the mosque SSID explicitly. Android can throttle Wi-Fi scans or return cached results; cached results do not prove that the selected network disappeared.
- The geofence requires precise location and background location permission on Android 10+. “Refresh current location” reads a fresh position without changing the configured zone. The separate current-location test-zone control registers a zone at that position.
- Grant Notification Policy Access for ringer changes. The app saves the prior ringer mode before silencing and restores it on local Imam release, timeout, safety exit or recovery. If access is revoked, restoration remains pending until access is restored.
- The Home countdown and Schedule page use the bundled `data/tarfaya_prayer_times_2026.csv` calendar. It contains Adhan times for 2026; this is not a live prayer-time feed. Confirm the calendar and offsets with the mosque before operational use.
- Scheduled Wi-Fi scans depend on the prototype app process. Process death, Android Doze, OEM battery management and background geofence delivery need physical-device testing.
- The Imam tab is local-only. Remote release and connected attendees require an approved, deployed backend, authenticated roles, client subscriptions and integrated command handling. No backend is deployed or connected in this build.
- The browser demo is a simulator, not a test of native Android permissions, radios or audio behavior.

## Product rule

Activation requires both selected SSID presence and geofence presence, and the current time must be within the prayer window. Restore the exact saved ringer profile when the Imam ends the local session, the configured duration expires, or a safety exit occurs.
