package `in`.melo.data

import android.app.Application
import android.content.Context
import `in`.melo.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Reports only aggregate active-playback presence while the user's analytics setting is enabled. */
object MeloTelemetry {
    private const val PREFS = "melo_telemetry"
    private const val INSTALL_ID = "install_id"
    private const val HEARTBEAT_MS = 5 * 60_000L
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var installationId = ""
    private var enabled = false
    private var playbackActive = false
    private var heartbeat: Job? = null
    private val inFlight = LinkedHashSet<Call>()

    fun initialize(application: Application, analyticsEnabled: Boolean) {
        synchronized(lock) {
            if (installationId.isEmpty()) {
                val preferences = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                installationId = preferences.getString(INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
                    preferences.edit().putString(INSTALL_ID, it).apply()
                }
            }
        }
        setEnabled(analyticsEnabled)
    }

    fun setEnabled(value: Boolean) {
        if (BuildConfig.MELO_STATS_ENDPOINT.isBlank()) return
        val callsToCancel = synchronized(lock) {
            if (enabled == value) return
            enabled = value
            if (value) {
                heartbeat = scope.launch {
                    if (synchronized(lock) { playbackActive }) sendNow()
                    while (isActive) {
                        delay(HEARTBEAT_MS)
                        if (synchronized(lock) { enabled && playbackActive }) sendNow()
                    }
                }
            } else {
                heartbeat?.cancel()
                heartbeat = null
            }
            if (value) emptyList() else inFlight.toList().also { inFlight.clear() }
        }
        callsToCancel.forEach(Call::cancel)
    }

    fun setPlaybackActive(value: Boolean) {
        val changed = synchronized(lock) {
            if (playbackActive == value) false else { playbackActive = value; true }
        }
        if (changed && value) sendNow()
    }

    private fun sendNow() {
        val endpoint = BuildConfig.MELO_STATS_ENDPOINT
        if (endpoint.isBlank()) return
        val payload = synchronized(lock) {
            if (!enabled || !playbackActive || installationId.isEmpty()) return
            JSONObject()
                .put("id", installationId)
                .put("platform", "android")
                .put("playbackActive", true)
                .toString()
        }
        scope.launch {
            val call = client.newCall(Request.Builder().url(endpoint)
                .post(payload.toRequestBody(jsonType)).build())
            val registered = synchronized(lock) {
                if (!enabled || !playbackActive) false else inFlight.add(call)
            }
            if (!registered) return@launch
            try {
                call.execute().use { }
            } catch (_: Exception) {
                // Presence expires if the best-effort heartbeat cannot reach the API.
            } finally {
                synchronized(lock) { inFlight.remove(call) }
            }
        }
    }
}
