package `in`.melo.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import `in`.melo.R
import `in`.melo.data.Music
import `in`.melo.presentation.components.SmartImage
import `in`.melo.presentation.components.SmartImageListTargetSize
import `in`.melo.presentation.components.artworkRim
import `in`.melo.ui.theme.MeloLayout

/** Four-row track shelf: each column pages sideways, leaving the next cover in view. */
@Composable
internal fun CompactTrackShelf(
    tracks: List<Music>,
    onClick: (Music) -> Unit,
    onOptions: (Music) -> Unit,
) {
    if (tracks.isEmpty()) return
    val pages = remember(tracks) { tracks.chunked(4) }
    val state = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = (maxWidth - MeloLayout.PageGutter * 2 - 24.dp).coerceAtLeast(160.dp)
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state, snapPosition = SnapPosition.Start),
            contentPadding = PaddingValues(horizontal = MeloLayout.PageGutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(pages, key = { index, page -> "$index:${page.first().id}" }) { _, page ->
                Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    page.forEach { track ->
                        CompactTrackRow(track, { onClick(track) }, { onOptions(track) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTrackRow(track: Music, onClick: () -> Unit, onOptions: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onOptions)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(8.dp)
        SmartImage(
            model = track.imageUrl, contentDescription = null,
            modifier = Modifier.size(56.dp).artworkRim(shape), shape = shape,
            targetSize = SmartImageListTargetSize,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onOptions, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.player_cd_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
    }
}
