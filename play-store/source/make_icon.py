"""
Grocy Fridge Scanner — app icon generator.

Produces:
  play-store/icon/play_store_icon_512.png   (Play Store listing icon, 512x512)
  play-store/icon/master_1024.png           (master, no mask)
  play-store/icon/foreground_1024.png       (adaptive icon foreground, transparent bg)
  play-store/icon/background_1024.png       (adaptive icon background)
  play-store/icon/preview_round_512.png     (rounded preview)

Design concept:
  - Teal gradient background (brand colour 0F766E -> 14B8A6)
  - White stylized fridge silhouette with door split and handle
  - Bright amber 'scan' viewfinder corners overlaying the fridge
  - Small AI sparkle in the top-right corner of the viewfinder

Adaptive icon safe zone: keep meaningful content within the central 66% of the canvas.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "icon")
os.makedirs(OUT, exist_ok=True)

SIZE = 1024
TEAL_DARK = (15, 118, 110)      # 0F766E
TEAL_MID = (20, 184, 166)       # 14B8A6
TEAL_DEEP = (17, 94, 89)        # 115E59
WHITE = (255, 255, 255)
AMBER = (251, 191, 36)          # FBBF24
AMBER_LIGHT = (253, 224, 71)    # FDE047


def gradient_background(size, top, bottom):
    img = Image.new("RGB", (size, size), top)
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        # smoothstep for less linear feel
        t = t * t * (3 - 2 * t)
        r = int(top[0] + (bottom[0] - top[0]) * t)
        g = int(top[1] + (bottom[1] - top[1]) * t)
        b = int(top[2] + (bottom[2] - top[2]) * t)
        for x in range(size):
            px[x, y] = (r, g, b)
    return img


def add_radial_glow(img, center, radius, color, opacity=80):
    """Adds a soft radial highlight to the image."""
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    cx, cy = center
    steps = 60
    for i in range(steps, 0, -1):
        r = int(radius * (i / steps))
        a = int(opacity * (1 - i / steps) ** 2)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*color, a))
    img.alpha_composite(glow)
    return img


def rounded_rect(draw, box, radius, fill, outline=None, width=0):
    x0, y0, x1, y1 = box
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def draw_fridge(draw, cx, cy, w, h, body_color=WHITE, accent=TEAL_DEEP):
    """Draw a stylized fridge centred at (cx, cy)."""
    x0 = cx - w // 2
    y0 = cy - h // 2
    x1 = cx + w // 2
    y1 = cy + h // 2
    radius = int(w * 0.10)

    # Subtle drop shadow under the fridge
    shadow_offset = int(h * 0.025)
    shadow_box = [x0 + 6, y0 + shadow_offset, x1 + 6, y1 + shadow_offset]
    # we'll skip explicit shadow here; viewfinder + gradient bg already produce depth

    # Body
    draw.rounded_rectangle([x0, y0, x1, y1], radius=radius, fill=body_color)

    # Door split (horizontal line dividing freezer / fridge)
    split_y = y0 + int(h * 0.34)
    split_thickness = max(3, int(h * 0.012))
    draw.rectangle([x0, split_y - split_thickness // 2, x1,
                    split_y + split_thickness // 2], fill=accent)

    # Freezer compartment (top) — slight inset to suggest a panel
    inset = int(w * 0.07)
    fz_box = [x0 + inset, y0 + inset, x1 - inset, split_y - inset // 2]
    draw.rounded_rectangle(fz_box, radius=int(w * 0.04), fill=accent)
    # Inner highlight
    inner_inset = int(w * 0.03)
    draw.rounded_rectangle(
        [fz_box[0] + inner_inset, fz_box[1] + inner_inset,
         fz_box[2] - inner_inset, fz_box[3] - inner_inset],
        radius=int(w * 0.03),
        fill=body_color,
    )

    # Fridge compartment (bottom)
    fc_box = [x0 + inset, split_y + inset // 2, x1 - inset, y1 - inset]
    draw.rounded_rectangle(fc_box, radius=int(w * 0.04), fill=accent)
    draw.rounded_rectangle(
        [fc_box[0] + inner_inset, fc_box[1] + inner_inset,
         fc_box[2] - inner_inset, fc_box[3] - inner_inset],
        radius=int(w * 0.03),
        fill=body_color,
    )

    # Door handles — right side
    handle_w = max(8, int(w * 0.025))
    # freezer handle
    h1 = [x1 - inset - handle_w * 4, y0 + int(h * 0.18),
          x1 - inset - handle_w * 2, y0 + int(h * 0.28)]
    draw.rounded_rectangle(h1, radius=handle_w, fill=accent)
    # fridge handle
    h2 = [x1 - inset - handle_w * 4, split_y + int(h * 0.10),
          x1 - inset - handle_w * 2, split_y + int(h * 0.45)]
    draw.rounded_rectangle(h2, radius=handle_w, fill=accent)


def draw_scan_corners(layer, cx, cy, w, h, color=AMBER, thickness=None, length_ratio=0.22):
    """Draw bright viewfinder corners around (cx, cy) with width/height w/h."""
    if thickness is None:
        thickness = max(8, int(w * 0.035))
    draw = ImageDraw.Draw(layer)
    x0 = cx - w // 2
    y0 = cy - h // 2
    x1 = cx + w // 2
    y1 = cy + h // 2
    L = int(min(w, h) * length_ratio)

    def L_shape(corner_x, corner_y, dx, dy):
        # horizontal arm
        hx0 = corner_x if dx > 0 else corner_x - L
        hx1 = corner_x + L if dx > 0 else corner_x
        hy0 = corner_y - thickness // 2 if dy > 0 else corner_y - thickness // 2
        hy1 = hy0 + thickness
        draw.rounded_rectangle([hx0, hy0, hx1, hy1], radius=thickness // 2, fill=color)
        # vertical arm
        vy0 = corner_y if dy > 0 else corner_y - L
        vy1 = corner_y + L if dy > 0 else corner_y
        vx0 = corner_x - thickness // 2 if dx > 0 else corner_x - thickness // 2
        vx1 = vx0 + thickness
        draw.rounded_rectangle([vx0, vy0, vx1, vy1], radius=thickness // 2, fill=color)

    L_shape(x0, y0, +1, +1)
    L_shape(x1, y0, -1, +1)
    L_shape(x0, y1, +1, -1)
    L_shape(x1, y1, -1, -1)


def draw_sparkle(draw, cx, cy, size, color=AMBER_LIGHT):
    """Four-point sparkle / AI star."""
    s = size
    # diamond cross
    pts1 = [(cx, cy - s), (cx + s * 0.3, cy), (cx, cy + s), (cx - s * 0.3, cy)]
    pts2 = [(cx - s, cy), (cx, cy - s * 0.3), (cx + s, cy), (cx, cy + s * 0.3)]
    draw.polygon(pts1, fill=color)
    draw.polygon(pts2, fill=color)
    # Small center
    r = int(s * 0.18)
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=WHITE)


def build_foreground(size=SIZE, transparent_bg=True):
    """Build only the foreground (fridge + viewfinder + sparkle) on transparent."""
    if transparent_bg:
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    else:
        img = Image.new("RGBA", (size, size), (*TEAL_DARK, 255))
    draw = ImageDraw.Draw(img)

    cx, cy = size // 2, size // 2
    # Fridge dimensions — fit within central 60% to respect adaptive icon safe zone
    fw = int(size * 0.50)
    fh = int(size * 0.62)
    draw_fridge(draw, cx, cy + int(size * 0.01), fw, fh,
                body_color=WHITE, accent=TEAL_DEEP)

    # Viewfinder corners around the fridge (slightly larger so corners frame the body)
    vw = int(size * 0.60)
    vh = int(size * 0.72)
    draw_scan_corners(img, cx, cy + int(size * 0.01), vw, vh,
                      color=AMBER, length_ratio=0.18)

    # AI sparkle near top-right of viewfinder
    spx = cx + int(vw * 0.42)
    spy = cy - int(vh * 0.50)
    draw_sparkle(draw, spx, spy, int(size * 0.045))

    return img


def build_background(size=SIZE):
    bg = gradient_background(size, TEAL_MID, TEAL_DARK).convert("RGBA")
    # Add a soft top-left highlight
    bg = add_radial_glow(bg, (int(size * 0.30), int(size * 0.30)),
                         int(size * 0.55), (255, 255, 255), opacity=55)
    # Subtle bottom-right shadow
    bg = add_radial_glow(bg, (int(size * 0.80), int(size * 0.85)),
                         int(size * 0.45), (0, 0, 0), opacity=45)
    return bg


def build_master(size=SIZE):
    bg = build_background(size)
    fg = build_foreground(size, transparent_bg=True)
    out = bg.copy()
    out.alpha_composite(fg)
    return out


def rounded_mask(size, radius_ratio=0.22):
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    r = int(size * radius_ratio)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
    return mask


def circle_mask(size):
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def main():
    master = build_master(SIZE)
    master.save(os.path.join(OUT, "master_1024.png"))

    fg = build_foreground(SIZE, transparent_bg=True)
    fg.save(os.path.join(OUT, "foreground_1024.png"))

    bg = build_background(SIZE)
    bg.save(os.path.join(OUT, "background_1024.png"))

    # Play Store 512x512 (rounded square preview also generated for marketing)
    ps = master.resize((512, 512), Image.LANCZOS)
    ps.save(os.path.join(OUT, "play_store_icon_512.png"))

    # Rounded square preview (Pixel-style mask)
    rounded = master.copy()
    rounded.putalpha(rounded_mask(SIZE, radius_ratio=0.22))
    rounded = rounded.resize((512, 512), Image.LANCZOS)
    rounded.save(os.path.join(OUT, "preview_rounded_512.png"))

    # Circle preview
    circ = master.copy()
    circ.putalpha(circle_mask(SIZE))
    circ = circ.resize((512, 512), Image.LANCZOS)
    circ.save(os.path.join(OUT, "preview_round_512.png"))

    # mipmap densities for the app
    # Android adaptive icon foreground/background are 108x108dp.
    # Standard launcher legacy sizes (square):
    #   mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192
    legacy = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]
    for name, sz in legacy:
        d = os.path.join(ROOT, os.pardir, "app", "src", "main", "res", f"mipmap-{name}")
        # we'll write via separate script call; just save into icon/ for staging
        out_dir = os.path.join(OUT, "mipmap-staging", f"mipmap-{name}")
        os.makedirs(out_dir, exist_ok=True)
        # legacy launcher: rounded square + circle
        sq = master.copy()
        sq.putalpha(rounded_mask(SIZE, radius_ratio=0.22))
        sq = sq.resize((sz, sz), Image.LANCZOS)
        sq.save(os.path.join(out_dir, "ic_launcher.png"))
        cr = master.copy()
        cr.putalpha(circle_mask(SIZE))
        cr = cr.resize((sz, sz), Image.LANCZOS)
        cr.save(os.path.join(out_dir, "ic_launcher_round.png"))

    print("OK")


if __name__ == "__main__":
    main()
