import { errorResponse, json } from "../lib/http.js";
import { parsePresence, recordPresence } from "../lib/stats.js";

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== "POST") return errorResponse(405, "method_not_allowed");
    const raw = await request.text();
    if (raw.length > 12_000) return errorResponse(413, "payload_too_large");
    let value: unknown;
    try { value = JSON.parse(raw); } catch { return errorResponse(400, "invalid_json"); }
    const input = parsePresence(value);
    if (!input) return errorResponse(400, "invalid_payload");
    try {
      const accepted = await recordPresence(input);
      if (!accepted) return errorResponse(429, "rate_limited");
      return json({ ok: true, heartbeatSeconds: 300 }, 200);
    } catch {
      return errorResponse(503, "stats_unavailable");
    }
  },
};
