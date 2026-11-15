# Coffee-shop proof-of-concept runbook

## Browser walkthrough (safe UI demo)

1. Open `demo/index.html`.
2. Press **Scan nearby Wi-Fi** and choose one of the illustrative sample networks, or add the actual coffee-shop SSID manually.
3. Set **Selected network is in range** and **Phone is inside the test area** to ON.
4. Leave D at 10 minutes for the first walkthrough.
5. Press **Set Iqama in 30 sec**. The demo should enter Silent immediately because L is 30 seconds.
6. Show the Imam control and press **End prayer now**. The simulated phone returns to its saved Normal / Vibrate / Silent profile.
7. Repeat with Wi-Fi OFF or geofence OFF to demonstrate that one signal alone cannot activate Mosque Mode.

## Native Android walkthrough (after building the APK)

1. Install the debug APK on the phone and keep the app open for this early test.
2. At the coffee shop, tap **Scan nearby Wi-Fi**, grant the Android permissions, then select a visible cafe SSID. Do not connect to it; selection is based on scan visibility.
3. Tap **Set current location as test-zone centre (150 m)**. Grant precise and background location permissions as Android requests them.
4. Grant **Notification Policy Access** in Settings. Set the phone to Normal or Vibrate before the test.
5. If testing the state machine independently from GPS/Wi-Fi, use the two demo override switches. For the real test, leave overrides OFF and use the live scan + geofence.
6. Press **Set test Iqama in 30 seconds**. The phone should silence immediately if the two presence conditions are true.
7. Press **IMAM: END PRAYER NOW**. Verify that the original ringer mode returns.
8. Keep the app in the foreground during this prototype test. Process-death/reboot restoration and remote release to multiple participants are not yet complete.

## What the coffee-shop demo proves

- A public Wi-Fi SSID can be selected without joining it.
- An area boundary narrows the radio range to a physical test zone.
- Iqama timing controls when activation occurs.
- D is adjustable, has a 10-minute default, and an Imam can release early.
- The participant device restores the exact saved ringer mode in the native version.

It does not yet prove reliable background Wi-Fi scanning on every Android OEM, or remote restore across multiple phones. Those are the next validation milestones.
