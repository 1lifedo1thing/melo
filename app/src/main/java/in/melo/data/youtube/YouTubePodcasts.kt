package `in`.melo.data.youtube

import android.content.Context
import `in`.melo.data.Music
import `in`.melo.data.MusicKind
import org.json.JSONArray
import org.json.JSONObject

const val YTM_PODCAST_PREFIX = "ytm:podcast:"

data class YouTubePodcastShelf(
    val title: String,
    val items: List<Music>,
    val episodes: Boolean,
)

data class YouTubePodcastFeed(
    val shelves: List<YouTubePodcastShelf>,
    val continuation: String? = null,
)

data class YouTubePodcastFeedState(
    val feed: YouTubePodcastFeed? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

data class CachedYouTubePodcastFeed(val feed: YouTubePodcastFeed, val savedAtMs: Long)

data class YouTubePodcastShow(
    val item: Music,
    val episodes: List<Music>,
)

/** YouTube Music's podcasts browse surfaces, independent of the music catalogue's episode filters. */
object YouTubePodcasts {
    private const val CACHE_NAME = "youtube_podcast_feed"
    private const val CACHE_KEY = "feed"
    private const val PODCASTS_HOME = "FEmusic_home"
    private const val PODCASTS_CHIP_PARAMS = "ggMGSgQIDBAD"
    private const val PODCASTS_LANDING = "FEmusic_non_music_audio"
    private const val PODCASTS_SEARCH_PARAMS = "EgWKAQJQAWoKEAkQChAFEAMQBA=="
    private const val EPISODES_SEARCH_PARAMS = "EgWKAQJIAWoKEAkQChAFEAMQBA=="
    private const val PODCAST_SHOW_PREFIX = "MPSP"

    fun cached(context: Context, listener: String, region: String): CachedYouTubePodcastFeed? {
        val raw = context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE).getString(CACHE_KEY, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optString("listener") != listener || root.optString("region") != region) return@runCatching null
            val feed = decodeFeed(root.optJSONObject("feed") ?: return@runCatching null) ?: return@runCatching null
            CachedYouTubePodcastFeed(feed, root.optLong("savedAtMs"))
        }.getOrNull()
    }

    fun saveCache(context: Context, listener: String, region: String, feed: YouTubePodcastFeed, savedAtMs: Long) {
        val root = JSONObject()
            .put("listener", listener)
            .put("region", region)
            .put("savedAtMs", savedAtMs)
            .put("feed", encodeFeed(feed))
        context.getSharedPreferences(CACHE_NAME, Context.MODE_PRIVATE).edit().putString(CACHE_KEY, root.toString()).apply()
    }

    private fun encodeFeed(feed: YouTubePodcastFeed): JSONObject = JSONObject()
        .put("continuation", feed.continuation.orEmpty())
        .put("shelves", JSONArray().apply {
            feed.shelves.forEach { shelf ->
                put(JSONObject()
                    .put("title", shelf.title)
                    .put("episodes", shelf.episodes)
                    .put("items", JSONArray().apply { shelf.items.forEach { put(encodeItem(it)) } }))
            }
        })

    private fun decodeFeed(value: JSONObject): YouTubePodcastFeed? {
        val shelves = value.optJSONArray("shelves") ?: return null
        val decoded = (0 until shelves.length()).mapNotNull { index ->
            val shelf = shelves.optJSONObject(index) ?: return@mapNotNull null
            val items = shelf.optJSONArray("items") ?: JSONArray()
            val parsedItems = (0 until items.length()).mapNotNull { itemIndex -> decodeItem(items.optJSONObject(itemIndex)) }
            parsedItems.takeIf { it.isNotEmpty() }?.let {
                YouTubePodcastShelf(shelf.optString("title"), it, shelf.optBoolean("episodes"))
            }
        }
        if (decoded.isEmpty()) return null
        return YouTubePodcastFeed(decoded, value.optString("continuation").takeIf(String::isNotBlank))
    }

    private fun encodeItem(item: Music) = JSONObject()
        .put("id", item.id).put("title", item.title).put("subtitle", item.subtitle)
        .put("kind", item.kind.name).put("imageUrl", item.imageUrl.orEmpty())
        .put("durationMs", item.durationMs).put("note", item.note)

    private fun decodeItem(value: JSONObject?): Music? {
        if (value == null) return null
        val kind = MusicKind.entries.firstOrNull { it.name == value.optString("kind") } ?: return null
        val id = value.optString("id").takeIf(String::isNotBlank) ?: return null
        val title = value.optString("title").takeIf(String::isNotBlank) ?: return null
        return Music(
            id = id,
            title = title,
            subtitle = value.optString("subtitle"),
            kind = kind,
            imageUrl = value.optString("imageUrl").takeIf(String::isNotBlank),
            durationMs = value.optInt("durationMs"),
            note = value.optString("note"),
        )
    }

    fun home(): YouTubePodcastFeed {
        val home = runCatching {
            request(
                "browse",
                JSONObject().put("browseId", PODCASTS_HOME).put("params", PODCASTS_CHIP_PARAMS),
            )
        }
        val landing = runCatching {
            request("browse", JSONObject().put("browseId", PODCASTS_LANDING))
        }
        val shelves = mergeShelves(home.getOrNull()?.let(::parseShelves).orEmpty() + landing.getOrNull()?.let(::parseShelves).orEmpty())
        if (shelves.isEmpty()) throw home.exceptionOrNull() ?: landing.exceptionOrNull()
            ?: YouTubeFailure("YouTube Music has no podcast shelves right now.")
        return YouTubePodcastFeed(shelves, home.getOrNull()?.let { findContinuation(it, preferShelf = false) })
    }

    fun more(continuation: String): YouTubePodcastFeed {
        val response = request("browse", JSONObject().put("continuation", continuation))
        return YouTubePodcastFeed(parseShelves(response), findContinuation(response, preferShelf = false))
    }

    fun search(query: String): List<YouTubePodcastShelf> {
        val showResponse = request(
            "search",
            JSONObject().put("query", query).put("params", PODCASTS_SEARCH_PARAMS),
        )
        var fallback: JSONObject? = null
        var shows = collectShows(showResponse)
        if (shows.isEmpty()) {
            fallback = runCatching { request("search", JSONObject().put("query", query)) }.getOrNull()
            shows = fallback?.let(::collectShows).orEmpty()
        }

        val episodeResponse = runCatching {
            request("search", JSONObject().put("query", query).put("params", EPISODES_SEARCH_PARAMS))
        }.getOrNull()
        var episodes = episodeResponse?.let(::collectEpisodes).orEmpty()
        if (episodes.isEmpty()) {
            val allResults = fallback ?: runCatching { request("search", JSONObject().put("query", query)) }.getOrNull()
            episodes = allResults?.let(::collectEpisodes).orEmpty()
        }
        return listOfNotNull(
            shows.distinctBy(Music::id).takeIf { it.isNotEmpty() }?.let { YouTubePodcastShelf("Shows", it, episodes = false) },
            episodes.distinctBy(Music::id).takeIf { it.isNotEmpty() }?.let { YouTubePodcastShelf("Episodes", it, episodes = true) },
        )
    }

    fun show(browseId: String): YouTubePodcastShow {
        val id = browseId.removePrefix(YTM_PODCAST_PREFIX)
        val response = request("browse", JSONObject().put("browseId", id))
        val title = playlistTitle(response) ?: throw YouTubeFailure("This podcast is unavailable.")
        val image = playlistImage(response)?.let { largestYouTubeImage(it, "") }
        val description = showDescription(response)
        val show = Music(YTM_PODCAST_PREFIX + id, title, description, MusicKind.Playlist, imageUrl = image)
        return YouTubePodcastShow(show, collectEpisodes(response, title))
    }

    private fun parseShelves(response: JSONObject): List<YouTubePodcastShelf> {
        val shelves = ArrayList<YouTubePodcastShelf>()
        walkJson(response) { node ->
            val carousel = node.optJSONObject("musicCarouselShelfRenderer") ?: return@walkJson
            val header = carousel.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            val title = text(header?.optJSONObject("title")) ?: return@walkJson
            val contents = carousel.optJSONArray("contents") ?: JSONArray()
            val episodes = (0 until contents.length()).mapNotNull { index ->
                contents.optJSONObject(index)?.optJSONObject("musicMultiRowListItemRenderer")?.let(::parseMultiRowEpisode)
            }
            val shows = (0 until contents.length()).mapNotNull { index ->
                contents.optJSONObject(index)?.optJSONObject("musicTwoRowItemRenderer")?.let(::parseShowCard)
            }
            val items = if (episodes.isNotEmpty()) episodes else shows
            if (items.isNotEmpty()) shelves += YouTubePodcastShelf(title, items.distinctBy(Music::id), episodes.isNotEmpty())
        }
        return shelves
    }

    internal fun mergeShelves(shelves: List<YouTubePodcastShelf>): List<YouTubePodcastShelf> {
        val merged = LinkedHashMap<Pair<String, Boolean>, YouTubePodcastShelf>()
        shelves.forEach { shelf ->
            val key = shelf.title.trim().lowercase() to shelf.episodes
            val old = merged[key]
            merged[key] = if (old == null) shelf else old.copy(items = (old.items + shelf.items).distinctBy(Music::id))
        }
        return merged.values.toList()
    }

    private fun collectShows(response: JSONObject): List<Music> {
        val items = LinkedHashMap<String, Music>()
        walkJson(response) { node ->
            val card = node.optJSONObject("musicTwoRowItemRenderer")
                ?: node.optJSONObject("musicResponsiveListItemRenderer")
                ?: node.optJSONObject("musicCardShelfRenderer")
                ?: return@walkJson
            parseShowCard(card)?.let { items.putIfAbsent(it.id, it) }
        }
        return items.values.toList()
    }

    private fun collectEpisodes(response: JSONObject, fallbackShow: String? = null): List<Music> {
        val items = LinkedHashMap<String, Music>()
        walkJson(response) { node ->
            node.optJSONObject("musicMultiRowListItemRenderer")?.let(::parseMultiRowEpisode)
                ?.let { episode -> if (episode.subtitle.isBlank()) episode.copy(subtitle = fallbackShow.orEmpty()) else episode }
                ?.let { items.putIfAbsent(it.id, it) }
            node.optJSONObject("musicResponsiveListItemRenderer")?.let { parseResponsiveEpisode(it, fallbackShow) }
                ?.let { items.putIfAbsent(it.id, it) }
        }
        return items.values.toList()
    }

    private fun parseShowCard(renderer: JSONObject): Music? {
        val titleHolder = renderer.optJSONObject("title")
            ?: renderer.optJSONArray("flexColumns")?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
            ?: renderer.optJSONObject("header")?.optJSONObject("musicCardShelfHeaderBasicRenderer")?.optJSONObject("title")
        val titleRun = titleHolder?.optJSONArray("runs")?.optJSONObject(0)
        val browse = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            ?: titleRun?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
        val browseId = browse?.optString("browseId")?.takeIf { it.startsWith(PODCAST_SHOW_PREFIX) } ?: return null
        val title = text(titleHolder) ?: return null
        val subtitleHolder = renderer.optJSONObject("subtitle")
            ?: renderer.optJSONArray("flexColumns")?.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
            ?: renderer.optJSONObject("subtitle")
        val subtitle = text(subtitleHolder).orEmpty()
        val image = thumbnail(renderer.optJSONObject("thumbnailRenderer") ?: renderer.optJSONObject("thumbnail"))
            ?.let { largestYouTubeImage(it, "") }
        return Music(YTM_PODCAST_PREFIX + browseId, title, subtitle, MusicKind.Playlist, imageUrl = image)
    }

    private fun request(endpoint: String, body: JSONObject): JSONObject =
        YouTubeLibrary.podcastRequest(endpoint, body)

    private fun parseMultiRowEpisode(renderer: JSONObject): Music? {
        val videoId = renderer.optJSONObject("onTap")?.optJSONObject("watchEndpoint")?.optString("videoId")
            ?.takeIf(String::isNotBlank)
            ?: renderer.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")
                ?.takeIf(String::isNotBlank)
            ?: return null
        val title = text(renderer.optJSONObject("title")) ?: return null
        val show = text(renderer.optJSONObject("secondTitle")).orEmpty()
        val progress = renderer.optJSONObject("playbackProgress")?.optJSONObject("musicPlaybackProgressRenderer")
        val duration = text(progress?.optJSONObject("durationText"))
        val image = thumbnail(renderer.optJSONObject("thumbnail"))
        return Music(
            id = YTM_TRACK_PREFIX + videoId,
            title = title,
            subtitle = show,
            kind = MusicKind.Track,
            imageUrl = largestYouTubeImage(image, videoId),
            durationMs = duration?.takeIf { it.matches(Regex("^\\d{1,2}(:\\d{2}){1,2}$")) }?.let(::durationMs) ?: 0,
            note = duration.orEmpty(),
        )
    }

    private fun parseResponsiveEpisode(renderer: JSONObject, fallbackShow: String?): Music? {
        val flex = renderer.optJSONArray("flexColumns") ?: return null
        val titleRun = flex.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0) ?: return null
        val videoId = titleRun.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
            ?.optString("videoId")?.takeIf(String::isNotBlank)
            ?: renderer.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf(String::isNotBlank)
            ?: return null
        val type = text(flex.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text"))
        val endpointType = titleRun.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
            ?.optJSONObject("watchEndpointMusicSupportedConfigs")?.optJSONObject("watchEndpointMusicConfig")
            ?.optString("musicVideoType").orEmpty()
        if (type?.contains("episode", ignoreCase = true) != true && !endpointType.contains("PODCAST", ignoreCase = true)) return null
        val title = titleRun.optString("text").takeIf(String::isNotBlank) ?: return null
        val subtitle = flex.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")?.let { runs ->
                (0 until runs.length()).mapNotNull { runs.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }
                    .filterNot { it.equals("Episode", true) || it == "•" }.joinToString(" · ")
            }.orEmpty().ifBlank { fallbackShow.orEmpty() }
        val image = thumbnail(renderer.optJSONObject("thumbnail"))
        val duration = renderer.optJSONArray("fixedColumns")?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")?.optJSONObject("text")?.let(::text)
        return Music(
            id = YTM_TRACK_PREFIX + videoId,
            title = title,
            subtitle = subtitle,
            kind = MusicKind.Track,
            imageUrl = largestYouTubeImage(image, videoId),
            durationMs = duration?.takeIf { it.matches(Regex("^\\d{1,2}(:\\d{2}){1,2}$")) }?.let(::durationMs) ?: 0,
            note = duration.orEmpty(),
        )
    }

    private fun text(holder: JSONObject?): String? {
        if (holder == null) return null
        val runs = holder.optJSONArray("runs")
        val value = runs?.let { array ->
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("text") }
                .joinToString("")
        }?.takeIf(String::isNotBlank) ?: holder.optString("simpleText").takeIf(String::isNotBlank)
        return value?.trim()
    }

    private fun thumbnail(holder: JSONObject?): String? {
        val thumbnails = holder?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: holder?.optJSONObject("croppedSquareThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: holder?.optJSONObject("musicThumbnail")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        return thumbnails?.optJSONObject((thumbnails.length() - 1).coerceAtLeast(0))?.optString("url")
            ?.takeIf(String::isNotBlank)
    }

    private fun showDescription(response: JSONObject): String {
        var description = ""
        walkJson(response) { node ->
            if (description.isNotBlank()) return@walkJson
            description = text(node.optJSONObject("musicResponsiveHeaderRenderer")?.optJSONObject("description"))
                .orEmpty()
        }
        return description
    }
}
