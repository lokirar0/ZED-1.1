package com.zed.app.feature.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.core.domain.model.AudioTrack
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Полноэкранный список треков. Фикс вёрстки: текст с weight(1f) + ellipsis,
// длительность справа с отступом. Долгое нажатие → «Добавить в плейлист».
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllTracksScreen(
    onBack: () -> Unit,
    onPlayed: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    var addToTrack by remember { mutableStateOf<AudioTrack?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(R.string.player_all_tracks),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        Text(
            text = stringResource(R.string.player_all_tracks),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.sm)
        )

        LazyColumn {
            itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                TrackCard(
                    track = track,
                    isCurrent = state.current?.id == track.id,
                    onClick = {
                        viewModel.playTrack(index)
                        onPlayed()
                    },
                    onLongClick = { addToTrack = track }
                )
                HorizontalDivider(color = colors.border, thickness = 1.dp)
            }
        }
    }

    // Долгое нажатие → «Добавить в плейлист»
    addToTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { addToTrack = null },
            containerColor = colors.surface,
            title = { Text(stringResource(R.string.player_add_to_playlist), color = colors.textDisplay) },
            text = {
                Column {
                    if (state.playlists.isEmpty()) {
                        Text(
                            stringResource(R.string.player_playlists_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    state.playlists.forEach { pl ->
                        Text(
                            text = pl.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = {
                                    viewModel.addToPlaylist(pl.id, track.id)
                                    addToTrack = null
                                })
                                .padding(vertical = ZedSpacing.sm)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addToTrack = null }) {
                    Text(
                        stringResource(R.string.cat_dialog_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}

// Карточка трека: фон #111111, 8dp, красная полоса 3dp у активного
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackCard(
    track: AudioTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalZedColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xs)
            .background(colors.surface, RoundedCornerShape(ZedRadius.md))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (isCurrent) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(colors.accent)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                if (track.isUnknownTitle) {
                    Text(
                        text = stringResource(R.string.player_unknown_track),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrent) colors.accent else colors.textDisplay,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (track.artist.isNotBlank()) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(ZedSpacing.md))
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
