# Build status for v0.4.0

## Implemented

- Home, Setup, Schedule, Imam and More tabs with system/light/dark appearance, Arabic/English language selection, Arabic RTL layout, a faint original zellij-inspired vector background and color-coded status states.
- Setup supports the bundled Tarfaya profile and adding local mosque profiles by name and coordinates. Profiles are saved on-device. Only Tarfaya has a supplied prayer calendar; other profiles show that scheduled timing is unavailable until their own Adhan data is added. Tarfaya times are never reused for another mosque.
- Tarfaya 2026 Adhan times derive Iqama from the supplied offsets: Fajer +23 minutes, Doher +15, Aser +15, Maghrib +10 and Isha +15.
- Silence begins 30 seconds before Iqama. Restore defaults to 10 minutes afterward and is configurable from 1–30 minutes. The exact saved ringer profile is restored.
- Wi-Fi and current-position refresh controls are separate. Changing mosque clears the previous inside-geofence state and ends any active local session.
- Imam End Salat and session/release counters remain local to this phone. Connected attendees and mosque-wide release are not connected to a backend.

## Verified GitHub Actions build

- Build source commit: `1501d6b07f7ff46fc3a01d4ac4a37d8c0e948711` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/1501d6b07f7ff46fc3a01d4ac4a37d8c0e948711).
- Workflow run: [Android debug build #6](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43794220934) — **success**.
- Browser smoke test, Android unit tests, `assembleDebug` and artifact upload all passed.
- Artifact: `mosque-auto-silent-debug-apk`, GitHub artifact ID `14193333533`, expires 2026-11-30.
- Downloaded APK: `artifacts/app-debug.apk` (4,059,245 bytes), inspected as a valid APK ZIP with 440 entries including `AndroidManifest.xml`, `classes.dex` and `resources.arsc`.
- APK SHA-256: `614eda9b92ff0d53a6eea2583555624053525abb9b4f1d87b9f62805ce09fc19`.
- The retrieved APK and ZIP are local workspace artifacts excluded from Git.
- The local shell has no detected JDK/Android SDK and cannot download Gradle. The v0.4.0 Android build was verified in GitHub Actions.

## Still requires Samsung Galaxy S23 Ultra validation

Check Normal/Vibrate/Silent restore behavior; Notification Policy Access denial/revocation; process death and reboot; geofence transitions with the app closed; Wi-Fi scan throttling; Doze and battery management; local Imam release; Arabic text direction and layout; zellij background visibility in both themes; and selected-mosque geofence behavior. Confirm mosque coordinates, the 2026 Adhan calendar and offsets on site. Add and verify each other mosque's own calendar before enabling scheduled timing there.
