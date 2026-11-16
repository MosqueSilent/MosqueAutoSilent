# Build status for v0.7.1

## Changes in this update

- English, Arabic and French are bundled and work without language-model downloads. Other ML Kit languages remain searchable and use on-device translation after their model download.
- Schedule rows label the Iqama and Adhan columns. Iqama time appears on the left with its Adhan-based offset, and Adhan time appears on the right.
- Optional-language translation refreshes preserve the selected tab. Selecting French does not start ML Kit or recreate the activity after translation.

## Verified GitHub Actions build

- Version: `0.7.1` (version code `8`).
- Source commit: `44cf9f6bcc35d2fe7c638ec5d7a8b68631081189` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/44cf9f6bcc35d2fe7c638ec5d7a8b68631081189).
- Workflow: [Android debug build #11](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43947390399) — **success**.
- Browser smoke tests, Android unit tests, `assembleDebug`, and artifact upload completed successfully.
- Artifact: `mosque-auto-silent-debug-apk`, ID `14263449994`, expires 2026-11-30. Download it from the Actions run above.
- Artifact archive SHA-256: `419488d524c6a15d81130d4007de52668531ba23f22beb82778da13a160a36dc`.
- APK package: `org.mosqueautosilent.app`; verified `versionCode=8`, `versionName=0.7.1`, `minSdk=26`, `targetSdk=35`, and `compileSdk=36` from the downloaded APK.
- APK SHA-256: `80F8B10B3EEFFFDD7C1CAFECD3C671B7149D7797F713BB668BA87D4C6B840729`.

## Still requires Samsung Galaxy S23 Ultra validation

Check clock/status-bar spacing and bottom scroll reachability; Arabic RTL and French UI; light/dark themes; schedule Adhan/Iqama columns and offset labels; tab preservation while selecting optional ML Kit languages; home Wi-Fi/geofence refresh and signal bars against Android Wi-Fi settings; Normal/Vibrate/Silent restore; Notification Policy Access denial/revocation; process death/reboot; geofence transitions; Wi-Fi throttling; Doze and battery restrictions; and local Imam release. Test mosque-name search, nearby suggestions with and without location permission, and verify each selected mosque's geofence pin on site because geocoder results are not guaranteed exact. Verify the 2026 Tarfaya calendar and offsets; add each other mosque's own calendar before enabling its scheduled timing.
