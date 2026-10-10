package com.zed.app.feature.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zed.app.R
import com.zed.app.core.domain.model.AudioTrack
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

private enum class Sheet { NONE, QUEUE, FX, MORE, LYRICS }

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

// Полноэкранный плеер. Haptic глобальный (MainActivity), здесь без ручных вызовов.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAllTracks: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenLiked: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val context = LocalContext.current
    var sheet by remember { mutableStateOf(Sheet.NONE) }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (ok) viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        if (granted) viewModel.refresh()
        else permissionLauncher.launch(audioPermission())
    }

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
                text = stringResource(R.string.tab_player),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.weight(1f).padding(start = ZedSpacing.sm)
            )
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Outlined.Search, null, tint = colors.textSecondary)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, null, tint = colors.textSecondary)
            }
        }

        Spacer(Modifier.weight(0.2f))

        // Обложка + dot-matrix оверлей
        Box(
            Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(1f)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .align(Alignment.CenterHorizontally)
        ) {
            AsyncImage(
                model = state.current?.artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
            DotMatrixOverlay(Modifier.matchParentSize())
        }

        Spacer(Modifier.height(ZedSpacing.xl))

        // Название + артист + лайк
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ZedSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                val cur = state.current
                if (cur == null) {
                    Text(
                        text = stringResource(R.string.player_queue_empty),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDisabled
                    )
                } else {
                    Text(
                        text = if (cur.isUnknownTitle) stringResource(R.string.player_unknown_track) else cur.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textDisplay,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (cur.artist.isNotBlank()) {
                        Spacer(Modifier.height(ZedSpacing.xxs))
                        Text(
                            text = cur.artist,
                            style = ZedDataNumber,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    if (state.current?.id in state.favoriteIds) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    null,
                    tint = if (state.current?.id in state.favoriteIds) colors.accent else colors.textSecondary
                )
            }
        }

        Spacer(Modifier.height(ZedSpacing.lg))

        // Слайдер прогресса
        Slider(
            value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.borderVisible
            ),
            modifier = Modifier.padding(horizontal = ZedSpacing.xl)
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xl)) {
            Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }

        Spacer(Modifier.height(ZedSpacing.lg))

        // Ряд управления
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ZedSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Outlined.Shuffle, null,
                tint = if (state.shuffle) colors.accent else colors.textDisabled,
                modifier = Modifier.size(22.dp).clickable { viewModel.toggleShuffle() }
            )
            Icon(
                Icons.Outlined.SkipPrevious, null,
                tint = colors.textPrimary,
                modifier = Modifier.size(32.dp).clickable { viewModel.previous() }
            )
            Box(
                Modifier
                    .size(64.dp)
                    .background(colors.accent, RoundedCornerShape(16.dp))
                    .clickable { viewModel.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Icon(
                Icons.Outlined.SkipNext, null,
                tint = colors.textPrimary,
                modifier = Modifier.size(32.dp).clickable { viewModel.next() }
            )
            Icon(
                if (state.repeatMode == 1) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                null,
                tint = if (state.repeatMode == 0) colors.textDisabled else colors.accent,
                modifier = Modifier.size(22.dp).clickable { viewModel.cycleRepeat() }
            )
        }

        Spacer(Modifier.weight(1f))

        // Нижняя панель: ⚙ | EQ | 📝 |  | ⋮
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Outlined.Settings, null, tint = colors.textSecondary,
                modifier = Modifier.size(22.dp).clickable(onClick = onOpenSettings))
            Icon(Icons.Outlined.GraphicEq, null, tint = colors.textSecondary,
                modifier = Modifier.size(22.dp).clickable { sheet = Sheet.FX })
            Icon(Icons.Outlined.Notes, null, tint = colors.textSecondary,
                modifier = Modifier.size(22.dp).clickable { sheet = Sheet.LYRICS })
            Icon(Icons.Outlined.QueueMusic, null, tint = colors.textSecondary,
                modifier = Modifier.size(22.dp).clickable { sheet = Sheet.QUEUE })
            Icon(Icons.Outlined.MoreVert, null, tint = colors.textSecondary,
                modifier = Modifier.size(22.dp).clickable { sheet = Sheet.MORE })
        }
    }

    // --- Bottom sheets ---
    when (sheet) {
        Sheet.FX -> FxBottomSheet(
            speed = state.speed,
            reverb = state.reverb,
            eqGains = state.eqGains,
            onSpeed = viewModel::setSpeed,
            onReverb = viewModel::setReverb,
            onEqBand = viewModel::setEqBand,
            onPreset = viewModel::applyEqPreset,
            onReset = viewModel::resetFx,
            onDismiss = { sheet = Sheet.NONE }
        )
        Sheet.QUEUE -> QueueSheet(
            queue = state.queue,
            queueIndex = state.queueIndex,
            onPlayAt = { i -> viewModel.playAt(i); sheet = Sheet.NONE },
            onPlayNext = { id -> viewModel.playNextById(id) },
            onMove = { id, steps -> viewModel.moveBySteps(id, steps) },
            onDismiss = { sheet = Sheet.NONE }
        )
        Sheet.LYRICS -> ModalBottomSheet(
            onDismissRequest = { sheet = Sheet.NONE },
            containerColor = colors.surface
        ) {
            SheetTitle(stringResource(R.string.player_lyrics))
            Text(
                stringResource(R.string.player_lyrics_none),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(ZedSpacing.xl)
            )
            Spacer(Modifier.height(ZedSpacing.xl))
        )
        Sheet.MORE -> ModalBottomSheet(
            onDismissRequest = { sheet = Sheet.NONE },
            containerColor = colors.surface
        ) {
            SheetTitle(stringResource(R.string.player_more))
            MoreRow(stringResource(R.string.player_eq_reverb)) { sheet = Sheet.FX }
            MoreRow(stringResource(R.string.player_all_tracks)) { sheet = Sheet.NONE; onOpenAllTracks() }
            MoreRow(stringResource(R.string.player_playlists_title)) { sheet = Sheet.NONE; onOpenPlaylists() }
            MoreRow(stringResource(R.string.player_liked_title)) { sheet = Sheet.NONE; onOpenLiked() }
            Spacer(Modifier.height(ZedSpacing.xl))
        }
        Sheet.NONE -> Unit
    }
}

// Очередь: тап — играть, долгий тап + драг — переместить, кнопки + / ↑ / ↓
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<AudioTrack>,
    queueIndex: Int,
    onPlayAt: (Int) -> Unit,
    onPlayNext: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalZedColors.current
    val density = LocalDensity.current
    val rowPx = with(density) { 56.dp.toPx() }

    var draggedId by remember { mutableStateOf<Long?>(null) }
    var acc by remember { mutableStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        SheetTitle(stringResource(R.string.player_queue))
        Text(
            text = stringResource(R.string.player_queue_hint),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textDisabled,
            modifier = Modifier.padding(horizontal = ZedSpacing.xl)
        )
        Spacer(Modifier.height(ZedSpacing.sm))

        LazyColumn {
            itemsIndexed(queue, key = { _, t -> t.id }) { index, track ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onPlayAt(index) }
                        .pointerInput(track.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = track.id
                                    acc = 0f
                                },
                                onDragEnd = { draggedId = null; acc = 0f },
                                onDragCancel = { draggedId = null; acc = 0f }
                            ) { change, dragAmount ->
                                change.consume()
                                acc += dragAmount.y
                                val steps = (acc / rowPx).toInt()
                                if (steps != 0 && draggedId != null) {
                                    onMove(draggedId!!, steps)
                                    acc -= steps * rowPx
                                }
                            }
                        }
                        .padding(horizontal = ZedSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d".format(index + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == queueIndex) colors.accent else colors.textDisabled,
                        modifier = Modifier.width(28.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (track.isUnknownTitle) stringResource(R.string.player_unknown_track) else track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == queueIndex) colors.accent else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onPlayNext(track.id) }) {
                        Icon(Icons.Outlined.PlaylistAdd, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onMove(track.id, -1) }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowUp, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onMove(track.id, 1) }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowDown, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(ZedSpacing.xl))
    }
}

@Composable
private fun SheetTitle(text: String) {
    val colors = LocalZedColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.textDisabled,
        modifier = Modifier.padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.sm)
    )
}

@Composable
private fun MoreRow(label: String, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = colors.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = ZedSpacing.xl, vertical = ZedSpacing.md)
    )
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
