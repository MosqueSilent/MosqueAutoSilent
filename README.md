# Mosque Auto-Silent

## Android prototype v0.6.0

Mosque Auto-Silent helps an attendee's Android phone silence its ringer during prayer. Activation requires both a selected Wi-Fi SSID and geofence presence. The app saves and restores the phone's previous ringer profile.

### Prayer timing

The currently supported Tarfaya calendar is bundled for 2026. Iqama is calculated from Adhan using the supplied offsets:

| Prayer | Iqama offset after Adhan |
| --- | ---: |
| Fajer | 23 minutes |
| Doher | 15 minutes |
| Aser | 15 minutes |
| Maghrib | 10 minutes |
| Isha | 15 minutes |

Silence starts 30 seconds before Iqama. Restore defaults to 10 minutes after Iqama and is configurable from 1 to 30 minutes. The app does not use Tarfaya's calendar for another selected mosque.

### Mosque, language and appearance

Setup lets the operator select Tarfaya or add local mosque profiles with names and coordinates. Tarfaya is the only profile with prayer calendar data; other profiles report that their calendar is missing and scheduled activation stays unavailable until their local timings are added. No MAWAQIT API is connected.

The More page has a native-name language dropdown with flag markers for English, Arabic and the languages supported by Google ML Kit on-device translation. Google describes ML Kit as a free mobile SDK; language models are about 30 MB each, and this app downloads the selected model over Wi-Fi. English and the reviewed Arabic phrasebook work immediately; other languages are translated on the phone after their model downloads. English stays visible as a fallback while the model downloads or if translation fails. These additional translations are machine generated and have not been reviewed for prayer or safety-critical wording. The app does not call Google Cloud Translation or require a Cloud API key. The faint Moroccan-style vector background is inspired by the submitted mosque's horseshoe arches, tiled columns, carved details and carpet tones. It is an original design rather than a faded photo.

### Home signals and refresh

Home has separate refresh icons for Wi-Fi scanning and the configured geofence. Wi-Fi results show estimated signal strength as a percentage and RSSI in dBm. The app applies system-bar insets and scroll padding so content clears the status/navigation bars and the last rows remain reachable.

### Imam controls and boundaries

The Imam tab has a local End Salat action and on-device counters. Connected attendees and mosque-wide release are unavailable because the backend is not deployed or connected. The original mosque mark and teal/green palette are inspired by MAWAQIT's visual direction; this project is not affiliated with MAWAQIT and does not use its logo.

## Components

- `android/` is the native Android app.
- `demo/` is a browser simulator; it cannot scan Wi-Fi radios or change Android sound settings.
- `backend/worker/` is an undeployed scaffold for future remote commands.

## Build the Android debug APK

Requirements: JDK 17 and Android SDK Platform 36. Gradle 8.11.1 is pinned by the wrapper; Android Gradle Plugin is 8.10.1.

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

Expected build output: `android/app/build/outputs/apk/debug/app-debug.apk`. The latest verified APK is available in the [v0.6.0 GitHub Actions artifact](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43832795524); see [docs/BUILD_STATUS.md](docs/BUILD_STATUS.md) for the commit, artifact ID and archive checksum. GitHub Actions runs the browser smoke test and Android unit tests, builds the APK, and uploads `mosque-auto-silent-debug-apk`.

## Prototype limits

- Wi-Fi scans can be throttled or cached by Android. Cached results do not establish that a selected network disappeared; the displayed percentage and dBm are estimates from the scan result.
- Background geofencing requires precise location and, on Android 10+, background location permission. Confirm the mosque geofence center/radius on site.
- Notification Policy Access is required for ringer changes. If access is revoked during restoration, the app leaves the saved restore pending.
- The only prayer calendar is Tarfaya's 2026 Adhan calendar. Other local mosque profiles need their own schedule data before automatic timing is available. This is not a live feed.
- Scheduled scans depend on the app process. Process death, Doze, OEM battery management and background geofence behavior need physical-device testing.
- The Imam action changes sound on this phone only. The attendee roster and mosque-wide control require an approved, deployed backend and authenticated clients; none is connected.
