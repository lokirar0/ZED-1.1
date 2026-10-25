package com.zed.app.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================
// СМЕНЯЕМЫЕ ИКОНКИ НИЖНЕГО МЕНЮ.
// Набор 0 = ЛИНИИ (Material outlined), набор 1 = ТОЧКИ (dot-matrix 5×5).
// Выбор хранится в SharedPreferences и живёт как Compose State —
// переключение в настройках мгновенно перерисовывает меню.
// ============================================================
object NavIconStore {

    private const val PREFS = "zed_ui_prefs"
    private const val KEY = "nav_icon_set"

    private var state: MutableState<Int>? = null

    fun observe(context: Context): State<Int> {
        val current = state
        if (current != null) return current
        val initial = mutableIntStateOf(get(context))
        state = initial
        return initial
    }

    fun get(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0)

    fun set(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY, value).apply()
        state?.value = value
    }
}

const val NAV_HABITS = "habits"
const val NAV_FINANCE = "finance"
const val NAV_CREDITS = "credits"
const val NAV_PLAYER = "player"

// Цвет берётся из LocalContentColor: selected/unselected оттенки работают как раньше
@Composable
fun NavIcon(route: String, set: Int, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    if (set == 1) {
        DotNavIcon(route, color, modifier)
    } else {
        val image = when (route) {
            NAV_HABITS -> Icons.Outlined.CheckCircle
            NAV_FINANCE -> Icons.Outlined.AccountBalanceWallet
            NAV_CREDITS -> Icons.Outlined.CalendarMonth
            else -> Icons.Outlined.MusicNote
        }
        Icon(image, null, tint = color, modifier = modifier.size(24.dp))
    }
}

@Composable
private fun DotNavIcon(route: String, color: Color, modifier: Modifier) {
    val pattern = when (route) {
        NAV_HABITS -> CHECK_PATTERN
        NAV_FINANCE -> COIN_PATTERN
        NAV_CREDITS -> CAL_PATTERN
        else -> WAVE_PATTERN
    }
    Canvas(modifier.size(24.dp)) {
        val cell = size.width / 5f
        val radius = cell * 0.21f
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (pattern[i][j]) {
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(cell * j + cell / 2f, cell * i + cell / 2f)
                    )
                }
            }
        }
    }
}

private val CHECK_PATTERN = listOf(
    booleanArrayOf(false, false, false, false, false),
    booleanArrayOf(true, false, false, false, true),
    booleanArrayOf(true, false, false, true, true),
    booleanArrayOf(true, true, true, true, false),
    booleanArrayOf(false, false, false, false, false)
)

private val COIN_PATTERN = listOf(
    booleanArrayOf(false, true, true, true, false),
    booleanArrayOf(true, false, false, false, true),
    booleanArrayOf(true, false, true, false, true),
    booleanArrayOf(true, false, false, false, true),
    booleanArrayOf(false, true, true, true, false)
)

private val CAL_PATTERN = listOf(
    booleanArrayOf(true, true, true, true, true),
    booleanArrayOf(true, false, false, false, true),
    booleanArrayOf(true, false, true, false, true),
    booleanArrayOf(true, false, false, false, true),
    booleanArrayOf(true, true, true, true, true)
)

private val WAVE_PATTERN = listOf(
    booleanArrayOf(false, false, true, false, false),
    booleanArrayOf(false, true, true, true, false),
    booleanArrayOf(true, true, false, true, true),
    booleanArrayOf(false, true, true, true, false),
    booleanArrayOf(false, false, true, false, false)
)
