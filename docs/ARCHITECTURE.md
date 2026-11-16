# Mosque Auto-Silent v0.5 architecture

## Activation and timing

`selected SSID present AND inside selected mosque geofence AND now >= Iqama - 30 seconds AND now < Iqama + D`

Tarfaya uses the bundled 2026 Adhan calendar and offsets: Fajer +23 minutes, Doher +15, Aser +15, Maghrib +10 and Isha +15. D defaults to 10 minutes and can be set to 1–30 minutes. The app persists the previous ringer mode before silencing and restores that exact mode on timeout or safety exit.

## Mosque profiles

The local selector contains Tarfaya plus user-added mosque profiles with names and coordinates. Only Tarfaya has a bundled prayer calendar. Other profiles show missing calendar data and have no automatic schedule event. The local test-Iqama control remains available for setup checks. Profiles and the selected mosque are stored on-device. No mosque directory or MAWAQIT service is queried.

## Presence and refresh

- The operator chooses the target SSID. Android may throttle scans and return cached results. Each visible SSID shows an estimated percentage and RSSI dBm from its strongest scan result.
- Home provides an icon to rescan Wi-Fi and an icon to re-register the configured geofence. The geofence icon uses the current-location test zone if selected, or the selected mosque's coordinates otherwise.
- Changing mosque clears the old inside-geofence state and restores an active local session; the new mosque geofence must be configured.

## Layout and appearance

System-bar and display-cutout insets are applied to the root view. Scroll views include bottom space and allow content to clear the navigation bar. Text views keep font padding, larger edge padding and up to two lines on buttons to reduce Arabic clipping.

The More page offers English and Arabic in a native dropdown with flag markers. Arabic uses RTL layout and the reviewed built-in phrasebook. Cloud/dynamic translation is not connected. The subtle original vector wallpaper draws horseshoe arches, column lines, zellij diamonds and restrained carpet colors based on the supplied mosque photo. Light and dark variants keep the pattern low contrast.

## Imam boundary

End Salat restores only this phone's saved ringer profile. Session/release totals are local. Attendee totals and remote release are unavailable: `backend/worker/` is a scaffold and no authenticated backend connection exists.

## Permissions and reliability

The app requests precise location, background location, Wi-Fi scan permissions as needed, and Notification Policy Access in context. Scheduled scans depend on the prototype process. Process death, Doze, OEM battery controls, permission revocation and background geofence delivery need physical-device validation.
