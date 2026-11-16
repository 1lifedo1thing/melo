package `in`.melo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import `in`.melo.presentation.utils.LocalReducedMotion
import kotlin.math.*

/** Dial's Spin -> Build -> Rest timing, adapted to Melo's dotted play mark. */
@Composable
fun MeloRefreshLogo(
    refreshing: Boolean,
    pullFraction: () -> Float,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val reduced = LocalReducedMotion.current || !animate
    val head = remember { mutableFloatStateOf(0f) }
    var spinning by remember { mutableStateOf(false) }
    val built = remember { Animatable(1f) }
    LaunchedEffect(refreshing, reduced) {
        if (reduced) {
            spinning = false
            built.snapTo(1f)
        } else if (refreshing) {
            spinning = true
            built.snapTo(0f)
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                head.floatValue = (head.floatValue + (now - last) / 1_100_000_000f) % 1f
                last = now
            }
        } else if (spinning) {
            spinning = false
            built.animateTo(1f, tween(660, easing = LinearEasing))
        }
    }
    val dots = remember {
        buildList {
            // Open dotted ring, with a play triangle inside.
            repeat(28) { index ->
                val angle = (index / 28f * 2f * PI - PI / 2f).toFloat()
                add(Offset(0.5f + cos(angle) * 0.43f, 0.5f + sin(angle) * 0.43f))
            }
            for (column in 0..4) {
                for (row in 0..(8 - column * 2)) {
                    add(Offset(0.35f + column * 0.075f, 0.26f + column * 0.06f + row * 0.06f))
                }
            }
        }
    }
    Canvas(modifier.size(36.dp).clearAndSetSemantics {}) {
        val side = size.minDimension
        val pull = pullFraction().coerceIn(0f, 1f)
        dots.forEachIndexed { index, point ->
            val alpha = when {
                reduced -> if (refreshing) 0.65f else 1f
                spinning && index < 28 -> {
                    val behind = (head.floatValue - index / 28f + 1f) % 1f
                    0.16f + 0.84f * exp(-behind / 0.13f)
                }
                spinning -> 0.16f
                built.value < 1f -> 0.16f + 0.84f * (built.value * dots.size - index).coerceIn(0f, 1f)
                pull > 0f -> 0.25f + 0.75f * (pull * dots.size - index).coerceIn(0f, 1f)
                else -> 1f
            }
            drawCircle(ink.copy(alpha = alpha), side * if (index < 28) 0.018f else 0.022f,
                Offset(point.x * side, point.y * side))
        }
    }
}
