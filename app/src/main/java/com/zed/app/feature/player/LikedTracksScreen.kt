package com.zed.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

// Экран «Избранное»: лайкнутые треки в стиле «ВСЕ ТРЕКИ»
@Composable
fun LikedTracksScreen(
    onBack: () -> Unit,
    onPlayed: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val liked = state.tracks.filter { it.id in state.favoriteIds }

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
                text = stringResource(R.string.player_liked_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        Text(
            text = stringResource(R.string.player_liked_title),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.sm)
        )

        if (liked.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("∅", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                Text(
                    text = stringResource(R.string.player_liked_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = ZedSpacing.md)
                )
            }
        } else {
            LazyColumn {
                itemsIndexed(liked, key = { _, t -> t.id }) { index, track ->
                    LikedCard(
                        track = track,
                        isCurrent = state.current?.id == track.id,
                        onClick = {
                            viewModel.playQueue(liked, index)
                            onPlayed()
                        }
                    )
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun LikedCard(track: AudioTrack, isCurrent: Boolean, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xs)
            .background(colors.surface, RoundedCornerShape(ZedRadius.md))
            .clickable(onClick = onClick)
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
                Text(
                    text = if (track.isUnknownTitle) stringResource(R.string.player_unknown_track) else track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) colors.accent else colors.textDisplay,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                text = "%d:%02d".format(track.durationMs / 60000, (track.durationMs / 1000) % 60),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }
    }
}
