package com.zed.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.zed.app.R
import com.zed.app.ui.theme.LocalZedColors
import com.zed.app.ui.theme.ZedRadius
import com.zed.app.ui.theme.ZedSpacing
import kotlinx.coroutines.delay

// Снимок всех статусов разрешений
private data class PermStatus(
    val notif: Boolean,
    val music: Boolean,
    val alarm: Boolean,
    val battery: Boolean,
    val fullscreen: Boolean
)

// Экран первичных разрешений после онбординга.
// ГЛАВНОЕ: статусы читаются ВНУТРИ remember(refresh) — чтение refresh
// заставляет Compose перерисовать экран на каждый тик (2 Гц) и при возврате.
// Без этого чтения «НЕТ» никогда не превращается в «ДА».
@Composable
fun PermissionsGate(onDone: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalZedColors.current

    var refresh by remember { mutableIntStateOf(0) }

    // Опрос статусов каждые 500 мс, пока экран открыт
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            refresh++
        }
    }
    // Перечитывание при возврате из системных экранов
    DisposableEffect(Unit) {
        val owner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    // ЧТЕНИЕ refresh внутри remember → рекомпозиция на каждый тик
    val st = remember(refresh) {
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?.areNotificationsEnabled() ?: true
        }
        val music = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
        val alarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        val battery = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
        val fullscreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            ContextCompat.getSystemService(context, NotificationManager::class.java)
                ?.canUseFullScreenIntent() ?: true
        PermStatus(notif, music, alarm, battery, fullscreen)
    }

    // Если системный диалог подавлен — открываем карточку приложения
    fun openAppDetails() {
        runCatching {
            context.startActivity(
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + context.packageName)
                )
            )
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        refresh++
        if (!ok) openAppDetails()
    }
    val musicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        refresh++
        if (!ok) openAppDetails()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(ZedSpacing.xl)
    ) {
        Spacer(Modifier.height(ZedSpacing.xl))
        Text(
            text = stringResource(R.string.perm_title),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textDisplay
        )
        Spacer(Modifier.height(ZedSpacing.md))
        Text(
            text = stringResource(R.string.perm_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(ZedSpacing.xl))

        PermRow(
            title = stringResource(R.string.perm_notifications),
            granted = st.notif,
            onGrant = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        )
        PermRow(
            title = stringResource(R.string.perm_music),
            granted = st.music,
            onGrant = {
                val p = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                musicLauncher.launch(p)
            }
        )
        PermRow(
            title = stringResource(R.string.perm_alarms),
            granted = st.alarm,
            onGrant = {
                runCatching {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            }
        )
        PermRow(
            title = stringResource(R.string.perm_fullscreen),
            granted = st.fullscreen,
            onGrant = {
                runCatching {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            }
        )
        PermRow(
            title = stringResource(R.string.perm_battery),
            granted = st.battery,
            onGrant = {
                runCatching {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            }
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(ZedSpacing.xl))

        TextButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, colors.accent, RoundedCornerShape(ZedRadius.md))
        ) {
            Text(
                text = stringResource(R.string.perm_done),
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent
            )
        }
    }
}

// Строка разрешения: название + статус ДА/НЕТ + кнопка ВЫДАТЬ
@Composable
private fun PermRow(title: String, granted: Boolean, onGrant: () -> Unit) {
    val colors = LocalZedColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = ZedSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (granted) stringResource(R.string.perm_yes) else stringResource(R.string.perm_no),
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) colors.success else colors.accent
        )
        if (!granted) {
            TextButton(
                onClick = onGrant,
                modifier = Modifier
                    .padding(start = ZedSpacing.md)
                    .border(1.dp, colors.borderVisible, RoundedCornerShape(ZedRadius.sm))
            ) {
                Text(
                    text = stringResource(R.string.perm_grant),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}
