"""Cuts the Codex art in design/art into app drawables.

    python tools/cut.py

planets (3x2) -> planet_<key>.webp, ranks (4x2) -> rank_<0..7>.webp, hero -> hero.webp,
icon -> mipmap ic_launcher_foreground.webp (the violet background is drawn by the adaptive icon).
Magenta is keyed by a flood fill from each cell's border, so violet and pink inside the art survive.
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
ART = ROOT / "design" / "art"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
RES = ROOT / "app" / "src" / "main" / "res"
PLANETS = ["felis", "canis", "psittacus", "glires", "mustela", "squamata"]


def dist(arr):
    """How far each pixel is from pure magenta (0 = exactly #FF00FF)."""
    r, g, b = arr[..., 0].astype(float), arr[..., 1].astype(float), arr[..., 2].astype(float)
    return np.sqrt((255 - r) ** 2 + g ** 2 + (255 - b) ** 2)


def key(im, flood=True):
    arr = np.array(im.convert("RGBA"))
    d = dist(arr)
    near = d < 150          # could be background
    if flood:
        lab, _ = ndimage.label(near)
        edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
        bg = np.isin(lab, list(edge))
        # background shut inside a shape (between an arm and a bag): pure magenta patches of some size
        lab2, n2 = ndimage.label(d < 70)
        if n2:
            sizes = ndimage.sum(np.ones_like(lab2), lab2, range(n2 + 1))
            holes = np.isin(lab2, [k for k in range(1, n2 + 1) if sizes[k] >= 25])
            bg = bg | ndimage.binary_dilation(holes, iterations=2) & near
    else:
        bg = d < 110
    alpha = arr[..., 3].astype(float)
    alpha[bg & (d < 95)] = 0
    fringe = bg & (d >= 95)
    # pull the pink out of the fringe and fade it
    k = np.clip((150 - d) / 55, 0, 1)
    for c in (0, 2):
        ch = arr[..., c].astype(float)
        arr[..., c] = np.where(fringe, ch - (ch - arr[..., 1]) * k, ch).astype(np.uint8)
    alpha = np.where(fringe, alpha * (1 - k), alpha)
    # pixels next to the cut-out that still look pink lose their tint too
    grown = ndimage.binary_dilation(alpha == 0, iterations=1) & (alpha > 0) & (d < 200)
    for c in (0, 2):
        ch = arr[..., c].astype(float)
        arr[..., c] = np.where(grown, np.minimum(ch, arr[..., 1] + 40), ch).astype(np.uint8)
    arr[..., 3] = alpha.astype(np.uint8)
    return Image.fromarray(arr)


def border_clean(cell):
    """Drops bits of the neighbouring cells: anything opaque that touches the cell's edge, except the biggest piece."""
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    if n == 0:
        return cell
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = max(range(1, n + 1), key=lambda k: sizes[k])
    for k in edge:
        if k != big:
            arr[lab == k, 3] = 0
    # specks far smaller than the creature are noise
    for k in range(1, n + 1):
        if sizes[k] < sizes[big] * .004:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)



def trim(im, pad=6):
    bb = im.getchannel("A").point(lambda v: 255 if v > 20 else 0).getbbox()
    if not bb:
        return im
    im = im.crop(bb)
    s = max(im.size) + pad * 2
    sq = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    sq.paste(im, ((s - im.width) // 2, (s - im.height) // 2))
    return sq


def sheet(name, cols, rows, names, size):
    p = ART / f"{name}.png"
    if not p.exists():
        print("missing", name); return
    im = Image.open(p).convert("RGBA")
    w, h = im.width / cols, im.height / rows
    for i, n in enumerate(names):
        x, y = i % cols, i // cols
        cell = im.crop((int(x * w), int(y * h), int((x + 1) * w), int((y + 1) * h)))
        cell = trim(border_clean(key(cell)))
        cell.thumbnail((size, size), Image.LANCZOS)
        cell.save(OUT / f"{n}.webp", "WEBP", quality=90)
        print(n, cell.size)


def mask(cell, size, frame_crop=0.0):
    """Gold lines on graphite -> white lines on transparency (alpha from brightness), tinted in the app."""
    g = np.array(cell.convert("L")).astype(float)
    bg = np.percentile(g, 20)
    hi = max(np.percentile(g, 99.5), bg + 30)
    a = np.clip((g - bg - 6) / (hi - bg - 6), 0, 1) ** 0.9
    out = np.zeros((*g.shape, 4), np.uint8)
    out[..., :3] = 255
    out[..., 3] = (a * 255).astype(np.uint8)
    im = trim(Image.fromarray(out), pad=4)
    im.thumbnail((size, size), Image.LANCZOS)
    return im


def engr(name, out, size):
    p = ART / f"{name}.png"
    if not p.exists():
        print("missing", name); return
    mask(Image.open(p), size).save(OUT / f"{out}.webp", "WEBP", quality=92)
    print(out)


def engr_sheet(name, cols, rows, names, size):
    p = ART / f"{name}.png"
    if not p.exists():
        print("missing", name); return
    im = Image.open(p)
    w, h = im.width / cols, im.height / rows
    for i, n in enumerate(names):
        x, y = i % cols, i // cols
        mask(im.crop((int(x * w), int(y * h), int((x + 1) * w), int((y + 1) * h))), size).save(OUT / f"{n}.webp", "WEBP", quality=92)
        print(n)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for k in PLANETS:
        engr(f"e_{k}", f"planet_{k}", 520)
    engr_sheet("e_ranks", 4, 2, [f"rank_{i}" for i in range(8)], 260)
    engr("e_hero", "hero", 640)
    engr_sheet("e_busts", 3, 2, [f"bust_{k}" for k in PLANETS], 360)
    engr_sheet("e_appetite", 4, 1, [f"appetite_cat_{i}" for i in range(4)], 300)
    for k in ["dog", "any", "bird", "rabbit", "ferret", "lizard"]:
        engr_sheet(f"e_appetite_{k}", 4, 1, [f"appetite_{k}_{i}" for i in range(4)], 300)
    engr_sheet("e_menu", 3, 2, [f"menu_{i}" for i in range(6)], 220)
    ic = ART / "icon_c.png"
    if ic.exists():
        src = Image.open(ic).convert("RGB")
        # The launcher crops to the middle two thirds: shrink the emblem onto its own graphite.
        im = Image.new("RGB", src.size, src.getpixel((4, 4)))
        k = 0.8
        small = src.resize((int(src.width * k), int(src.height * k)), Image.LANCZOS)
        im.paste(small, ((src.width - small.width) // 2, (src.height - small.height) // 2))
        for d, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
            f = RES / f"mipmap-{d}"; f.mkdir(exist_ok=True)
            im.resize((px, px), Image.LANCZOS).save(f / "ic_launcher_foreground.webp", "WEBP", quality=92)
        im.resize((512, 512), Image.LANCZOS).save(ROOT / "design" / "icon-512.png")
        print("icon")


if __name__ == "__main__":
    main()
