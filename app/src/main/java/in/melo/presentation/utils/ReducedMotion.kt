package `in`.melo.presentation.utils

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Follows Android's Remove animations option, including changes while Melo is open. */
@Composable
fun observeReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = !ValueAnimator.areAnimatorsEnabled()
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

val LocalReducedMotion = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean = LocalReducedMotion.current
