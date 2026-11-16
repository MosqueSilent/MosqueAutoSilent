# Build status for v0.7.1

## Changes in this update

- English, Arabic and French are bundled and work without language-model downloads. Other ML Kit languages remain searchable and use on-device translation after their model download.
- Schedule rows label the Iqama and Adhan columns. Iqama time appears on the left with its Adhan-based offset, and Adhan time appears on the right.
- Optional-language translation refreshes preserve the selected tab. Selecting French does not start ML Kit or recreate the activity after translation.

## Verified GitHub Actions build

- Version: `0.7.1` (version code `8`).
- Source commit: `5069ed33a16188cf638d26b6546600ddb7c0f975` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/5069ed33a16188cf638d26b6546600ddb7c0f975).
- Workflow: [Android debug build #12](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43948762809) — **success**.
- Browser smoke tests, Android unit tests, `assembleDebug`, and artifact upload completed successfully.
- Artifact: `mosque-auto-silent-debug-apk`, ID `14267510417`, expires 2026-11-30. Download it from the Actions run above.
- Artifact archive SHA-256: `d889e8f0b1a5ab132dc43a914af2d42000a57e9644398d0332c91f27ba7f221a`.
- APK package: `org.mosqueautosilent.app`; verified `versionCode=8`, `versionName=0.7.1`, `minSdk=26`, `targetSdk=35`, and `compileSdk=36` from the downloaded APK.
- APK SHA-256: `F376413BAD2A64260825FBC2C6974CF44606BAC0DC0B610855FB1A24823C64B8`.

## Still requires Samsung Galaxy S23 Ultra validation

Check clock/status-bar spacing and bottom scroll reachability; Arabic RTL and French UI; light/dark themes; schedule Adhan/Iqama columns and offset labels; tab preservation while selecting optional ML Kit languages; home Wi-Fi/geofence refresh and signal bars against Android Wi-Fi settings; Normal/Vibrate/Silent restore; Notification Policy Access denial/revocation; process death/reboot; geofence transitions; Wi-Fi throttling; Doze and battery restrictions; and local Imam release. Test mosque-name search, nearby suggestions with and without location permission, and verify each selected mosque's geofence pin on site because geocoder results are not guaranteed exact. Verify the 2026 Tarfaya calendar and offsets; add each other mosque's own calendar before enabling its scheduled timing.
