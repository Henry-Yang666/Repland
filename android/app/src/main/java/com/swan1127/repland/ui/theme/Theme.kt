package com.swan1127.repland.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CalmAccentDark,
    onPrimary = NightPaper,
    primaryContainer = Color(0xFF263F46),
    onPrimaryContainer = Color(0xFFCBE7ED),
    secondary = Color(0xFFD4C4F0),
    secondaryContainer = Color(0xFF383149),
    onSecondaryContainer = Color(0xFFEFE7FA),
    tertiary = Color(0xFFDCC0A0),
    tertiaryContainer = Color(0xFF483929),
    onTertiaryContainer = Color(0xFFF3E8D8),
    background = NightPaper,
    onBackground = Color(0xFFE9EBF1),
    surface = NightPaper,
    surfaceContainerLow = Color(0xFF232730),
    surfaceContainer = Color(0xFF292E38),
    surfaceContainerHigh = Color(0xFF303642),
    surfaceContainerHighest = Color(0xFF39404D),
    outline = Color(0xFF9097A7),
    outlineVariant = Color(0xFF404653),
    onSurface = Color(0xFFE9EBF1),
    onSurfaceVariant = Color(0xFFB8BECA),
)

private val LightColorScheme = lightColorScheme(
    primary = CalmAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F3F5),
    onPrimaryContainer = Color(0xFF234B55),
    secondary = Color(0xFF666079),
    secondaryContainer = Color(0xFFF1EEF8),
    onSecondaryContainer = Color(0xFF494158),
    tertiary = Color(0xFF80613E),
    tertiaryContainer = Color(0xFFFFF5DC),
    onTertiaryContainer = Color(0xFF574225),
    background = WarmPaper,
    onBackground = Color(0xFF242831),
    surface = WarmPaper,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F2F7),
    surfaceContainerHigh = Color(0xFFEBEDF4),
    surfaceContainerHighest = Color(0xFFE3E6F0),
    outline = Color(0xFF818695),
    outlineVariant = Color(0xFFE3E6EF),
    onSurface = Color(0xFF242831),
    onSurfaceVariant = Color(0xFF606572),
)

@Composable
fun ReplandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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

    ReplandMotionProvider {
        CompositionLocalProvider(LocalReplandVisuals provides if (darkTheme) DarkVisuals else LightVisuals) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography,
                shapes = ReplandShapes,
                content = content
            )
        }
    }
}
