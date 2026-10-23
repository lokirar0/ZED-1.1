package com.zed.app.core.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.zed.app.MainActivity
import com.zed.app.R
import com.zed.app.feature.player.ZedPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// Виджет «ZED · PLAYER»: обложка + трек + play/pause + next.
// Читает состояние напрямую из MediaSession через MediaController
// (работает даже когда приложение убито). Кнопки шлют broadcast
// сюда же и управляют сессией: playPause / seekToNextMediaItem.
// Обновление: кнопки, выход из приложения (рефрешер), цикл 30 мин.
// ============================================================
class PlayerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "zed.widget.PLAYER_TOGGLE"
        const val ACTION_NEXT = "zed.widget.PLAYER_NEXT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE, ACTION_NEXT -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val controller = connect(context)
                        try {
                            if (controller != null) {
                                if (intent.action == ACTION_TOGGLE) controller.playPause()
                                else controller.seekToNextMediaItem()
                            }
                        } finally {
                            controller?.release()
                        }
                        // Даём сессии применить команду и перерисовываем виджет
                        delay(300)
                        updateNow(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    // --- приватное ---

    private fun connect(context: Context): MediaController? = runCatching {
        val token = SessionToken(context, ComponentName(context, ZedPlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync().get()
    }.getOrNull()

    private fun updateNow(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
        if (ids.isNotEmpty()) render(context, mgr, ids)
    }

    private fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val controller = connect(context)
        try {
            val meta = controller?.currentMediaItem?.mediaMetadata
            val playing = controller?.isPlaying ?: false
            val hasTrack = meta != null

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_player)

                if (hasTrack) {
                    views.setViewVisibility(R.id.widget_player_art, View.VISIBLE)
                    views.setTextViewText(
                        R.id.widget_player_title,
                        meta?.title?.toString() ?: context.getString(R.string.widget_player_idle)
                    )
                    views.setTextViewText(
                        R.id.widget_player_artist,
                        meta?.artist?.toString() ?: "ZED"
                    )
                    val bmp = loadArt(context, controller)
                    if (bmp != null) {
                        views.setImageViewBitmap(R.id.widget_player_art, bmp)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_player_art, View.GONE)
                    views.setTextViewText(
                        R.id.widget_player_title,
                        context.getString(R.string.widget_player_idle)
                    )
                    views.setTextViewText(R.id.widget_player_artist, "ZED · PLAYER")
                }

                views.setImageViewResource(
                    R.id.widget_player_play,
                    if (playing) R.drawable.widget_ic_pause else R.drawable.widget_ic_play
                )
                views.setOnClickPendingIntent(
                    R.id.widget_player_play,
                    broadcastPi(context, ACTION_TOGGLE, 11)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_player_next,
                    broadcastPi(context, ACTION_NEXT, 12)
                )
                views.setOnClickPendingIntent(R.id.widget_player_root, openAppPi(context))
                mgr.updateAppWidget(id, views)
            }
        } finally {
            controller?.release()
        }
    }

    // Обложка из artworkUri медиасессии (content://media/...)
    private fun loadArt(context: Context, controller: MediaController?): Bitmap? {
        val uri = controller?.currentMediaItem?.mediaMetadata?.artworkUri ?: return null
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Третий аргумент (CancellationSignal) обязателен в Kotlin — null
                context.contentResolver.loadThumbnail(uri, android.util.Size(256, 256), null)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        }.getOrNull()
    }

    private fun broadcastPi(context: Context, action: String, req: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            req,
            Intent(context, PlayerWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun openAppPi(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            13,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
