package `in`.melo.presentation.components

import `in`.melo.ui.theme.MeloAlpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.melo.ui.theme.MeloLayout

/** Rounded, rimmed action panel shared by the page header menus. */
@Composable
fun MeloMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 248.dp, max = 304.dp),
        shape = MeloLayout.PanelShape,
        containerColor = colors.surface,
        shadowElevation = 12.dp,
        border = BorderStroke(0.75.dp, Brush.verticalGradient(listOf(
            colors.onSurface.copy(alpha = 0.22f),
            colors.onSurface.copy(alpha = 0.06f),
            colors.onSurface.copy(alpha = 0.12f),
        ))),
    ) {
        Column(
            Modifier.background(Brush.verticalGradient(listOf(
                colors.onSurface.copy(alpha = if (LocalSurfaceGradients.current) 0.025f else 0f), Color.Transparent,
            ))).padding(horizontal = MeloLayout.Space2),
            content = content,
        )
    }
}

@Composable
fun MeloMenuItem(label: String, icon: @Composable () -> Unit, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        leadingIcon = {
            Box(
                Modifier.size(34.dp).clip(MeloLayout.CardShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = MeloAlpha.Divider)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(20.dp)) { icon() }
            }
        },
        onClick = onClick,
        enabled = enabled,
        colors = if (destructive) MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
            leadingIconColor = MaterialTheme.colorScheme.error,
        ) else MenuDefaults.itemColors(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MeloLayout.PanelShape),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun MeloMenuDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = MeloAlpha.Divider),
    )
}
