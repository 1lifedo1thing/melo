import { errorResponse } from "../../../lib/http.js";
import { livePlaybackCount } from "../../../lib/stats.js";

export default {
  async fetch(request: Request): Promise<Response> {
    if (request.method !== "GET") return errorResponse(405, "method_not_allowed");
    try {
      const value = (await livePlaybackCount()).toString();
      const label = "ACTIVE PLAYBACK";
      const labelWidth = 142;
      const valueWidth = Math.max(45, value.length * 8.25 + 24);
      const width = labelWidth + valueWidth;
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="28" role="img" aria-label="${label}: ${value}"><title>${label}: ${value}</title><g shape-rendering="crispEdges"><rect width="${labelWidth}" height="28" fill="#0d1117"/><rect x="${labelWidth}" width="${valueWidth}" height="28" fill="#d6335c"/></g><g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="100"><text transform="scale(.1)" x="${labelWidth * 5}" y="175" textLength="${(label.length * 6.4) * 10}">${label}</text><text transform="scale(.1)" x="${(labelWidth + valueWidth / 2) * 10}" y="175" textLength="${value.length * 8.25 * 10}" font-weight="bold">${value}</text></g></svg>`;
      return new Response(svg, {
        status: 200,
        headers: {
          "Content-Type": "image/svg+xml; charset=utf-8",
          "Cache-Control": "public, max-age=30, s-maxage=30, stale-while-revalidate=15",
          "Access-Control-Allow-Origin": "*",
          "X-Content-Type-Options": "nosniff",
        },
      });
    } catch {
      return errorResponse(503, "stats_unavailable");
    }
  },
};
