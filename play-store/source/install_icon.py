"""
Install the new icon into the Android app's mipmap and drawable resources.

Strategy:
  - For each density, write a PNG foreground (transparent bg) into mipmap-{d}/ic_launcher_foreground.png
  - For each density, write a PNG background (gradient) into mipmap-{d}/ic_launcher_background.png
  - Write legacy ic_launcher.png and ic_launcher_round.png (already-masked) into mipmap-{d}/
  - Rewrite mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml to use mipmap drawables
  - Delete the old vector ic_launcher_foreground.xml drawable
"""
import os
import sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
APP_RES = os.path.join(ROOT, "app", "src", "main", "res")
ICON_DIR = os.path.join(ROOT, "play-store", "icon")

# Adaptive icon legacy sizes for the 108dp foreground/background canvas
# (foreground/background are 108x108dp, system masks them down to 66dp content)
DENSITIES = [
    ("mdpi", 108),       # 1.0x
    ("hdpi", 162),       # 1.5x
    ("xhdpi", 216),      # 2.0x
    ("xxhdpi", 324),     # 3.0x
    ("xxxhdpi", 432),    # 4.0x
]
# Legacy launcher PNG sizes (already-masked round/square)
LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

master = Image.open(os.path.join(ICON_DIR, "master_1024.png")).convert("RGBA")
fg = Image.open(os.path.join(ICON_DIR, "foreground_1024.png")).convert("RGBA")
bg = Image.open(os.path.join(ICON_DIR, "background_1024.png")).convert("RGBA")
rounded = Image.open(os.path.join(ICON_DIR, "preview_rounded_512.png")).convert("RGBA")
circle = Image.open(os.path.join(ICON_DIR, "preview_round_512.png")).convert("RGBA")

for name, sz in DENSITIES:
    d = os.path.join(APP_RES, f"mipmap-{name}")
    os.makedirs(d, exist_ok=True)
    fg.resize((sz, sz), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"))
    bg.resize((sz, sz), Image.LANCZOS).save(os.path.join(d, "ic_launcher_background.png"))

    legacy = LEGACY_SIZES[name]
    rounded.resize((legacy, legacy), Image.LANCZOS).save(os.path.join(d, "ic_launcher.png"))
    circle.resize((legacy, legacy), Image.LANCZOS).save(os.path.join(d, "ic_launcher_round.png"))

print("PNG densities written.")

# Rewrite adaptive icon XMLs
adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
'''
anydpi = os.path.join(APP_RES, "mipmap-anydpi-v26")
os.makedirs(anydpi, exist_ok=True)
with open(os.path.join(anydpi, "ic_launcher.xml"), "w", encoding="utf-8") as f:
    f.write(adaptive_xml)
with open(os.path.join(anydpi, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
    f.write(adaptive_xml)
print("Adaptive icon XMLs rewritten.")

# Remove old vector foreground drawable so resource compiler doesn't complain
old_vec = os.path.join(APP_RES, "drawable", "ic_launcher_foreground.xml")
if os.path.exists(old_vec):
    os.remove(old_vec)
    print(f"Removed {old_vec}")
