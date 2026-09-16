package com.irsyadlabs.espbridge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.irsyadlabs.espbridge.core.model.AppTheme

val LocalAppTheme = staticCompositionLocalOf { AppTheme.ILLUSTRATIVE }

private val IllustrativeColors = lightColorScheme(
    primary = SketchTeal,
    onPrimary = Color.White,
    secondary = SketchPeach,
    onSecondary = Color.White,
    tertiary = SketchYellow,
    onTertiary = SketchBorder,
    background = SketchBg,
    onBackground = SketchBorder,
    surface = SketchSurface,
    onSurface = SketchBorder,
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = SketchBorder,
    outline = SketchBorder,
    outlineVariant = SketchBorder,
    error = SketchRed,
    onError = Color.White
)

private val ComicColors = lightColorScheme(
    primary = ComicPink,
    onPrimary = Color.White,
    secondary = ComicTeal,
    onSecondary = Color.White,
    tertiary = ComicYellow,
    onTertiary = ComicText,
    background = ComicBg,
    onBackground = ComicText,
    surface = Color.White,
    onSurface = ComicText,
    surfaceVariant = Color(0xFFFAE8FF),
    onSurfaceVariant = ComicLavender,
    outline = ComicBorder,
    outlineVariant = ComicBorder,
    error = ComicPink,
    onError = Color.White
)

@Composable
fun XichiTheme(
    theme: AppTheme = AppTheme.ILLUSTRATIVE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.ILLUSTRATIVE -> IllustrativeColors
        AppTheme.COMIC -> ComicColors
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EspTypography,
            content = content
        )
    }
}
