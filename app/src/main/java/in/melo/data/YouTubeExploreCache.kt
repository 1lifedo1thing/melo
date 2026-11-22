package `in`.melo.data

import org.json.JSONArray
import org.json.JSONObject

/** Small, region-scoped cache for the YouTube Music Explore category grid. */
internal data class YouTubeExploreCache(
    val region: String,
    val savedAtMs: Long,
    val sections: List<YouTubeExploreSection>,
)

internal object YouTubeExploreCacheCodec {
    fun encode(cache: YouTubeExploreCache): String = JSONObject()
        .put("schema", 1)
        .put("region", cache.region)
        .put("savedAtMs", cache.savedAtMs)
        .put("sections", JSONArray().apply {
            cache.sections.forEach { section ->
                put(JSONObject().put("title", section.title).put("categories", JSONArray().apply {
                    section.categories.forEach { category ->
                        put(JSONObject().apply {
                            put("title", category.title)
                            put("browseId", category.browseId)
                            category.params?.let { put("params", it) }
                            category.stripeColor?.let { put("stripeColor", it) }
                            category.imageUrl?.let { put("imageUrl", it) }
                        })
                    }
                }))
            }
        })
        .toString()

    fun decode(encoded: String?): YouTubeExploreCache? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val root = JSONObject(encoded)
            if (root.optInt("schema") != 1) return null
            val sectionsJson = root.getJSONArray("sections")
            val sections = (0 until sectionsJson.length()).mapNotNull { sectionIndex ->
                val sectionJson = sectionsJson.optJSONObject(sectionIndex) ?: return@mapNotNull null
                val categoriesJson = sectionJson.optJSONArray("categories") ?: JSONArray()
                val categories = (0 until categoriesJson.length()).mapNotNull { categoryIndex ->
                    val categoryJson = categoriesJson.optJSONObject(categoryIndex) ?: return@mapNotNull null
                    val browseId = categoryJson.optString("browseId").takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    YouTubeExploreCategory(
                        title = categoryJson.optString("title"),
                        browseId = browseId,
                        params = categoryJson.optString("params").takeIf(String::isNotBlank),
                        stripeColor = categoryJson.takeIf { it.has("stripeColor") }?.optLong("stripeColor"),
                        imageUrl = categoryJson.optString("imageUrl").takeIf(String::isNotBlank),
                    )
                }
                YouTubeExploreSection(sectionJson.optString("title"), categories).takeIf { categories.isNotEmpty() }
            }
            YouTubeExploreCache(
                region = root.optString("region"),
                savedAtMs = root.optLong("savedAtMs"),
                sections = sections,
            ).takeIf { it.region.isNotBlank() && it.savedAtMs > 0 && it.sections.isNotEmpty() }
        }.getOrNull()
    }
}
