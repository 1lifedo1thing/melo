package `in`.melo.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/** Controls Firebase Analytics collection without recording any Melo-specific content events. */
object UsageAnalytics {
    fun setEnabled(context: Context, enabled: Boolean) {
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME } ?: return
        FirebaseAnalytics.getInstance(context.applicationContext).setAnalyticsCollectionEnabled(enabled)
    }
}
