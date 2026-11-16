package `in`.melo.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import `in`.melo.R
import `in`.melo.data.HomeRecommendationPick
import `in`.melo.data.Music
import `in`.melo.data.YouTubeHomeChip
import `in`.melo.presentation.components.SmartImage
import `in`.melo.presentation.components.artworkRim
import `in`.melo.presentation.components.homePress
import `in`.melo.presentation.components.LocalHomeGradients
import `in`.melo.presentation.components.MoodArtwork
import `in`.melo.data.HomeMood
import `in`.melo.ui.theme.MeloLayout
import kotlinx.coroutines.launch

internal data class HomeSpeedDialEntry(
    val item: Music,
    val title: String = item.title,
    val artworkUrl: String? = item.imageUrl,
    val recommendation: HomeRecommendationPick? = null,
    val isNewRelease: Boolean = false,
)

private const val SPEED_DIAL_COLUMNS = 3
private const val SLOT_BLANK = -1
private const val SLOT_LUCKY = -2
private const val SLOT_HISTORY = -3
internal const val SPEED_DIAL_MAX_ITEMS = 54
private val SPEED_DIAL_GAP = 8.dp
private val SPEED_DIAL_SHAPE = RoundedCornerShape(10.dp)

private fun speedDialSlots(count: Int, pinned: Int, lucky: Boolean, history: Boolean, rows: Int): IntArray {
    val pageSize = SPEED_DIAL_COLUMNS * rows
    val extras = (if (lucky) 1 else 0) + (if (history) 1 else 0)
    val needed = count + extras
    if (needed == 0) return IntArray(0)
    val pages = ((needed + pageSize - 1) / pageSize).coerceAtLeast(1)
    val slots = IntArray(pages * pageSize) { SLOT_BLANK }

    // Keep Lucky and History together on the final page. If a packed page leaves
    // too little room, move only the unpinned tail entries forward to make space.
    val lastPageFill = if (count > 0 && count % pageSize == 0) pageSize else count % pageSize
    val overflow = (lastPageFill + extras - pageSize).coerceAtLeast(0)
    val moveToLastPage = minOf(overflow, (count - pinned.coerceIn(0, count)).coerceAtLeast(0))
    val leadingCount = count - moveToLastPage
    var next = 0
    for (index in 0 until leadingCount) slots[index] = next++
    val lastPageStart = (pages - 1) * pageSize
    repeat(moveToLastPage) { offset -> slots[lastPageStart + offset] = next++ }
    var actionSlot = if (moveToLastPage > 0) lastPageStart + moveToLastPage else maxOf(count, lastPageStart)
    if (lucky) slots[actionSlot++] = SLOT_LUCKY
    if (history) slots[actionSlot] = SLOT_HISTORY
    return slots
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeSpeedDial(
    entries: List<HomeSpeedDialEntry>,
    pinnedIds: Set<String>,
    categoryChips: List<YouTubeHomeChip>,
    selectedCategory: YouTubeHomeChip?,
    onCategoryClick: (YouTubeHomeChip) -> Unit,
    onItemClick: (HomeSpeedDialEntry) -> Unit,
    onItemLongPress: (HomeSpeedDialEntry) -> Unit,
    hasLuckyTile: Boolean,
    onLuckyClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onReorderPins: (() -> Unit)?,
    onCustomize: (() -> Unit)? = null,
    title: String? = null,
    rows: Int = 4,
    /** The first Speed Dial / Artist Dial page fills the visible Home viewport. */
    pageHeight: Dp? = null,
    footerReserve: Dp = 0.dp,
    /** Available height for square tiles after the mood row and page controls. */
    maxGridHeight: Dp? = null,
    showLabels: Boolean = true,
    showPinIcon: Boolean = true,
    tileStyle: String = `in`.melo.data.preferences.AppSettings.DIAL_TILE_STYLE_GLASS,
    showNewReleaseTags: Boolean = true,
    showHistoryTile: Boolean = true,
    onMissingArtwork: ((Music) -> Unit)? = null,
    loading: Boolean = false,
    onScrollToFeed: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (!loading && entries.isEmpty() && categoryChips.isEmpty() && title == null && !hasLuckyTile && !showHistoryTile) return
    val pinnedCount = entries.takeWhile { it.item.id in pinnedIds }.size
    val safeRows = rows.coerceIn(3, 4)
    val pageSize = SPEED_DIAL_COLUMNS * safeRows
    val slots = if (loading) IntArray(pageSize) { it }
        else if (entries.isEmpty() && !hasLuckyTile && !showHistoryTile) IntArray(0)
        else speedDialSlots(entries.size, pinnedCount, hasLuckyTile, history = showHistoryTile, rows = safeRows)
    val pageCount = slots.size / pageSize
    val pagerState = rememberPagerState(pageCount = { pageCount })

    val columnModifier = modifier.fillMaxWidth().then(
        if (pageHeight != null) Modifier.height(pageHeight) else Modifier.padding(bottom = 24.dp),
    )
    Column(columnModifier.padding(bottom = footerReserve), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (title != null || onReorderPins != null || onCustomize != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = MeloLayout.PageGutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (onReorderPins != null) {
                    IconButton(onClick = onReorderPins, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.SwapVert,
                            contentDescription = stringResource(R.string.pins_reorder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onCustomize != null) {
                    IconButton(onClick = onCustomize, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = stringResource(R.string.home_dial_customize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            // Dial leaves a little breathing room above the first row of art.
            Spacer(Modifier.height(14.dp))
        }

        if (categoryChips.isNotEmpty()) {
            HomeCategoryChips(categoryChips, selectedCategory, onCategoryClick)
        }

        if (loading || slots.isNotEmpty()) BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tileWidth = (maxWidth - MeloLayout.PageGutter * 2 - SPEED_DIAL_GAP * (SPEED_DIAL_COLUMNS - 1)) / SPEED_DIAL_COLUMNS
            val fitTileHeight = maxGridHeight?.let { (it - SPEED_DIAL_GAP * (safeRows - 1)) / safeRows }
            val tileSide = fitTileHeight?.let { minOf(tileWidth, maxOf(it, tileWidth * 0.8f)) } ?: tileWidth
            val gridHeight = tileSide * safeRows + SPEED_DIAL_GAP * (safeRows - 1)
            val sideInset = (maxWidth - tileSide * SPEED_DIAL_COLUMNS - SPEED_DIAL_GAP * (SPEED_DIAL_COLUMNS - 1)) / 2
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(gridHeight),
                contentPadding = PaddingValues(horizontal = sideInset),
                pageSpacing = 8.dp,
            ) { page ->
                val pageSlots = slots.drop(page * pageSize).take(pageSize)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(SPEED_DIAL_GAP),
                ) {
                    repeat(safeRows) { rowIndex ->
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(SPEED_DIAL_GAP),
                        ) {
                            repeat(SPEED_DIAL_COLUMNS) { columnIndex ->
                                val slot = pageSlots[rowIndex * SPEED_DIAL_COLUMNS + columnIndex]
                                when {
                                    loading -> HomeSkeletonTile(Modifier.weight(1f).fillMaxHeight())
                                    slot >= 0 -> {
                                        val entry = entries[slot]
                                        LaunchedEffect(entry.item.id, entry.artworkUrl) {
                                            if (entry.artworkUrl.isNullOrBlank()) onMissingArtwork?.invoke(entry.item)
                                        }
                                        SpeedDialTile(
                                            entry = entry,
                                            pinned = entry.item.id in pinnedIds,
                                            showLabel = showLabels,
                                            showPinIcon = showPinIcon,
                                            tileStyle = tileStyle,
                                            showNewReleaseTag = showNewReleaseTags,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                            onClick = { onItemClick(entry) },
                                            onLongPress = { onItemLongPress(entry) },
                                        )
                                    }
                                    slot == SLOT_LUCKY -> SpeedDialActionTile(
                                        label = stringResource(R.string.home_speed_dial_lucky),
                                        icon = Icons.Rounded.Shuffle,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        onClick = onLuckyClick,
                                    )
                                    slot == SLOT_HISTORY -> SpeedDialActionTile(
                                        label = stringResource(R.string.history_title),
                                        icon = Icons.Rounded.History,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        onClick = onHistoryClick,
                                    )
                                    else -> Spacer(Modifier.weight(1f).fillMaxHeight())
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!loading && entries.isEmpty() && title != null) {
            Text(
                text = stringResource(R.string.home_dial_mixes_empty),
                modifier = Modifier.fillMaxWidth().padding(horizontal = MeloLayout.PageGutter, vertical = 28.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (pageCount > 1) {
            val pageLabel = stringResource(R.string.home_speed_dial_page, pagerState.currentPage + 1, pageCount)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = pageLabel },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { index ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(6.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                            )
                    )
                }
            }
        }
        if (onScrollToFeed != null) {
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onScrollToFeed,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(40.dp),
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.home_dial_scroll_to_feed),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The Mixes Dial sits one horizontal swipe beside Speed Dial, as in Dial. */
@Composable
internal fun HomeDialPager(
    mixesAvailable: Boolean,
    speedDial: @Composable () -> Unit,
    mixesDial: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = if (mixesAvailable) 2 else 1
    if (pageCount == 1) {
        Box(modifier.fillMaxWidth()) { speedDial() }
        return
    }
    // Mixes sits to the left; Speed Dial is the page Home opens on.
    val state = rememberPagerState(initialPage = if (mixesAvailable) 1 else 0, pageCount = { pageCount })
    LaunchedEffect(mixesAvailable) {
        if (mixesAvailable && state.currentPage != 1) state.scrollToPage(1)
    }
    HorizontalPager(
        state = state,
        beyondViewportPageCount = 1,
        pageSpacing = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) { page ->
        if (page == 0) mixesDial() else speedDial()
    }
}

@Composable
internal fun HomeCategoryChips(
    chips: List<YouTubeHomeChip>,
    selected: YouTubeHomeChip?,
    onClick: (YouTubeHomeChip) -> Unit,
) {
    val rowState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(chips, selected?.params) {
        val index = chips.indexOfFirst { it.params == selected?.params }
        if (selected != null && index >= 0) rowState.animateScrollToItem(index)
    }
    LazyRow(
        state = rowState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = MeloLayout.PageGutter),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(chips, key = YouTubeHomeChip::params) { chip ->
            val isSelected = chip.params == selected?.params
            val interactions = remember { MutableInteractionSource() }
            val background by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                animationSpec = tween(240),
                label = "homeCategoryBackground",
            )
            val foreground by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                label = "homeCategoryForeground",
            )
            Box(
                modifier = Modifier
                    .homePress(interactions)
                    .height(48.dp)
                    .selectable(selected = isSelected, role = Role.Tab, interactionSource = interactions, indication = null) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick(chip)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.height(40.dp).clip(RoundedCornerShape(12.dp)).background(background)
                        .border(0.75.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chip.title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        color = foreground, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialTile(
    entry: HomeSpeedDialEntry,
    pinned: Boolean,
    showLabel: Boolean,
    showPinIcon: Boolean,
    tileStyle: String,
    showNewReleaseTag: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .homePress(interactions)
            .aspectRatio(1f)
            .clip(SPEED_DIAL_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                role = Role.Button,
                interactionSource = interactions,
                indication = null,
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .semantics {
                contentDescription = listOf(entry.title, entry.item.subtitle)
                    .filter(String::isNotBlank)
                    .joinToString(", ")
                role = Role.Button
            },
    ) {
        SmartImage(
            model = entry.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (showLabel) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))),
            )
            Text(
                text = entry.title,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 6.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (pinned && showPinIcon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White,
                )
            }
        }
        if (showNewReleaseTag && (entry.isNewRelease || entry.recommendation?.type == `in`.melo.data.HomeRecommendationType.LATEST_RELEASE)) {
            Text(
                text = stringResource(R.string.home_new_music_tag),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
            )
        }
        when (tileStyle) {
            `in`.melo.data.preferences.AppSettings.DIAL_TILE_STYLE_EDGE -> {
                Box(Modifier.fillMaxSize().artworkRim(SPEED_DIAL_SHAPE))
            }
            `in`.melo.data.preferences.AppSettings.DIAL_TILE_STYLE_GLASS -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .artworkRim(SPEED_DIAL_SHAPE, glass = true),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .homePress(interactions)
            .aspectRatio(1f)
            .clip(SPEED_DIAL_SHAPE)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .artworkRim(SPEED_DIAL_SHAPE, glass = true)
            .combinedClickable(role = Role.Button, interactionSource = interactions, indication = null, onClick = onClick, onLongClick = null)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        MoodArtwork(if (icon == Icons.Rounded.Shuffle) HomeMood.AFTER_HOURS else HomeMood.RELAX, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))))
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp), tint = Color.White)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
