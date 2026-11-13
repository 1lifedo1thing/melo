# Melo visual system

Melo is a native music player for deliberate listening. The UI should give music room and make the next playback action obvious. Nothing OS is a reference for restraint and dotted typography, not a source of copied assets.

## Tokens

| Role | Dark | Light |
| --- | --- | --- |
| Background | `#0B0B0B` | `#F7F6F2` |
| Surface | `#101010` | `#F7F6F2` |
| Raised surface | `#191919` | `#ECEBE7` |
| Main content | `#F1F0EC` | `#171717` |
| Secondary content | `#ADADA9` | `#5C5C58` |
| Subtle divider | `#383838` | `#CBCBC7` |

Use semantic Material color roles rather than ad hoc screen colors. Meaningful control outlines use the stronger `outline` role. Primary and secondary text exceed WCAG AA normal-text contrast on their corresponding base surfaces. Error states use explicit messages as well as their surface treatment.

## App mark

The app mark uses the user-supplied dotted play symbol, open ring, subtle outer dot ring, and grey audio pulse. `melo-mark.svg` preserves the supplied vector master; `melo-logo.png` is the 512 px export. Android foreground geometry follows the SVG without a baked-in rounded mask. Themed icons preserve the layers through opacity. The small notification icon retains the main mark and audio pulse, omitting the decorative outer ring for clarity. Album artwork stays in full colour.

## Typography

- Doto: the identity only. Bundled and licensed in `Doto-OFL.txt`.
- Google Sans Flex, ROND axis 0: clear, unrounded app typography from the existing bundled font.
- Platform monospace: small editorial labels.
- Display: 44/50 sp; headline: 28/34; title: 22/28; body: 16/24; supporting text: 14/21.
- No stretched text, remote font dependency, or all-caps body copy.

## Layout and interaction

Use a 4/8 dp spacing rhythm with 24 dp home gutters and 24 dp between sections. Artwork has 8 dp corners; cards have 12–20 dp corners. Primary actions can be circular. Android controls keep at least 48 dp targets.

The home screen has a compact title and shortcuts, with real listening content from configurable shelves immediately below. Empty shelves stay hidden; the header always offers useful routes. Rendering options live under Appearance → Advanced. Motion follows Android’s Remove animations preference. Artist photos and song, album, and playlist covers retain their original colours. The interface around the artwork uses the neutral theme tokens.

Navigation has three stable tabs: Home, Search, Library. The active tab uses its filled icon, readable label, dot, and selected semantics. Playback has fixed skip targets and a round play/pause button. No resizing or bouncing controls; no animated decorative record.

Light and system appearance remain available in settings. Optional album-derived player palettes are inherited advanced customization; the new-install default uses the global monochrome palette.

## Review boundaries

The logo and banner are identity assets, not app screenshots. Live playback must be verified on ARM64 with decoded PCM past the lead-in, after seeks, and across track changes before claiming an end-to-end playback test.
