package `in`.melo.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import `in`.melo.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils

val LocalMeloDarkTheme = staticCompositionLocalOf { false }
val LocalShowScrollbar = staticCompositionLocalOf { true }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun MeloStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

// Every Material role is neutral, including fixed roles used by transport controls.
// Explicit roles prevent Material's default lavender palette leaking into sheets.
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF1F0EC), onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF242424), onPrimaryContainer = Color(0xFFF1F0EC),
    secondary = Color(0xFFD3D2CE), onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF292929), onSecondaryContainer = Color(0xFFF1F0EC),
    tertiary = Color(0xFFE4E3DF), onTertiary = Color(0xFF111111),
    tertiaryContainer = Color(0xFF292929), onTertiaryContainer = Color(0xFFF1F0EC),
    background = Color(0xFF0B0B0B), onBackground = Color(0xFFF1F0EC),
    surface = Color(0xFF101010), onSurface = Color(0xFFF1F0EC),
    surfaceVariant = Color(0xFF282828), onSurfaceVariant = Color(0xFFADADA9),
    surfaceContainerLowest = Color(0xFF0B0B0B), surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF191919), surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF2C2C2C), surfaceBright = Color(0xFF383838),
    surfaceDim = Color(0xFF101010), surfaceTint = Color.Transparent,
    outline = Color(0xFF858581), outlineVariant = Color(0xFF383838),
    inverseSurface = Color(0xFFE8E7E3), inverseOnSurface = Color(0xFF171717),
    inversePrimary = Color(0xFF292929),
    error = Color(0xFFF1F0EC), onError = Color(0xFF111111),
    errorContainer = Color(0xFF343434), onErrorContainer = Color(0xFFF1F0EC),
    primaryFixed = Color(0xFFF1F0EC), primaryFixedDim = Color(0xFFD3D2CE),
    onPrimaryFixed = Color(0xFF111111), onPrimaryFixedVariant = Color(0xFF303030),
    secondaryFixed = Color(0xFFE4E3DF), secondaryFixedDim = Color(0xFFD3D2CE),
    onSecondaryFixed = Color(0xFF111111), onSecondaryFixedVariant = Color(0xFF303030),
    tertiaryFixed = Color(0xFFF1F0EC), tertiaryFixedDim = Color(0xFFF1F0EC),
    onTertiaryFixed = Color(0xFF111111), onTertiaryFixedVariant = Color(0xFF303030),
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF171717), onPrimary = Color(0xFFF7F6F2),
    primaryContainer = Color(0xFFE4E3DF), onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF383838), onSecondary = Color(0xFFF7F6F2),
    secondaryContainer = Color(0xFFE4E3DF), onSecondaryContainer = Color(0xFF171717),
    tertiary = Color(0xFF292929), onTertiary = Color(0xFFF7F6F2),
    tertiaryContainer = Color(0xFFE4E3DF), onTertiaryContainer = Color(0xFF171717),
    background = Color(0xFFF7F6F2), onBackground = Color(0xFF171717),
    surface = Color(0xFFF7F6F2), onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFE4E3DF), onSurfaceVariant = Color(0xFF5C5C58),
    surfaceContainerLowest = Color(0xFFF7F6F2), surfaceContainerLow = Color(0xFFF0EFEB),
    surfaceContainer = Color(0xFFECEBE7), surfaceContainerHigh = Color(0xFFE4E3DF),
    surfaceContainerHighest = Color(0xFFDCDCD8), surfaceBright = Color(0xFFFAF9F5),
    surfaceDim = Color(0xFFDCDCD8), surfaceTint = Color.Transparent,
    outline = Color(0xFF73736F), outlineVariant = Color(0xFFCBCBC7),
    inverseSurface = Color(0xFF171717), inverseOnSurface = Color(0xFFF1F0EC),
    inversePrimary = Color(0xFFF1F0EC),
    error = Color(0xFF292929), onError = Color(0xFFF7F6F2),
    errorContainer = Color(0xFFE4E3DF), onErrorContainer = Color(0xFF171717),
    primaryFixed = Color(0xFF171717), primaryFixedDim = Color(0xFF292929),
    onPrimaryFixed = Color(0xFFF7F6F2), onPrimaryFixedVariant = Color(0xFFE4E3DF),
    secondaryFixed = Color(0xFF292929), secondaryFixedDim = Color(0xFF383838),
    onSecondaryFixed = Color(0xFFF7F6F2), onSecondaryFixedVariant = Color(0xFFE4E3DF),
    tertiaryFixed = Color(0xFF171717), tertiaryFixedDim = Color(0xFF171717),
    onTertiaryFixed = Color(0xFFF7F6F2), onTertiaryFixedVariant = Color(0xFFE4E3DF),
)

@Composable
fun MeloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    content: @Composable () -> Unit
) {
    // Melo keeps the interface monochrome regardless of wallpaper or album art.
    val finalColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MeloStatusBarStyle(
        color = finalColorScheme.background,
        navigationColor = finalColorScheme.background
    )

    val reducedMotion = `in`.melo.presentation.utils.observeReducedMotion()
    CompositionLocalProvider(
        LocalMeloDarkTheme provides darkTheme,
        `in`.melo.presentation.utils.LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
