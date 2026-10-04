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
 * Paper and ink: warm off-white paper, black ink, one mustard accent and soft pastels for people (the look
 * first built for the owner's home). Cards are plain white with large rounds. Status colours are only used
 * next to a word, and every text colour is at least 4.5:1 on its surface. Dark mode swaps to near-black paper
 * with mustard as the accent, since black ink would vanish.
 */
private object Palette {
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long) = if (Palette.dark) Color(dark) else Color(light)

/** Solid fill for hero cards; white text on it is 21:1 (light) and 15:1 (dark). */
val HeroBlue: Color get() = pick(0xFF000000, 0xFF26251F)

/** The accent: progress, links, highlights. Ink on paper; mustard on dark paper. */
val Ember: Color get() = pick(0xFF000000, 0xFFF0D68C)
/** Mustard, for fills only (never text on paper: too light). */
val Mustard: Color = Color(0xFFF0D68C)
/** Good / done. */
val Leaf: Color get() = pick(0xFF0F766E, 0xFF2DD4BF)
/** Watch / in progress. */
val Sun: Color get() = pick(0xFFB45309, 0xFFFBBF24)
/** Bad / missed. */
val Alert: Color get() = pick(0xFFB91C1C, 0xFFF87171)
/** Information, water: same accent, to keep the palette to one colour. */
val Aqua: Color get() = pick(0xFF000000, 0xFFF0D68C)
/** Calm: meditation, sleep. */
val Violet: Color get() = pick(0xFF6D28D9, 0xFFA78BFA)

private val Light = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0D68C),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF000000),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8E9C0),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    error = Color(0xFFB91C1C),
    background = Color(0xFFEEECE7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFEEECE7),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF5F3EE),
    onSurfaceVariant = Color(0xFF5A5A57),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF5F3EE),
    surfaceContainerHighest = Color(0xFFE7E4DD),
    outline = Color(0xFF8A8780),
    outlineVariant = Color(0xFFE5E3DE),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF0D68C),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3A3322),
    onPrimaryContainer = Color(0xFFF8E9C0),
    secondary = Color(0xFFF0D68C),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2E2A20),
    onSecondaryContainer = Color(0xFFF8E9C0),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF000000),
    error = Color(0xFFF87171),
    background = Color(0xFF0F0F0E),
    onBackground = Color(0xFFF5F3EE),
    surface = Color(0xFF0F0F0E),
    onSurface = Color(0xFFF5F3EE),
    surfaceVariant = Color(0xFF242320),
    onSurfaceVariant = Color(0xFFABABAB),
    surfaceContainer = Color(0xFF1C1B19),
    surfaceContainerHigh = Color(0xFF242320),
    surfaceContainerHighest = Color(0xFF2E2D29),
    outline = Color(0xFF6B6A66),
    outlineVariant = Color(0xFF2A2927),
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
