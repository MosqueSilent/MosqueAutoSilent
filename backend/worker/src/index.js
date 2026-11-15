/**
 * Mosque Auto-Silent control channel (prototype scaffold).
 * - Attendee devices connect to /v1/mosques/:id/connect over WebSocket.
 * - The Imam/admin sends POST /v1/mosques/:id/end-prayer with a bearer token.
 * - The Durable Object broadcasts END_PRAYER to currently connected devices.
 *
 * Set ADMIN_TOKEN with `npx wrangler secret put ADMIN_TOKEN` before deployment.
 * This is a prototype control plane; production requires per-mosque admin auth,
 * signed attendee enrollment tokens, audit logging and abuse controls.
 */

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const match = url.pathname.match(/^\/v1\/mosques\/([A-Za-z0-9_-]+)\/(connect|end-prayer|state)$/);
    if (!match) return json({ error: "Not found" }, 404);

    const [, mosqueId, action] = match;
    const roomId = env.MOSQUE_ROOMS.idFromName(mosqueId);
    const room = env.MOSQUE_ROOMS.get(roomId);

    if (action === "connect") {
      if (request.headers.get("Upgrade") !== "websocket") return json({ error: "WebSocket upgrade required" }, 426);
      return room.fetch(new Request("https://room/connect", { headers: request.headers }));
    }

    if (action === "end-prayer") {
      if (!env.ADMIN_TOKEN || request.headers.get("Authorization") !== `Bearer ${env.ADMIN_TOKEN}`) {
        return json({ error: "Unauthorised" }, 401);
      }
      const event = {
        type: "END_PRAYER",
        mosqueId,
        issuedAt: new Date().toISOString(),
        eventId: crypto.randomUUID()
      };
      const result = await room.fetch(new Request("https://room/broadcast", {
        method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(event)
      }));
      return json({ ok: result.ok, event });
    }

    if (action === "state") {
      return room.fetch(new Request("https://room/state"));
    }

    return json({ error: "Unsupported action" }, 400);
  }
};

export class MosqueRoom {
  constructor(ctx, env) { this.ctx = ctx; this.env = env; }

  async fetch(request) {
    const url = new URL(request.url);
    if (url.pathname === "/connect") {
      const pair = new WebSocketPair();
      const [client, server] = Object.values(pair);
      this.ctx.acceptWebSocket(server);
      // Do not replay a previous END_PRAYER command to a newly joined device.
      return new Response(null, { status: 101, webSocket: client });
    }
    if (url.pathname === "/broadcast" && request.method === "POST") {
      const event = await request.json();
      await this.ctx.storage.put("lastEvent", event);
      let delivered = 0;
      for (const socket of this.ctx.getWebSockets()) {
        try { socket.send(JSON.stringify(event)); delivered += 1; } catch (_) { /* stale socket */ }
      }
      return json({ ok: true, delivered });
    }
    if (url.pathname === "/state") {
      return json({ lastEvent: await this.ctx.storage.get("lastEvent") ?? null, connected: this.ctx.getWebSockets().length });
    }
    return json({ error: "Not found" }, 404);
  }

  async webSocketMessage(socket, message) {
    if (message === "PING") socket.send("PONG");
  }
  async webSocketClose(socket) { try { socket.close(); } catch (_) {} }
}

function json(value, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "access-control-allow-origin": "*" }
  });
}
