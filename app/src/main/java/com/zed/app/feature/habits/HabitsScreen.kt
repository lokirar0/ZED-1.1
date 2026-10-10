package com.zed.app.feature.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.feature.habits.components.HabitCard
import com.zed.app.ui.components.ZedTopBar
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

@Composable
fun HabitsScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: (Int) -> Unit,
    onOpenStats: () -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.habits_deleted)
    val undoText = stringResource(R.string.undo)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(
                title = stringResource(R.string.tab_habits),
                onSettingsClick = onOpenSettings
            )

            // Строка статистики = кнопка входа в экран статистики
            if (state.todayTotal > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(ZedRadius.md),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, colors.borderVisible),
                    onClick = onOpenStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xxs)
                ) {
                    Row(
                        Modifier.padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.habits_stats,
                                state.todayDone,
                                state.todayTotal,
                                state.bestStreak
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.stats_title),
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
            }

            if (state.loaded && state.habits.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "∅",
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.textDisabled
                    )
                    Text(
                        text = stringResource(R.string.habits_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textDisplay,
                        modifier = Modifier.padding(top = ZedSpacing.lg)
                    )
                    Text(
                        text = stringResource(R.string.habits_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = ZedSpacing.sm)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(ZedSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
                ) {
                    items(state.habits, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        // Свайп = удаление со снимком; snackbar держит ОТМЕНУ,
                        // пока не скрыт или не нажата кнопка
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                viewModel.deleteWithSnapshot(item.id)
                                val result = snackbarHostState.showSnackbar(
                                    message = deletedText,
                                    actionLabel = undoText
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreLast()
                                }
                            }
                        }
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xl),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Outlined.Delete, null, tint = colors.accent)
                                }
                            },
                            content = {
                                HabitCard(
                                    item = item,
                                    onToggle = { viewModel.toggle(item.id) },
                                    onClick = { onOpenEditor(item.id) }
                                )
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onOpenEditor(-1) },
            shape = RoundedCornerShape(ZedRadius.md),
            containerColor = colors.accent,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(ZedSpacing.xl)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.habits_add))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
