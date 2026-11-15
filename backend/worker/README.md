# Remote Imam release scaffold

This is an optional backend scaffold, not deployed and not yet wired to the Android UI.

## What it does

- `GET /v1/mosques/{mosqueId}/connect` upgrades an attendee connection to WebSocket.
- `POST /v1/mosques/{mosqueId}/end-prayer` checks `Authorization: Bearer $ADMIN_TOKEN`, persists an event, and broadcasts `END_PRAYER` to connected devices.
- `GET /v1/mosques/{mosqueId}/state` returns the latest room event and connection count.

## Deploy

1. Install Node.js and Wrangler.
2. From this directory run `npx wrangler login`.
3. Set an admin secret: `npx wrangler secret put ADMIN_TOKEN`.
4. Deploy: `npx wrangler deploy`.

Do not deploy this prototype publicly as-is. Before real mosque use, add per-mosque admin roles, secure QR enrollment / signed participant tokens, rate limits, event expiry and replay protection, data minimisation, auditing, and a recovery path. The Android app must be wired to subscribe and handle `END_PRAYER` before the Imam button can release other devices.
