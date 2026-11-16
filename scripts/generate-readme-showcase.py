from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "assets" / "img" / "readme"
OUTPUT = ASSETS / "melo-product-showcase.png"

WIDTH, HEIGHT = 2600, 2050
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
        "home": ASSETS / "screens" / "home-feed.png",
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
    tracked_text(draw, (2170, 93), "ANDROID MUSIC · 1.1.0", font(FONT_MEDIUM, 21), MUTED, 3)
    draw.line((124, 171, WIDTH - 124, 171), fill=(47, 47, 45), width=2)

    tracked_text(draw, (124, 214), "A MUSIC SPACE FOR ANDROID", font(FONT_MEDIUM, 19), GOLD, 4)
    headline = font(FONT_HEAVY, 92)
    draw.text((124, 260), "Your music.", font=headline, fill=IVORY)
    draw.text((124, 355), "Your space.", font=headline, fill=IVORY)
    draw.text((124, 471), "Spotify · YouTube Music · your local files", font=font(FONT_REGULAR, 29), fill=MUTED)

    screen_width = 540
    with Image.open(sources["home"]) as home_capture:
        screen_height = round(screen_width * home_capture.height / home_capture.width)
    screen_y = 545
    left_x, right_x = 124, WIDTH - 124 - screen_width
    paste_phone(canvas, sources["home"], left_x, screen_y, screen_width, screen_height)
    paste_phone(canvas, sources["player"], right_x, screen_y, screen_width, screen_height)

    center_left, center_right = 820, 1830
    center_width = center_right - center_left
    tracked_text(draw, (center_left, 793), "DISCOVER", font(FONT_MEDIUM, 19), GOLD, 4)
    draw.text((center_left, 839), "A home that moves\nwith your mood.", font=font(FONT_HEAVY, 58),
              fill=IVORY, spacing=6)
    draw.multiline_text((center_left, 1008),
                        "Pick a feeling, find something new,\nor jump back into a favorite.",
                        font=font(FONT_REGULAR, 27), fill=MUTED, spacing=10)
    draw.line((center_left, 1170, center_left + 84, 1170), fill=GOLD, width=4)
    tracked_text(draw, (center_left, 1213), "LISTEN YOUR WAY", font(FONT_MEDIUM, 18), MUTED, 3)

    tags = ("MOODS", "FRESH PICKS", "YOUR LIBRARY")
    tag_font = font(FONT_MEDIUM, 17)
    cursor = center_left
    tag_y = 1270
    for label in tags:
        text_width = int(sum(draw.textlength(char, font=tag_font) + 2 for char in label) + 38)
        draw.rounded_rectangle((cursor, tag_y, cursor + text_width, tag_y + 49), radius=24,
                               fill=(18, 18, 18), outline=(59, 57, 52), width=2)
        tracked_text(draw, (cursor + 19, tag_y + 15), label, tag_font, IVORY, 2)
        cursor += text_width + 13

    caption_y = screen_y + screen_height + 40
    tracked_text(draw, (left_x, caption_y), "01  /  HOME", font(FONT_MEDIUM, 20), GOLD, 3)
    draw.text((left_x, caption_y + 38), "Mood-led picks and fresh discovery", font=font(FONT_REGULAR, 25), fill=IVORY)
    tracked_text(draw, (right_x, caption_y), "02  /  NOW PLAYING", font(FONT_MEDIUM, 20), GOLD, 3)
    draw.text((right_x, caption_y + 38), "Artwork up front. Controls close by.", font=font(FONT_REGULAR, 25), fill=IVORY)

    footer_y = HEIGHT - 67
    draw.line((124, footer_y - 28, WIDTH - 124, footer_y - 28), fill=(47, 47, 45), width=2)
    tracked_text(draw, (124, footer_y), "MELO  /  OPEN SOURCE  /  ANDROID 8+", font(FONT_MEDIUM, 17), MUTED, 3)
    tracked_text(draw, (2174, footer_y), "V1.1.0", font(FONT_MEDIUM, 17), MUTED, 3)

    canvas.convert("RGB").save(OUTPUT, optimize=True)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
