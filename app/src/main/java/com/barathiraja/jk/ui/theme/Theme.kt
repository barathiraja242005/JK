package com.barathiraja.jk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.R
import com.barathiraja.jk.data.ThemeMode

/*
 * Minimal and professional: neutral greys, one blue accent, plain white cards with a fine border.
 * Blue is the accent because it is the colour most consistently read as trustworthy and competent.
 * Status colours are only used next to a word, and every text colour is at least 4.5:1 on its surface.
 */
private object Palette {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long) = if (Palette.dark) Color(dark) else Color(light)

/** Solid blue for hero cards; white text on it is 6.7:1 in both themes. */
val HeroBlue = Color(0xFF1D4ED8)

/** The accent: progress, links, highlights. */
val Ember: Color get() = pick(0xFF2563EB, 0xFF60A5FA)
/** Good / done. */
val Leaf: Color get() = pick(0xFF0F766E, 0xFF2DD4BF)
/** Watch / in progress. */
val Sun: Color get() = pick(0xFFB45309, 0xFFFBBF24)
/** Bad / missed. */
val Alert: Color get() = pick(0xFFB91C1C, 0xFFF87171)
/** Information, water: same accent, to keep the palette to one colour. */
val Aqua: Color get() = pick(0xFF2563EB, 0xFF60A5FA)
/** Calm: meditation, sleep. */
val Violet: Color get() = pick(0xFF6D28D9, 0xFFA78BFA)

private val Light = lightColorScheme(
    primary = Color(0xFF18181B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4F4F5),
    onPrimaryContainer = Color(0xFF18181B),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF4FF),
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    error = Color(0xFFB91C1C),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF09090B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF52525B),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF4F4F5),
    outline = Color(0xFFA1A1AA),
    outlineVariant = Color(0xFFE4E4E7),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFAFAFA),
    onPrimary = Color(0xFF09090B),
    primaryContainer = Color(0xFF27272A),
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF09090B),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF09090B),
    error = Color(0xFFF87171),
    background = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainer = Color(0xFF18181B),
    surfaceContainerHigh = Color(0xFF27272A),
    outline = Color(0xFF52525B),
    outlineVariant = Color(0xFF27272A),
)

/*
 * IBM Plex Sans everywhere (one family keeps it calm and consistent): a professional grotesque with
 * clearly different I, l and 1. IBM Plex Mono for join codes. Both SIL OFL.
 */
@OptIn(ExperimentalTextApi::class)
private fun plex(weight: Int) = Font(R.font.plex_sans, weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Plex = FontFamily(listOf(400, 500, 600, 700).map(::plex))
val CodeFont = FontFamily(Font(R.font.plex_mono, FontWeight.SemiBold))

private fun TextStyle.p(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) =
    copy(fontFamily = Plex, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking.sp)

/** Sentence case, semibold headings, regular body text never below 15sp. */
private val JkType = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.p(48, 56, FontWeight.SemiBold, -1.0),
        displayMedium = t.displayMedium.p(40, 48, FontWeight.SemiBold, -0.8),
        displaySmall = t.displaySmall.p(32, 40, FontWeight.SemiBold, -0.5),
        headlineLarge = t.headlineLarge.p(28, 36, FontWeight.SemiBold, -0.4),
        headlineMedium = t.headlineMedium.p(24, 32, FontWeight.SemiBold, -0.3),
        headlineSmall = t.headlineSmall.p(20, 28, FontWeight.SemiBold, -0.2),
        titleLarge = t.titleLarge.p(18, 26, FontWeight.SemiBold),
        titleMedium = t.titleMedium.p(16, 24, FontWeight.SemiBold),
        titleSmall = t.titleSmall.p(15, 22, FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.p(16, 24, FontWeight.Normal),
        bodyMedium = t.bodyMedium.p(15, 22, FontWeight.Normal),
        bodySmall = t.bodySmall.p(13, 18, FontWeight.Normal),
        labelLarge = t.labelLarge.p(14, 20, FontWeight.Medium, 0.1),
        labelMedium = t.labelMedium.p(12, 16, FontWeight.Medium, 0.3),
        labelSmall = t.labelSmall.p(11, 16, FontWeight.Medium, 0.4),
    )
}

@Composable
fun JkTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Written before any child reads the accents in this pass, so they always match the scheme.
    if (Palette.dark != dark) Palette.dark = dark
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = JkType, content = content)
}
