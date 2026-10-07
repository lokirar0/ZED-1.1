package com.zed.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlinx.coroutines.launch

// Онбординг: 4 экрана в стиле Nothing — много воздуха, точечные цифры
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val colors = LocalZedColors.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val titles = listOf(
        stringResource(R.string.onb1_title),
        stringResource(R.string.onb2_title),
        stringResource(R.string.onb3_title),
        stringResource(R.string.onb4_title)
    )
    val bodies = listOf(
        stringResource(R.string.onb1_body),
        stringResource(R.string.onb2_body),
        stringResource(R.string.onb3_body),
        stringResource(R.string.onb4_body)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(ZedSpacing.xl)
    ) {
        // Пропустить
        TextButton(onClick = { viewModel.finish(); onFinished() }) {
            Text(
                text = stringResource(R.string.onb_skip),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                // Номер экрана точечным шрифтом
                Text(
                    text = "0${page + 1}",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.accent
                )
                Spacer(Modifier.height(ZedSpacing.lg))
                Text(
                    text = titles[page],
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textDisplay
                )
                Spacer(Modifier.height(ZedSpacing.md))
                Text(
                    text = bodies[page],
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary
                )
            }
        }

        // Индикатор: квадраты вместо точек (Glyph-матрица)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .size(8.dp)
                        .padding(1.dp)
                        .then(
                            if (index == pagerState.currentPage)
                                Modifier.background(colors.accent)
                            else
                                Modifier.border(1.dp, colors.borderVisible)
                        )
                )
                Spacer(Modifier.size(ZedSpacing.sm))
            }
        }

        Spacer(Modifier.height(ZedSpacing.xl))

        // Далее / Начать: прозрачная кнопка с акцентной обводкой
        val isLast = pagerState.currentPage == 3
        TextButton(
            onClick = {
                if (isLast) {
                    viewModel.finish()
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, colors.accent, RoundedCornerShape(ZedRadius.md))
        ) {
            Text(
                text = if (isLast) stringResource(R.string.onb_start) else stringResource(R.string.onb_next),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent
            )
        }
    }
}
