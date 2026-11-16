from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "assets" / "img" / "readme"
OUTPUT = ASSETS / "melo-product-showcase.png"

WIDTH, HEIGHT = 2600, 1940
BACKGROUND = (8, 9, 10)
IVORY = (245, 243, 237)
MUTED = (174, 172, 166)
GOLD = (207, 178, 119)
EDGE = (53, 51, 47)

FONT_HEAVY = ROOT / "app" / "src" / "main" / "res" / "font" / "sf_pro_display_heavy.otf"
FONT_MEDIUM = ROOT / "app" / "src" / "main" / "res" / "font" / "sf_pro_display_medium.otf"
FONT_REGULAR = ROOT / "app" / "src" / "main" / "res" / "font" / "sf_pro_display_regular.otf"


def font(path: Path, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(path), size)


def tracked_text(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str,
                 typeface: ImageFont.FreeTypeFont, fill: tuple[int, int, int],
                 tracking: int = 3) -> None:
    x, y = xy
    for character in text:
        draw.text((x, y), character, font=typeface, fill=fill)
        x += draw.textlength(character, font=typeface) + tracking


def draw_wordmark(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    letters = (
        ("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
        ("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
        ("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
        ("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    )
    step, radius = 10, 2
    for letter_index, rows in enumerate(letters):
        for row_index, row in enumerate(rows):
            for column_index, pixel in enumerate(row):
                if pixel == "1":
                    px = x + letter_index * 62 + column_index * step
                    py = y + row_index * step
                    draw.ellipse((px - radius, py - radius, px + radius, py + radius), fill=IVORY)


def rounded_capture(image: Image.Image, size: tuple[int, int], radius: int) -> Image.Image:
    image = Image.open(image).convert("RGB").resize(size, Image.Resampling.LANCZOS)
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius=radius, fill=255)
    result = Image.new("RGBA", size, (0, 0, 0, 0))
    result.paste(image, (0, 0), mask)
    return result


def paste_phone(canvas: Image.Image, source: Path, x: int, y: int,
                width: int, height: int) -> None:
    pad = 11
    frame_box = (x - pad, y - pad, x + width + pad, y + height + pad)
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (frame_box[0], frame_box[1] + 20, frame_box[2], frame_box[3] + 20),
        radius=44, fill=(0, 0, 0, 190)
    )
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(26)))
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(frame_box, radius=44, fill=(21, 21, 20), outline=EDGE, width=3)
    capture = rounded_capture(source, (width, height), 32)
    canvas.alpha_composite(capture, (x, y))


def main() -> None:
    sources = {
        "speed_dial": ASSETS / "screens" / "speed-dial.png",
        "home": ASSETS / "screens" / "home-feed.png",
        "podcasts": ASSETS / "screens" / "podcasts.png",
        "player": ASSETS / "screens" / "now-playing.png",
    }
    for path in sources.values():
        if not path.is_file():
            raise SystemExit(f"Missing selected screenshot: {path}")

    canvas = Image.new("RGBA", (WIDTH, HEIGHT), (*BACKGROUND, 255))
    ambient = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ambient_draw = ImageDraw.Draw(ambient)
    ambient_draw.ellipse((760, 510, 1880, 1610), fill=(113, 83, 42, 32))
    canvas.alpha_composite(ambient.filter(ImageFilter.GaussianBlur(180)))

    draw = ImageDraw.Draw(canvas)
    draw_wordmark(draw, 124, 91)
    tracked_text(draw, (2050, 93), "ANDROID MUSIC · 1.1.0", font(FONT_MEDIUM, 21), MUTED, 3)
    draw.line((124, 171, WIDTH - 124, 171), fill=(47, 47, 45), width=2)

    tracked_text(draw, (124, 215), "A LISTENING SPACE FOR ALL YOUR MUSIC", font(FONT_MEDIUM, 19), GOLD, 4)
    draw.text((124, 255), "Your music. Your space.", font=font(FONT_HEAVY, 74), fill=IVORY)
    draw.text((124, 350), "Spotify · YouTube Music · your local library",
              font=font(FONT_REGULAR, 27), fill=MUTED)

    screen_width = 500
    screen_heights = {}
    for name, source in sources.items():
        with Image.open(source) as capture:
            screen_heights[name] = round(screen_width * capture.height / capture.width)
    screen_y = 458
    gap = 48
    row_width = screen_width * len(sources) + gap * (len(sources) - 1)
    start_x = (WIDTH - row_width) // 2
    captions = (
        ("SPEED DIAL", "Your favorites, one tap away"),
        ("HOME FEED", "Mood-led picks and music discovery"),
        ("PODCASTS", "Find shows and episode history"),
        ("NOW PLAYING", "Full-color art, focused controls"),
    )
    for index, (name, (title, subtitle)) in enumerate(zip(sources, captions), start=1):
        x = start_x + (index - 1) * (screen_width + gap)
        paste_phone(canvas, sources[name], x, screen_y, screen_width, screen_heights[name])
        caption_y = screen_y + screen_heights[name] + 30
        tracked_text(draw, (x, caption_y), f"0{index}  /  {title}", font(FONT_MEDIUM, 17), GOLD, 2)
        draw.text((x, caption_y + 34), subtitle, font=font(FONT_REGULAR, 20), fill=IVORY)

    footer_y = HEIGHT - 57
    draw.line((124, footer_y - 28, WIDTH - 124, footer_y - 28), fill=(47, 47, 45), width=2)
    tracked_text(draw, (124, footer_y), "MELO  /  OPEN SOURCE  /  ANDROID 8+", font(FONT_MEDIUM, 17), MUTED, 3)
    tracked_text(draw, (2174, footer_y), "V1.1.0", font(FONT_MEDIUM, 17), MUTED, 3)

    canvas.convert("RGB").save(OUTPUT, optimize=True)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
