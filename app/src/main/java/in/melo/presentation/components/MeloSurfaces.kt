package `in`.melo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import `in`.melo.ui.theme.MeloLayout

val LocalSurfaceGradients = staticCompositionLocalOf { true }

/** The same quiet surface, soft gradient and rim across settings, sheets and controls. */
fun Modifier.meloPanel(shape: Shape = MeloLayout.PanelShape, color: Color? = null): Modifier = composed {
    val colors = MaterialTheme.colorScheme
    val ground = color ?: colors.surface
    clip(shape)
        .background(ground)
        .background(Brush.verticalGradient(listOf(
            colors.onSurface.copy(alpha = if (LocalSurfaceGradients.current) 0.025f else 0f), Color.Transparent,
        )))
        .border(0.75.dp, Brush.verticalGradient(listOf(
            colors.onSurface.copy(alpha = 0.16f), colors.onSurface.copy(alpha = 0.055f),
            colors.onSurface.copy(alpha = 0.09f),
        )), shape)
}

/** Keeps Material's dismissal, focus, insets and drag behavior with Melo's shared surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeloBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = MeloLayout.SheetShape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
    ) {
        // Draw inside Material's translated sheet. Decorations on the incoming
        // modifier stay outside its offset and can clip the moving content.
        val tint = MaterialTheme.colorScheme.onSurface
            .copy(alpha = if (LocalSurfaceGradients.current) 0.025f else 0f)
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(tint, Color.Transparent))),
            content = content,
        )
    }
}
