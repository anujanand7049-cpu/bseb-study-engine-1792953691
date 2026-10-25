package `in`.bseb.studyengine.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Semantic BSEB Academic Palette (Deep Nalanda Indigo + Warm Mithila Saffron + Emerald Success)
val NalandaIndigoLight = Color(0xFF1A365D)
val NalandaIndigoContainerLight = Color(0xFFD6E4FF)
val MithilaSaffronLight = Color(0xFFC05621)
val MithilaSaffronContainerLight = Color(0xFFFFEBD6)
val EmeraldSuccess = Color(0xFF1E7E34)
val CrimsonMistake = Color(0xFFC53030)

val NalandaIndigoDark = Color(0xFF90CDF4)
val NalandaIndigoContainerDark = Color(0xFF1E3A5F)
val MithilaSaffronDark = Color(0xFFFBD38D)
val MithilaSaffronContainerDark = Color(0xFF5A2B0E)

private val LightColorPalette = lightColorScheme(
    primary = NalandaIndigoLight,
    onPrimary = Color.White,
    primaryContainer = NalandaIndigoContainerLight,
    onPrimaryContainer = Color(0xFF0A192F),
    secondary = MithilaSaffronLight,
    onSecondary = Color.White,
    secondaryContainer = MithilaSaffronContainerLight,
    onSecondaryContainer = Color(0xFF3B1806),
    tertiary = EmeraldSuccess,
    error = CrimsonMistake,
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF1A202C),
    surface = Color.White,
    onSurface = Color(0xFF1A202C),
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF4A5568)
)

private val DarkColorPalette = darkColorScheme(
    primary = NalandaIndigoDark,
    onPrimary = Color(0xFF0A192F),
    primaryContainer = NalandaIndigoContainerDark,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = MithilaSaffronDark,
    onSecondary = Color(0xFF3B1806),
    secondaryContainer = MithilaSaffronContainerDark,
    onSecondaryContainer = Color(0xFFFFEBD6),
    tertiary = Color(0xFF68D391),
    error = Color(0xFFFEB2B2),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

// Devanagari-friendly Typography with generous line heights so matras never clip on phone screens
val BsebTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 31.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)

@Composable
fun BsebStudyEngineTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorPalette
        else -> LightColorPalette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BsebTypography,
        content = content
    )
}
