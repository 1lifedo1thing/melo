package `in`.melo.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.melo.R
import `in`.melo.data.HomeMood
import `in`.melo.data.HomeRecommendationPick
import `in`.melo.data.HomeRecommendationType
import `in`.melo.data.Music
import `in`.melo.data.MusicKind
import `in`.melo.presentation.components.SmartImage
import `in`.melo.presentation.components.MoodArtwork
import `in`.melo.presentation.components.LocalHomeGradients
import `in`.melo.presentation.components.artworkRim
import `in`.melo.presentation.components.homePress
import `in`.melo.ui.theme.MeloLayout

/* Full-art recommendation cards and a scannable Recents feed using Melo data and actions. */

private const val TOP_PICK_RATIO = 0.75f
private val TOP_PICK_SHAPE = RoundedCornerShape(12.dp)

@Composable
internal fun ForYouShelf(
    picks: List<HomeRecommendationPick>,
    moods: List<HomeMood>,
    recent: List<Music>,
    onPickClick: (HomeRecommendationPick) -> Unit,
    onLongPress: (Music) -> Unit,
    onMoodClick: (HomeMood) -> Unit,
    onShowAllMoods: () -> Unit,
    onRecentClick: (Music) -> Unit,
    onRecentLongPress: (Music) -> Unit,
    onOpenHistory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
        if (picks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = MeloLayout.PageGutter),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_for_you_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                BoxWithConstraints {
                    val cardWidth = topPickWidth(maxWidth)
                    val shelfState = rememberLazyListState()
                    LazyRow(
                        state = shelfState,
                        flingBehavior = rememberSnapFlingBehavior(shelfState),
                        contentPadding = PaddingValues(horizontal = MeloLayout.PageGutter),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(picks, key = { "${it.type}:${it.item.id}" }, contentType = { it.type }) { pick ->
                            when (pick.type) {
                                HomeRecommendationType.ARTIST_AND_SIMILAR,
                                HomeRecommendationType.FEATURED_ARTIST -> ForYouRadioCard(
                                    seed = pick.item,
                                    eyebrow = stringResource(if (pick.type == HomeRecommendationType.ARTIST_AND_SIMILAR)
                                        R.string.home_artist_similar_tag else R.string.home_featured_artist_tag),
                                    title = pick.title,
                                    subtitle = pick.subtitle,
                                    artworkUrls = pick.artworkUrls,
                                    modifier = Modifier.width(cardWidth),
                                    onClick = { onPickClick(pick) },
                                )
                                HomeRecommendationType.MOOD -> ForYouMoodCard(
                                    mood = pick.mood ?: HomeMood.FEEL_GOOD,
                                    modifier = Modifier.width(cardWidth),
                                    onClick = { onPickClick(pick) },
                                )
                                else -> ForYouPlaylistCard(
                                    item = pick.item,
                                    eyebrow = stringResource(when (pick.type) {
                                        HomeRecommendationType.LATEST_RELEASE -> R.string.home_new_release_tag
                                        HomeRecommendationType.PERSONAL_STATION -> R.string.home_personal_station_tag
                                        HomeRecommendationType.NEW_RELEASE_MIX -> R.string.home_new_release_mix_tag
                                        HomeRecommendationType.DISCOVERY_STATION -> R.string.home_discovery_station_tag
                                        else -> R.string.home_for_you_title
                                    }),
                                    title = when (pick.type) {
                                        HomeRecommendationType.PERSONAL_STATION -> pick.title.takeIf { pick.item.kind == MusicKind.Playlist }
                                            ?: stringResource(R.string.home_personal_station_title)
                                        HomeRecommendationType.DISCOVERY_STATION -> stringResource(R.string.home_discovery_station_title)
                                        else -> pick.title
                                    },
                                    subtitle = pick.subtitle,
                                    artworkUrls = pick.artworkUrls,
                                    modifier = Modifier.width(cardWidth),
                                    onClick = { onPickClick(pick) },
                                    onLongPress = if (pick.type == HomeRecommendationType.LATEST_RELEASE) {
                                        { onLongPress(pick.item) }
                                    } else null,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            RecentlyPlayedFeed(
                tracks = recent,
                onClick = onRecentClick,
                onLongPress = onRecentLongPress,
                onOpenAll = onOpenHistory,
            )
        }
        if (moods.isNotEmpty()) FindYourMood(moods, onMoodClick, onShowAllMoods)
    }
}

private fun topPickWidth(available: Dp): Dp = minOf((available - MeloLayout.PageGutter * 2) * 0.66f, 300.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ForYouPlaylistCard(
    item: Music,
    eyebrow: String,
    title: String,
    subtitle: String,
    artworkUrls: List<String>,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .homePress(interactions)
            .aspectRatio(TOP_PICK_RATIO)
            .clip(TOP_PICK_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .artworkRim(TOP_PICK_SHAPE)
            .combinedClickable(role = Role.Button, interactionSource = interactions, indication = null, onClick = onClick, onLongClick = onLongPress),
    ) {
        SmartImage(
            model = artworkUrls.firstOrNull() ?: item.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            shape = TOP_PICK_SHAPE,
            targetSize = coil.size.Size(800, 1000),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.32f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.52f),
                        1f to Color.Black.copy(alpha = 0.96f),
                    ),
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ForYouRadioCard(
    seed: Music,
    eyebrow: String,
    title: String,
    subtitle: String,
    artworkUrls: List<String>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val gradients = LocalHomeGradients.current
    Box(
        modifier.aspectRatio(TOP_PICK_RATIO).homePress(interactions)
            .clip(TOP_PICK_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .artworkRim(TOP_PICK_SHAPE, glass = true)
            .combinedClickable(role = Role.Button, interactionSource = interactions, indication = null, onClick = onClick),
    ) {
        SmartImage(
            model = artworkUrls.firstOrNull() ?: seed.imageUrl,
            contentDescription = null, modifier = Modifier.fillMaxSize(),
            targetSize = coil.size.Size(800, 1000),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.05f),
            0.4f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.96f),
        )))
        if (gradients) Box(Modifier.fillMaxSize().background(Brush.linearGradient(
            listOf(Color(0xFF81376E).copy(alpha = 0.30f), Color.Transparent, Color(0xFF111B48).copy(alpha = 0.5f)),
        )))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(eyebrow, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ForYouMoodCard(mood: HomeMood, modifier: Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier.aspectRatio(TOP_PICK_RATIO).homePress(interactions).clip(TOP_PICK_SHAPE)
            .artworkRim(TOP_PICK_SHAPE, glass = true)
            .combinedClickable(role = Role.Button, interactionSource = interactions, indication = null, onClick = onClick),
    ) {
        MoodArtwork(mood, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.home_mood_for_you_tag), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            Text(stringResource(moodLabel(mood)), style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FindYourMood(moods: List<HomeMood>, onMoodClick: (HomeMood) -> Unit, onShowAllMoods: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = MeloLayout.PageGutter, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home_find_your_mood), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onShowAllMoods) { Text(stringResource(R.string.home_show_all_moods)) }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = MeloLayout.PageGutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(moods, key = { it.name }) { mood ->
                val interactions = remember { MutableInteractionSource() }
                Box(
                    Modifier.width(154.dp).aspectRatio(1.15f).homePress(interactions)
                        .clip(TOP_PICK_SHAPE).artworkRim(TOP_PICK_SHAPE)
                        .combinedClickable(role = Role.Button, interactionSource = interactions, indication = null, onClick = { onMoodClick(mood) }),
                ) {
                    MoodArtwork(mood, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                    Text(
                        stringResource(moodLabel(mood)), Modifier.align(Alignment.BottomStart).padding(12.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeMoodPickerDialog(onDismiss: () -> Unit, onMoodClick: (HomeMood) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_find_your_mood)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HomeMood.entries.toList().chunked(3).forEach { rowMoods ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowMoods.forEach { mood ->
                            TextButton(
                                onClick = { onMoodClick(mood); onDismiss() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(moodLabel(mood)), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        repeat(3 - rowMoods.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_close_moods)) }
        },
    )
}

private fun moodLabel(mood: HomeMood): Int = when (mood) {
    HomeMood.ENERGY -> R.string.home_mood_energy
    HomeMood.RELAX -> R.string.home_mood_relax
    HomeMood.FEELING_BLUE -> R.string.home_mood_feeling_blue
    HomeMood.FEEL_GOOD -> R.string.home_mood_feel_good
    HomeMood.FOCUS -> R.string.home_mood_focus
    HomeMood.HEARTBREAK -> R.string.home_mood_heartbreak
    HomeMood.LOVE -> R.string.home_mood_love
    HomeMood.AFTER_HOURS -> R.string.home_mood_after_hours
    HomeMood.ALONE_TIME -> R.string.home_mood_alone_time
    HomeMood.CHILL -> R.string.home_mood_chill
    HomeMood.COMMUTING -> R.string.home_mood_commuting
    HomeMood.EATING_COOKING -> R.string.home_mood_eating_cooking
    HomeMood.EVENING -> R.string.home_mood_evening
    HomeMood.FITNESS -> R.string.home_mood_fitness
    HomeMood.GAMING -> R.string.home_mood_gaming
    HomeMood.HOME -> R.string.home_mood_home
    HomeMood.MORNING -> R.string.home_mood_morning
    HomeMood.MOTIVATION -> R.string.home_mood_motivation
    HomeMood.OUTDOORS -> R.string.home_mood_outdoors
    HomeMood.PARTY -> R.string.home_mood_party
    HomeMood.SLEEP -> R.string.home_mood_sleep
    HomeMood.SOCIAL -> R.string.home_mood_social
    HomeMood.SPORTS -> R.string.home_mood_sports
    HomeMood.SUMMERTIME -> R.string.home_mood_summertime
    HomeMood.HOLIDAY -> R.string.home_mood_holiday
    HomeMood.WEEKEND -> R.string.home_mood_weekend
    HomeMood.WELLBEING -> R.string.home_mood_wellbeing
    HomeMood.WORK -> R.string.home_mood_work
}

@Composable
private fun RecentlyPlayedFeed(
    tracks: List<Music>,
    onClick: (Music) -> Unit,
    onLongPress: (Music) -> Unit,
    onOpenAll: () -> Unit,
) {
    val openAllDescription = stringResource(R.string.home_recent_feed_open_all)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = MeloLayout.PageGutter, end = MeloLayout.PageGutter / 2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_recent_feed_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = onOpenAll,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = openAllDescription
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        CompactTrackShelf(tracks, onClick, onLongPress)
    }
}
