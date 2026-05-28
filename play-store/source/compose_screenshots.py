"""
Compose final phone screenshots from raw device captures.

Steps:
  - Copy raw captures to play-store/screenshots/phone/<n>_<name>.png
  - Redact private Grocy URL + API key on the Settings shot
  - Build a populated Review screen composite using the real UI styling
"""
import os
import re
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "source")
OUT = os.path.join(ROOT, "screenshots", "phone")
os.makedirs(OUT, exist_ok=True)

# Font resolution: prefer Inter then fall back to system
def find_font(size, weight="regular"):
    candidates = [
        r"C:\Windows\Fonts\seguivar.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf" if weight == "bold" else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if weight == "bold" else r"C:\Windows\Fonts\arial.ttf",
    ]
    for p in candidates:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                continue
    return ImageFont.load_default()


def redact_settings(src_path: str, dst_path: str):
    """Cover the Grocy URL and API key portion with dummy text."""
    img = Image.open(src_path).convert("RGBA")
    d = ImageDraw.Draw(img)
    w, h = img.size

    # Coordinates derived from inspecting the actual capture (1008 wide).
    # URL field row: approx y=440..540 in original coords on a 1008x2244 capture.
    # API key field row: approx y=585..685.
    # The text starts after the icon at about x=110, ends well before the eye icon at x=920.
    # We blank the area with the same dark surface fill, then draw masked text.

    surface = (24, 31, 30, 255)  # match dark surface
    accent = (220, 226, 226, 255)

    # Detect device width to scale rectangle positions.
    # Original capture was 1008x2244; if other widths supplied, scale.
    base_w = 1008
    sx = w / base_w

    def s(x):
        return int(x * sx)

    # URL value area — wipe and repaint with a generic example.com URL.
    # The API key field already renders as dots, so it does not need redaction.
    url_box = [s(170), s(620), s(905), s(708)]
    d.rectangle(url_box, fill=surface)
    fnt = find_font(int(46 * sx), weight="regular")
    d.text((s(180), s(632)), "https://grocy.example.com", fill=accent, font=fnt)

    img.save(dst_path)


def crop_status_bar_notifications(src_path: str, dst_path: str):
    """Wipe notification icons (between the clock and the system icons) so
    unrelated apps don't show up in the Play Store shot."""
    img = Image.open(src_path).convert("RGBA")
    d = ImageDraw.Draw(img)
    w, h = img.size
    sx = w / 1008
    # Pixel 8 Pro status bar layout: clock at x≈0..165, notification icons x≈170..400,
    # padding, then system icons (battery + wifi + signal) on the right.
    # Wipe x=180..640 only, keeping the clock and right-side icons intact.
    d.rectangle([int(180 * sx), int(20 * sx), int(640 * sx), int(95 * sx)],
                fill=(15, 25, 24, 255))
    img.save(dst_path)


def add_caption_strip(src_path: str, dst_path: str, title: str, subtitle: str):
    """Optionally lay a marketing caption strip across the top safe area.

    Currently unused — we keep screenshots pristine — but the function is
    here in case you want to layer titles on each shot.
    """
    img = Image.open(src_path).convert("RGBA")
    w, h = img.size
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    sx = w / 1008
    d.rectangle([0, 0, w, int(220 * sx)], fill=(15, 118, 110, 235))
    fnt_t = find_font(int(60 * sx), weight="bold")
    fnt_s = find_font(int(34 * sx))
    d.text((int(40 * sx), int(48 * sx)), title, fill=(255, 255, 255, 255), font=fnt_t)
    d.text((int(40 * sx), int(130 * sx)), subtitle, fill=(220, 240, 235, 255), font=fnt_s)
    out = Image.alpha_composite(img, overlay)
    out.save(dst_path)


# Map of raw input -> final filename
PHONE_SCREENSHOTS = [
    ("01_scanner_raw.png", "01_scanner.png", "raw"),
    ("scan_immediate.png", "02_analyzing.png", "raw"),
    ("02_inventory_raw.png", "03_inventory.png", "raw"),
    ("02b_inventory_expanded_raw.png", "04_inventory_quick_actions.png", "raw"),
    ("03b_history_expanded_raw.png", "05_history_detail.png", "raw"),
    ("04_settings_raw.png", "06_settings.png", "redact_settings"),
]


def main():
    for src_name, dst_name, treatment in PHONE_SCREENSHOTS:
        src = os.path.join(SRC, src_name)
        dst = os.path.join(OUT, dst_name)
        if treatment == "raw":
            shutil.copyfile(src, dst)
        elif treatment == "redact_settings":
            redact_settings(src, dst)
        # also wipe notification icons in the status bar for a cleaner look
        crop_status_bar_notifications(dst, dst)
        print(f"  wrote {dst}")

    print(f"\n{len(PHONE_SCREENSHOTS)} phone screenshots written to {OUT}")


if __name__ == "__main__":
    main()
