package com.radio.agilesouthwest.kmpradioplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radio.agilesouthwest.kmpradioplayer.media.PlaybackState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayerSheetContent(
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.playbackState.collectAsState()
    val station = state.currentStation ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = station.favicon,
            contentDescription = null,
            modifier = Modifier
                .size(240.dp)
                .clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(Icons.Default.Radio),
            error = rememberVectorPainter(Icons.Default.Radio)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = station.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1
        )
        Text(
            text = station.tags ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isSeekable) {
            Slider(
                value = state.currentPosition.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.small)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.skipBackward() }) {
                Icon(
                    imageVector = if (state.isSeekable) Icons.Default.Replay5 else Icons.Default.SkipPrevious,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = { viewModel.togglePlayback() },
                modifier = Modifier.size(64.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            IconButton(onClick = { viewModel.skipForward() }) {
                Icon(
                    imageVector = if (state.isSeekable) Icons.Default.Forward5 else Icons.Default.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel = koinViewModel(),
    onClick: () -> Unit
) {
    val state by viewModel.playbackState.collectAsState()
    val station = state.currentStation ?: return

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = station.favicon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Default.Radio),
                error = rememberVectorPainter(Icons.Default.Radio)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (state.isLoading) {
                    Text("Buffering...", style = MaterialTheme.typography.bodySmall)
                }
            }

            IconButton(onClick = { viewModel.togglePlayback() }) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}
