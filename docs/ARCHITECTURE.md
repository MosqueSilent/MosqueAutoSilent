# Mosque Auto-Silent v0.2 architecture

## Activation

`selected SSID detected AND inside geofence AND now >= Iqama - 30s AND now < Iqama + D`

Defaults: L=30 seconds; D=10 minutes. D is adjustable by the local app operator. The Imam release control restores the saved profile early and suppresses reactivation for that same Iqama event.

## Presence strategy

- Scan on demand during setup.
- Schedule a scan at Iqama − 60 seconds rather than scanning continuously.
- Keep a fresh in-range scan result latched through the short prayer session; do not infer absence from a throttled/cached scan.
- Geofence uses a circular region centred at Tarfaya Mosque: 33.90066, -5.54761. Default radius 100 m, configurable in app. Current device location can be used for coffee-shop testing.

## Permissions

Permissions are not all install-time grants. Android runtime permissions and special settings access must be requested in context: precise location, background location, Wi-Fi scan access as required by Android version, and Notification Policy Access.

## Limitations

The local Imam control only affects the phone running the app. Remote mosque-wide release requires a deployed backend, authenticated mosque-admin role, registered attendee clients and reliable command delivery. MAWAQIT integration remains subject to authorised access.
