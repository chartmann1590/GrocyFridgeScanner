"""
Build the Play Store feature video:
  * Generate scene slides (1920x1080) using the phone shots and a teal background.
  * Generate per-scene voiceover with Windows SAPI (Zira voice).
  * Stitch with ffmpeg, burn captions, output mp4 + srt + script.
"""
import os
import subprocess
import json
import wave
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.dirname(os.path.abspath(__file__))
PHONE = os.path.join(ROOT, "screenshots", "phone")
ICON = os.path.join(ROOT, "icon", "play_store_icon_512.png")
OUT_DIR = os.path.join(ROOT, "video")
os.makedirs(OUT_DIR, exist_ok=True)
BUILD_DIR = os.path.join(SRC, "video_build")
os.makedirs(BUILD_DIR, exist_ok=True)

W, H = 1920, 1080
TEAL_DARK = (10, 36, 36)
TEAL = (15, 118, 110)
TEAL_LIGHT = (45, 212, 191)
WHITE = (245, 248, 248)
MUTED = (190, 210, 208)

# Scene list — narration + caption (caption can break onto two lines using \n).
SCENES = [
    {
        "name": "01_intro",
        "narration": "Meet Grocy Fridge Scanner. Your kitchen's new AI inventory clerk.",
        "caption": "Meet Grocy Fridge Scanner.\nYour kitchen's new AI inventory clerk.",
        "phone": None,            # title card uses the app icon, not a screenshot
        "headline": "Grocy Fridge Scanner",
        "subline": "On-device AI for your Grocy stock.",
    },
    {
        "name": "02_scan",
        "narration": "Just point your phone at any fridge or cupboard, and snap.",
        "caption": "Point. Snap. Done.",
        "phone": "02_analyzing.png",
        "headline": "Snap a photo",
        "subline": "Fridge, cupboard — anywhere food lives.",
    },
    {
        "name": "03_ai",
        "narration": "On-device AI detects every item. No cloud. No data ever leaves your phone.",
        "caption": "On-device AI.\nNothing leaves your phone.",
        "phone": "02_analyzing.png",
        "headline": "On-device intelligence",
        "subline": "Gemma 4 runs locally. No cloud.",
    },
    {
        "name": "04_review",
        "narration": "Review every change, then sync straight to your self-hosted Grocy.",
        "caption": "Review and sync to your Grocy.",
        "phone": "03_inventory.png",
        "headline": "Review then sync",
        "subline": "Every change in your control.",
    },
    {
        "name": "05_manage",
        "narration": "Track every shelf at a glance, with one-tap quick actions.",
        "caption": "Quick actions. Real-time stock.",
        "phone": "04_inventory_quick_actions.png",
        "headline": "Manage your stock",
        "subline": "Use one. Add one. Use all.",
    },
    {
        "name": "06_outro",
        "narration": "Grocy Fridge Scanner. Open source. Available now.",
        "caption": "Grocy Fridge Scanner.\nOpen source. Available now.",
        "phone": None,
        "headline": "Grocy Fridge Scanner",
        "subline": "MIT licensed. Built with Kotlin and Gemma.",
    },
]


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


def frame_phone(ph: Image.Image, target_h: int) -> Image.Image:
    ratio = target_h / ph.height
    target_w = int(ph.width * ratio)
    p = ph.resize((target_w, target_h), Image.LANCZOS).convert("RGBA")
    pad = 12
    rim = 4
    fw, fh = target_w + (pad + rim) * 2, target_h + (pad + rim) * 2
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    fd.rounded_rectangle([0, 0, fw, fh], radius=66, fill=(6, 12, 12, 255))
    fd.rounded_rectangle([rim, rim, fw - rim, fh - rim], radius=62,
                         fill=(0, 0, 0, 255))
    frame.alpha_composite(p, (pad + rim, pad + rim))
    return frame


def build_slide(scene, out_path):
    canvas = gradient_diag((W, H), TEAL_DARK, (4, 18, 22)).convert("RGBA")
    glow(canvas, 200, 200, 420, TEAL_LIGHT, 50)
    glow(canvas, 1750, 880, 480, TEAL, 70)
    glow(canvas, 960, -80, 320, TEAL_LIGHT, 40)

    d = ImageDraw.Draw(canvas)
    fnt_brand = find_font(28, bold=True)

    if scene["phone"] is None:
        # Title card — icon center-left, big headline center-right
        if os.path.exists(ICON):
            icn = Image.open(ICON).convert("RGBA")
            target = 360
            icn = icn.resize((target, target), Image.LANCZOS)
            # Shadow under icon
            sh = Image.new("RGBA", (target + 80, target + 80), (0, 0, 0, 0))
            sd = ImageDraw.Draw(sh)
            sd.rounded_rectangle([40, 40, 40 + target, 40 + target],
                                 radius=88, fill=(0, 0, 0, 170))
            sh = sh.filter(ImageFilter.GaussianBlur(28))
            ix = 280
            iy = (H - target) // 2
            canvas.alpha_composite(sh, (ix - 40, iy - 30))
            canvas.alpha_composite(icn, (ix, iy))

        d.text((760, 360), "GROCY FRIDGE SCANNER",
               fill=TEAL_LIGHT, font=fnt_brand)
        fnt_h = find_font(96, bold=True)
        # Wrap headline
        d.text((760, 420), scene["headline"], fill=WHITE, font=fnt_h)
        fnt_s = find_font(38)
        d.text((760, 600), scene["subline"], fill=MUTED, font=fnt_s)
    else:
        # Phone shot composition: text on left, phone on right
        ph = Image.open(os.path.join(PHONE, scene["phone"])).convert("RGBA")
        framed = frame_phone(ph, H - 220)
        sh = Image.new("RGBA",
                       (framed.width + 120, framed.height + 120),
                       (0, 0, 0, 0))
        sd = ImageDraw.Draw(sh)
        sd.rounded_rectangle([60, 60, 60 + framed.width, 60 + framed.height],
                             radius=66, fill=(0, 0, 0, 180))
        sh = sh.filter(ImageFilter.GaussianBlur(32))
        px = W - framed.width - 130
        py = (H - framed.height) // 2
        canvas.alpha_composite(sh, (px - 60, py - 40))
        canvas.alpha_composite(framed, (px, py))

        d.text((130, 170), "GROCY FRIDGE SCANNER",
               fill=TEAL_LIGHT, font=fnt_brand)
        fnt_h = find_font(78, bold=True)
        # Headline — manual wrap on space if too long
        words = scene["headline"].split(" ")
        text_w = px - 130 - 60
        lines, cur = [], ""
        for w in words:
            tentative = (cur + " " + w).strip()
            bb = d.textbbox((0, 0), tentative, font=fnt_h)
            if bb[2] - bb[0] > text_w:
                if cur:
                    lines.append(cur)
                cur = w
            else:
                cur = tentative
        if cur:
            lines.append(cur)

        y = 240
        for ln in lines:
            d.text((130, y), ln, fill=WHITE, font=fnt_h)
            y += 90
        y += 20
        d.rectangle([130, y, 220, y + 6], fill=TEAL_LIGHT)
        y += 40
        fnt_s = find_font(36)
        # Subline wrap
        words = scene["subline"].split(" ")
        lines, cur = [], ""
        for w in words:
            tentative = (cur + " " + w).strip()
            bb = d.textbbox((0, 0), tentative, font=fnt_s)
            if bb[2] - bb[0] > text_w:
                if cur:
                    lines.append(cur)
                cur = w
            else:
                cur = tentative
        if cur:
            lines.append(cur)
        for ln in lines:
            d.text((130, y), ln, fill=MUTED, font=fnt_s)
            y += 50

    canvas.convert("RGB").save(out_path, quality=92, optimize=True)


def synth_voice(text: str, out_path: str):
    """Use Windows SAPI Zira voice to TTS the line to a WAV file."""
    # Write a small inline PS script and run it
    script = f'''
Add-Type -AssemblyName System.Speech;
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;
try {{ $s.SelectVoice("Microsoft Zira Desktop") }} catch {{}}
$s.Rate = -1;
$s.Volume = 100;
$s.SetOutputToWaveFile("{out_path}");
$s.Speak({json.dumps(text)});
$s.Dispose();
'''
    subprocess.run(["powershell", "-NoProfile", "-Command", script],
                   check=True)


def wav_duration(path: str) -> float:
    with wave.open(path, "rb") as w:
        frames = w.getnframes()
        rate = w.getframerate()
        return frames / float(rate)


def to_srt_time(t: float) -> str:
    h = int(t // 3600)
    m = int((t % 3600) // 60)
    s = int(t % 60)
    ms = int((t - int(t)) * 1000)
    return f"{h:02d}:{m:02d}:{s:02d},{ms:03d}"


def build():
    print("== Generating slides + voiceover ==")
    durations = []
    for i, sc in enumerate(SCENES, 1):
        slide_png = os.path.join(BUILD_DIR, f"slide_{i:02d}.png")
        wav = os.path.join(BUILD_DIR, f"voice_{i:02d}.wav")
        build_slide(sc, slide_png)
        synth_voice(sc["narration"], wav)
        d = wav_duration(wav)
        # Allow a small tail so captions don't cut off
        d = max(d + 0.6, 3.5)
        durations.append(d)
        print(f"  scene {i}: {d:.2f}s  '{sc['narration']}'")

    # Combine voiceover wavs into one with the scene durations as silence-padded segments.
    print("\n== Building audio track ==")
    # Use ffmpeg concat + apad per-clip to enforce scene timing
    audio_inputs = []
    audio_filters = []
    for i, d in enumerate(durations):
        wav = os.path.join(BUILD_DIR, f"voice_{i+1:02d}.wav")
        audio_inputs += ["-i", wav]
        audio_filters.append(
            f"[{i}:a]apad=whole_dur={d:.3f}[a{i}]"
        )
    n = len(durations)
    concat_inputs = "".join(f"[a{i}]" for i in range(n))
    afilter = ";".join(audio_filters) + f";{concat_inputs}concat=n={n}:v=0:a=1[aout]"
    audio_out = os.path.join(BUILD_DIR, "audio.m4a")
    cmd = ["ffmpeg", "-y", *audio_inputs, "-filter_complex", afilter,
           "-map", "[aout]", "-c:a", "aac", "-b:a", "192k", audio_out]
    subprocess.run(cmd, check=True, capture_output=True)
    print(f"  wrote {audio_out}")

    # Build the visual concat: one segment per slide using `-loop 1 -t duration`
    print("\n== Building video track ==")
    video_inputs = []
    video_filters = []
    for i, d in enumerate(durations):
        slide = os.path.join(BUILD_DIR, f"slide_{i+1:02d}.png")
        video_inputs += ["-loop", "1", "-t", f"{d:.3f}", "-i", slide]
        # Optional gentle zoom: zoompan would be overkill; just scale and fps.
        video_filters.append(
            f"[{i}:v]scale=1920:1080,setsar=1,format=yuv420p,fps=30[v{i}]"
        )
    concat_v = "".join(f"[v{i}]" for i in range(n))
    vfilter = ";".join(video_filters) + f";{concat_v}concat=n={n}:v=1:a=0[vout]"
    video_out = os.path.join(BUILD_DIR, "video_no_audio.mp4")
    cmd = ["ffmpeg", "-y", *video_inputs, "-filter_complex", vfilter,
           "-map", "[vout]", "-c:v", "libx264", "-pix_fmt", "yuv420p",
           "-preset", "medium", "-crf", "18", video_out]
    subprocess.run(cmd, check=True, capture_output=True)
    print(f"  wrote {video_out}")

    # Build SRT
    print("\n== Writing captions ==")
    srt_path = os.path.join(OUT_DIR, "captions.srt")
    with open(srt_path, "w", encoding="utf-8") as f:
        cursor = 0.0
        for i, sc in enumerate(SCENES, 1):
            start = cursor
            end = cursor + durations[i - 1]
            f.write(f"{i}\n")
            f.write(f"{to_srt_time(start)} --> {to_srt_time(end)}\n")
            f.write(sc["caption"] + "\n\n")
            cursor = end
    print(f"  wrote {srt_path}")

    # Write narration script
    script_path = os.path.join(OUT_DIR, "voiceover_script.txt")
    with open(script_path, "w", encoding="utf-8") as f:
        f.write("Grocy Fridge Scanner — Play Store promo video script\n")
        f.write("=" * 60 + "\n\n")
        cursor = 0.0
        for i, sc in enumerate(SCENES, 1):
            f.write(f"[Scene {i} — {durations[i-1]:.2f}s, "
                    f"{to_srt_time(cursor)}]\n")
            f.write(f"Narration: {sc['narration']}\n")
            f.write(f"Caption:   {sc['caption']}\n\n")
            cursor += durations[i - 1]
        f.write(f"Total runtime: {cursor:.2f}s\n")
    print(f"  wrote {script_path}")

    # Combine video + audio + burn captions
    print("\n== Final mux + caption burn ==")
    final = os.path.join(OUT_DIR, "promo_with_captions.mp4")
    # ffmpeg subtitles filter needs forward slashes
    srt_for_filter = srt_path.replace("\\", "/").replace(":", "\\:")
    sub_style = ("Fontname=Segoe UI Semibold,"
                 "Fontsize=44,"
                 "PrimaryColour=&H00F5F8F8,"
                 "OutlineColour=&H000C2424,"
                 "BorderStyle=3,"
                 "Outline=4,"
                 "Shadow=0,"
                 "MarginV=80,"
                 "Alignment=2")
    vfilter = f"subtitles='{srt_for_filter}':force_style='{sub_style}'"
    cmd = ["ffmpeg", "-y", "-i", video_out, "-i", audio_out,
           "-vf", vfilter,
           "-c:v", "libx264", "-pix_fmt", "yuv420p",
           "-preset", "medium", "-crf", "20",
           "-c:a", "copy", "-shortest", final]
    res = subprocess.run(cmd, capture_output=True)
    if res.returncode != 0:
        print("FFMPEG STDERR:")
        print(res.stderr.decode("utf-8", errors="replace"))
        raise SystemExit("ffmpeg failed")
    print(f"  wrote {final}")

    # Also write a no-captions version in case the user prefers to upload to YouTube
    # and use YouTube's auto-captions feature.
    final_clean = os.path.join(OUT_DIR, "promo_no_captions.mp4")
    cmd = ["ffmpeg", "-y", "-i", video_out, "-i", audio_out,
           "-c:v", "copy", "-c:a", "copy", "-shortest", final_clean]
    subprocess.run(cmd, check=True, capture_output=True)
    print(f"  wrote {final_clean}")


if __name__ == "__main__":
    build()
