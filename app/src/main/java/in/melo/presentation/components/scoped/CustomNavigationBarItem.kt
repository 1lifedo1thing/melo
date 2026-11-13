package `in`.melo.presentation.components.scoped

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Stable touch targets, native tab semantics, and a quiet dot for the active tab. */
@Composable
fun RowScope.CustomNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    selectedIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compactMode: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    contentDescription: String? = null,
    alwaysShowLabel: Boolean = true,
    selectedIconColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTextColor: Color = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val showLabel = !compactMode && label != null && (alwaysShowLabel || selected)
    Column(
        modifier.fillMaxHeight().heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.Tab,
                interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (!showLabel && contentDescription != null) this.contentDescription = contentDescription
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) selectedIconColor else unselectedIconColor) {
            Box(Modifier.size(24.dp).clearAndSetSemantics {}) {
                if (selected) selectedIcon() else icon()
            }
        }
        if (showLabel) ProvideTextStyle(MaterialTheme.typography.labelMedium.copy(
            color = if (selected) selectedTextColor else unselectedTextColor,
        )) { label?.invoke() }
        Box(Modifier.size(4.dp).background(
            if (selected) selectedIconColor else Color.Transparent, CircleShape))
    }
}
