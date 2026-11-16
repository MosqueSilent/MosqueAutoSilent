# Build status for v0.7.0

## Implemented

- Home, Setup, Schedule, Imam and More tabs with system/light/dark appearance.
- English and the built-in Arabic phrasebook remain available immediately. French is now the third language choice, alongside English and Arabic; it uses the on-device translation model after its Wi-Fi download. Other ML Kit languages remain searchable in the picker.
- Add Mosque searches a mosque name and city through Android's system geocoder. Nearby suggestions use the last known phone location when location permission is available; selecting a result fills its coordinates. Geocoder results are best guesses and must be verified on site.
- Language, mosque, theme and Wi-Fi choices use searchable rounded selection sheets instead of basic spinners.
- Schedule rows use content-based height to keep Adhan and Iqama times visible in English and Arabic.
- Home Wi-Fi strength displays color-coded signal bars instead of a percentage; selected-network RSSI remains visible in dBm. Wi-Fi and geofence refresh controls are filled, animated icons, and home cards include Wi-Fi/location icons.
- Root layout applies system-bar and display-cutout insets. Scroll views have bottom space, and text/buttons preserve font padding and can wrap so Arabic text and final setup/schedule items have room.
- Background is an original low-contrast vector inspired by the supplied mosque photo's arches, tiled columns, zellij geometry and carpet colors. The image itself is not included in source.
- Local mosque profiles remain selectable. Tarfaya is the only profile with a bundled 2026 Adhan calendar; other profiles do not receive scheduled timing until their own prayer data is added.

## Verified GitHub Actions build

- Source commit: `ad749db1f35d2c8fd66ff8ab22d3051fa9ba73e8` — [view commit](https://github.com/MosqueSilent/MosqueAutoSilent/commit/ad749db1f35d2c8fd66ff8ab22d3051fa9ba73e8).
- Workflow run: [Android debug build #10](https://github.com/MosqueSilent/MosqueAutoSilent/actions/runs/43837940341) — **success**.
- Browser smoke tests, Android unit tests, `assembleDebug` and artifact upload all passed.
- Artifact: `mosque-auto-silent-debug-apk`, ID `14211822049`, uploaded 2026-11-16 and expires 2026-11-30. Download it from the Actions run above.
- The workflow built `android/app/build/outputs/apk/debug/app-debug.apk` and uploaded it in the artifact archive. Artifact archive SHA-256: `cbed079960318810a98d236215d6825bd2c654ea092c07a7d204053ddf8de86c`.

## Still requires Samsung Galaxy S23 Ultra validation

Check clock/status-bar spacing and bottom scroll reachability; Arabic RTL text; light/dark themes; schedule Adhan/Iqama labels; home Wi-Fi/geofence refresh and signal bars against Android Wi-Fi settings; Normal/Vibrate/Silent restore; Notification Policy Access denial/revocation; process death/reboot; geofence transitions; Wi-Fi throttling; Doze and battery restrictions; and local Imam release. Test mosque-name search, nearby suggestions with and without location permission, and verify each selected mosque's geofence pin on site because geocoder results are not guaranteed exact. Install the v0.7.0 artifact and test French model download/translation on-device. Verify the 2026 Tarfaya calendar and offsets; add each other mosque's own calendar before enabling its scheduled timing.
