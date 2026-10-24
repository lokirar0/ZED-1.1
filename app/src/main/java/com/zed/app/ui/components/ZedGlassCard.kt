package com.zed.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedSpacing

// ============================================================
// Стеклянная карточка в стиле Nothing OS (как на макетах):
// мягкий вертикальный градиент «матовое стекло», тонкая светлая
// кромка, скругление 20dp. В тёмной теме — серое стекло на
// True Black, в светлой — белое с лёгкой тенью кромки.
// Все функции экранов остаются прежними: карточка — только обёртка.
// ============================================================
@Composable
fun ZedGlassCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalZedColors.current
    val dark = colors.background.red < 0.5f
    val gradient = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color(0xFF1C1C1C)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFDCDCDC)))
    }
    val edge = if (dark) Color(0x26FFFFFF) else Color(0x33000000)
    val titleColor = if (dark) Color(0xFFBFBFBF) else Color(0xFF555555)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(gradient, RoundedCornerShape(20.dp))
            .border(1.dp, edge, RoundedCornerShape(20.dp))
            .padding(ZedSpacing.lg)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = titleColor,
                modifier = Modifier.padding(bottom = ZedSpacing.sm)
            )
        }
        content()
    }
}
