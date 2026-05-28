"""Make small previews of tablet shots so the assistant can view them."""
from PIL import Image
import os, glob

for src_dir in [r"h:\grocy\play-store\screenshots\tablet-7in",
                r"h:\grocy\play-store\screenshots\tablet-10in"]:
    out_dir = os.path.join(r"h:\grocy\play-store\source\preview",
                           os.path.basename(src_dir))
    os.makedirs(out_dir, exist_ok=True)
    for p in glob.glob(os.path.join(src_dir, "*.png")):
        img = Image.open(p)
        img.thumbnail((1100, 800), Image.LANCZOS)
        name = os.path.basename(p).replace(".png", "_preview.png")
        img.save(os.path.join(out_dir, name))
        print(name, img.size)
