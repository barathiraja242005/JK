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
 * Athletic red: one hue (red) on charcoal and white, with standard status colours.
 *
 *  - Off-white page and white cards (about 60%): clean and breathable, so the red and charcoal stand out.
 *  - Charcoal (about 30%): text, the bottom bar and the one dark hero card per page. Strength and weight that
 *    balance the energy of red (near-black text is 19:1 on white).
 *  - Deep athletic red (about 7%): the main action on a screen and "you are here". Red reads as intensity and
 *    action, the core of a fitness brand. White on it is 4.8:1.
 *  - Bright red (about 3%): emphasis on the dark cards (progress, the top number), a lighter step of the same
 *    hue so the hierarchy needs no new colour. It is only used on charcoal (5.4:1) or as a fill with dark text.
 *  - Green, amber and error red: only for states (done, needs a look, went wrong), never decoration, so they
 *    keep their meaning. Status always comes with a word.
 *
 * Every text colour is at least 4.5:1 on its surface (WCAG AA).
 */
private object Palette {
    var dark by mutableStateOf(false)
}

/** A colour that follows light/dark mode. */
internal fun pick(light: Long, dark: Long) = if (Palette.dark) Color(dark) else Color(light)

/** Deep athletic red as a fill (buttons, the open tab). White text on it is 4.8:1. */
val Red: Color = Color(0xFFD92D20)
/** Bright red for emphasis on dark cards, or as a fill with dark text. */
val Accent: Color = Color(0xFFFF4B4B)
/** Success green as a fill (ticks); dark icons on it. */
val Success: Color = Color(0xFF22C55E)
/** Amber as a fill (waiting, medals); dark text on it. */
val Amber: Color = Color(0xFFF59E0B)
/** Charcoal; the fill of hero cards and the bottom bar. */
val Ink: Color = Color(0xFF17181C)

/** Solid fill for hero cards; white text on it in both modes. */
val HeroBlue: Color get() = pick(0xFF17181C, 0xFF1F2126)

/** The accent for text, icons and progress: deep red on white, bright red on black. */
val Ember: Color get() = pick(0xFFD92D20, 0xFFFF4B4B)
/** Good / done. */
val Leaf: Color get() = pick(0xFF15803D, 0xFF4ADE80)
/** Watch / in progress. */
val Sun: Color get() = pick(0xFFB45309, 0xFFFBBF24)
/** Bad / missed. */
val Alert: Color get() = pick(0xFFDC2626, 0xFFF87171)
/** Information, water: the accent. */
val Aqua: Color get() = pick(0xFFD92D20, 0xFFFF4B4B)
/** Calm: meditation, sleep. A quiet slate, so calm screens stay calm. */
val Violet: Color get() = pick(0xFF667085, 0xFF98A2B3)

private val Light = lightColorScheme(
    primary = Color(0xFFD92D20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDECEA),
    onPrimaryContainer = Color(0xFFB42318),
    secondary = Color(0xFF17181C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F1F3),
    onSecondaryContainer = Color(0xFF0B0C0F),
    tertiary = Color(0xFFFF4B4B),
    onTertiary = Color(0xFF0B0C0F),
    error = Color(0xFFDC2626),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF0B0C0F),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF0B0C0F),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF667085),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F1F3),
    surfaceContainerHighest = Color(0xFFE1E3E6),
    outline = Color(0xFF98A2B3),
    outlineVariant = Color(0xFFE1E3E6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF4B4B),
    onPrimary = Color(0xFF0B0C0F),
    primaryContainer = Color(0xFF2D1214),
    onPrimaryContainer = Color(0xFFFFB4B0),
    secondary = Color(0xFFF5F5F6),
    onSecondary = Color(0xFF0B0C0F),
    secondaryContainer = Color(0xFF1F2126),
    onSecondaryContainer = Color(0xFFF5F5F6),
    tertiary = Color(0xFFFF4B4B),
    onTertiary = Color(0xFF0B0C0F),
    error = Color(0xFFF87171),
    background = Color(0xFF0B0C0F),
    onBackground = Color(0xFFF5F5F6),
    surface = Color(0xFF0B0C0F),
    onSurface = Color(0xFFF5F5F6),
    surfaceVariant = Color(0xFF1F2126),
    onSurfaceVariant = Color(0xFF98A2B3),
    surfaceContainer = Color(0xFF17181C),
    surfaceContainerHigh = Color(0xFF1F2126),
    surfaceContainerHighest = Color(0xFF2A2D33),
    outline = Color(0xFF667085),
    outlineVariant = Color(0xFF2A2D33),
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

/** Sentence case; bold headings for a confident, athletic voice; regular body text never below 15sp. */
private val JkType = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.p(40, 48, FontWeight.Bold, -1.0),
        displayMedium = t.displayMedium.p(34, 42, FontWeight.Bold, -0.8),
        displaySmall = t.displaySmall.p(28, 34, FontWeight.Bold, -0.5),
        headlineLarge = t.headlineLarge.p(24, 30, FontWeight.Bold, -0.4),
        headlineMedium = t.headlineMedium.p(21, 28, FontWeight.Bold, -0.3),
        headlineSmall = t.headlineSmall.p(18, 24, FontWeight.SemiBold, -0.2),
        titleLarge = t.titleLarge.p(17, 24, FontWeight.SemiBold),
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
