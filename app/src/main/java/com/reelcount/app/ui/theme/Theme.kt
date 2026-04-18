package com.reelcount.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Color Palette ─────────────────────────────────────────────────────────
// Deep purple/violet dark theme with vibrant accent

val PurplePrimary = Color(0xFF9C6FFF)       // Vibrant purple
val PurpleContainer = Color(0xFF3D2B7A)     // Deep purple container
val PurpleOnContainer = Color(0xFFE8DEFF)
val NeonAccent = Color(0xFF00E5CC)          // Teal/neon accent
val InstagramOrange = Color(0xFFFF6B35)
val YoutubeRed = Color(0xFFFF2D55)
val SurfaceDark = Color(0xFF0F0B1E)         // Very dark background
val SurfaceVariantDark = Color(0xFF1A1530)  // Card background
val OnSurfaceDark = Color(0xFFEEEBFF)
val OutlineDark = Color(0xFF3D3558)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color(0xFF1A0066),
    primaryContainer = PurpleContainer,
    onPrimaryContainer = PurpleOnContainer,
    secondary = NeonAccent,
    onSecondary = Color(0xFF003330),
    secondaryContainer = Color(0xFF004D47),
    onSecondaryContainer = Color(0xFF9FFFF4),
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF221D38),
    onSurfaceVariant = Color(0xFFC8C0E8),
    outline = OutlineDark,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B3FD4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF2200A8),
    secondary = Color(0xFF007B72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0FFF8),
    onSecondaryContainer = Color(0xFF00201D),
    background = Color(0xFFF8F5FF),
    onBackground = Color(0xFF1A1625),
    surface = Color.White,
    onSurface = Color(0xFF1A1625),
    surfaceVariant = Color(0xFFEDE6FF),
    onSurfaceVariant = Color(0xFF4A4060),
    outline = Color(0xFF7B7094)
)

val ReelCountTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        letterSpacing = (-2).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun ReelCountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReelCountTypography,
        content = content
    )
}
