# Prototype verification plan

## Browser demo checks

1. Open `demo/index.html`.
2. Select **Scan nearby Wi-Fi** and add `Cafe_Guest_WiFi`.
3. Enable the Wi-Fi-in-range and inside-geofence switches.
4. Set Iqama to 30 seconds from now.
5. Confirm the simulated profile changes from the selected previous profile to Silent.
6. Tap **End prayer now** and confirm the previous profile is restored.
7. Confirm the session does not reactivate during the same Iqama window after manual Imam release.
8. Repeat, then disable Wi-Fi or geofence while active; confirm restoration.
9. Repeat without selecting a target SSID; confirm Wi-Fi presence alone cannot activate.
10. Repeat without entering the geofence; confirm no activation.
11. Set D to 1 minute and confirm automatic restoration at Iqama + 1 minute.
12. Set D to 10 minutes and confirm the default.
13. Set previous profile to Vibrate or Silent; confirm it restores to that same state.

## Native Android coffee-shop checks

- Enable Android Location.
- Tap Scan nearby Wi-Fi; grant location / nearby devices permissions.
- Select the coffee-shop SSID. Do not connect to it; visibility is what matters.
- Tap Set current location as test-zone centre; grant precise and background location permissions as prompted.
- Turn on the demo Wi-Fi and inside-zone overrides only when necessary to isolate the state machine.
- Grant Notification Policy Access from Android settings.
- Set test Iqama in 30 seconds. Confirm the device ringer mode becomes Silent.
- Tap Imam: End prayer now; confirm the original ringer mode returns and does not reactivate for that same Iqama event.
- Repeat with Normal, Vibrate and Silent as the original profile.
- Test scan freshness and repeatability in the same physical location. Record device model, Android build, scan timestamp, selected SSID, RSSI if available, and whether the scan was fresh or cached.

## Safety / acceptance criteria

- No audio setting is changed until Notification Policy Access is granted.
- Do not overwrite the saved original profile while a session is already active.
- End-prayer is idempotent: repeated taps must not toggle or corrupt the restored profile.
- A manual Imam release suppresses reactivation for the same prayer event.
- An exit from the geofence or confirmed loss of the selected Wi-Fi signal restores the saved profile.
- A scan result older than two minutes is not considered a current positive presence signal in the Android test harness.
- The test must never treat Adhan values from the supplied annual calendar as Iqama.
