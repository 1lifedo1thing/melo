package `in`.melo.presentation.screens

import `in`.melo.presentation.utils.LocalAppHapticsConfig
import `in`.melo.presentation.utils.performAppCompatHapticFeedback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import `in`.melo.presentation.components.MeloBottomSheet as ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import androidx.core.view.HapticFeedbackConstantsCompat
import `in`.melo.presentation.components.meloPanel
import `in`.melo.R
import `in`.melo.data.model.Song
import `in`.melo.data.model.Album
import `in`.melo.data.model.Artist
import `in`.melo.data.model.Playlist
import `in`.melo.ui.theme.GoogleSansRounded
import `in`.melo.ui.theme.LocalMeloDarkTheme
import `in`.melo.presentation.viewmodel.ColorSchemePair
import `in`.melo.presentation.viewmodel.PlayerViewModel
import `in`.melo.presentation.components.SmartImage
import `in`.melo.presentation.components.ShimmerBox
import `in`.melo.presentation.components.AutoScrollingTextOnDemand
import `in`.melo.presentation.components.subcomps.AutoSizingTextToFill
import `in`.melo.presentation.components.subcomps.TightWrapText
import `in`.melo.presentation.components.MiniPlayerHeight
import `in`.melo.utils.formatDuration
import `in`.melo.ui.theme.ExpTitleTypography

@Composable
fun SettingsSection(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 10.dp)
        ) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
        title: String,
        subtitle: String,
        leadingIcon: @Composable () -> Unit,
        trailingIcon: @Composable () -> Unit = {},
        onClick: () -> Unit
) {
    Surface(
            color = Color.Transparent,
            modifier =
                    Modifier.fillMaxWidth()
                            .meloPanel()
                            .clickable(onClick = onClick)
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
        ) {
            Box(
                    modifier = Modifier.padding(end = 14.dp).size(36.dp).meloPanel(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
            ) { leadingIcon() }

            Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                trailingIcon()
            }
        }
    }
}

@Composable
fun SwitchSettingItem(
        title: String,
        subtitle: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        leadingIcon: @Composable (() -> Unit)? = null,
        enabled: Boolean = true
) {
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current

    Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().meloPanel()
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leadingIcon != null) {
                Box(
                        modifier = Modifier.padding(end = 4.dp).size(24.dp),
                        contentAlignment = Alignment.Center
                ) { leadingIcon() }
            }

            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color =
                                if (enabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                                if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = { newValue ->
                    if (enabled) {
                        performAppCompatHapticFeedback(
                            view,
                            appHapticsConfig,
                            HapticFeedbackConstantsCompat.GESTURE_START
                        )
                        onCheckedChange(newValue)
                    }
                },
                enabled = enabled,
                thumbContent = {
                    AnimatedContent(
                        targetState = checked,
                        transitionSpec = { fadeIn(tween(100)) togetherWith fadeOut(tween(100)) },
                        label = "switch_thumb_icon"
                    ) { isChecked ->
                        Icon(
                            imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorItem(
        label: String,
        description: String,
        options: Map<String, String>,
        selectedKey: String,
        onSelectionChanged: (String) -> Unit,
        leadingIcon: @Composable () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedOption = options[selectedKey] ?: selectedKey

    Surface(
            color = Color.Transparent,
            modifier =
                    Modifier.fillMaxWidth().meloPanel().clickable {
                        showSheet = true
                    }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(end = 14.dp).size(36.dp).meloPanel(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                ) { leadingIcon() }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedOption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSheet) {
        `in`.melo.presentation.components.MeloBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall, // Larger header
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options.entries.toList()) { (key, optionLabel) ->
                        val isSelected = key == selectedKey
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        
                        Surface(
                            onClick = {
                                onSelectionChanged(key)
                                showSheet = false
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = containerColor,
                            modifier = Modifier.fillMaxWidth().height(72.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = optionLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = contentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = stringResource(R.string.common_selected),
                                        tint = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SliderSettingsItem(
        label: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: (() -> Unit)? = null,
        valueText: (Float) -> String,
        enabled: Boolean = true,
) {
    Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().meloPanel()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = valueText(value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun ActionSettingsItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().meloPanel()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(end = 14.dp).size(36.dp).meloPanel(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Action
            FilledTonalButton(
                onClick = onPrimaryAction,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(primaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Secondary Action (Optional)
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSecondaryAction,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(secondaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
