package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PitchGreen500,
    onPrimary = PitchEmerald950,
    primaryContainer = PitchEmerald700,
    onPrimaryContainer = PitchGreen200,
    secondary = BrazilGold500,
    onSecondary = PitchEmerald950,
    secondaryContainer = BrazilGold900,
    onSecondaryContainer = BrazilGold400,
    tertiary = InfoSky,
    onTertiary = PitchEmerald950,
    background = PitchEmerald950,
    onBackground = TextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = SlateCardElevated,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    outline = Color(0xFF28473A)
)

private val LightColorScheme = lightColorScheme(
    primary = PitchEmerald700,
    onPrimary = Color.White,
    primaryContainer = PitchGreen200,
    onPrimaryContainer = PitchEmerald950,
    secondary = BrazilGold500,
    onSecondary = PitchEmerald950,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = BrazilGold900,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = DaylightBg,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHigh = Color(0xFFDCE7E0),
    error = AlertRed,
    onError = Color.White,
    outline = Color(0xFFB5C9BE)
)

val BrasManagerShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BrasManagerTheme(
    darkTheme: Boolean = true, // Default to crisp night-stadium tactical dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BrasManagerTypography,
        shapes = BrasManagerShapes,
        content = content
    )
}
