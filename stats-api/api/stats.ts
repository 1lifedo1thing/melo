import { errorResponse, json } from "../lib/http.js";
import { publicStats } from "../lib/stats.js";

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== "GET") return errorResponse(405, "method_not_allowed");
    try {
      return json(await publicStats(), 200, "public, max-age=15, s-maxage=15");
    } catch {
      return errorResponse(503, "stats_unavailable");
    }
  },
};
