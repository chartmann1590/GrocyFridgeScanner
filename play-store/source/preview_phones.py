"""Make small preview thumbnails of phone screenshots so the assistant can view them."""
from PIL import Image
import os, glob

src_dir = r"h:\grocy\play-store\screenshots\phone"
out_dir = r"h:\grocy\play-store\source\preview"
os.makedirs(out_dir, exist_ok=True)
for p in glob.glob(os.path.join(src_dir, "*.png")):
    img = Image.open(p)
    img.thumbnail((700, 1500), Image.LANCZOS)
    name = os.path.basename(p).replace(".png", "_preview.png")
    img.save(os.path.join(out_dir, name))
    print(name, img.size)
