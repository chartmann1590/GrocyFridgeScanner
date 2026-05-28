"""Crop strips of the settings screenshot to find URL/API rows."""
from PIL import Image
import os

src = r"h:\grocy\play-store\source\04_settings_raw.png"
out_dir = r"h:\grocy\play-store\source\inspect"
os.makedirs(out_dir, exist_ok=True)

img = Image.open(src)
print("size:", img.size)
# Save bands every 100px from 0..1500
for y in range(0, 1500, 100):
    band = img.crop((0, y, img.width, y + 100))
    band.save(os.path.join(out_dir, f"band_{y:04d}.png"))
print("done")
