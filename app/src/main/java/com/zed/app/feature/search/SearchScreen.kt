package com.zed.app.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

// Единый поиск: привычки, финансы, кредиты, музыка.
// Тап по музыке = сразу воспроизвести трек (очередь = вся библиотека).
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToTab: (String) -> Unit,
    onPlayTrack: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    val groups = listOf(
        "habits" to R.string.tab_habits,
        "finance" to R.string.tab_finance,
        "credits" to R.string.tab_credits,
        "player_screen" to R.string.tab_player
    )

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textDisabled
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(ZedRadius.md),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.borderVisible,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = colors.accent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                modifier = Modifier.weight(1f).padding(end = ZedSpacing.lg)
            )
        }

        when {
            state.query.isBlank() -> Column(
                Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⌕", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                Text(
                    text = stringResource(R.string.search_scope),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDisabled,
                    modifier = Modifier.padding(top = ZedSpacing.md)
                )
            }

            state.results.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("∅", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                Text(
                    text = stringResource(R.string.search_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = ZedSpacing.md)
                )
            }

            else -> LazyColumn {
                groups.forEach { (route, titleRes) ->
                    val group = state.results.filter { it.tabRoute == route }
                    if (group.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textDisabled,
                                modifier = Modifier.padding(
                                    horizontal = ZedSpacing.lg,
                                    vertical = ZedSpacing.sm
                                )
                            )
                        }
                        items(group) { result ->
                            SearchResultRow(result) {
                                // Музыка играет сразу, остальное ведёт на вкладку
                                if (result.trackId != -1L) onPlayTrack(result.trackId)
                                else onNavigateToTab(result.tabRoute)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchItem, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.xxs)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(ZedSpacing.md)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
