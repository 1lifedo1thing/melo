package `in`.melo.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GitHubReleaseTest {
    @Test fun comparesVersionsNumericallyAndPadsMissingComponents() {
        assertTrue(Version.parse("v1.10.0")!! > Version.parse("1.9.9")!!)
        assertTrue(Version.parse("v2")!! > Version.parse("1.99.99")!!)
        assertEquals(0, Version.parse("1.2.0")!!.compareTo(Version.parse("1.2")!!))
        assertTrue(Version.parse("1.2.2")!! < Version.parse("1.2.3")!!)
        listOf("", "v", "1.2-beta", "1..2", "-1.2", "1.2.999999999999999999999999").forEach {
            assertNull(it, Version.parse(it))
        }
    }

    @Test fun selectsReleaseApkInsteadOfChecksumsAndRendersNotes() {
        val apk = asset("Melo-v1.3.0.apk").put("digest", "sha256:" + "ab".repeat(32))
        val release = parse(release(apk, asset("Melo-v1.3.0.apk.sha256")).toString())!!
        assertEquals("Melo-v1.3.0.apk", release.apk!!.name)
        assertEquals("ab".repeat(32), release.apk!!.sha256)
        assertEquals("## Added\n- Updates", release.notes)
    }

    @Test fun excludesDraftsPrereleasesAndInvalidVersions() {
        assertNull(parse(release().put("draft", true).toString()))
        assertNull(parse(release().put("prerelease", true).toString()))
        assertNull(parse(release().put("tag_name", "v1.3.0-rc1").toString()))
    }

    @Test fun neverDownloadsFromAnotherRepositoryOrUntrustedUrl() {
        listOf(
            "http://github.com/aarush-io/Melo/releases/download/v1.3.0/app.apk",
            "https://github.com.evil.test/aarush-io/Melo/releases/download/v1.3.0/app.apk",
            "https://github.com/attacker/Melo/releases/download/v1.3.0/app.apk",
            "https://github.com/aarush-io/Melo-other/releases/download/v1.3.0/app.apk",
            "https://github.com:444/aarush-io/Melo/releases/download/v1.3.0/app.apk",
        ).forEach { url ->
            assertNull(url, parse(release(asset("app.apk").put("browser_download_url", url)).toString())!!.apk)
        }
    }

    @Test fun rejectsBadDigestsAndDoesNotGuessBetweenAlternativeBuilds() {
        assertNull(parse(release(asset("app.apk").put("digest", "sha256:abc")).toString())!!.apk)
        assertNull(parse(release(asset("arm.apk"), asset("x86.apk")).toString())!!.apk)
        assertEquals("Melo-v1.3.0.apk", parse(release(asset("arm.apk"), asset("Melo-v1.3.0.apk")).toString())!!.apk!!.name)
        assertNull(parse(release(asset("app.apk").put("state", "new")).toString())!!.apk)
    }

    @Test fun missingApkKeepsReleasePageAvailable() {
        val release = parse(release().put("html_url", "https://evil.test/").toString())!!
        assertNull(release.apk)
        assertEquals("https://github.com/aarush-io/Melo/releases/latest", release.page)
    }

    @Test fun unconfiguredBuildCannotReadUpstreamReleases() {
        assertNull(GitHubRelease.parse(release(asset("Melo-v1.3.0.apk")).toString(), ""))
    }

    @Test fun repositoryUrlComparisonIsCaseInsensitive() {
        val upstream = release(asset("Melo-v1.3.0.apk").put("browser_download_url",
            "https://github.com/aarush-io/melo/releases/download/v1.3.0/Melo-v1.3.0.apk"))
        assertEquals("Melo-v1.3.0.apk", parse(upstream.toString())!!.apk?.name)
    }

    private fun parse(json: String) = GitHubRelease.parse(json, "aarush-io/Melo")

    private fun release(vararg assets: JSONObject) = JSONObject().put("tag_name", "v1.3.0")
        .put("body", "## Added\n- Updates").put("draft", false).put("prerelease", false)
        .put("html_url", "https://github.com/aarush-io/Melo/releases/tag/v1.3.0")
        .put("assets", JSONArray(assets.toList()))

    private fun asset(name: String) = JSONObject().put("name", name).put("size", 12345)
        .put("browser_download_url", "https://github.com/aarush-io/Melo/releases/download/v1.3.0/$name")
}
