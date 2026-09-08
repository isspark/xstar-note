package com.xstar.notebook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val BrandGradient = listOf(
    Color(0xFF7048F0),
    Color(0xFFB23AE8),
    Color(0xFFF0518E),
)

val LocalAppGradient = staticCompositionLocalOf { BrandGradient }

@Composable
fun appGradientColors(): List<Color> = LocalAppGradient.current

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4CE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7DFFF),
    onPrimaryContainer = Color(0xFF21066B),
    secondary = Color(0xFF008C76),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9FF2E0),
    onSecondaryContainer = Color(0xFF00201B),
    tertiary = Color(0xFFFF6B35),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCE),
    onTertiaryContainer = Color(0xFF5A1900),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B20),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B20),
    surfaceVariant = Color(0xFFEBE0F5),
    onSurfaceVariant = Color(0xFF4C4554),
    outline = Color(0xFF7E7585),
    outlineVariant = Color(0xFFCFC4D9),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    surfaceContainerHighest = Color(0xFFE6E0E9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8B8FF),
    onPrimary = Color(0xFF310B91),
    primaryContainer = Color(0xFF4E2AC9),
    onPrimaryContainer = Color(0xFFE7DFFF),
    secondary = Color(0xFF80D9C5),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF9FF2E0),
    tertiary = Color(0xFFFFB59B),
    onTertiary = Color(0xFF5A1900),
    tertiaryContainer = Color(0xFF7E2E00),
    onTertiaryContainer = Color(0xFFFFDBCE),
    background = Color(0xFF141318),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF141318),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF2B2430),
    onSurfaceVariant = Color(0xFFCCC5D4),
    outline = Color(0xFF958F9E),
    outlineVariant = Color(0xFF4B4554),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerHighest = Color(0xFF332C38),
)

private val XstarTypography = Typography()

private val XstarShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun XstarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val gradient = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val primary = if (darkTheme) colorScheme.primaryContainer else colorScheme.primary
            val secondary = if (darkTheme) colorScheme.secondaryContainer else colorScheme.secondary
            val tertiary = if (darkTheme) colorScheme.tertiaryContainer else colorScheme.tertiary
            listOf(
                primary,
                lerp(primary, secondary, 0.48f),
                lerp(primary, tertiary, 0.62f),
            )
        }
        darkTheme -> BrandGradient.map { lerp(it, colorScheme.background, 0.26f) }
        else -> BrandGradient
    }

    CompositionLocalProvider(LocalAppGradient provides gradient) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XstarTypography,
            shapes = XstarShapes,
            content = content,
        )
    }
}
