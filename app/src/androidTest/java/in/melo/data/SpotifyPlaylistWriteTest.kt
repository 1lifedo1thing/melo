package `in`.melo.data

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class SpotifyPlaylistWriteTest {
    @Test fun lifecycle(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("spotifyPlaylistWrite") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SpotifyAccount.require(app)
        val catalog = SpotifyCatalog(SpotifyAccount.transport, session)
        val library = SpotifyLibrary(SpotifyAccount.transport, session, catalog).load()
        val requested = InstrumentationRegistry.getArguments().getString("spotifyTrackUris")?.split(',')
        val tracks = (if (requested != null) catalog.hydrate(requested) else library.savedTracks).take(2)
        assertEquals(2, tracks.size)
        val service = SpotifyPlaylists(app)
        // Clean remnants of interrupted earlier runs of this explicitly named test.
        library.playlists.filter { it.title in setOf("Melo — temporary test", "Melo — renamed test") }.forEach {
            service.setPublished(it.id, false); service.unfollow(it.id)
        }
        var playlist: Music? = null
        try {
            playlist = service.create("Melo — temporary test", tracks.take(1))
            Log.i("MeloPlaylistTest", "created playlist")
            service.rename(playlist.id, "Melo — renamed test")
            service.add(playlist.id, tracks.drop(1))
            Log.i("MeloPlaylistTest", "renamed and added")
            // A duplicate, then removal of that second copy only: the first copy keeps its place.
            service.add(playlist.id, tracks.take(1))
            service.remove(playlist.id, tracks[0].id, occurrence = 1)
            Log.i("MeloPlaylistTest", "duplicate added and removed by row")
            assertTrue(service.isOwned(playlist.id))
            service.setPublished(playlist.id, true)
            assertTrue(service.published(playlist.id))
            Log.i("MeloPlaylistTest", "published on server")
            service.setPublished(playlist.id, false)
            assertFalse(service.published(playlist.id))
            Log.i("MeloPlaylistTest", "unpublished on server")
            val image = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(75, 95, 155)) }
            val bytes = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
            image.recycle()
            service.setCover(playlist.id, bytes)
            Log.i("MeloPlaylistTest", "cover uploaded")
            withTimeout(45_000) {
                while (true) {
                    val remote = SpotifyCatalog(SpotifyAccount.transport, session).detail(playlist)
                    if (remote.item.title == "Melo — renamed test" && remote.tracks.map(Music::id) == tracks.map(Music::id) && remote.item.imageUrl != null) break
                    delay(1500)
                }
            }
            assertTrue(service.recommendations(playlist.id, tracks, emptyList()).isNotEmpty())
            Log.i("MeloPlaylistTest", "remote content and suggestions verified")
        } finally {
            playlist?.let { service.setPublished(it.id, false); service.unfollow(it.id) }
            val cache = CatalogStore(app)
            cache.saveLibrary(SpotifyLibrary(SpotifyAccount.transport, session, catalog).load(cache.loadLibrary()))
            service.close()
            Log.i("MeloPlaylistTest", "test playlist removed and library cache refreshed")
        }
    }

    /** A local file added from Melo is stored as Spotify's own `spotify:local:` reference and read back as such. */
    @Test fun localFileReference(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("spotifyPlaylistWrite") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val session = SpotifyAccount.require(app)
        val catalog = SpotifyCatalog(SpotifyAccount.transport, session)
        val library = SpotifyLibrary(SpotifyAccount.transport, session, catalog).load()
        val track = library.savedTracks.first()
        val file = Music("${LOCAL_TRACK_PREFIX}1", "Melo test: file", "Melo & Co", MusicKind.Track, album = "Tests", durationMs = 123_400)
        val service = SpotifyPlaylists(app)
        library.playlists.filter { it.title == "Melo — local file test" }.forEach { service.unfollow(it.id) }
        var playlist: Music? = null
        try {
            playlist = service.create("Melo — local file test", listOf(track))
            service.add(playlist.id, listOf(file))
            val expected = "spotify:local:Melo+%26+Co:Tests:Melo+test%3A+file:123"
            withTimeout(45_000) {
                while (true) {
                    val remote = SpotifyCatalog(SpotifyAccount.transport, session).detail(playlist)
                    if (remote.tracks.map(Music::id) == listOf(track.id, expected)) {
                        val local = remote.tracks.last()
                        assertEquals("Melo test: file", local.title)
                        assertEquals("Melo & Co", local.subtitle)
                        assertEquals(123_000, local.durationMs)
                        break
                    }
                    delay(1500)
                }
            }
            Log.i("MeloPlaylistTest", "local file reference stored and read back")
        } finally {
            playlist?.let { service.unfollow(it.id) }
            val cache = CatalogStore(app)
            cache.saveLibrary(SpotifyLibrary(SpotifyAccount.transport, session, catalog).load(cache.loadLibrary()))
            service.close()
        }
    }
}
