package com.zed.app.core.notifications

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import com.zed.app.ui.theme.ZedTheme

// Полноэкранный будильник приёма: всплывает поверх блокировки,
// кнопки «ПОДОЖДАТЬ 5 МИН» / «ОТКЛЮЧИТЬ».
// ВИБРАЦИЯ В ТАКТ: пока экран открыт, вибрируем циклом тем же ритмом
// VIBRO_RHYTHM, что и звук канала — пульс-пульс-пульс… пауза.
class ZedAlarmActivity : ComponentActivity() {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        // Цикловая вибрация ритмом ZED (repeat = 0 → бесконечно до cancel)
        runCatching {
            vibrator.vibrate(VibrationEffect.createWaveform(NotificationHelper.VIBRO_RHYTHM, 0))
        }

        val title = intent.getStringExtra(ReminderScheduler.KEY_TITLE).orEmpty()
        val time = intent.getStringExtra(ReminderScheduler.KEY_TIME_LABEL).orEmpty()

        setContent {
            ZedTheme(darkTheme = true) {
                AlarmUi(
                    time = time,
                    title = title,
                    onSnooze = { send(ReminderScheduler.ACTION_SNOOZE) },
                    onDismiss = { send(ReminderScheduler.ACTION_DISMISS) }
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching { vibrator.cancel() }
        super.onDestroy()
    }

    // Шлём тот же payload в ресивер: он поставит snooze или засчитает приём
    private fun send(action: String) {
        runCatching { vibrator.cancel() }
        val i = Intent(this, ReminderReceiver::class.java).setAction(action)
        intent.extras?.let { i.putExtras(it) }
        sendBroadcast(i)
        finish()
    }
}

@Composable
private fun AlarmUi(
    time: String,
    title: String,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalZedColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = ZedSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            color = colors.accent
        )
        Spacer(Modifier.height(ZedSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textDisplay,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, colors.accent, RoundedCornerShape(ZedRadius.md))
        ) {
            Text(
                text = stringResource(R.string.alarm_snooze),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent
            )
        }
        Spacer(Modifier.height(ZedSpacing.md))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.md))
        ) {
            Text(
                text = stringResource(R.string.alarm_dismiss),
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.weight(0.5f))
    }
}
