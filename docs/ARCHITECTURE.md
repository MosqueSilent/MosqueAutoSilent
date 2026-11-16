# Mosque Auto-Silent v0.4 architecture

## Activation and timing

`selected SSID present AND inside selected mosque geofence AND now >= Iqama - 30 seconds AND now < Iqama + D`

Tarfaya uses the bundled 2026 Adhan calendar and offsets: Fajer +23 minutes, Doher +15, Aser +15, Maghrib +10 and Isha +15. D defaults to 10 minutes and can be set to 1–30 minutes. The app persists the previous ringer mode before silencing and restores that exact mode on timeout or safety exit.

## Mosque profiles

The local selector contains Tarfaya plus user-added mosque profiles with names and coordinates. Only Tarfaya has a bundled prayer calendar. For another mosque, the schedule page reports missing calendar data and the automatic schedule path returns no event. The local test-Iqama control remains available for setup checks. Profiles and the selected mosque are stored on-device. No mosque directory or MAWAQIT service is queried.

## Presence

- The operator chooses the target SSID. Android may throttle scans and return cached results.
- The selected mosque geofence is configured from its stored coordinates. Refreshing current location reads and displays a position without changing that geofence.
- Changing mosque clears the old inside-geofence state and restores an active local session; the new mosque geofence must be configured.

## Language and appearance

The More page offers English and Arabic. Arabic uses a right-to-left layout and a built-in phrasebook for the prototype UI. Light/dark/system appearance is available. The background is an original, low-contrast geometric zellij-inspired vector in both themes.

## Imam boundary

End Salat restores only this phone's saved ringer profile. Session/release totals are local. Attendee totals and remote release are unavailable: `backend/worker/` is a scaffold and no authenticated backend connection exists.

## Permissions and reliability

The app requests precise location, background location, Wi-Fi scan permissions as needed, and Notification Policy Access in context. Scheduled scans depend on the prototype process. Process death, Doze, OEM battery controls, permission revocation and background geofence delivery need physical-device validation.
