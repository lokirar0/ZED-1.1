package com.zed.app.feature.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.feature.habits.components.HabitCard
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedSpacing

@Composable
fun HabitsArchiveScreen(
    onBack: () -> Unit,
    viewModel: HabitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(R.string.habits_archive_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        if (state.loaded && state.archivedHabits.isEmpty()) {
            Text(
                text = stringResource(R.string.habits_archive_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(ZedSpacing.xl)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(ZedSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
            ) {
                items(state.archivedHabits, key = { it.id }) { item ->
                    HabitCard(
                        item = item,
                        onToggle = {},
                        onClick = null,
                        onArchive = { viewModel.setArchived(item.id, false) }
                    )
                }
            }
        }
    }
}
