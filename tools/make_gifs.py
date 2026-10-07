#!/usr/bin/env python3
# ZED: генерация 5 dot-matrix GIF (512x512, прозрачный фон, бесшовный цикл 3 c).
# Палитра ограничена токенами Nothing OS: #E8E8E8 #999999 #666666 #222222 #D71921.
import math
import os

from PIL import Image, ImageDraw

SIZE = 512
FRAMES = 36          # 36 кадров = 3 секунды при 12 fps
DELAY = 80           # мс на кадр (~12.5 fps — для dot-стиля достаточно)
DOT = 10             # размер «пикселя»
CX = SIZE // 2
OUT = os.path.join(os.path.dirname(__file__), "out")

# Индексы палитры: 0 = прозрачный, далее токены ZED
TRANS, BRIGHT, DIM, OFF, ACCENT, MID = 0, 1, 2, 3, 4, 5
PALETTE = [
    (0, 0, 0),        # 0 transparent
    (232, 232, 232),  # 1 #E8E8E8
    (102, 102, 102),  # 2 #666666
    (34, 34, 34),     # 3 #222222 (off / ступенчатое свечение)
    (215, 25, 33),    # 4 #D71921
    (153, 153, 153),  # 5 #999999
]


def new_frame():
    img = Image.new("P", (SIZE, SIZE), TRANS)
    flat = [c for rgb in PALETTE for c in rgb]
    img.putpalette(flat + [0] * (768 - len(flat)))
    return img


def sq(d, x, y, idx, glow=False, s=DOT):
    x, y = int(x), int(y)
    if glow:
        # Ступенчатое «свечение»: квадрат OFF вокруг яркой точки, без градиентов
        d.rectangle([x - s, y - s, x + s * 2, y + s * 2], fill=OFF)
    d.rectangle([x, y, x + s - 1, y + s - 1], fill=idx)


# 1. ВРАЩАЮЩЕЕСЯ КОЛЬЦО: два встречных кольца + красный бегунок
def ring_frames():
    frames = []
    for f in range(FRAMES):
        ph = f / FRAMES
        img = new_frame()
        d = ImageDraw.Draw(img)
        for r, count, dirn in [(235, 32, 1), (205, 28, -1)]:
            for i in range(count):
                a = i * 2 * math.pi / count + ph * 2 * math.pi * dirn
                pulse = 0.5 + 0.5 * math.sin(a * 3 + ph * 2 * math.pi)
                idx = BRIGHT if pulse > 0.6 else DIM
                sq(d, CX + math.cos(a) * r, CX + math.sin(a) * r, idx, glow=(idx == BRIGHT))
        ra = ph * 2 * math.pi
        sq(d, CX + math.cos(ra) * 235, CX + math.sin(ra) * 235, ACCENT, glow=True, s=14)
        frames.append(img)
    return frames


# 2. ЭКВАЛАЙЗЕР ПО КРУГУ: 32 радиальных стопки, красные пики
def eq_frames():
    frames = []
    for f in range(FRAMES):
        ph = f / FRAMES
        fast = (f * 4 / FRAMES) % 1.0   # 4 цикла за петлю = бесшовно
        img = new_frame()
        d = ImageDraw.Draw(img)
        for i in range(32):
            a = i * 2 * math.pi / 32
            level = 0.25 + 0.75 * abs(
                math.sin(i * 1.7 + fast * 2 * math.pi) * math.sin(i * 0.53 + ph * 2 * math.pi)
            )
            steps = max(1, min(6, int(level * 6)))
            for s in range(steps):
                r = 174 + s * 16
                idx = ACCENT if (s == steps - 1 and s >= 4) else (BRIGHT if s % 2 == 0 else DIM)
                sq(d, CX + math.cos(a) * r, CX + math.sin(a) * r, idx, glow=(idx == ACCENT))
        frames.append(img)
    return frames


# 3. ПУЛЬСИРУЮЩИЕ ТОЧКИ: LED-дыхание по кольцу + красный бегунок
def breath_frames():
    frames = []
    count = 40
    for f in range(FRAMES):
        ph = f / FRAMES
        img = new_frame()
        d = ImageDraw.Draw(img)
        for i in range(count):
            a = i * 2 * math.pi / count
            wave = 0.5 + 0.5 * math.sin(ph * 2 * math.pi - i * 0.35)
            idx = BRIGHT if wave > 0.75 else (DIM if wave > 0.35 else OFF)
            sq(d, CX + math.cos(a) * 225, CX + math.sin(a) * 225, idx, glow=(idx == BRIGHT))
        ri = int(ph * count) % count
        ra = ri * 2 * math.pi / count
        sq(d, CX + math.cos(ra) * 225, CX + math.sin(ra) * 225, ACCENT, glow=True, s=13)
        frames.append(img)
    return frames


# 4. МАТРИЦА-ВОЛНА: сетка 16x16, прозрачный круглый центр, красный гребень
def wave_frames():
    frames = []
    n = 16
    step = SIZE / n
    for f in range(FRAMES):
        ph = f / FRAMES
        img = new_frame()
        d = ImageDraw.Draw(img)
        for x in range(n):
            for y in range(n):
                px = x * step + step / 2
                py = y * step + step / 2
                dist = math.hypot(px - CX, py - CX)
                if dist < 154:
                    continue  # прозрачный центр
                wave = 0.5 + 0.5 * math.sin(ph * 2 * math.pi - dist / 41)
                idx = ACCENT if wave > 0.85 else (BRIGHT if wave > 0.5 else DIM)
                sq(d, px, py, idx, glow=(idx == ACCENT), s=8)
        frames.append(img)
    return frames


# 5. ГЛИФ-ПОЛОСЫ: 4 колонны по бокам + красный сканирующий ряд
def glyph_frames():
    frames = []
    cols = [41, 82, 430, 471]
    rows = 14
    for f in range(FRAMES):
        ph = f / FRAMES
        active = int(ph * rows) % rows
        img = new_frame()
        d = ImageDraw.Draw(img)
        for ci, x in enumerate(cols):
            for r in range(rows):
                y = 51 + r * (410 / rows)
                moving = (r + ci * 3) % rows == active
                idx = ACCENT if moving else (BRIGHT if (r + ci) % 4 == 0 else DIM)
                sq(d, x, y, idx, glow=(idx in (ACCENT, BRIGHT)))
        frames.append(img)
    return frames


def save(frames, name):
    os.makedirs(OUT, exist_ok=True)
    frames[0].save(
        os.path.join(OUT, name),
        save_all=True,
        append_images=frames[1:],
        duration=DELAY,
        loop=0,            # бесконечный цикл
        transparency=TRANS,
        disposal=2,        # кадр стирается до прозрачного фона — без «хвостов»
    )
    print("saved", name)


if __name__ == "__main__":
    save(ring_frames(), "fx_ring.gif")
    save(eq_frames(), "fx_eq.gif")
    save(breath_frames(), "fx_breath.gif")
    save(wave_frames(), "fx_wave.gif")
    save(glyph_frames(), "fx_glyph.gif")
    print("done:", os.listdir(OUT))
