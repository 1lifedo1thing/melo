package `in`.melo.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import `in`.melo.data.HomeMood
import `in`.melo.presentation.utils.LocalReducedMotion

val LocalHomeGradients = staticCompositionLocalOf { true }
val LocalHomePressMotion = staticCompositionLocalOf { true }

/** A subtle light-catching rim for artwork thumbnails. */
fun Modifier.artworkRim(shape: Shape, glass: Boolean = false): Modifier = drawWithCache {
    val width = if (glass) 1.25.dp.toPx() else 0.75.dp.toPx()
    val outline = shape.createOutline(size.copy(width = size.width - width, height = size.height - width), layoutDirection, this)
    val rim = Brush.verticalGradient(
        0f to Color.White.copy(alpha = if (glass) 0.48f else 0.22f),
        0.22f to Color.White.copy(alpha = 0.16f),
        0.65f to Color.White.copy(alpha = 0.06f),
        1f to Color.White.copy(alpha = 0.16f),
    )
    onDrawWithContent {
        drawContent()
        translate(width / 2f, width / 2f) { drawOutline(outline, rim, style = Stroke(width)) }
    }
}

@Composable
fun Modifier.meloPress(source: MutableInteractionSource, enabled: Boolean = true): Modifier {
    val pressed by source.collectIsPressedAsState()
    val motionEnabled = enabled && LocalHomePressMotion.current && !LocalReducedMotion.current
    val scale by animateFloatAsState(
        if (pressed && motionEnabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 650f), label = "artworkPress",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun Modifier.homePress(source: MutableInteractionSource): Modifier = meloPress(source)

/** Mood colors paired with contrasting text for legibility. */
fun moodColors(mood: HomeMood): Pair<Color, Color> = when (mood) {
    HomeMood.ENERGY, HomeMood.FITNESS, HomeMood.SPORTS -> Color(0xFF147A35) to Color(0xFFE2F6A2)
    HomeMood.RELAX -> Color(0xFF087FAE) to Color(0xFFB4E9FF)
    HomeMood.FEEL_GOOD, HomeMood.MORNING, HomeMood.SUMMERTIME -> Color(0xFF9D5B00) to Color(0xFFFFE8A0)
    HomeMood.FOCUS, HomeMood.EATING_COOKING -> Color(0xFFA24313) to Color(0xFFFFD6A4)
    HomeMood.HEARTBREAK, HomeMood.HOLIDAY -> Color(0xFFAA3027) to Color(0xFFFFA690)
    HomeMood.LOVE, HomeMood.SOCIAL, HomeMood.PARTY -> Color(0xFFA82B68) to Color(0xFFFFB8DC)
    HomeMood.AFTER_HOURS, HomeMood.SLEEP, HomeMood.WEEKEND -> Color(0xFF39216F) to Color(0xFFC9B6FF)
    HomeMood.CHILL, HomeMood.HOME, HomeMood.WELLBEING -> Color(0xFF0C706B) to Color(0xFFA6F0E6)
    HomeMood.COMMUTING, HomeMood.OUTDOORS -> Color(0xFF1D633E) to Color(0xFFA8EFD0)
    else -> Color(0xFF294D8D) to Color(0xFFB8D8FF)
}

/** Quiet, static mood artwork; no perpetual animation or decoder on the scrolling feed. */
@Composable
fun MoodArtwork(mood: HomeMood, modifier: Modifier = Modifier) {
    val (base, ink) = moodColors(mood)
    val gradients = LocalHomeGradients.current
    Canvas(modifier) {
        drawRect(if (gradients) Brush.linearGradient(listOf(base, base.copy(red = base.red * 0.55f, green = base.green * 0.55f, blue = base.blue * 0.7f))) else Brush.linearGradient(listOf(base, base)))
        val cx = size.width * 0.55f
        val cy = size.height * 0.37f
        val r = size.width * 0.33f
        when (mood.ordinal % 3) {
            0 -> {
                val path = Path().apply {
                    moveTo(cx, cy - r)
                    cubicTo(cx + r * 0.12f, cy - r * 0.12f, cx + r * 0.12f, cy - r * 0.12f, cx + r, cy)
                    cubicTo(cx + r * 0.12f, cy + r * 0.12f, cx + r * 0.12f, cy + r * 0.12f, cx, cy + r)
                    cubicTo(cx - r * 0.12f, cy + r * 0.12f, cx - r * 0.12f, cy + r * 0.12f, cx - r, cy)
                    cubicTo(cx - r * 0.12f, cy - r * 0.12f, cx - r * 0.12f, cy - r * 0.12f, cx, cy - r)
                    close()
                }
                drawPath(path, ink, style = Stroke(2.dp.toPx()))
            }
            1 -> repeat(4) { index ->
                val shift = (index - 1.5f) * r * 0.24f
                val path = Path().apply {
                    moveTo(cx - r, cy + shift)
                    cubicTo(cx - r * 0.4f, cy - r + shift, cx + r * 0.4f, cy + r + shift, cx + r, cy + shift)
                }
                drawPath(path, ink.copy(alpha = 1f - index * 0.14f), style = Stroke(2.dp.toPx()))
            }
            else -> repeat(4) { index -> drawCircle(ink.copy(alpha = 0.8f - index * 0.13f), r * (0.3f + index * 0.24f), Offset(cx, cy), style = Stroke(2.dp.toPx())) }
        }
    }
}
