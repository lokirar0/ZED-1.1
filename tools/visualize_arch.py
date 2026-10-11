# -*- coding: utf-8 -*-
"""Визуализация архитектуры приложения ZED (Android/Kotlin/Compose)."""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

plt.rcParams["font.family"] = "DejaVu Sans"

BG = "#0e1116"
PANEL = "#161b26"
EDGE = "#2a3245"
TXT = "#e6e9f0"
SUB = "#9aa4b8"
ACCENTS = {
    "habits": "#4cc38a",
    "finance": "#5aa7f0",
    "credits": "#f0a35a",
    "player": "#c67af0",
    "settings": "#e0e05a",
    "core": "#6f7b95",
}

fig, ax = plt.subplots(figsize=(20, 13))
fig.patch.set_facecolor(BG)
ax.set_facecolor(BG)
ax.set_xlim(0, 100)
ax.set_ylim(0, 100)
ax.axis("off")

def box(x, y, w, h, title, lines=None, color="#5aa7f0", fs=11, tfs=11):
    b = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.4,rounding_size=1.2",
                       fc=PANEL, ec=color, lw=1.6)
    ax.add_patch(b)
    if lines:
        ax.text(x + w / 2, y + h - 1.6, title, ha="center", va="top",
                color=color, fontsize=tfs, fontweight="bold")
        for i, ln in enumerate(lines):
            ax.text(x + w / 2, y + h - 4.2 - i * 2.4, ln, ha="center", va="top",
                    color=TXT, fontsize=fs - 2)
    else:
        ax.text(x + w / 2, y + h / 2, title, ha="center", va="center",
                color=TXT, fontsize=fs, fontweight="bold")

def panel(x, y, w, h, label, color):
    p = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.5,rounding_size=1.5",
                       fc="#10151f", ec=color, lw=2.2, alpha=0.9)
    ax.add_patch(p)
    ax.text(x + 0.8, y + h - 1.2, label, ha="left", va="top",
            color=color, fontsize=13, fontweight="bold")

def arrow(x1, y1, x2, y2, color="#8899bb", style="-|>", lw=1.6, rad=0.0, ls="-"):
    a = FancyArrowPatch((x1, y1), (x2, y2), arrowstyle=style, mutation_scale=16,
                        color=color, lw=lw, linestyle=ls,
                        connectionstyle=f"arc3,rad={rad}", alpha=0.9)
    ax.add_patch(a)

# Заголовок
ax.text(50, 98, "ZED — Android-приложение (Kotlin · Jetpack Compose · Room · Hilt)",
        ha="center", color=TXT, fontsize=19, fontweight="bold")
ax.text(50, 94.6, "MainActivity → ZedAppTheme → ZedNavHost (нижняя навигация: Привычки · Финансы · Кредиты · Плеер)",
        ha="center", color=SUB, fontsize=12)

# ================= ЛОГИКА НАВИГАЦИИ (экраны) =================
panel(1, 40, 63, 50, "UI-СЛОЙ · Навигация (ZedNavHost.kt)", "#5aa7f0")

# Вкладки
tabs = [
    ("Привычки", "HabitsScreen", ACCENTS["habits"], 3.5),
    ("Финансы", "FinanceScreen", ACCENTS["finance"], 18.5),
    ("Кредиты", "CreditsScreen", ACCENTS["credits"], 33.5),
    ("Плеер", "PlayerScreen", ACCENTS["player"], 48.5),
]
for name, scr, c, x in tabs:
    box(x, 79, 13, 7.5, None)
    ax.text(x + 6.5, 84.2, name, ha="center", color=c, fontsize=12, fontweight="bold")
    ax.text(x + 6.5, 81.2, scr, ha="center", color=SUB, fontsize=8.5)
    ax.text(x + 6.5, 77.2, f"route:\n{name and ''}", ha="center", color=SUB, fontsize=7)

ax.text(3.5, 76.2, "habits", ha="left", color=SUB, fontsize=8, style="italic")
ax.text(18.5, 76.2, "finance", ha="left", color=SUB, fontsize=8, style="italic")
ax.text(33.5, 76.2, "credits", ha="left", color=SUB, fontsize=8, style="italic")
ax.text(48.5, 76.2, "player_screen?play={id}", ha="left", color=SUB, fontsize=8, style="italic")

# Дочерние экраны
sub_screens = [
    ("habit_editor/{id}", 3.5, ACCENTS["habits"]),
    ("habits_stats", 3.5, ACCENTS["habits"], 18.5),
]
box(3.5, 66, 13, 5.5, "habit_editor/{id}")
box(3.5, 59, 13, 5.5, "habits_stats")
box(18.5, 66, 13, 5.5, "finance_calendar")
box(18.5, 59, 13, 5.5, "finance_editor")
box(33.5, 66, 13, 5.5, "credit_editor/{id}")
box(48.5, 66, 13, 5.5, "all_tracks")
box(48.5, 59, 13, 5.5, "liked_tracks")
box(48.5, 52, 13, 5.5, "playlists")

for x in tabs:
    pass
# стрелки вкладка -> дочерние
arrow(10, 78.6, 10, 72, ACCENTS["habits"]); arrow(10, 78.6, 10, 65, ACCENTS["habits"], rad=-0.15)
arrow(25, 78.6, 25, 72, ACCENTS["finance"]); arrow(25, 78.6, 25, 65, ACCENTS["finance"], rad=-0.15)
arrow(40, 78.6, 40, 72, ACCENTS["credits"])
arrow(55, 78.6, 55, 72, ACCENTS["player"])
arrow(55, 78.6, 55, 65, ACCENTS["player"], rad=-0.2)
arrow(55, 78.6, 55, 58, ACCENTS["player"], rad=-0.35)

# onboarding, search, settings
box(3.5, 44, 13, 5.5, "onboarding (старт)")
box(25.5, 44, 13, 5.5, "search")
box(47, 44, 15, 5.5, "settings")
box(47, 36.5, 7, 5.5, "year_report")
box(55, 36.5, 7, 5.5, "analytics")
arrow(10, 49.5, 10, 58.5, SUB, rad=0.2)   # onboarding -> habits
arrow(10, 78.6, 10, 50, SUB, rad=0.0, ls="--")  # hab->settings? no
ax.text(17, 46.8, "onFinished → habits", color=SUB, fontsize=7.5)
arrow(32, 49.5, 55, 55, SUB, rad=-0.25, ls="--")
ax.text(36, 51.5, "onNavigateToTab / onPlayTrack", color=SUB, fontsize=7.5)
arrow(54.5, 49.5, 50.5, 42.5, SUB); arrow(54.5, 49.5, 58.5, 42.5, SUB)
arrow(25, 78.6, 50, 50, ACCENTS["settings"], rad=0.35, ls="--", lw=1.2)
ax.text(41, 60.5, "⚙ из любой вкладки → settings", color=ACCENTS["settings"], fontsize=8)

# ================= ПЛЕЕР-ДОЧЕРНИЕ подписи уже есть; виджеты =================
panel(66, 71, 33, 19, "ВЕЙДЖЕТЫ И ФОНОВЫЕ КОМПОНЕНТЫ", "#6f7b95")
box(68, 82, 13, 6, "TodayWidget\n(Home screen)")
box(83, 82, 14, 6, "BudgetWidget\n(Home screen)")
box(68, 74.5, 13, 6, "ReminderReceiver\n+ BootReceiver")
box(83, 74.5, 14, 6, "ZedPlaybackService\n(MediaSession)")
arrow(74.5, 81.6, 74.5, 80.8, SUB)
arrow(90, 81.6, 90, 80.8, SUB)

# ================= DI =================
panel(66, 55, 33, 14, "DI (Hilt)", "#c67af0")
box(68, 58, 14, 6, "ZedApplication\n@HiltAndroidApp")
box(84, 58, 13, 6, "DatabaseModule\nAppModule")
arrow(75, 57.6, 82, 52, ACCENTS["core"], rad=-0.2)

# ================= ViewModel слой =================
panel(1, 27, 63, 11, "ViewModel-СЛОЙ (MVVM, StateFlow)", "#4cc38a")
vms = ["HabitsVM", "HabitEditorVM", "HabitStatsVM", "FinanceVM", "FinanceCalVM",
       "TxEditorVM", "CreditsVM", "CreditEdVM", "PlayerVM", "SearchVM", "SettingsVM", "YearReportVM"]
x = 3
for v in vms:
    w = 4.6
    box(x, 29.5, w, 5.5, v, fs=7.5)
    x += w + 0.55

# ================= Repository =================
panel(1, 14, 63, 11, "DOMAIN / DATA-СЛОЙ · Репозитории", "#5aa7f0")
repos = ["HabitRepo", "CategoryRepo", "TransactionRepo", "CreditRepo", "PlayerRepo",
         "ActivityRepo", "SettingsRepo", "BackupManager", "CsvExporter"]
x = 3
for r in repos:
    w = 6.2
    box(x, 16.5, w, 5.5, r, fs=7.5)
    x += w + 0.6

# ================= Room DB =================
panel(66, 8, 33, 45, "ZedDatabase (Room, SQLite)", "#f0a35a")
tables = [
    ("habits", ACCENTS["habits"]), ("habit_completions", ACCENTS["habits"]),
    ("habit_goals", ACCENTS["habits"]), ("sessions", ACCENTS["habits"]),
    ("categories", ACCENTS["finance"]), ("transactions", ACCENTS["finance"]),
    ("credits", ACCENTS["credits"]), ("credit_payments", ACCENTS["credits"]),
    ("favorite_tracks", ACCENTS["player"]), ("playlists", ACCENTS["player"]),
    ("playlist_tracks", ACCENTS["player"]),
]
y = 47
for t, c in tables:
    box(69, y, 27, 3.1, t, fs=8.5, color=c)
    y -= 3.6

# связи слоёв
arrow(32, 27.4, 32, 25.4, "#4cc38a", lw=2.5)
ax.text(33, 26, "collect / events", color=SUB, fontsize=8)
arrow(32, 14.4, 32, 11.5, "#5aa7f0", lw=2.5)
arrow(20, 14, 70, 10, "#f0a35a", rad=-0.25, lw=2)
ax.text(45, 8, "DAO / queries → Room", color=SUB, fontsize=8.5)
arrow(50, 38, 75, 30, "#5aa7f0", rad=0.2, ls="--")   # VM -> db? лучше repo->db
arrow(90, 54.6, 90, 53.4, "#c67af0", lw=2)
ax.text(91, 55, "inject", color=SUB, fontsize=8)

# Util блок
panel(1, 2, 63, 10, "CORE / UTILS", "#6f7b95")
utils = ["StreakCalculator", "PaymentCalculator", "MoneyFormatter", "LocaleHelper",
         "AudioEffectsManager", "LocalAudioScanner", "NotificationHelper", "ZedAudioProcessor"]
x = 3
for u in utils:
    w = 7.1
    box(x, 4, w, 5, u, fs=6.8)
    x += w + 0.6

# легенда цветов
legend_y = 3.2
lx = 66
labels = [("Привычки", ACCENTS["habits"]), ("Финансы", ACCENTS["finance"]),
          ("Кредиты", ACCENTS["credits"]), ("Плеер", ACCENTS["player"]),
          ("Настройки", ACCENTS["settings"]), ("Core", ACCENTS["core"])]
ax.text(66, 5.8, "Легенда модулей:", color=TXT, fontsize=9, fontweight="bold")
xx = lx
for i, (nm, c) in enumerate(labels):
    ax.add_patch(FancyBboxPatch((xx, 3.4), 1.6, 1.6, boxstyle="round,pad=0.15", fc=c, ec="none"))
    ax.text(xx + 2.1, 4.2, nm, color=TXT, fontsize=8.5, va="center")
    xx += 5.5

plt.tight_layout()
plt.savefig("/workspace/architecture_zed.png", dpi=160, facecolor=BG)
print("saved")
