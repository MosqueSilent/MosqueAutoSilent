# Mosque Auto-Silent v0.3 architecture

## Activation and timing

Activation requires all of the following:

`selected SSID present AND inside the configured geofence AND now >= Iqama - 30 seconds AND now < Iqama + D`

The bundled 2026 Tarfaya Adhan calendar supplies the Adhan time. Iqama offsets are Fajer +23 minutes, Doher +15, Aser +15, Maghrib +10 and Isha +15. D defaults to 10 minutes and can be configured from 1 to 30 minutes. When activated, the app saves the previous ringer mode before silencing. It restores that exact mode on timeout or safety exit.

## Presence

- The operator explicitly selects a target SSID. Wi-Fi scans are performed during setup and near the next Iqama; Android may throttle scans and return cached results.
- The phone must be inside the registered geofence. Tarfaya center is 33.90066, -5.54761, with a configurable radius. A current-location test zone is available.
- Refreshing current location only reads and displays a fresh position. It does not relocate the registered geofence.

## Imam controls and data boundaries

The Imam tab's End Salat action restores this device's saved ringer mode and marks the current local session ended. Its session and release counters are stored locally. Connected attendee totals and mosque-wide End Salat are unavailable. `backend/worker/` is an un-deployed scaffold, and the app has no authenticated remote-control connection.

## UI

The native app uses a light/dark/system theme, color-coded waiting/ready/active statuses, Home Iqama countdown, Setup, Schedule, Imam and More tabs. Branding uses a project-original vector mark and palette inspired by MAWAQIT's visual direction; there is no affiliation or shared brand asset.

## Permissions and reliability limits

Android runtime permission and system access are requested in context: precise location, background location, Wi-Fi scan access as required by Android, and Notification Policy Access. Scheduled Wi-Fi scanning depends on the prototype process. Process death, Doze, OEM battery controls, permission revocation and background geofence delivery require physical-device validation before relying on unattended operation.
