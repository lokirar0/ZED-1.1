package com.zed.app.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// ============================================================
// «Все треки»: полный список библиотеки.
// ФИКС: лента открывается С ИГРАЮЩЕГО ТРЕКА (initialFirstVisibleItemIndex),
// а не с начала списка — листать вверх/вниз можно сразу от него.
// Тап — играть трек (очередь = вся библиотека) и вернуться в плеер.
// Долгий тап — шит «добавить в плейлист».
// ============================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllTracksScreen(
    onBack: () -> Unit,
    onPlayed: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    var playlistFor by remember { mutableStateOf<AudioTrack?>(null) }

    // Индекс играющего трека: от него стартует лента
    val currentIdx = state.tracks.indexOfFirst { it.id == state.current?.id }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = currentIdx.coerceAtLeast(0)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Верхний бар
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

        if (state.loaded && state.tracks.isEmpty()) {
            // Пустая библиотека
            Column(
                Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("∅", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                Text(
                    text = stringResource(R.string.player_queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = ZedSpacing.md)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(ZedSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
            ) {
                itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        index = index,
                        track = track,
                        isCurrent = track.id == state.current?.id,
                        onClick = {
                            viewModel.playTrack(index)
                            onPlayed()
                        },
                        onLongClick = { playlistFor = track }
                    )
                }
            }
        }
    }

    // --- Шит: добавить трек в плейлист ---
    playlistFor?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { playlistFor = null },
            containerColor = colors.surface
        ) {
            Text(
                text = stringResource(R.string.player_playlists_title),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textDisabled,
                modifier = Modifier.padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.sm)
            )
            if (state.playlists.isEmpty()) {
                Text(
                    text = "∅",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = ZedSpacing.xl)
                )
            }
            state.playlists.forEach { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                viewModel.addToPlaylist(pl.id, track.id)
                                playlistFor = null
                            }
                        )
                        .padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pl.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = pl.trackIds.size.toString(),
                        style = ZedDataNumber,
                        color = colors.textDisabled
                    )
                }
            }
            Spacer(Modifier.height(ZedSpacing.xl))
        }
    }
}

// Строка трека: номер, название, артист; играющий подсвечен красным
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    index: Int,
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
        border = BorderStroke(1.dp, if (isCurrent) colors.accent else colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(ZedSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "%02d".format(index + 1),
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) colors.accent else colors.textDisabled,
                modifier = Modifier.width(28.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (track.isUnknownTitle) stringResource(R.string.player_unknown_track) else track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) colors.accent else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.artist.isNotBlank()) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
