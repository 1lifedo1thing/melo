package `in`.melo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import `in`.melo.presentation.components.ShimmerBox
import `in`.melo.ui.theme.MeloLayout

@Composable
internal fun HomeSkeletonTile(modifier: Modifier = Modifier) {
    ShimmerBox(modifier.aspectRatio(1f).clip(MeloLayout.CardShape))
}

/** Geometry matches the discovery cards and four-row track shelves after loading. */
@Composable
internal fun HomeFeedSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.padding(horizontal = MeloLayout.PageGutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(Modifier.width(180.dp).height(26.dp).clip(MeloLayout.ArtworkShape))
            Row(horizontalArrangement = Arrangement.spacedBy(MeloLayout.Space4)) {
                ShimmerBox(Modifier.weight(1f).aspectRatio(0.75f).clip(MeloLayout.CardShape))
                ShimmerBox(Modifier.weight(0.48f).aspectRatio(0.36f).clip(MeloLayout.CardShape))
            }
        }
        Column(Modifier.padding(horizontal = MeloLayout.PageGutter), verticalArrangement = Arrangement.spacedBy(MeloLayout.Space1)) {
            ShimmerBox(Modifier.width(140.dp).height(26.dp).clip(MeloLayout.ArtworkShape))
            Spacer(Modifier.height(MeloLayout.Space3))
            repeat(4) {
                Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(56.dp).clip(MeloLayout.ArtworkShape))
                    Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.65f).height(16.dp).clip(MeloLayout.ArtworkShape))
                        ShimmerBox(Modifier.fillMaxWidth(0.45f).height(12.dp).clip(MeloLayout.ArtworkShape))
                    }
                }
            }
        }
    }
}
