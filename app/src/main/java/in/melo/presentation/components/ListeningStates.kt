package `in`.melo.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import `in`.melo.R
import `in`.melo.ui.theme.MeloLayout

@Composable
fun ListeningStatusCard(title: String, message: String, action: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = MeloLayout.CardShape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun ListeningShelfPlaceholder(modifier: Modifier = Modifier, label: String? = null) {
    Column(modifier.padding(horizontal = MeloLayout.PageGutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label ?: stringResource(R.string.melo_loading_library), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1f).clip(MeloLayout.ArtworkShape))
                    ShimmerBox(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(MeloLayout.ArtworkShape))
                    ShimmerBox(Modifier.fillMaxWidth(0.55f).height(10.dp).clip(MeloLayout.ArtworkShape))
                }
            }
        }
    }
}
