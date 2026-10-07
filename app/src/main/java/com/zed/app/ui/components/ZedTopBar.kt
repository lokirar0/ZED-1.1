package com.zed.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zed.app.ui.navigation.LocalZedActions
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedSpacing

// Верхняя панель в стиле Nothing: заголовок Doto слева, тонкие иконки справа.
// Иконка поиска есть на всех вкладках автоматически (через LocalZedActions).
@Composable
fun ZedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    val colors = LocalZedColors.current
    val actions = LocalZedActions.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ZedSpacing.lg, vertical = ZedSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = colors.textDisplay,
            modifier = Modifier.weight(1f)
        )
        // Единый поиск по приложению
        IconButton(onClick = actions.openSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.textSecondary
            )
        }
        if (onSettingsClick != null) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }
        }
    }
}
