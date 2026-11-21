# Melo live playback API

This is a standalone Vercel Functions project. Set the Vercel project root to `stats-api`, connect Upstash Redis, and add the environment variables listed in `.env.example`.

## Endpoints

- `POST /api/presence` records an opted-in Android installation while its player is actively playing. A heartbeat updates one sorted-set entry; inactive payloads are accepted without a Redis request.
- `GET /api/stats` returns the current active playback count.
- `GET /api/stats/live/badge.svg` returns the GitHub README badge for active playback.

Presence expires after 390 seconds without a five-minute heartbeat. Installation UUIDs are HMACed before storage. The count endpoint removes expired entries as it reads. The API stores no account identifiers, track IDs, titles, artists, search text, or event metrics.

Detailed playback summaries (play starts, elapsed time while the player reports playing, buffering duration, and playback errors grouped by catalog source) go to Firebase Analytics. Elapsed player time is an estimate; it cannot prove that audio was audible to a person. Search and Radio usage are not collected. The same **Share usage analytics** setting controls Firebase collection and live-presence heartbeats.

To display summed durations in Google Analytics reports, register the event parameters `playing_ms` and `buffering_ms` as event-scoped custom metrics for `melo_player_playing_time` and `melo_buffering`. Firebase's Events report shows event counts by default; parameter reports need custom definitions and may take time to populate.

## Required Vercel environment variables

- Upstash REST URL and write token. The API accepts `UPSTASH_REDIS_REST_URL` / `UPSTASH_REDIS_REST_TOKEN`, Vercel Marketplace's `KV_REST_API_URL` / `KV_REST_API_TOKEN`, or the custom-prefix `STORAGE_URL` / `STORAGE_TOKEN` names. Do not use the read-only token because the API writes presence.
- `MELO_STATS_HASH_SECRET` — a random secret of at least 32 characters. Keep it in Vercel environment variables; never place it in Android code or commit it.

The Android build defaults to `https://api.melo.aarushsagar.com/api/presence`; override it with `-PmeloStatsEndpoint=...` when building for another deployment.

Each active heartbeat uses one Redis command. Each uncached count read uses two (remove expired members, then count live scores). A continuously active installation sends about 8,640 writes in a 30-day month, plus one immediate write when playback starts. Actual usage is lower when playback is intermittent. Stats responses are CDN cached for 15 seconds and badge responses for 30 seconds. These numbers reduce normal command use, but they cannot guarantee staying below Upstash's monthly quota for every traffic pattern.
