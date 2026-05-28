"""
Build the Play Store feature graphic (1024x500).
Composition: dark teal gradient, decorative glow, app name + tagline on left,
phone shot of the inventory screen on the right with a slight tilt.
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PHONE = os.path.join(ROOT, "screenshots", "phone")
ICON = os.path.join(ROOT, "icon", "play_store_icon_512.png")
OUT_DIR = os.path.join(ROOT, "feature-graphic")
os.makedirs(OUT_DIR, exist_ok=True)
OUT = os.path.join(OUT_DIR, "feature_graphic_1024x500.png")

W, H = 1024, 500
TEAL_DARK = (10, 36, 36)
TEAL = (15, 118, 110)
TEAL_LIGHT = (45, 212, 191)
WHITE = (245, 248, 248)
MUTED = (190, 210, 208)


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


def gradient_diag(size, c1, c2):
    img = Image.new("RGB", size, c1)
    px = img.load()
    w, h = size
    for y in range(h):
        for x in range(w):
            t = (x + y * 1.3) / (w + h * 1.3)
            c = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
            px[x, y] = c
    return img


def glow(canvas, cx, cy, r, color, alpha):
    ov = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(*color, alpha))
    ov = ov.filter(ImageFilter.GaussianBlur(r // 3))
    canvas.alpha_composite(ov)


def build():
    canvas = gradient_diag((W, H), TEAL_DARK, (4, 18, 22)).convert("RGBA")
    glow(canvas, 80, 100, 240, TEAL_LIGHT, 70)
    glow(canvas, 980, 460, 280, TEAL, 90)
    glow(canvas, 540, -40, 200, TEAL_LIGHT, 50)

    d = ImageDraw.Draw(canvas)

    # Icon top-left
    if os.path.exists(ICON):
        icn = Image.open(ICON).convert("RGBA")
        icn.thumbnail((72, 72), Image.LANCZOS)
        canvas.alpha_composite(icn, (40, 38))

    fnt_brand = find_font(22, bold=True)
    d.text((128, 56), "GROCY FRIDGE SCANNER", fill=TEAL_LIGHT, font=fnt_brand)

    # Headline
    fnt_h = find_font(56, bold=True)
    d.text((40, 150), "Snap your fridge.", fill=WHITE, font=fnt_h)
    d.text((40, 215), "AI updates Grocy.", fill=WHITE, font=fnt_h)

    # Tagline
    fnt_t = find_font(22)
    d.text((40, 300),
           "On-device intelligence. No cloud. No data ever leaves your phone.",
           fill=MUTED, font=fnt_t)

    # Bullet row
    fnt_b = find_font(18, bold=True)
    bullets = ["GEMMA 4 ON-DEVICE", "SELF-HOSTED", "OPEN SOURCE"]
    bx = 40
    by = 360
    for b in bullets:
        bbox = d.textbbox((0, 0), b, font=fnt_b)
        w = bbox[2] - bbox[0]
        d.rounded_rectangle([bx - 12, by - 8, bx + w + 12, by + 26],
                            radius=14, outline=TEAL_LIGHT, width=2)
        d.text((bx, by), b, fill=TEAL_LIGHT, font=fnt_b)
        bx += w + 36

    # Right side: phone screenshot
    phone_path = os.path.join(PHONE, "03_inventory.png")
    if os.path.exists(phone_path):
        ph = Image.open(phone_path).convert("RGBA")
        target_h = 460
        ratio = target_h / ph.height
        target_w = int(ph.width * ratio)
        ph = ph.resize((target_w, target_h), Image.LANCZOS)

        # Frame
        pad = 6
        rim = 3
        fw, fh = target_w + (pad + rim) * 2, target_h + (pad + rim) * 2
        frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
        fd = ImageDraw.Draw(frame)
        fd.rounded_rectangle([0, 0, fw, fh], radius=42, fill=(6, 12, 12, 255))
        fd.rounded_rectangle([rim, rim, fw - rim, fh - rim], radius=40,
                             fill=(0, 0, 0, 255))
        frame.alpha_composite(ph, (pad + rim, pad + rim))

        # Tilt slightly
        frame = frame.rotate(-6, resample=Image.BICUBIC, expand=True)

        # Shadow
        sh = Image.new("RGBA",
                       (frame.width + 80, frame.height + 80), (0, 0, 0, 0))
        sd = ImageDraw.Draw(sh)
        sd.rounded_rectangle([40, 40, 40 + frame.width, 40 + frame.height],
                             radius=42, fill=(0, 0, 0, 170))
        sh = sh.filter(ImageFilter.GaussianBlur(22))

        px = W - frame.width + 40
        py = (H - frame.height) // 2
        canvas.alpha_composite(sh, (px - 40, py - 30))
        canvas.alpha_composite(frame, (px, py))

    canvas.convert("RGB").save(OUT, quality=95, optimize=True)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
