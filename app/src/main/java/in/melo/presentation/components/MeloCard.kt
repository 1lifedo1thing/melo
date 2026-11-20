package `in`.melo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import `in`.melo.ui.theme.MeloLayout

/** A quiet, rimmed content surface with no tonal or shadow elevation. */
@Composable
fun MeloCard(
    modifier: Modifier = Modifier,
    shape: Shape = MeloLayout.CardShape,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.meloPanel(shape, color),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) { Column(content = content) }
}
