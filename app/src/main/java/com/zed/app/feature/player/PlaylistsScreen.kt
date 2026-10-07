package com.zed.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Экран «Плейлисты»: список, создание, воспроизведение, удаление
@Composable
fun PlaylistsScreen(
    onBack: () -> Unit,
    onPlayed: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    var newName by remember { mutableStateOf("") }

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
                text = stringResource(R.string.player_playlists_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        Column(Modifier.padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md)) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.player_new_list)) },
                singleLine = true,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderVisible,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.textSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(ZedSpacing.sm))
            TextButton(
                onClick = {
                    if (newName.trim().isNotEmpty()) {
                        viewModel.createPlaylist(newName.trim())
                        newName = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(colors.accent, RoundedCornerShape(ZedRadius.md))
            ) {
                Text(
                    text = stringResource(R.string.player_create_playlist),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        LazyColumn {
            items(state.playlists, key = { it.id }) { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xs)
                        .background(colors.surface, RoundedCornerShape(ZedRadius.md))
                        .clickable {
                            viewModel.playPlaylist(pl)
                            onPlayed()
                        }
                        .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = pl.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textDisplay
                        )
                        Text(
                            text = pl.trackIds.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = {
                        viewModel.playPlaylist(pl)
                        onPlayed()
                    }) {
                        Icon(Icons.Outlined.PlayArrow, null, tint = colors.textSecondary)
                    }
                    IconButton(onClick = { viewModel.deletePlaylist(pl.id) }) {
                        Icon(Icons.Outlined.Delete, null, tint = colors.warning)
                    }
                }
                Spacer(Modifier.height(ZedSpacing.xs))
            }
        }
    }
}
