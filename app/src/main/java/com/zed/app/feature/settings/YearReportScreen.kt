package com.zed.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedDataNumber
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing

// Годовой отчёт: крупные точечные цифры, монохром + красный акцент
@Composable
fun YearReportScreen(
    onBack: () -> Unit,
    viewModel: YearReportViewModel = hiltViewModel()
) {
    val r by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalZedColors.current

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ZedSpacing.xs, vertical = ZedSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.textSecondary)
            }
            Text(
                text = stringResource(R.string.report_title) + " · " + r.year,
                style = MaterialTheme.typography.displaySmall,
                color = colors.textDisplay,
                modifier = Modifier.padding(start = ZedSpacing.sm)
            )
        }

        Column(
            Modifier.padding(ZedSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZedSpacing.md)
        ) {
            ReportCard(stringResource(R.string.report_marks), r.marksTotal.toString(), null)
            ReportCard(stringResource(R.string.report_best_streak), r.bestStreak.toString(), r.bestStreakHabit)
            ReportCard(stringResource(R.string.report_best_day), r.bestWeekday, null)
            ReportCard(stringResource(R.string.report_expense), r.expenseTotal, null)
            ReportCard(stringResource(R.string.report_income), r.incomeTotal, null)
            ReportCard(stringResource(R.string.report_top_cat), r.topCategorySum, r.topCategory)
            ReportCard(stringResource(R.string.report_pricey_month), r.priceyMonth, null)
            ReportCard(stringResource(R.string.report_credits), r.creditsPaid.toString(), null)
            ReportCard(stringResource(R.string.report_liked), r.likedTracks.toString(), null)
            ReportCard(stringResource(R.string.report_playlists), r.playlists.toString(), null)
        }
    }
}

// Карточка показателя: лейбл моно-серый, число крупное точечное, подпись красная
@Composable
private fun ReportCard(label: String, value: String, sub: String?) {
    val colors = LocalZedColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(ZedRadius.md),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, colors.borderVisible),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ZedSpacing.lg)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textDisabled)
            Spacer(Modifier.height(ZedSpacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = colors.textDisplay
            )
            if (sub != null) {
                Spacer(Modifier.height(ZedSpacing.xxs))
                Text(sub, style = ZedDataNumber, color = colors.accent)
            }
        }
    }
}
