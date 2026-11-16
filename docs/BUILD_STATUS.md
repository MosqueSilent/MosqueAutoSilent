# Build status for v0.7.1

## Changes in this update

- English, Arabic and French are bundled and work without language-model downloads. Other ML Kit languages remain searchable and use on-device translation after their model download.
- Schedule rows label the Iqama and Adhan columns. Iqama time appears on the left with its Adhan-based offset, and Adhan time appears on the right.
- Optional-language translation refreshes preserve the selected tab. Selecting French does not start ML Kit or recreate the activity after translation.

## Build status

- Version: `0.7.1` (version code `8`).
- The local environment lacks Android SDK Platform 36. The local build could not proceed because the pinned Gradle wrapper expects Windows PowerShell's `Get-FileHash`, which is unavailable in this sandbox shell. The pinned Gradle archive and JDK 17 distribution were independently checked against their published SHA-256 values; no v0.7.1 APK has been produced here yet.
- Previous verified release: [v0.7.0 Android debug build #10](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43837940341) — **success**.
- Previous artifact: `mosque-auto-silent-debug-apk`, ID `14211822049`, expires 2026-11-30. This is the v0.7.0 binary and does not include the changes listed above.
- Previous artifact archive SHA-256: `cbed079960318810a98d236215d6825bd2c654ea092c07a7d204053ddf8de86c`.

## Previous verified build details

- Source commit: `ad749db1f35d2c8fd66ff8ab22d3051fa9ba73e8` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/ad749db1f35d2c8fd66ff8ab22d3051fa9ba73e8).
- Browser smoke tests, Android unit tests, `assembleDebug` and artifact upload all passed for v0.7.0.
- The workflow built `android/app/build/outputs/apk/debug/app-debug.apk` and uploaded it in the artifact archive.

## Still requires Samsung Galaxy S23 Ultra validation

Check clock/status-bar spacing and bottom scroll reachability; Arabic RTL and French UI; light/dark themes; schedule Adhan/Iqama columns and offset labels; tab preservation while selecting optional ML Kit languages; home Wi-Fi/geofence refresh and signal bars against Android Wi-Fi settings; Normal/Vibrate/Silent restore; Notification Policy Access denial/revocation; process death/reboot; geofence transitions; Wi-Fi throttling; Doze and battery restrictions; and local Imam release. Test mosque-name search, nearby suggestions with and without location permission, and verify each selected mosque's geofence pin on site because geocoder results are not guaranteed exact. Verify the 2026 Tarfaya calendar and offsets; add each other mosque's own calendar before enabling its scheduled timing.
