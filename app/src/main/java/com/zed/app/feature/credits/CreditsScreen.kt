package com.zed.app.feature.credits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
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
import com.zed.app.ui.components.ZedTopBar
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

@Composable
fun CreditsScreen(
    onOpenSettings: () -> Unit,
    onOpenEditor: (Int) -> Unit,
    viewModel: CreditsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedText = stringResource(R.string.credits_deleted)
    val undoText = stringResource(R.string.undo)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ZedTopBar(title = stringResource(R.string.tab_credits), onSettingsClick = onOpenSettings)

            if (state.loaded && state.credits.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(ZedSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("∅", style = MaterialTheme.typography.displayMedium, color = colors.textDisabled)
                    Text(
                        text = stringResource(R.string.credits_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textDisplay,
                        modifier = Modifier.padding(top = ZedSpacing.lg)
                    )
                    Text(
                        text = stringResource(R.string.credits_empty_body),
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
                    item { BurdenCard(burdenText = state.burdenText, activeCount = state.activeCount) }
                    items(state.credits, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        // Свайп = удаление со снимком + snackbar с ОТМЕНОЙ
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
                                ) { Icon(Icons.Outlined.Delete, null, tint = colors.accent) }
                            },
                            content = {
                                CreditCard(
                                    item = item,
                                    onTogglePaid = { paid -> viewModel.togglePaid(item.id, paid) },
                                    onOpen = { onOpenEditor(item.id) }
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
            Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.credits_add))
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BurdenCard(burdenText: String, activeCount: Int) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(ZedSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.credits_burden),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(ZedSpacing.xs))
                Text(text = burdenText, style = ZedDataNumber, color = colors.textDisplay)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.credits_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(ZedSpacing.xs))
                Text(text = activeCount.toString(), style = ZedDataNumber, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun CreditCard(
    item: CreditUiItem,
    onTogglePaid: (Boolean) -> Unit,
    onOpen: () -> Unit
) {
    val colors = LocalZedColors.current

    val countdown = when {
        item.paidThisMonth -> stringResource(R.string.credits_paid)
        item.days < 0 -> stringResource(R.string.credits_overdue)
        else -> stringResource(R.string.credits_due, item.days.toInt())
    }
    val countdownColor = when {
        item.paidThisMonth -> colors.success
        item.countdownDanger || item.days < 0 -> colors.accent
        else -> colors.textPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            Modifier.padding(ZedSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(ZedSpacing.xs))
                Text(text = item.monthlyText, style = ZedDataNumber, color = colors.textSecondary)
                Spacer(Modifier.height(ZedSpacing.xs))
                Text(
                    text = stringResource(R.string.credits_payday, item.payDay),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textDisabled
                )
            }
            Spacer(Modifier.width(ZedSpacing.md))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = countdown, style = ZedDataNumber, color = countdownColor)
                Spacer(Modifier.height(ZedSpacing.sm))
                PaidBox(paid = item.paidThisMonth) { onTogglePaid(!item.paidThisMonth) }
            }
        }
    }
}

@Composable
private fun PaidBox(paid: Boolean, onClick: () -> Unit) {
    val colors = LocalZedColors.current
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                if (paid) Modifier.background(colors.success, RoundedCornerShape(ZedRadius.sm))
                else Modifier.border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.sm))
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (paid) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
