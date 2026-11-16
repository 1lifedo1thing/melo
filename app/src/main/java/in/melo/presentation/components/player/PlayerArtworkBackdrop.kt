package `in`.melo.presentation.components.player

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import `in`.melo.presentation.utils.LocalReducedMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

// Modified for Melo on 2026-11-16. Licensed under GPL-3.0; see LICENSE.
// A small texture preserves the cover's color proportions without a live screen-sized blur.
private val meshCache = LruCache<String, ImageBitmap>(64)

@Composable
internal fun PlayerArtworkBackdrop(artworkUrl: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    var mesh by remember { mutableStateOf(artworkUrl?.let(meshCache::get)) }
    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            mesh = null
            return@LaunchedEffect
        }
        meshCache.get(artworkUrl)?.let { mesh = it; return@LaunchedEffect }
        // Retain the last cover's colors while the new one is being decoded.
        repeat(3) { attempt ->
            if (attempt > 0) delay(800L)
            val result = context.imageLoader.execute(
                ImageRequest.Builder(context).data(artworkUrl).size(120).allowHardware(false).build()
            )
            val drawable = (result as? SuccessResult)?.drawable ?: return@repeat
            val ready = withContext(Dispatchers.Default) {
                artworkMesh(drawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap(), artworkUrl.hashCode())
            } ?: return@repeat
            meshCache.put(artworkUrl, ready)
            mesh = ready
            return@LaunchedEffect
        }
    }

    var shown by remember { mutableStateOf(mesh) }
    var incoming by remember { mutableStateOf<ImageBitmap?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(mesh, reducedMotion) {
        incoming?.let { shown = it }
        incoming = null
        val next = mesh
        when {
            next == null -> shown = null
            shown == null || reducedMotion -> shown = next
            next !== shown -> {
                incoming = next
                fade.snapTo(0f)
                fade.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
                shown = next
                incoming = null
            }
        }
    }
    Canvas(modifier) {
        drawRect(Color(0xFF121212))
        val bounds = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
        shown?.let { drawImage(it, dstSize = bounds, filterQuality = FilterQuality.Low) }
        incoming?.let { drawImage(it, dstSize = bounds, alpha = fade.value, filterQuality = FilterQuality.Low) }
        // Melo keeps white foreground controls, including on very bright covers.
        drawRect(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.56f),
            0.35f to Color.Black.copy(alpha = 0.40f),
            0.60f to Color.Black.copy(alpha = 0.58f),
            1f to Color.Black.copy(alpha = 0.62f),
        ))
    }
}

private fun artworkMesh(source: ImageBitmap, seed: Int): ImageBitmap? {
    val width = source.width
    val height = source.height
    if (width < 1 || height < 1) return null
    val cols = 6.coerceAtMost(width)
    val rows = 6.coerceAtMost(height)
    val red = LongArray(rows * cols)
    val green = LongArray(rows * cols)
    val blue = LongArray(rows * cols)
    val count = IntArray(rows * cols)
    val line = IntArray(width)
    for (y in 0 until height) {
        source.readPixels(line, startX = 0, startY = y, width = width, height = 1)
        val rowBase = ((height - 1 - y) * rows / height) * cols
        for (x in 0 until width) {
            val cell = rowBase + x * cols / width
            val pixel = line[x]
            red[cell] += (pixel shr 16) and 0xFF
            green[cell] += (pixel shr 8) and 0xFF
            blue[cell] += pixel and 0xFF
            count[cell]++
        }
    }
    val grid = IntArray(rows * cols) { cell ->
        val n = count[cell].coerceAtLeast(1)
        val color = argb((red[cell] / n).toInt(), (green[cell] / n).toInt(), (blue[cell] / n).toInt())
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(color, it) }
        hsl[1] = (hsl[1] * 1.12f).coerceAtMost(1f)
        hsl[2] = hsl[2].coerceAtLeast(0.045f)
        ColorUtils.HSLToColor(hsl)
    }
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val rotated = grid.copyOf()
    for (row in 1 until rows) for (x in 0 until cols) {
        val src = if (mirror) cols - 1 - x else x
        rotated[row * cols + x] = grid[row * cols + (src + shift) % cols]
    }
    val texels = IntArray(32 * 32)
    for (ty in 0 until 32) {
        val fy = (ty + 0.5f) / 32 * rows - 0.5f
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val wy = smoothstep(fy - y0)
        for (tx in 0 until 32) {
            val fx = (tx + 0.5f) / 32 * cols - 0.5f
            val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val wx = smoothstep(fx - x0)
            val top = lerpArgb(rotated[y0 * cols + x0], rotated[y0 * cols + x1], wx)
            val bottom = lerpArgb(rotated[y1 * cols + x0], rotated[y1 * cols + x1], wx)
            texels[ty * 32 + tx] = lerpArgb(top, bottom, wy)
        }
    }
    return Bitmap.createBitmap(texels, 32, 32, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private fun smoothstep(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

private fun lerpArgb(from: Int, to: Int, t: Float): Int {
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }
    return argb(channel(16), channel(8), channel(0))
}

private fun argb(red: Int, green: Int, blue: Int): Int =
    (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
