import { createHmac } from "node:crypto";
import { Redis } from "@upstash/redis";

export type PresenceInput = {
  id: string;
  platform: "android";
  playbackActive: boolean;
};

const HEARTBEAT_SECONDS = 300;
const PRESENCE_TTL_MS = (HEARTBEAT_SECONDS + 90) * 1000;
const INSTALL_ID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

let redisClient: Redis | undefined;

function redis(): Redis {
  if (redisClient) return redisClient;
  const url = process.env.UPSTASH_REDIS_REST_URL ?? process.env.KV_REST_API_URL ?? process.env.STORAGE_URL;
  const token = process.env.UPSTASH_REDIS_REST_TOKEN ?? process.env.KV_REST_API_TOKEN ?? process.env.STORAGE_TOKEN;
  if (!url || !token) throw new Error("Stats storage is not configured.");
  redisClient = new Redis({ url, token });
  return redisClient;
}

function installationKey(id: string): string {
  const secret = process.env.MELO_STATS_HASH_SECRET;
  if (!secret || secret.length < 32) throw new Error("Stats hashing is not configured.");
  return createHmac("sha256", secret).update(id).digest("hex");
}

/** Accepts older app payloads, but keeps only the active-playback bit. */
export function parsePresence(value: unknown): PresenceInput | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  const body = value as Record<string, unknown>;
  if (typeof body.id !== "string" || !INSTALL_ID_PATTERN.test(body.id)) return null;
  if (body.platform !== "android" || typeof body.playbackActive !== "boolean") return null;
  return { id: body.id, platform: "android", playbackActive: body.playbackActive };
}

export async function recordPresence(input: PresenceInput): Promise<boolean> {
  if (!input.playbackActive) return true;

  const db = redis();
  const now = Date.now();
  const id = installationKey(input.id);
  await db.zadd("melo:presence:playback", { score: now, member: id });
  return true;
}

export async function activePlaybackCount(): Promise<number> {
  const now = Date.now();
  const count = await redis().zcount("melo:presence:playback", now - PRESENCE_TTL_MS, "+inf");
  return Number(count ?? 0);
}

export async function publicStats(): Promise<Record<string, unknown>> {
  return {
    updatedAt: new Date().toISOString(),
    activePlaybackNow: await activePlaybackCount(),
    presenceExpiresAfterSeconds: PRESENCE_TTL_MS / 1000,
  };
}

export async function livePlaybackCount(): Promise<number> {
  return activePlaybackCount();
}
