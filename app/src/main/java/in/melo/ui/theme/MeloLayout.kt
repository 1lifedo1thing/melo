package `in`.melo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp

/** Shared geometry and spacing for the listening surfaces. */
object MeloLayout {
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space12 = 48.dp

    val PageGutter = Space4
    val ItemGap = Space2
    val SectionGap = Space8

    val ArtworkShape = RoundedCornerShape(8.dp)
    val CardShape = RoundedCornerShape(12.dp)
    val PanelShape = RoundedCornerShape(20.dp)
    val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val PillShape = RoundedCornerShape(percent = 50)
    val ArtistShape = CircleShape

    val ButtonLarge = 56.dp
    val ButtonMedium = 48.dp
    val ControlSize = 48.dp
    val ControlVisual = 40.dp

    val ArtCompact = 48.dp
    val ArtList = 56.dp
    val ArtHero = 64.dp
    val RowMinHeight = 72.dp

    val IconSmall = 16.dp
    val IconMedium = 20.dp
    val IconDefault = 24.dp
    val IconLarge = 32.dp
}
