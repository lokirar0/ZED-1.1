# -*- coding: utf-8 -*-
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

plt.rcParams["font.family"] = "DejaVu Sans"
BG="#0e1116"; PANEL="#161b26"; TXT="#e6e9f0"; SUB="#9aa4b8"
A={"habits":"#4cc38a","finance":"#5aa7f0","credits":"#f0a35a","player":"#c67af0","settings":"#e0e05a","core":"#6f7b95"}

fig,(ax1,ax2)=plt.subplots(1,2,figsize=(20,10))
for ax in (ax1,ax2):
    ax.set_facecolor(BG); ax.axis("off"); ax.set_xlim(0,100); ax.set_ylim(0,100)
fig.patch.set_facecolor(BG)

def node(ax,x,y,w,h,label,color,fs=9):
    ax.add_patch(FancyBboxPatch((x-w/2,y-h/2),w,h,boxstyle="round,pad=0.3,rounding_size=1",fc=PANEL,ec=color,lw=1.8))
    ax.text(x,y,label,ha="center",va="center",color=TXT,fontsize=fs,fontweight="bold")

def edge(ax,x1,y1,x2,y2,color="#8899bb",rad=0.0,ls="-",lw=1.5):
    ax.add_patch(FancyArrowPatch((x1,y1),(x2,y2),arrowstyle="-|>",mutation_scale=14,color=color,lw=lw,linestyle=ls,connectionstyle=f"arc3,rad={rad}"))

# ===== Левый: граф навигации =====
ax=ax1
ax.text(50,97,"Граф навигации (ZedNavHost)",ha="center",color=TXT,fontsize=15,fontweight="bold")
ax.text(50,93.5,"сплошные = navigate;  пунктир = доступ / возврат",ha="center",color=SUB,fontsize=9)

node(ax,50,86,22,6,"onboarding",A["core"])
tabs=[("habits",20,A["habits"]),("finance",40,A["finance"]),("credits",60,A["credits"]),("player_screen",80,A["player"])]
for name,x,c in tabs: node(ax,x,70,17,6,name,c,8.5)
for name,x,c in tabs: edge(ax,50,83,x,73,A["core"],rad=(x-50)/300)
ax.text(50,80.3,"onFinished -> habits + popUpTo(onboarding){inclusive}",color=SUB,fontsize=7.5,ha="center")

node(ax,12,54,15,5.5,"habit_editor/{id}",A["habits"],7.5)
node(ax,29,54,13,5.5,"habits_stats",A["habits"],8)
edge(ax,20,67,12,56.7,A["habits"]); edge(ax,20,67,29,56.7,A["habits"])

node(ax,40,54,17,5.5,"finance_calendar",A["finance"],8)
node(ax,40,43,18,5.5,"finance_editor",A["finance"],8)
edge(ax,40,67,40,56.7,A["finance"]); edge(ax,40,51.3,40,45.7,A["finance"])
ax.text(49,48,"onAddTransaction",color=SUB,fontsize=7.5)

node(ax,60,54,16,5.5,"credit_editor/{id}",A["credits"],7.5)
edge(ax,60,67,60,56.7,A["credits"])

node(ax,73,54,12,5.5,"all_tracks",A["player"],8)
node(ax,87,54,12,5.5,"liked_tracks",A["player"],8)
node(ax,80,44,12,5.5,"playlists",A["player"],8)
edge(ax,80,67,73,56.7,A["player"]); edge(ax,80,67,87,56.7,A["player"]); edge(ax,80,67,80,46.7,A["player"])

node(ax,30,26,12,5.5,"search",A["settings"])
node(ax,55,26,13,5.5,"settings",A["settings"])
node(ax,48,14,15,5.5,"year_report",A["settings"],8)
node(ax,65,14,13,5.5,"analytics",A["settings"],8)
for name,x,c in tabs: edge(ax,x+4,67,55,28.7,A["settings"],rad=0.15,ls="--",lw=1)
edge(ax,30,23.3,80,23.3,A["player"],rad=-0.15)
ax.text(55,19.8,"onPlayTrack(id) -> player_screen?play={id}",color=A["player"],fontsize=7.5,ha="center")
ax.text(55,17.3,"onNavigateToTab -> любая вкладка (launchSingleTop)",color=SUB,fontsize=7.5,ha="center")
edge(ax,55,23.3,48,16.7,A["settings"]); edge(ax,55,23.3,65,16.7,A["settings"])
ax.text(14,30,"search — из плеера",color=SUB,fontsize=7.5)

# ===== Правый: схема БД =====
ax=ax2
ax.text(50,97,"ZedDatabase (Room, SQLite) — 11 таблиц",ha="center",color=TXT,fontsize=15,fontweight="bold")

def tbl(x,y,name,cols,color,w=26):
    h=4+len(cols)*2.4
    ax.add_patch(FancyBboxPatch((x-w/2,y-h),w,h,boxstyle="round,pad=0.3,rounding_size=1",fc=PANEL,ec=color,lw=2))
    ax.text(x,y-1.6,name,ha="center",va="top",color=color,fontsize=9.5,fontweight="bold")
    for i,c in enumerate(cols):
        ax.text(x-w/2+1.2,y-4.8-i*2.4,c,ha="left",va="top",color=TXT,fontsize=7)
    return y-h

tbl(25,92,"habits",["PK id · title · color","daily_target · reminder"],A["habits"])
tbl(75,92,"habit_completions",["PK id · FK habit_id","date · value"],A["habits"])
tbl(25,72,"habit_goals",["PK id · FK habit_id","period · target"],A["habits"])
tbl(75,72,"sessions",["PK id · date","type · duration_min"],A["habits"])
tbl(25,54,"categories",["PK id · name · icon","is_income"],A["finance"])
tbl(75,54,"transactions",["PK id · FK category_id","amount · type · date"],A["finance"])
tbl(25,36,"credits",["PK id · bank","total · rate · term"],A["credits"])
tbl(75,36,"credit_payments",["PK id · FK credit_id","amount · date"],A["credits"])
tbl(25,16,"favorite_tracks",["PK id · path · title"],A["player"],w=26)
tbl(72,16,"playlists / playlist_tracks",["FK playlist_id · track_id","position"],A["player"],w=32)

edge(ax,38,86,62,86,A["habits"]); ax.text(50,87.3,"1 : N",ha="center",color=SUB,fontsize=8)
edge(ax,38,66,62,66,A["habits"],ls="--"); ax.text(50,67.3,"FK -> habits",ha="center",color=SUB,fontsize=7)
edge(ax,38,48,62,48,A["finance"]); ax.text(50,49.3,"1 : N",ha="center",color=SUB,fontsize=8)
edge(ax,38,30,62,30,A["credits"]); ax.text(50,31.3,"1 : N",ha="center",color=SUB,fontsize=8)
edge(ax,38,12,56,12,A["player"]); ax.text(47,13.3,"M : N",ha="center",color=SUB,fontsize=8)
ax.text(50,4,"Widget-ы (TodayWidget / BudgetWidget) и ReminderScheduler\nчитают те же таблицы через DAO; экспорт — CsvExporter / BackupManager",
        ha="center",color=SUB,fontsize=8.5)

plt.tight_layout()
plt.savefig("/workspace/navigation_and_db.png",dpi=160,facecolor=BG)
print("saved2")
