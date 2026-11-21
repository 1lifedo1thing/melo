package `in`.melo.data.youtube

import `in`.melo.data.ArtistProfile
import `in`.melo.data.CatalogDetail
import `in`.melo.data.HomeArtistRecommendations
import `in`.melo.data.HomeArtistRelease
import `in`.melo.data.HomeHistoryTrack
import `in`.melo.data.HomeMood
import `in`.melo.data.HomeMoodPlaylistShelf
import `in`.melo.data.YouTubeHomeChip
import `in`.melo.data.YouTubeHomePage
import `in`.melo.data.YouTubeHomeShelf
import `in`.melo.data.YouTubeExploreCategory
import `in`.melo.data.YouTubeExploreSection
import `in`.melo.data.YouTubeWatchTarget
import `in`.melo.data.Music
import `in`.melo.data.MusicKind
import `in`.melo.data.isPodcastSearchContainer
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.net.URLDecoder
import java.util.Locale

const val YTM_ALBUM_PREFIX = "ytm:album:"
const val YTM_ARTIST_PREFIX = "ytm:artist:"

/** The album and artists YouTube Music links to a track. */
data class YouTubeRelations(val album: Music?, val artists: List<Music>)

/**
 * Album and artist pages of YouTube Music (InnerTube `browse` with `MPREb_…` and `UC…` IDs), read
 * anonymously like the rest of the catalogue. Every call blocks: run it on an IO thread.
 */
object YouTubePages {
    private val relations = ConcurrentHashMap<String, YouTubeRelations>()
    private val artistSearchCache = ConcurrentHashMap<String, Music>()

    /** Kept from each search or browse row, so a track menu can open its album or artist at once. */
    internal fun remember(videoId: String, value: YouTubeRelations) {
        if (value.album != null || value.artists.isNotEmpty()) relations[videoId] = value
    }

    /** Relations parsed with a track row, without making another catalogue request. */
    internal fun rememberedRelations(videoId: String): YouTubeRelations? = relations[videoId]

    /** The category chips YouTube Music returns in the Home header, in its own order. */
    internal fun homeChips(response: JSONObject): List<YouTubeHomeChip> {
        val chips = response.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")?.optJSONObject("header")
            ?.optJSONObject("chipCloudRenderer")?.optJSONArray("chips")
            ?: return emptyList()
        return (0 until chips.length()).mapNotNull { index ->
            val chip = chips.optJSONObject(index)?.optJSONObject("chipCloudChipRenderer") ?: return@mapNotNull null
            val title = chip.optJSONObject("text")?.homeTextValue()
                ?.trim()?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
            val endpoint = chip.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                ?: return@mapNotNull null
            val params = endpoint.optString("params").takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (endpoint.optString("browseId") != "FEmusic_home" || isPodcastsCategory(params)) return@mapNotNull null
            YouTubeHomeChip(title, params)
        }.distinctBy(YouTubeHomeChip::params)
    }

    /** The first page of YouTube Music's personalized Home feed, with music-only shelves. */
    internal fun homePage(response: JSONObject): YouTubeHomePage {
        val sections = response.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
            ?: return YouTubeHomePage(browseShelves(response), findContinuation(response, preferShelf = false))
        val shelves = (0 until sections.length()).mapNotNull { index ->
            val section = sections.optJSONObject(index) ?: return@mapNotNull null
            section.optJSONObject("musicCarouselShelfRenderer")?.let(::homeCarouselShelf)
                ?: section.optJSONObject("musicShelfRenderer")?.let(::homePlainShelf)
        }.filter { it.items.isNotEmpty() }
        return YouTubeHomePage(shelves.musicOnly(), findContinuation(response, preferShelf = false))
    }

    /** The server-defined Explore grids, excluding the personalized shortcut grid that repeats them. */
    internal fun exploreCategories(response: JSONObject): List<YouTubeExploreSection> {
        val sections = response.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
            ?: return emptyList()
        val parsed = (0 until sections.length()).mapNotNull { index ->
            val grid = sections.optJSONObject(index)?.optJSONObject("gridRenderer") ?: return@mapNotNull null
            val title = grid.optJSONObject("header")?.optJSONObject("gridHeaderRenderer")
                ?.optJSONObject("title")?.homeTextValue().orEmpty()
            val rows = grid.optJSONArray("items") ?: return@mapNotNull null
            val categories = (0 until rows.length()).mapNotNull { rowIndex ->
                val button = rows.optJSONObject(rowIndex)?.optJSONObject("musicNavigationButtonRenderer")
                    ?: return@mapNotNull null
                val endpoint = button.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
                    ?: button.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                    ?: return@mapNotNull null
                val browseId = endpoint.optString("browseId").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val label = button.optJSONObject("buttonText")?.homeTextValue()
                    ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val colorText = button.optJSONObject("solid")?.optString("leftStripeColor").orEmpty()
                val stripeColor = colorText.toLongOrNull()
                    ?: colorText.removePrefix("#").removePrefix("0x").removePrefix("0X").toULongOrNull(16)?.toLong()
                YouTubeExploreCategory(
                    title = label,
                    browseId = browseId,
                    params = endpoint.optString("params").takeIf(String::isNotBlank),
                    stripeColor = stripeColor,
                )
            }.distinctBy(YouTubeExploreCategory::key)
            YouTubeExploreSection(title, categories).takeIf { categories.isNotEmpty() }
        }
        return parsed.filterIndexed { index, section ->
            !section.title.equals("For you", ignoreCase = true) &&
                section.categories.none { category ->
                    parsed.drop(index + 1).any { later -> later.categories.any { it.key == category.key } }
                }
        }
    }

    /** Music and playlist shelves on a category page, independent of the Home tab wrapper. */
    internal fun browseShelves(response: JSONObject): List<YouTubeHomeShelf> {
        val shelves = ArrayList<YouTubeHomeShelf>()
        walkJson(response) { node ->
            node.optJSONObject("musicCarouselShelfRenderer")?.let(::homeCarouselShelf)?.let(shelves::add)
            node.optJSONObject("musicShelfRenderer")?.let(::homePlainShelf)?.let(shelves::add)
        }
        return shelves.distinctBy { it.title.lowercase(Locale.ROOT) }
    }

    /** A category feed's later page, whose continuation envelope omits the Home tab wrapper. */
    internal fun homeContinuationPage(response: JSONObject): YouTubeHomePage {
        val shelves = ArrayList<YouTubeHomeShelf>()
        walkJson(response) { node ->
            node.optJSONObject("musicCarouselShelfRenderer")?.let(::homeCarouselShelf)?.let(shelves::add)
            node.optJSONObject("musicShelfRenderer")?.let(::homePlainShelf)?.let(shelves::add)
        }
        return YouTubeHomePage(shelves.musicOnly(), findContinuation(response, preferShelf = false))
    }

    private fun homeCarouselShelf(renderer: JSONObject): YouTubeHomeShelf? {
        val header = renderer.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
        val title = header?.optJSONObject("title")?.homeTextValue()
            ?.takeIf(String::isNotBlank) ?: return null
        val contents = renderer.optJSONArray("contents") ?: return null
        val items = (0 until contents.length()).mapNotNull { index ->
            val row = contents.optJSONObject(index) ?: return@mapNotNull null
            row.optJSONObject("musicTwoRowItemRenderer")?.let(::homeTwoRowItem)
                ?: row.optJSONObject("musicResponsiveListItemRenderer")?.let { track ->
                    if (isPodcastRow(track)) null else parseTrackRow(track)
                }
        }
        return YouTubeHomeShelf(title, items.distinctBy(Music::id)).takeIf { it.items.isNotEmpty() }
    }

    private fun homePlainShelf(renderer: JSONObject): YouTubeHomeShelf? {
        val title = renderer.optJSONObject("title")?.homeTextValue()
            ?.takeIf(String::isNotBlank) ?: return null
        val contents = renderer.optJSONArray("contents") ?: return null
        val items = (0 until contents.length()).mapNotNull { index ->
            val row = contents.optJSONObject(index)?.optJSONObject("musicResponsiveListItemRenderer") ?: return@mapNotNull null
            if (isPodcastRow(row)) null else parseTrackRow(row)
        }
        return YouTubeHomeShelf(title, items.distinctBy(Music::id)).takeIf { it.items.isNotEmpty() }
    }

    private fun homeTwoRowItem(renderer: JSONObject): Music? {
        val endpoint = renderer.optJSONObject("navigationEndpoint")
        val browseId = endpoint?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
        if (browseId.startsWith("MPSP") || browseId.startsWith("MPED") || isPodcastTwoRowItem(renderer)) return null
        val item = twoRowItem(renderer, "")
        if (item != null) return item
        val title = renderer.optJSONObject("title")?.runs()?.firstText() ?: return null
        val resolvedBrowseId = browseId.takeIf(String::isNotBlank)
        val videoId = endpoint?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf(String::isNotBlank)
            ?: resolvedBrowseId?.takeIf { it.startsWith("MPED") }?.removePrefix("MPED")
        if (videoId != null) {
            val subtitle = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.texts().orEmpty()
                .filterNot { it == " • " }.joinToString(" · ")
            val image = renderer.optJSONObject("thumbnailRenderer")?.let(::thumbnailOf)
            return Music(YTM_TRACK_PREFIX + videoId, title, subtitle, MusicKind.Track,
                imageUrl = largestYouTubeImage(image, videoId))
        }
        if (resolvedBrowseId == null) return null
        val image = renderer.optJSONObject("thumbnailRenderer")?.let(::thumbnailOf)
            ?.let { largestYouTubeImage(it, "") }
        return when {
            resolvedBrowseId.startsWith("UC") -> Music(YTM_ARTIST_PREFIX + resolvedBrowseId, title, "Artist", MusicKind.Artist, imageUrl = image)
            resolvedBrowseId.startsWith("MPRE") -> Music(YTM_ALBUM_PREFIX + resolvedBrowseId, title, "", MusicKind.Album, imageUrl = image)
            else -> {
                val subtitle = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.texts().orEmpty()
                    .filterNot { it == " • " }.joinToString(" · ")
                Music(YTM_PLAYLIST_PREFIX + resolvedBrowseId, title, subtitle, MusicKind.Playlist, imageUrl = image)
            }
        }
    }

    private fun isPodcastTwoRowItem(renderer: JSONObject): Boolean {
        var podcast = renderer.optString("musicVideoType").contains("PODCAST", true) ||
            renderer.optString("musicVideoType").contains("EPISODE", true)
        walkJson(renderer) { node ->
            val kind = node.optJSONObject("watchEndpointMusicSupportedConfigs")
                ?.optJSONObject("watchEndpointMusicConfig")?.optString("musicVideoType").orEmpty()
            if (kind.contains("PODCAST", true) || kind.contains("EPISODE", true)) podcast = true
        }
        val subtitle = renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.texts().orEmpty()
        return podcast || subtitle.any { it.equals("Podcast", true) || it.equals("Episode", true) }
    }

    internal fun isPodcastRow(renderer: JSONObject): Boolean {
        var podcast = renderer.optString("musicVideoType").contains("PODCAST", true) ||
            renderer.optString("musicVideoType").contains("EPISODE", true)
        walkJson(renderer) { node ->
            val kind = node.optJSONObject("watchEndpointMusicSupportedConfigs")
                ?.optJSONObject("watchEndpointMusicConfig")?.optString("musicVideoType").orEmpty()
            if (kind.contains("PODCAST", true) || kind.contains("EPISODE", true)) podcast = true
        }
        val columns = renderer.optJSONArray("flexColumns")
        val label = columns?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
        return podcast || label.equals("Podcast", true) || label.equals("Episode", true)
    }

    private fun List<YouTubeHomeShelf>.musicOnly(): List<YouTubeHomeShelf> = filterNot { shelf ->
        shelf.items.isEmpty() || shelf.items.all { item ->
            item.id.startsWith(YTM_PLAYLIST_PREFIX + "MPSP") || item.id.startsWith(YTM_TRACK_PREFIX) &&
                (item.subtitle.startsWith("Podcast", true) || item.subtitle.startsWith("Episode", true))
        }
    }.map { shelf ->
        shelf.copy(items = shelf.items.filterNot { item ->
            item.id.startsWith(YTM_PLAYLIST_PREFIX + "MPSP") || item.id.startsWith(YTM_TRACK_PREFIX) &&
                (item.subtitle.startsWith("Podcast", true) || item.subtitle.startsWith("Episode", true))
        })
    }

    private fun isPodcastsCategory(params: String): Boolean = "SgQIDBAD" in params

    private fun JSONObject?.homeTextValue(): String = this?.let { value ->
        value.optString("simpleText").takeIf(String::isNotBlank)
            ?: value.optJSONArray("runs")?.texts()?.joinToString("").orEmpty()
    }.orEmpty()

    /** History rows retain the linked channel and episode marker used for taste ranking. */
    internal fun recommendationHistoryRows(response: JSONObject): List<HomeHistoryTrack> {
        val rows = ArrayList<HomeHistoryTrack>()
        walkJson(response) { node ->
            val renderer = node.optJSONObject("musicResponsiveListItemRenderer") ?: return@walkJson
            val track = parseTrackRow(renderer) ?: return@walkJson
            val videoId = track.id.removePrefix(YTM_TRACK_PREFIX)
            val relations = rememberedRelations(videoId)
            val groups = renderer.optJSONArray("flexColumns")
            val label = groups?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")?.optJSONArray("runs")?.let { it.optJSONObject(0)?.optString("text") }.orEmpty()
            var endpointType = ""
            walkJson(renderer) { child ->
                child.optJSONObject("watchEndpointMusicSupportedConfigs")?.optJSONObject("watchEndpointMusicConfig")
                    ?.optString("musicVideoType")?.takeIf(String::isNotBlank)?.let { endpointType = it }
            }
            val rowType = renderer.optString("musicVideoType")
            val isEpisode = label.equals("Episode", true) || label.equals("Podcast", true) ||
                endpointType.contains("PODCAST", true) || endpointType.contains("EPISODE", true) ||
                rowType.contains("PODCAST", true) || rowType.contains("EPISODE", true)
            rows += HomeHistoryTrack(track, relations?.artists.orEmpty(), isEpisode)
        }
        return rows
    }

    /** Resolves a local Replay artist name to the matching YouTube artist channel, when available. */
    internal fun searchArtist(name: String): Music? {
        val key = artistKey(name)
        if (key.isBlank()) return null
        artistSearchCache[key]?.let { return it }
        val found = searchAll(name).asSequence().filter { it.kind == MusicKind.Artist }
            .map { it to artistKey(it.title) }
            .filter { (_, candidate) -> candidate == key || candidate.startsWith("${key}topic") || key.startsWith(candidate) }
            .maxByOrNull { (_, candidate) -> if (candidate == key) 2 else 1 }
            ?.first ?: return null
        artistSearchCache[key] = found
        return found
    }

    private fun artistKey(value: String): String = value.lowercase(java.util.Locale.ROOT)
        .replace(Regex("\\b(official|topic|vevo)\\b"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), "")

    /** From what search rows gave, then the `next` endpoint, then a search for the track itself. */
    fun relations(videoId: String, title: String, artist: String): YouTubeRelations? {
        relations[videoId]?.takeIf { it.album != null }?.let { return it }
        val next = runCatching { YouTubeMusic.browseRequest("next", JSONObject().put("videoId", videoId).put("isAudioOnly", true)) }.getOrNull()
        if (next != null) {
            var byline: JSONArray? = null
            walkJson(next) { node ->
                val panel = node.optJSONObject("playlistPanelVideoRenderer") ?: return@walkJson
                if (byline == null && panel.optString("videoId") == videoId) byline = panel.optJSONObject("longBylineText")?.optJSONArray("runs")
            }
            byline?.let(::relationsOf)?.let { remember(videoId, it) }
        }
        relations[videoId]?.takeIf { it.album != null }?.let { return it }
        if (title.isNotBlank()) runCatching { YouTubeMusic.searchSongs("$title $artist".trim()) }
        relations[videoId]?.let { return it }
        return relations[videoId]
    }

    /**
     * The unfiltered search: the top result card, then songs, videos, albums, artists and playlists
     * mixed in YouTube Music's own order. Podcast/show containers, explicitly marked episode rows
     * and user profiles are left out. Track titles and artist names are never used as filter terms.
     */
    fun searchAll(query: String): List<Music> {
        val response = YouTubeMusic.browseRequest("search", JSONObject().put("query", query))
        val found = ArrayList<Music>()
        var cardArtist = ""
        walkJson(response) { node ->
            node.optJSONObject("musicCardShelfRenderer")?.let { card ->
                val run = card.optJSONObject("title")?.runs()?.optJSONObject(0) ?: return@let
                val entity = entityOf(run, card.optJSONObject("subtitle")?.optJSONArray("runs")?.texts().orEmpty().filter { it != " • " },
                    thumbnailOf(card.optJSONObject("thumbnail")))
                if (entity != null && !entity.isPodcastSearchContainer()) { found += entity; if (entity.kind == MusicKind.Artist) cardArtist = entity.title }
            }
            val row = node.optJSONObject("musicResponsiveListItemRenderer") ?: return@walkJson
            searchRow(row, cardArtist)?.let(found::add)
        }
        return found.filterNot { it.isPodcastSearchContainer() }.distinctBy(Music::id)
    }

    private fun searchRow(row: JSONObject, cardArtist: String): Music? {
        parseTrackRow(row)?.let { track ->
            // Use YouTube's row media type, never words from the song title or artist.
            if (isPodcastRow(row)) return null
            return if (track.subtitle.isBlank()) track.copy(subtitle = cardArtist) else track
        }
        if (isPodcastRow(row)) return null
        val flex = row.optJSONArray("flexColumns") ?: return null
        val second = flex.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")?.texts().orEmpty().filter { it != " • " }
        val label = second.firstOrNull().orEmpty()
        if (label == "Episode" || label == "Podcast" || label == "Profile") return null
        val browse = row.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
        if (browse == null) return null
        val browseId = browse.optString("browseId")
        if (browseId.startsWith("MPSP") || browseId.startsWith("MPED")) return null
        val titleRun = flex.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0) ?: return null
        val image = row.optJSONObject("thumbnail")?.let(::thumbnailOf)
        return entityOf(JSONObject(titleRun.toString()).put("navigationEndpoint", JSONObject().put("browseEndpoint", browse)), second, image)
    }

    /** An album, artist or playlist link with its subtitle parts ("Album", "Daft Punk", "2013"). */
    private fun entityOf(run: JSONObject, subtitle: List<String>, image: String?): Music? {
        val title = run.optString("text").takeIf(String::isNotBlank) ?: return null
        val picture = image?.let { largestYouTubeImage(it, "") }
        run.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf(String::isNotBlank)?.let { videoId ->
            return Music(YTM_TRACK_PREFIX + videoId, title, subtitle.getOrNull(1).orEmpty(), MusicKind.Track,
                imageUrl = largestYouTubeImage(image, videoId))
        }
        val browse = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: return null
        val id = browse.optString("browseId").takeIf(String::isNotBlank) ?: return null
        val type = browse.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
        val details = subtitle.drop(1)
        return when (type) {
            "MUSIC_PAGE_TYPE_ALBUM" -> Music(YTM_ALBUM_PREFIX + id, title, details.firstOrNull().orEmpty(), MusicKind.Album, imageUrl = picture,
                note = details.lastOrNull { it.matches(Regex("\\d{4}")) }.orEmpty())
            "MUSIC_PAGE_TYPE_ARTIST" -> Music(YTM_ARTIST_PREFIX + id, title, "Artist", MusicKind.Artist, imageUrl = picture)
            "MUSIC_PAGE_TYPE_PLAYLIST" -> Music(YTM_PLAYLIST_PREFIX + id.removePrefix("VL"), title, details.firstOrNull().orEmpty(),
                MusicKind.Playlist, imageUrl = picture, ownerUsername = details.firstOrNull())
            else -> null
        }
    }

    fun album(browseId: String): CatalogDetail {
        val response = YouTubeMusic.browseRequest("browse", JSONObject().put("browseId", browseId))
        val header = responsiveHeader(response) ?: throw YouTubeFailure("This album is unavailable.")
        val title = header.optJSONObject("title")?.runs()?.firstText() ?: throw YouTubeFailure("This album is unavailable.")
        val artists = header.optJSONObject("straplineTextOne")?.optJSONArray("runs")?.let(::artistsOf).orEmpty()
        val artistNames = artists.joinToString(", ") { it.title }
        val year = header.optJSONObject("subtitle")?.optJSONArray("runs")?.texts()?.lastOrNull { it.matches(Regex("\\d{4}")) }.orEmpty()
        val image = thumbnail(header)?.let { largestYouTubeImage(it, "") }
        val album = Music(YTM_ALBUM_PREFIX + browseId, title, artistNames, MusicKind.Album, imageUrl = image, note = year)
        val tracks = parseTrackRows(response).map { track ->
            track.copy(subtitle = track.subtitle.ifBlank { artistNames }, album = title, imageUrl = image ?: track.imageUrl)
        }
        val albumMusic = album
        tracks.forEach { track -> track.youTubeVideoId?.let { remember(it, YouTubeRelations(albumMusic, artists)) } }
        return CatalogDetail(album.copy(tracks = tracks.map(Music::id)), tracks, artists)
    }

    fun artist(browseId: String): CatalogDetail {
        val response = YouTubeMusic.browseRequest("browse", JSONObject().put("browseId", browseId))
        var header: JSONObject? = null
        walkJson(response) { node ->
            if (header == null) header = node.optJSONObject("musicImmersiveHeaderRenderer") ?: node.optJSONObject("musicVisualHeaderRenderer")
        }
        val head = header ?: responsiveHeader(response) ?: throw YouTubeFailure("This artist is unavailable.")
        val name = head.optJSONObject("title")?.runs()?.firstText() ?: throw YouTubeFailure("This artist is unavailable.")
        val picture = thumbnail(head)
        val description = head.optJSONObject("description")?.optJSONArray("runs")?.texts()?.joinToString("").orEmpty()
        val audience = head.optJSONObject("monthlyListenerCount")?.optJSONArray("runs")?.firstText()
        val artist = Music(YTM_ARTIST_PREFIX + browseId, name, "Artist", MusicKind.Artist, imageUrl = picture, note = description)
        var songs = emptyList<Music>()
        val plays = HashMap<String, Long>()
        val albums = ArrayList<Music>()
        val singles = ArrayList<Music>()
        val similar = ArrayList<Music>()
        walkJson(response) { node ->
            node.optJSONObject("musicShelfRenderer")?.let { shelf ->
                if (songs.isNotEmpty()) return@let
                val rows = shelf.optJSONArray("contents") ?: return@let
                songs = (0 until rows.length()).mapNotNull { index ->
                    val row = rows.optJSONObject(index)?.optJSONObject("musicResponsiveListItemRenderer") ?: return@mapNotNull null
                    parseTrackRow(row)?.also { track -> playsOf(row)?.let { plays[track.id] = it } }
                }
            }
            node.optJSONObject("musicCarouselShelfRenderer")?.let { carousel ->
                val title = carousel.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("title")?.runs()?.firstText().orEmpty()
                val items = carousel.optJSONArray("contents") ?: return@let
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index)?.optJSONObject("musicTwoRowItemRenderer")?.let { twoRowItem(it, name) } ?: continue
                    when {
                        item.kind == MusicKind.Artist -> similar += item
                        item.kind == MusicKind.Album && title.contains("single", ignoreCase = true) -> singles += item
                        item.kind == MusicKind.Album -> albums += item
                    }
                }
            }
        }
        val releases = (albums + singles).distinctBy(Music::id)
        val profile = ArtistProfile(
            monthlyListeners = audience?.let(::abbreviatedCount), approximate = true,
            playCounts = plays, albums = albums.map(Music::id), singles = singles.map(Music::id),
            biography = description,
        )
        return CatalogDetail(artist, songs, releases + similar.distinctBy(Music::id), artist = profile)
    }

    /** One artist-page request gives Top Picks its releases, related artists, Mix and Shuffle queues. */
    internal fun artistRecommendations(browseId: String): HomeArtistRecommendations {
        val response = YouTubeMusic.browseRequest("browse", JSONObject().put("browseId", browseId))
        var hero: JSONObject? = null
        walkJson(response) { node ->
            if (hero == null) hero = node.optJSONObject("musicImmersiveHeaderRenderer")
                ?: node.optJSONObject("musicVisualHeaderRenderer")
        }
        val header = hero ?: responsiveHeader(response) ?: throw YouTubeFailure("This artist is unavailable.")
        val name = header.optJSONObject("title")?.runs()?.firstText()
            ?: throw YouTubeFailure("This artist is unavailable.")
        val image = thumbnail(header)
        val artist = Music(YTM_ARTIST_PREFIX + browseId, name, "Artist", MusicKind.Artist, imageUrl = image)
        val listeners = header.optJSONObject("monthlyListenerCount")?.let(::textValue)?.takeIf(String::isNotBlank)
            ?: header.optJSONObject("subscriberCountText")?.let(::textValue)
        val releases = ArrayList<HomeArtistRelease>()
        val similar = ArrayList<Music>()
        var similarShelfFound = false
        walkJson(response) { node ->
            val carousel = node.optJSONObject("musicCarouselShelfRenderer") ?: return@walkJson
            val shelfTitle = carousel.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?.optJSONObject("title")?.let(::textValue).orEmpty()
            val rows = carousel.optJSONArray("contents") ?: return@walkJson
            val items = (0 until rows.length()).mapNotNull { index ->
                rows.optJSONObject(index)?.optJSONObject("musicTwoRowItemRenderer")?.let { twoRowItem(it, name) }
            }
            if (!similarShelfFound && items.isNotEmpty() && items.all {
                    it.kind == MusicKind.Artist && it.id.removePrefix(YTM_ARTIST_PREFIX).startsWith("UC")
                }) {
                items.forEach(similar::add)
                similarShelfFound = true
            }
            val firstRelease = items.firstOrNull { it.kind == MusicKind.Album }
            if (firstRelease != null) {
                val source = (0 until rows.length()).asSequence().mapNotNull { index ->
                    rows.optJSONObject(index)?.optJSONObject("musicTwoRowItemRenderer")
                }.firstOrNull { twoRowItem(it, name)?.id == firstRelease.id }
                val rawSubtitle = source?.optJSONObject("subtitle")?.optJSONArray("runs")?.texts()
                    ?.filter { it != " • " }?.joinToString(" · ").orEmpty()
                val releaseText = if (rawSubtitle.isNotBlank()) rawSubtitle else firstRelease.subtitle
                releases += HomeArtistRelease(
                    item = firstRelease.copy(subtitle = releaseText,
                        note = Regex("\\b(19|20)\\d{2}\\b").findAll(releaseText).lastOrNull()?.value ?: firstRelease.note),
                    singleOrEp = Regex("\\b(single|ep)\\b", RegexOption.IGNORE_CASE).containsMatchIn("$shelfTitle $releaseText"),
                )
            }
        }
        return HomeArtistRecommendations(
            artist = artist,
            releases = releases,
            relatedArtists = similar.distinctBy(Music::id),
            monthlyListeners = listeners,
            mixTarget = headerWatchTarget(header, "startRadioButton"),
            shuffleTarget = headerWatchTarget(header, "playButton"),
        )
    }

    /** Playlist cards returned by YouTube Music's personalized mix shelf or a mood category. */
    internal fun recommendationPlaylists(response: JSONObject): List<Music> {
        val items = ArrayList<Music>()
        walkJson(response) { node ->
            node.optJSONObject("musicTwoRowItemRenderer")?.let { twoRowItem(it, "") }
                ?.takeIf { it.kind == MusicKind.Playlist && it.id.startsWith(YTM_PLAYLIST_PREFIX) }
                ?.let(items::add)
        }
        return items.distinctBy(Music::id)
    }

    /** The playlists behind a YouTube Music mood; this costs one public browse request per tap. */
    internal fun moodPlaylistShelves(mood: HomeMood): List<HomeMoodPlaylistShelf> {
        val response = YouTubeMusic.browseRequest("browse", JSONObject()
            .put("browseId", HomeMood.CATEGORY_BROWSE_ID)
            .put("params", mood.categoryParams))
        val shelves = ArrayList<HomeMoodPlaylistShelf>()
        walkJson(response) { node ->
            val carousel = node.optJSONObject("musicCarouselShelfRenderer") ?: return@walkJson
            val shelfTitle = carousel.optJSONObject("header")
                ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?.optJSONObject("title")?.let(::textValue).orEmpty()
            val rows = carousel.optJSONArray("contents") ?: return@walkJson
            val playlists = (0 until rows.length()).mapNotNull { index ->
                rows.optJSONObject(index)?.optJSONObject("musicTwoRowItemRenderer")?.let { twoRowItem(it, "") }
            }.filter { it.kind == MusicKind.Playlist && it.id.removePrefix(YTM_PLAYLIST_PREFIX).startsWith("VL") }
            if (playlists.isNotEmpty()) shelves += HomeMoodPlaylistShelf(shelfTitle, playlists)
        }
        return shelves
    }

    /** Compatibility for other browse consumers; recommendation queues need the shelf grouping. */
    internal fun moodPlaylists(mood: HomeMood): List<Music> = moodPlaylistShelves(mood)
        .flatMap(HomeMoodPlaylistShelf::playlists).distinctBy(Music::id)

    private fun headerWatchTarget(header: JSONObject, buttonName: String): YouTubeWatchTarget? {
        val button = header.optJSONObject(buttonName)?.optJSONObject("buttonRenderer") ?: return null
        val endpoint = button.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint") ?: return null
        val playlistId = endpoint.optString("playlistId").takeIf(String::isNotBlank) ?: return null
        val encodedParams = endpoint.optString("params").takeIf(String::isNotBlank)
        return YouTubeWatchTarget(
            playlistId = playlistId,
            videoId = endpoint.optString("videoId").takeIf(String::isNotBlank),
            params = encodedParams?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) },
        )
    }

    private fun textValue(json: JSONObject): String = json.optJSONArray("runs")?.texts()?.joinToString("")
        ?.takeIf(String::isNotBlank) ?: json.optString("simpleText").takeIf(String::isNotBlank).orEmpty()

    /** Track rows from YouTube Music's `next` response, used for Mixes and radio queues. */
    internal fun watchQueueRows(response: JSONObject, preferAudio: Boolean = false): List<Music> {
        val rows = LinkedHashMap<String, Music>()
        val replacements = HashMap<String, JSONObject>()
        val dropped = HashSet<String>()
        if (preferAudio) {
            walkJson(response) { node ->
                val wrapper = node.optJSONObject("playlistPanelVideoWrapperRenderer") ?: return@walkJson
                val halves = ArrayList<JSONObject>()
                wrapper.optJSONObject("primaryRenderer")?.optJSONObject("playlistPanelVideoRenderer")?.let(halves::add)
                wrapper.optJSONArray("counterpart")?.let { counterparts ->
                    for (index in 0 until counterparts.length()) {
                        counterparts.optJSONObject(index)?.optJSONObject("counterpartRenderer")
                            ?.optJSONObject("playlistPanelVideoRenderer")?.let(halves::add)
                    }
                }
                val primary = halves.firstOrNull() ?: return@walkJson
                val chosen = halves.firstOrNull(::isAudioTrack) ?: primary
                val primaryId = primary.optString("videoId").takeIf(String::isNotBlank) ?: return@walkJson
                if (chosen !== primary) replacements[primaryId] = chosen
                halves.filter { it !== primary }.mapNotNullTo(dropped) { it.optString("videoId").takeIf(String::isNotBlank) }
            }
        }
        walkJson(response) { node ->
            val renderer = node.optJSONObject("playlistPanelVideoRenderer") ?: return@walkJson
            val rowId = renderer.optString("videoId").takeIf(String::isNotBlank) ?: return@walkJson
            if (rowId in dropped) return@walkJson
            val selectedRenderer = replacements[rowId] ?: renderer
            val videoId = selectedRenderer.optString("videoId").takeIf(String::isNotBlank) ?: return@walkJson
            val type = selectedRenderer.optString("musicVideoType")
            val endpointType = selectedRenderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                ?.optJSONObject("watchEndpointMusicSupportedConfigs")?.optJSONObject("watchEndpointMusicConfig")
                ?.optString("musicVideoType").orEmpty()
            if (type.contains("PODCAST", ignoreCase = true) || type.contains("EPISODE", ignoreCase = true) ||
                endpointType.contains("PODCAST", true) || endpointType.contains("EPISODE", true)) return@walkJson
            val titleHolder = selectedRenderer.optJSONObject("title")
            val title = titleHolder?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?.takeIf(String::isNotBlank) ?: titleHolder?.optString("simpleText")?.takeIf(String::isNotBlank)
                ?: return@walkJson
            val byline = selectedRenderer.optJSONObject("longBylineText")?.optJSONArray("runs")
                ?: selectedRenderer.optJSONObject("shortBylineText")?.optJSONArray("runs")
            val artist = byline?.let { runs ->
                (0 until runs.length()).mapNotNull { runs.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }
                    .filterNot { it == "•" }.joinToString(" · ")
            }.orEmpty()
            val thumbs = selectedRenderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val image = thumbs?.optJSONObject((thumbs.length() - 1).coerceAtLeast(0))?.optString("url")
                ?.takeIf(String::isNotBlank)
            val duration = selectedRenderer.optJSONObject("lengthText")?.let { holder ->
                holder.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.takeIf(String::isNotBlank)
                    ?: holder.optString("simpleText").takeIf(String::isNotBlank)
            }?.let(::durationMs) ?: 0
            rows.putIfAbsent(videoId, Music(
                id = YTM_TRACK_PREFIX + videoId, title = title, subtitle = artist, kind = MusicKind.Track,
                imageUrl = largestYouTubeImage(image, videoId), durationMs = duration,
            ))
        }
        if (rows.isNotEmpty()) return rows.values.toList()
        return parseTrackRows(response).distinctBy(Music::id)
    }

    private fun isAudioTrack(renderer: JSONObject): Boolean =
        renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
            ?.optJSONObject("watchEndpointMusicSupportedConfigs")?.optJSONObject("watchEndpointMusicConfig")
            ?.optString("musicVideoType") == "MUSIC_VIDEO_TYPE_ATV"

    private fun twoRowItem(item: JSONObject, artistName: String): Music? {
        val title = item.optJSONObject("title")?.runs()?.firstText() ?: return null
        val browse = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: return null
        val id = browse.optString("browseId").takeIf(String::isNotBlank) ?: return null
        val type = browse.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
        val subtitle = item.optJSONObject("subtitle")?.optJSONArray("runs")?.texts().orEmpty().filter { it != " • " }
        val image = item.optJSONObject("thumbnailRenderer")?.let(::thumbnailOf)?.let { largestYouTubeImage(it, "") }
        return when (type) {
            "MUSIC_PAGE_TYPE_ALBUM" -> Music(YTM_ALBUM_PREFIX + id, title, artistName, MusicKind.Album, imageUrl = image,
                note = subtitle.lastOrNull { it.matches(Regex("\\d{4}")) }.orEmpty())
            "MUSIC_PAGE_TYPE_ARTIST" -> Music(YTM_ARTIST_PREFIX + id, title, "Artist", MusicKind.Artist, imageUrl = image)
            "MUSIC_PAGE_TYPE_PLAYLIST" -> Music(YTM_PLAYLIST_PREFIX + id, title, subtitle.joinToString(" · "), MusicKind.Playlist,
                imageUrl = image)
            else -> null
        }
    }

    private fun responsiveHeader(response: JSONObject): JSONObject? {
        var header: JSONObject? = null
        walkJson(response) { node -> if (header == null) header = node.optJSONObject("musicResponsiveHeaderRenderer") }
        return header
    }

    private fun thumbnail(header: JSONObject): String? = thumbnailOf(header.optJSONObject("thumbnail"))

    private fun thumbnailOf(holder: JSONObject?): String? {
        val list = holder?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: holder?.optJSONObject("croppedSquareThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: return null
        return list.optJSONObject(list.length() - 1)?.optString("url")?.takeIf(String::isNotBlank)
    }

    private fun playsOf(row: JSONObject): Long? {
        val flex = row.optJSONArray("flexColumns") ?: return null
        for (column in 0 until flex.length()) {
            val runs = flex.optJSONObject(column)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")?.optJSONArray("runs") ?: continue
            runs.texts().firstNotNullOfOrNull { PLAYS.matchEntire(it)?.groupValues?.get(1) }?.let { return abbreviatedCount(it) }
        }
        return null
    }

    /** Runs that link to artist pages, as albums and track bylines list them. */
    internal fun artistsOf(runs: JSONArray): List<Music> = (0 until runs.length()).mapNotNull { index ->
        val run = runs.optJSONObject(index) ?: return@mapNotNull null
        val browse = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: return@mapNotNull null
        val type = browse.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
        if (type != "MUSIC_PAGE_TYPE_ARTIST" && !browse.optString("browseId").startsWith("UC")) return@mapNotNull null
        Music(YTM_ARTIST_PREFIX + browse.optString("browseId"), run.optString("text"), "Artist", MusicKind.Artist)
    }

    internal fun relationsOf(runs: JSONArray): YouTubeRelations {
        var album: Music? = null
        for (index in 0 until runs.length()) {
            val run = runs.optJSONObject(index) ?: continue
            val browse = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: continue
            val type = browse.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
            if (type == "MUSIC_PAGE_TYPE_ALBUM") album = Music(YTM_ALBUM_PREFIX + browse.optString("browseId"), run.optString("text"), "", MusicKind.Album)
        }
        return YouTubeRelations(album, artistsOf(runs))
    }

    private fun JSONObject.runs(): JSONArray? = optJSONArray("runs")
    private fun JSONArray.firstText(): String? = optJSONObject(0)?.optString("text")?.takeIf(String::isNotBlank)
    private fun JSONArray.texts(): List<String> = (0 until length()).mapNotNull { optJSONObject(it)?.optString("text") }

    private val PLAYS = Regex("^([\\d.,]+[KMB]?) (?:plays|views)$")
}

/** "28.3M", "150K", "1.2B": the approximate value YouTube abbreviates. */
internal fun abbreviatedCount(text: String): Long? {
    val match = Regex("([\\d.,]+)\\s*([KMB])?").find(text.replace(",", "")) ?: return null
    val number = match.groupValues[1].toDoubleOrNull() ?: return null
    val scale = when (match.groupValues[2]) { "K" -> 1e3; "M" -> 1e6; "B" -> 1e9; else -> 1.0 }
    return (number * scale).toLong().takeIf { it > 0 }
}
