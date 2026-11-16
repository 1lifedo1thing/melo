# Dial UI adaptations

Melo's Home visual update uses the local source of [steeben/Dial](https://github.com/steeben/Dial), revision `dc8799a`, under GPL-3.0, the same license as this repository.

- `EvenFling.kt` and `DialPageFling.kt` adapt Dial's frame timing and page settling. Melo uses its own list keys and section spacing, and turns page settling off with Android's reduced motion setting.
- `MeloHomeStyle.kt` adapts the thumbnail rim from `DialTileEdge.kt` and mood colors from `TopPicksShelf.kt`. Mood artwork is drawn locally as static paths; it does not run a permanent animation on the feed.
- Speed Dial uses three square columns, 8 dp gaps, 10 dp corners, a bottom title gradient, single-line labels, and size fitting above the persistent playback area.
- Home categories use selected state, touch feedback, and the existing category feed behavior. Mixes remains to the left of Speed Dial; artist pages settle vertically before the discovery feed.
- Recommendation cards use large cropped artwork with a dark caption gradient. Mood cards use individual colors and emblems. Recents uses four readable rows per horizontal page.
- All track shelves, including Quick Picks and Recents, use four rows per horizontal page, a visible next-page cover, and individual overflow menus.
- Mood chips use a consistent 40 dp surface inside a 48 dp touch target. The active chip supplies the selection label, without a duplicate page heading.
- The Melo dotted mark follows Dial's spin, rebuild, and rest sequence on refresh and category requests. Pull progress lights the dots; matching dial and feed skeletons cover loading content. Cached content remains available during automatic refreshes.
- Home settings persist Artwork gradients, Touch animations, and Animate refresh logo. Motion also follows Android's Remove animations setting.
- Header action menus use a rounded 24 dp panel, a subtle surface gradient, a light-catching border, divided action groups, and 56 dp minimum rows.
- Podcast show and episode cards share a 168 dp width, square artwork, and reserved two-line title and subtitle blocks, keeping their dimensions equal even when metadata is short or absent.

The Melo identity is retained. App typography uses Dial's bundled SF Pro Display regular, medium, semibold, bold and heavy fonts. The dark canvas is black; surfaces and dividers use restrained neutral tones. Navigation uses a selected capsule. Playback and service authentication are unchanged by this visual pass.

## Player artwork

`PlayerArtworkBackdrop.kt` adapts Dial's `ArtworkMeshBackdrop.kt` at the same revision: a flipped 6 by 6 grid of the actual cover colors, seeded horizontal rotation below the first row, modest saturation lift, and smoothstep interpolation into a 32 by 32 texture. Decode and mesh generation run away from the UI thread. A 64-entry cache avoids repeated work. The previous image stays visible during loading and crossfades for 900 ms; Android's reduced motion setting disables the fade. A stronger scrim preserves Melo's white foreground on bright covers. Melo's transport controls, progress, touch targets, gestures and callbacks retain their existing layout and behavior. Appearance settings can disable the player artwork gradient independently.

YouTube discovery uses the device's network country, SIM country, time-zone region, then system locale. Old manual country choices migrate to automatic device country; there is no country selector in Settings.

## Shared app surfaces

`MeloSurfaces.kt` supplies the panel gradient and rim used across settings, controls, and bottom sheets. All Material modal sheets retain their existing state, dismissal, focus, and inset behavior through `MeloBottomSheet`; track options, playlist actions, sort, cast, timer, lyric options, conversion, and artist selection use this shared presentation. The queue and mini player use the same surface treatment.

Sheet decoration is drawn inside Material's translated content, with the native sheet owning its background and clipping. Drawing the panel on the incoming modal modifier clips content before the sheet's movement and produces a blank panel above it. Home's feed category row is a real sticky list header below the app bar, so it reserves space above Top Picks and other shelves instead of covering them.

Library uses square album covers with metadata outside the image, consistent compact artwork rows, and category tabs with the same selected fill and rounded outline as Home. Selection callbacks, numbered badges, source badges, long-press menus, and compact mode are preserved. Search uses the same input surface, filter chips, and lighter result rows. Collection and artist pages keep a neutral canvas under their full-color artwork. Settings use stronger section headings, consistent icon wells, and rounded panels. Surface gradients and touch animations are available in Appearance; Android's Remove animations setting remains authoritative.
