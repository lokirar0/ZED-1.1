package com.zed.app.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
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

// Список треков в стиле карточек «ПРИВЫЧЕК»: рамка 1dp, моно-цифры,
// квадрат-индикатор справа (как чекбокс), воздушные зазоры вместо разделителей.
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
        // Шапка: назад + заголовок Doto (как на вкладке привычек)
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
                modifier = Modifier
                    .weight(1f)
                    .padding(start = ZedSpacing.sm)
            )
        }

        // Карточка-сводка (стиль «СЕГОДНЯ 1/2 · РЕКОРД 1 → СТАТИСТИКА»):
        // количество треков + действие «Обновить»
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(ZedRadius.md),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, colors.borderVisible),
            onClick = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xxs)
        ) {
            Row(
                Modifier.padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.player_tracks_count, state.tracks.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.player_rescan),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    null,
                    tint = colors.textDisabled
                )
            }
        }

        Spacer(Modifier.height(ZedSpacing.sm))

        // Карточки треков: зазоры вместо разделителей, как в привычках
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xxs),
            verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
        ) {
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

// Карточка трека = карточка привычки:
// строка 1 — название (Space Grotesk, белым),
// строка 2 — артист серым моно + длительность красным моно (как «СЕРИЯ: 1  19:59»),
// справа — квадрат-индикатор: у текущего красный с белым треугольником (как чекбокс).
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackCard(
    track: AudioTrack,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            Modifier.padding(ZedSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                // Название
                Text(
                    text = if (track.isUnknownTitle) {
                        stringResource(R.string.player_unknown_track)
                    } else {
                        track.title
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        track.isUnknownTitle -> colors.textDisabled
                        isCurrent -> colors.accent
                        else -> colors.textDisplay
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(ZedSpacing.xs))
                // Вторая строка: артист серым моно + длительность красным моно
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDuration(track.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent
                    )
                }
            }
            Spacer(Modifier.width(ZedSpacing.md))
            // Квадрат-индикатор (как чекбокс привычки)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (isCurrent) {
                            Modifier.background(colors.accent, RoundedCornerShape(ZedRadius.sm))
                        } else {
                            Modifier.border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.sm))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
