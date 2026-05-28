"""
Render a stylized fridge-interior illustration to use inside the Scanner viewport.
Outputs a PNG sized to the camera viewport (918x810 at 1008x2244 device res).
"""
import os
import random
from PIL import Image, ImageDraw, ImageFilter

W, H = 918, 810
OUT = os.path.join(os.path.dirname(__file__), "fridge_interior.png")


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(size, top, bottom, direction="vertical"):
    img = Image.new("RGB", size, top)
    px = img.load()
    w, h = size
    for y in range(h):
        t = y / max(1, h - 1)
        if direction == "vertical":
            c = lerp(top, bottom, t)
            for x in range(w):
                px[x, y] = c
    return img


def soft_shadow(img, box, color=(0, 0, 0, 80), blur=12):
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    d.rectangle(box, fill=color)
    overlay = overlay.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(overlay)


def container(img, x, y, w, h, body, top=None, label=None, kind="bottle"):
    d = ImageDraw.Draw(img)
    if kind == "bottle":
        # Body
        d.rounded_rectangle([x, y + h * 0.20, x + w, y + h], radius=int(w * 0.15), fill=body)
        # Neck
        nw = int(w * 0.42)
        d.rectangle([x + (w - nw) // 2, y + h * 0.08, x + (w + nw) // 2, y + h * 0.30], fill=body)
        # Cap
        cw = int(w * 0.52)
        cap_color = top or (60, 60, 60)
        d.rounded_rectangle([x + (w - cw) // 2, y, x + (w + cw) // 2, y + h * 0.12],
                            radius=4, fill=cap_color)
    elif kind == "carton":
        d.rectangle([x, y, x + w, y + h], fill=body)
        # Roof
        d.polygon([(x, y + h * 0.18), (x + w * 0.5, y), (x + w, y + h * 0.18)], fill=top or body)
    elif kind == "jar":
        d.rounded_rectangle([x, y + h * 0.10, x + w, y + h], radius=int(w * 0.18), fill=body)
        d.rounded_rectangle([x + w * 0.05, y, x + w * 0.95, y + h * 0.16],
                            radius=4, fill=top or (180, 160, 120))
    elif kind == "can":
        d.rounded_rectangle([x, y, x + w, y + h], radius=int(w * 0.10), fill=body)
        d.rectangle([x, y + h * 0.06, x + w, y + h * 0.10], fill=(240, 240, 240))
        d.rectangle([x, y + h * 0.90, x + w, y + h * 0.94], fill=(240, 240, 240))
    elif kind == "produce":
        # round shape
        d.ellipse([x, y, x + w, y + h], fill=body)
    elif kind == "pack":
        # bag-like
        d.rounded_rectangle([x, y, x + w, y + h], radius=int(w * 0.08), fill=body)
    # Label band
    if label and kind in ("bottle", "carton", "jar", "can", "pack"):
        if kind == "bottle":
            lb = [x + w * 0.05, y + h * 0.55, x + w * 0.95, y + h * 0.85]
        elif kind == "carton":
            lb = [x + w * 0.05, y + h * 0.40, x + w * 0.95, y + h * 0.70]
        elif kind == "jar":
            lb = [x + w * 0.07, y + h * 0.40, x + w * 0.93, y + h * 0.75]
        elif kind == "can":
            lb = [x + w * 0.04, y + h * 0.30, x + w * 0.96, y + h * 0.70]
        else:
            lb = [x + w * 0.05, y + h * 0.40, x + w * 0.95, y + h * 0.70]
        d.rounded_rectangle(lb, radius=4, fill=label)


def build():
    # Outer interior gradient — cool whitish blue-grey, with a soft vignette
    img = gradient((W, H), (235, 240, 245), (200, 210, 220)).convert("RGBA")

    d = ImageDraw.Draw(img)

    # Shelf dividers (3 shelves)
    shelf_color = (180, 188, 196, 255)
    shelf_h = 16
    shelf_ys = [int(H * 0.30), int(H * 0.55), int(H * 0.80)]
    for y in shelf_ys:
        d.rectangle([0, y, W, y + shelf_h], fill=shelf_color)
        soft_shadow(img, [0, y + shelf_h, W, y + shelf_h + 30],
                    color=(0, 0, 0, 60), blur=14)

    # Side walls (slight shadow on left/right)
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.rectangle([0, 0, 60, H], fill=(0, 0, 0, 30))
    od.rectangle([W - 60, 0, W, H], fill=(0, 0, 0, 30))
    overlay = overlay.filter(ImageFilter.GaussianBlur(8))
    img.alpha_composite(overlay)

    # Shelf 1 (top): bottles + jar + carton
    y0 = int(H * 0.04)
    container(img, 60, y0, 90, int(H * 0.26), body=(250, 240, 200), top=(180, 100, 50),
              label=(220, 180, 40), kind="bottle")  # honey-ish
    container(img, 175, y0, 100, int(H * 0.26), body=(255, 255, 255), top=(15, 118, 110),
              label=(15, 118, 110), kind="bottle")  # milk
    container(img, 300, y0, 110, int(H * 0.26), body=(255, 255, 255), top=(220, 80, 70),
              label=(220, 80, 70), kind="carton")  # juice carton
    container(img, 430, y0 + 18, 90, int(H * 0.21), body=(245, 230, 180), top=(70, 50, 30),
              label=(200, 130, 50), kind="jar")  # peanut butter
    container(img, 545, y0, 95, int(H * 0.26), body=(40, 140, 70), top=(40, 140, 70),
              label=(255, 220, 60), kind="bottle")  # green bottle
    container(img, 660, y0 + 10, 100, int(H * 0.24), body=(80, 50, 30), top=(50, 30, 20),
              label=(220, 200, 80), kind="jar")  # nutella-ish
    container(img, 780, y0, 100, int(H * 0.26), body=(235, 60, 60), top=(180, 30, 30),
              label=(255, 255, 255), kind="bottle")  # ketchup

    # Shelf 2 (middle): cheese, eggs box, yogurts, butter
    y1 = int(H * 0.34)
    # Cheese (yellow rectangular pack)
    container(img, 50, y1, 140, int(H * 0.18), body=(245, 210, 90),
              label=(220, 50, 60), kind="pack")
    # Yogurt cups (4 small cups)
    yogurt_color = [(245, 245, 245), (255, 220, 180), (255, 200, 200), (240, 235, 200)]
    for i, col in enumerate(yogurt_color):
        cx = 210 + i * 70
        d.rounded_rectangle([cx, y1 + 30, cx + 60, y1 + 145], radius=8, fill=col)
        d.rectangle([cx - 2, y1 + 26, cx + 62, y1 + 38], fill=(220, 220, 220))
    # Egg carton
    egg_x = 510
    d.rounded_rectangle([egg_x, y1 + 20, egg_x + 200, y1 + 150], radius=10, fill=(170, 140, 100))
    for i in range(3):
        for j in range(2):
            cx = egg_x + 30 + i * 55
            cy = y1 + 50 + j * 50
            d.ellipse([cx - 18, cy - 18, cx + 18, cy + 18], fill=(245, 235, 215))
    # Butter
    container(img, 730, y1 + 20, 150, int(H * 0.16), body=(255, 240, 180), top=None,
              label=(220, 50, 60), kind="pack")

    # Shelf 3 (lower): produce drawer + cans
    y2 = int(H * 0.58)
    # Big produce drawer
    d.rounded_rectangle([40, y2, W - 40, y2 + int(H * 0.22)], radius=12, fill=(225, 230, 230))
    # Apples
    container(img, 80, y2 + 20, 70, 70, body=(220, 50, 50), kind="produce")
    container(img, 150, y2 + 30, 60, 60, body=(220, 60, 50), kind="produce")
    container(img, 215, y2 + 25, 65, 65, body=(70, 150, 70), kind="produce")
    # Oranges
    container(img, 295, y2 + 25, 70, 70, body=(255, 140, 40), kind="produce")
    container(img, 370, y2 + 30, 60, 60, body=(255, 150, 50), kind="produce")
    # Lemons
    container(img, 445, y2 + 35, 50, 50, body=(255, 230, 80), kind="produce")
    container(img, 500, y2 + 38, 50, 50, body=(255, 235, 100), kind="produce")
    # Spinach pack (green)
    container(img, 575, y2 + 20, 130, 130, body=(60, 130, 70), label=(255, 255, 255), kind="pack")
    # Strawberries pack
    container(img, 730, y2 + 20, 150, 130, body=(220, 60, 80), label=(255, 255, 255), kind="pack")

    # Bottom row: cans
    y3 = int(H * 0.82)
    can_specs = [
        (60, (180, 50, 50), (255, 240, 80)),    # tomato sauce
        (155, (40, 40, 40), (250, 220, 100)),   # black beans
        (250, (210, 170, 60), (200, 80, 60)),   # corn
        (345, (50, 100, 160), (255, 255, 255)), # tuna
        (440, (180, 50, 50), (255, 240, 80)),   # tomato sauce
        (535, (40, 40, 40), (250, 220, 100)),   # beans
        (630, (200, 150, 50), (50, 80, 50)),    # soup
        (725, (50, 100, 160), (255, 255, 255)), # tuna
        (820, (180, 50, 50), (255, 240, 80)),   # tomato sauce
    ]
    for x, body, label in can_specs:
        container(img, x, y3, 80, 130, body=body, label=label, kind="can")

    # Slight vignette
    vignette = Image.new("RGBA", img.size, (0, 0, 0, 0))
    vd = ImageDraw.Draw(vignette)
    for i in range(20):
        a = int(8 * (i / 20))
        vd.rectangle([i, i, W - i, H - i], outline=(0, 0, 0, a), width=1)
    img.alpha_composite(vignette)

    # Light fall-off from top (overhead light)
    light = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(light)
    cx, cy = W // 2, -100
    for i in range(60, 0, -1):
        r = int(W * (i / 30))
        a = int(70 * ((i / 60) ** 2))
        ld.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 240, a))
    light = light.filter(ImageFilter.GaussianBlur(40))
    img.alpha_composite(light)

    img.save(OUT)
    print(f"Wrote {OUT} ({img.size})")


if __name__ == "__main__":
    build()
