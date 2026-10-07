package com.zed.app.feature.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    // Строку получаем ЗДЕСЬ: внутри LaunchedEffect stringResource() вызывать нельзя
    val deletedText = stringResource(R.string.habits_deleted)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(
                title = stringResource(R.string.tab_habits),
                onSettingsClick = onOpenSettings
            )

            // Строка статистики: СЕГОДНЯ x/y · РЕКОРД n
            if (state.todayTotal > 0) {
                Text(
                    text = stringResource(R.string.habits_stats, state.todayDone, state.todayTotal, state.bestStreak),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xs)
                )
            }

            if (state.loaded && state.habits.isEmpty()) {
                // Пустое состояние
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
                        // Свайп в любую сторону = удаление
                        val dismissState = rememberSwipeToDismissBoxState()
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                viewModel.delete(item.id)
                                snackbarHostState.showSnackbar(deletedText)
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
                                HabitCard(item = item, onToggle = { viewModel.toggle(item.id) })
                            }
                        )
                    }
                }
            }
        }

        // FAB: красный квадрат со скруглением 8dp
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
