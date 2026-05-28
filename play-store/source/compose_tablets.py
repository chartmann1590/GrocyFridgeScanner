"""
Compose tablet-aspect Play Store screenshots (7" and 10") by placing the
phone screenshots onto a branded landscape canvas with title + bullets.

7": 1920x1200 (16:10)
10": 2560x1600 (16:10)
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PHONE = os.path.join(ROOT, "screenshots", "phone")
OUT_7 = os.path.join(ROOT, "screenshots", "tablet-7in")
OUT_10 = os.path.join(ROOT, "screenshots", "tablet-10in")
os.makedirs(OUT_7, exist_ok=True)
os.makedirs(OUT_10, exist_ok=True)

TEAL_DARK = (12, 47, 47)
TEAL = (15, 118, 110)
TEAL_LIGHT = (45, 212, 191)
SURFACE = (15, 25, 24)
WHITE = (245, 248, 248)
MUTED = (180, 200, 198)


def find_font(size, bold=False):
    paths = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                continue
    return ImageFont.load_default()


def gradient(size, top, bottom):
    img = Image.new("RGB", size, top)
    px = img.load()
    w, h = size
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def soft_glow(canvas: Image.Image, cx, cy, radius, color, alpha=120):
    overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
               fill=(color[0], color[1], color[2], alpha))
    overlay = overlay.filter(ImageFilter.GaussianBlur(radius // 3))
    canvas.alpha_composite(overlay)


def add_phone_frame(phone_img: Image.Image, target_h: int) -> Image.Image:
    """Resize phone shot to target height and add a subtle device frame."""
    ratio = target_h / phone_img.height
    target_w = int(phone_img.width * ratio)
    p = phone_img.resize((target_w, target_h), Image.LANCZOS).convert("RGBA")

    pad = 14
    rim = 4
    fw, fh = target_w + (pad + rim) * 2, target_h + (pad + rim) * 2
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    # Outer rim
    radius = 70
    fd.rounded_rectangle([0, 0, fw, fh], radius=radius, fill=(8, 14, 14, 255))
    # Inner bezel
    fd.rounded_rectangle([rim, rim, fw - rim, fh - rim],
                         radius=radius - rim, fill=(0, 0, 0, 255))
    # Paste phone
    frame.alpha_composite(p, (pad + rim, pad + rim))
    return frame


def shadow_for(img: Image.Image, blur=30, offset=(0, 18), alpha=160):
    shadow = Image.new("RGBA", (img.width + blur * 4, img.height + blur * 4),
                       (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([blur * 2, blur * 2, blur * 2 + img.width,
                          blur * 2 + img.height], radius=70,
                         fill=(0, 0, 0, alpha))
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    return shadow


def compose(size, phone_paths, title, bullets, out_path):
    W, H = size
    canvas = gradient((W, H), TEAL_DARK, SURFACE).convert("RGBA")

    # Decorative blobs
    soft_glow(canvas, int(W * 0.10), int(H * 0.15), int(H * 0.45), TEAL_LIGHT, 60)
    soft_glow(canvas, int(W * 0.85), int(H * 0.85), int(H * 0.50), TEAL, 70)

    # Layout: text on the left, phone(s) on the right
    text_w = int(W * 0.46)
    pad_x = int(W * 0.05)
    pad_y = int(H * 0.10)

    d = ImageDraw.Draw(canvas)

    # Title
    title_size = int(H * 0.06)
    fnt_t = find_font(title_size, bold=True)
    fnt_b = find_font(int(H * 0.028))

    # Wrap title at width
    words = title.split(" ")
    lines = []
    cur = ""
    for w in words:
        tentative = (cur + " " + w).strip()
        bbox = d.textbbox((0, 0), tentative, font=fnt_t)
        if bbox[2] - bbox[0] > text_w - pad_x:
            if cur:
                lines.append(cur)
            cur = w
        else:
            cur = tentative
    if cur:
        lines.append(cur)

    y = pad_y
    for line in lines:
        d.text((pad_x, y), line, fill=WHITE, font=fnt_t)
        y += int(title_size * 1.15)

    y += int(H * 0.035)

    # Subtitle accent line
    d.rectangle([pad_x, y, pad_x + int(W * 0.07), y + int(H * 0.006)],
                fill=TEAL_LIGHT)
    y += int(H * 0.04)

    # Bullets
    bullet_size = int(H * 0.028)
    fnt_bullet = find_font(bullet_size)
    for b in bullets:
        d.ellipse([pad_x, y + int(bullet_size * 0.35), pad_x + 14,
                   y + int(bullet_size * 0.35) + 14], fill=TEAL_LIGHT)
        # wrap bullet to text_w - 60
        bw = text_w - 60
        words = b.split(" ")
        lines_b = []
        cur = ""
        for w in words:
            tentative = (cur + " " + w).strip()
            bb = d.textbbox((0, 0), tentative, font=fnt_bullet)
            if bb[2] - bb[0] > bw:
                if cur:
                    lines_b.append(cur)
                cur = w
            else:
                cur = tentative
        if cur:
            lines_b.append(cur)
        ty = y
        for ln in lines_b:
            d.text((pad_x + 36, ty), ln, fill=MUTED, font=fnt_bullet)
            ty += int(bullet_size * 1.35)
        y = ty + int(bullet_size * 0.5)

    # Place phone(s) on right
    phone_h = int(H * 0.86)
    # First phone slightly tilted, second behind it if 2 provided
    if len(phone_paths) == 1:
        ph = Image.open(phone_paths[0])
        framed = add_phone_frame(ph, phone_h)
        # shadow behind
        sh = shadow_for(framed)
        sx = int(W * 0.62 - sh.width // 2 + framed.width // 2)
        sy = int(H * 0.07 - sh.height // 2 + framed.height // 2 + 20)
        canvas.alpha_composite(sh, (sx, sy))
        px = int(W * 0.62)
        py = int((H - framed.height) // 2)
        canvas.alpha_composite(framed, (px, py))
    else:
        ph1 = Image.open(phone_paths[0])
        ph2 = Image.open(phone_paths[1])
        f1 = add_phone_frame(ph1, phone_h)
        f2 = add_phone_frame(ph2, int(phone_h * 0.92))
        # Place second (smaller) behind/right
        x2 = int(W * 0.78)
        y2 = int(H * 0.06)
        sh2 = shadow_for(f2)
        canvas.alpha_composite(sh2,
                               (x2 - sh2.width // 2 + f2.width // 2,
                                y2 - sh2.height // 2 + f2.height // 2 + 20))
        canvas.alpha_composite(f2, (x2, y2))
        # Place first (larger) in front/left
        x1 = int(W * 0.54)
        y1 = int(H * 0.07)
        sh1 = shadow_for(f1)
        canvas.alpha_composite(sh1,
                               (x1 - sh1.width // 2 + f1.width // 2,
                                y1 - sh1.height // 2 + f1.height // 2 + 20))
        canvas.alpha_composite(f1, (x1, y1))

    canvas.convert("RGB").save(out_path, quality=92, optimize=True)
    print(f"  wrote {out_path}  ({W}x{H})")


SLIDES = [
    {
        "name": "01_scan",
        "title": "Scan your fridge. AI does the rest.",
        "bullets": [
            "Snap a photo of any shelf or cupboard",
            "On-device Gemma 4 detects every item",
            "No cloud — your photos never leave the phone",
        ],
        "phones": ["02_analyzing.png"],
    },
    {
        "name": "02_inventory",
        "title": "Your Grocy stock at a glance.",
        "bullets": [
            "17 products synced from one scan",
            "Locations, units, and counts always current",
            "Search and sort across every shelf in your house",
        ],
        "phones": ["03_inventory.png", "04_inventory_quick_actions.png"],
    },
    {
        "name": "03_quick_actions",
        "title": "One tap to log every kitchen change.",
        "bullets": [
            "Use 1, Add 1, Use all, or set an exact amount",
            "Delete products without leaving the app",
            "Everything writes straight to your self-hosted Grocy",
        ],
        "phones": ["04_inventory_quick_actions.png"],
    },
    {
        "name": "04_history",
        "title": "Every scan kept for review.",
        "bullets": [
            "Photo, timestamp, and item-level deltas",
            "Filter by Fridge or Cupboards",
            "Retry a sync or re-run the AI from scratch",
        ],
        "phones": ["05_history_detail.png"],
    },
    {
        "name": "05_privacy",
        "title": "Private by design. Yours by default.",
        "bullets": [
            "AES-256 encrypted API key storage",
            "Works against any Grocy URL — HTTP or HTTPS",
            "MIT-licensed open source on GitHub",
        ],
        "phones": ["06_settings.png"],
    },
]


def build(size, out_dir):
    for s in SLIDES:
        phones = [os.path.join(PHONE, p) for p in s["phones"]]
        out = os.path.join(out_dir, f"{s['name']}.png")
        compose(size, phones, s["title"], s["bullets"], out)


if __name__ == "__main__":
    print("7-inch tablet (1920x1200):")
    build((1920, 1200), OUT_7)
    print("\n10-inch tablet (2560x1600):")
    build((2560, 1600), OUT_10)
    print("\ndone.")
