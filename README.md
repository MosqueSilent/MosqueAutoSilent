# MosqueAutoSilent
MosqueAutoSilent

---

# Mosque Auto-Silent — Prototype v0.2.0

**Pilot:** Tarfaya Mosque, Meknes (MAWAQIT listing previously identified as mosque 5655)
**Prototype defaults:** lead time L = 30 seconds; automatic duration D = 10 minutes after Iqama; Imam early-release control enabled.

This package contains three different things, and they have different capabilities:

1. **Interactive browser demo** (`demo/index.html`) — a polished, clickable simulator for showing the logic and the Imam release flow. It does not scan real Wi-Fi radios or change Android sound settings.
2. **Native Android source project** (`android/`) — a first native test harness with live foreground Wi-Fi SSID scanning, a current-location geofence registration flow, Android ringer-mode save/silence/restore, adjustable D and the local Imam release control. It is source code, not a compiled APK.
3. **Cloud control-channel scaffold** (`backend/worker/`) — a Cloudflare Worker + Durable Object WebSocket endpoint for a future Imam-to-attendee `END_PRAYER` signal. It is not deployed or connected to the Android app in this prototype.

## v0.2 interface updates

The browser demo now has Home, Setup, Schedule and Admin pages, Tarfaya mosque address/coordinates, a 50–300 m geofence-radius control, and a Tarfaya-centre selector. The Android app has matching Home, Setup, Schedule and Admin navigation, Tarfaya geofence coordinates, a saved adjustable 50–400 m radius, and a coffee-shop current-location option. Operators must explicitly select an SSID; the app does not silently choose the first visible network.

The native app schedules its Wi-Fi scan for Iqama − 60 seconds instead of continuously scanning every 45 seconds. A fresh detected SSID is held as the presence result through the short prayer window; cached/throttled results are not treated as proof that the network disappeared.

## Start the interactive demo

- Open `demo/index.html` in a modern browser. The UI and simulation work when opened as a local file.
- Or serve the folder: from the package root run `python -m http.server 8000`, then open `http://localhost:8000/demo/`.
- On a phone, the simplest method is to transfer/open `demo/index.html`. For PWA installation and offline caching, host the `demo/` folder on an HTTPS origin.

### Demo sequence

1. Tap **Scan nearby Wi-Fi**. The browser will show illustrative sample networks only.
2. Add one or more SSIDs to the target list.
3. Toggle **Selected network is in range** on.
4. Toggle **Phone is inside the test area** on.
5. Tap **Set Iqama in 30 sec**. Since L is 30 seconds, the silent window begins immediately.
6. The simulated phone profile becomes **Silent** and D counts down from the Iqama time.
7. Tap **End prayer now** to restore the previous profile immediately, or wait for D to expire.
8. Test safety exits by switching Wi-Fi or geofence off while active; the demo restores the previous profile.

## Build the Android source project

Requirements: Android SDK Platform 36 and JDK 17. The checked-in bootstrap wrapper downloads the pinned Gradle 8.11.1 distribution and verifies its published SHA-256 before extracting it; Android Gradle Plugin 8.10.1 supports API 36.

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

APK output:

`android/app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow runs the browser logic smoke test, Android unit tests and debug build on pushes and manual dispatch, then uploads the APK as `mosque-auto-silent-debug-apk`.

### Android prototype behaviours

- **Wi-Fi scan:** the app requests location / nearby-device permissions and lists visible SSIDs from Android scan results. Select the target SSID in the spinner. Android may throttle or return cached results; cached results do not establish presence.
- **Geofence:** tap **Set current location as test-zone centre** to register a 150 m test geofence. Android background geofencing requires precise location and, on Android 10+, background location permission. A demo switch is provided to force the inside/outside state during tests.
- **Audio:** grant Notification Policy Access in Android settings. The app synchronously saves the original ringer mode before setting silent. It restores the saved mode on local release, automatic expiry, geofence exit, or phone restart. A denied/revoked policy grant leaves a pending restore for retry; the app cannot change ringer mode until the user restores that access.
- **Timing:** press one of the test-Iqama buttons. Silence begins at Iqama minus 30 seconds. Auto-restore is at Iqama plus D minutes. D is adjustable from 1–30 minutes and defaults to 10. Activation requires the manually selected SSID to be present, the geofence to be active, and the current time to be in the prayer window.
- **Imam release:** the native button currently restores the sound profile on the device running the app. Multi-device release is not connected until the backend and attendee subscriptions are deployed.

## Important limits of v0.2.0

- A debug APK is not included in the source package. See `docs/BUILD_STATUS.md` for the latest verified local and CI build results.
- The browser demo cannot enumerate nearby Wi-Fi SSIDs or change the phone's audio settings. It is a simulator for an in-person concept demonstration.
- The native Android scan code remains a prototype. Android may throttle Wi-Fi scans. Scheduled scans belong to the app process; process death can miss an activation, so the target Samsung device must be stress-tested before selecting a long-term background strategy. On restart, any saved local audio session is restored rather than silently resumed.
- The geofence registration path is included, but reliability under Doze, OEM battery management and background permission behaviour still needs physical-device testing.
- The Imam button is local-only in this build. Remote release to all attendees requires the included backend to be deployed, app clients to authenticate and maintain/restore their control channel, and the event handling to be integrated into the native app.
- The annual MAWAQIT PDF has only Fajr, Shurûq, Dhuhr, Asr, Maghrib and Isha columns. It has no Iqama values. It is included as prayer-time reference data only; the demo uses a manually scheduled test Iqama.

## Main design rule

Activate only when all three are true:

`selected mosque SSID in range AND inside geofence AND now >= Iqama - 30 seconds AND now < Iqama + D`

Restore when any safety exit occurs:

- Imam presses **End prayer now**;
- automatic end time `Iqama + D` is reached;
- Wi-Fi signal is lost; or
- the phone exits the geofence.

The original ringer mode is captured only once per active session and restored once. An already-silent device returns to silent, not normal.
