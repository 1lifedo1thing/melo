package `in`.melo.data

import org.json.JSONArray
import org.json.JSONObject

/** The signed-in Spotify listener's small profile snapshot used by Melo's account UI. */
data class SpotifyUserProfile(
    val username: String,
    val displayName: String? = null,
    val imageUrl: String? = null,
)

/** Reads only the current listener's public-facing profile through Spotify's native spclient. */
internal suspend fun spotifyCurrentUserProfile(
    transport: SpotifyTransport,
    session: SpotifySession,
): SpotifyUserProfile {
    val username = session.username()
    val url = spotifyUrl("user-profile-view/v3/profile").newBuilder()
        .addPathSegment(username)
        .addQueryParameter("playlist_limit", "0")
        .addQueryParameter("artist_limit", "0")
        .build()
    val response = JSONObject(
        authenticatedSpotifyRequest(transport, session, url, "Spotify account profile", contentType = "application/json")
            .decodeToString(),
    )
    val json = response.optJSONObject("profile") ?: response
    val displayName = sequenceOf("display_name", "displayName", "name")
        .mapNotNull { json.optString(it).takeIf(String::isNotBlank) }
        .firstOrNull()
    val imageUrl = sequenceOf("image_url", "imageUrl", "avatar_url", "picture")
        .mapNotNull { profileImage(json.opt(it)) }
        .firstOrNull()
        ?: profileImages(json.optJSONArray("images"))
    return SpotifyUserProfile(username, displayName, imageUrl)
}

internal object SpotifyUserProfileCodec {
    fun encode(profile: SpotifyUserProfile): String = JSONObject()
        .put("username", profile.username)
        .put("displayName", profile.displayName)
        .put("imageUrl", profile.imageUrl)
        .toString()

    fun decode(encoded: String?): SpotifyUserProfile? = runCatching {
        val json = JSONObject(encoded ?: return null)
        val username = json.optString("username").takeIf(String::isNotBlank) ?: return null
        SpotifyUserProfile(
            username = username,
            displayName = json.optString("displayName").takeIf(String::isNotBlank),
            imageUrl = spotifyArtwork(json.optString("imageUrl").takeIf(String::isNotBlank)),
        )
    }.getOrNull()
}

private fun profileImage(value: Any?): String? = when (value) {
    is String -> spotifyArtwork(value)
    is JSONObject -> sequenceOf("url", "uri", "image_url")
        .mapNotNull { value.optString(it).takeIf(String::isNotBlank)?.let(::spotifyArtwork) }
        .firstOrNull()
    else -> null
}

private fun profileImages(images: JSONArray?): String? = images?.let { values ->
    (0 until values.length()).asSequence().mapNotNull { profileImage(values.opt(it)) }.firstOrNull()
}
