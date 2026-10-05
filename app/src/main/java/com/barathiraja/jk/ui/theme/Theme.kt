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
 * Blue, lime, charcoal on white: the "Electric" palette.
 *
 *  - White (about 60%): the page. Clean and open, so the saturated blue and lime stay sharp. Cards are a soft
 *    cool grey, so they separate from the page without borders.
 *  - Charcoal (about 25%): text, the bottom bar and the one dark hero card per page. Strength and a premium
 *    athletic feel, grounding the brighter colours (17.7:1 for white on it).
 *  - Electric blue (about 10%): the main action on a screen and "you are here". Blue reads as confident and
 *    trustworthy, and suits the stable, focused work of running a gym. White on it is 5.4:1.
 *  - Lime (about 5%): reward and the top spot: awards, progress, done ticks. Yellow-green is the most eye-catching
 *    hue and sits opposite blue, so it pops. It is only ever a fill with charcoal text on it (11.8:1), never text.
 *  - Silver and soft grey: lines, tracks and quiet chips, a cool metallic tone that matches gym equipment.
 *  - Warm peach: the one warm colour, kept for "needs a look" so it stands apart from the cool palette.
 *
 * Every text colour is at least 4.5:1 on its surface (WCAG AA). Status is never shown by colour alone. In dark
 * mode blue text lightens so it stays readable; blue buttons keep white text.
 */
private object Palette {
    var dark by mutableStateOf(false)
}

/** A colour that follows light/dark mode. */
internal fun pick(light: Long, dark: Long) = if (Palette.dark) Color(dark) else Color(light)

/** Electric blue as a fill (buttons, the open tab). White text on it is 5.4:1. */
val Brand: Color = Color(0xFF1565D8)
/** Lime, for fills only (awards, top spot, progress). Always charcoal text on it. */
val Lime: Color = Color(0xFFA8E600)
/** Charcoal ink; also the fill of hero cards and the bottom bar. */
val Ink: Color = Color(0xFF17181C)

/** Solid fill for hero cards; white text on it in both modes. */
val HeroBlue: Color get() = pick(0xFF17181C, 0xFF1E2026)

/** The accent for text, icons and progress: blue on white, light blue on black. */
val Ember: Color get() = pick(0xFF1565D8, 0xFF5B9BFF)
/** Good / done: ink, with a tick or a word next to it. */
val Leaf: Color get() = pick(0xFF17181C, 0xFFF2F3F5)
/** Watch / in progress: warm brown on white, peach on black. */
val Sun: Color get() = pick(0xFF9A4A1E, 0xFFF4A07C)
/** Bad / missed. */
val Alert: Color get() = pick(0xFFB3261E, 0xFFFF8A80)
/** Information, water: the accent. */
val Aqua: Color get() = pick(0xFF1565D8, 0xFF5B9BFF)
/** Calm: meditation, sleep. A quiet grey, so calm screens stay calm. */
val Violet: Color get() = pick(0xFF5E6368, 0xFFA3A8AD)

private val Light = lightColorScheme(
    primary = Color(0xFF1565D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EDFB),
    onPrimaryContainer = Color(0xFF0E4BA6),
    secondary = Color(0xFF17181C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF9D2),
    onSecondaryContainer = Color(0xFF17181C),
    tertiary = Color(0xFFA8E600),
    onTertiary = Color(0xFF17181C),
    error = Color(0xFFB3261E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF17181C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17181C),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF5E6368),
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerHigh = Color(0xFFE8EAED),
    surfaceContainerHighest = Color(0xFFDDE0E4),
    outline = Color(0xFF8A9096),
    outlineVariant = Color(0xFFE8EAED),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    onPrimary = Color(0xFF0B0C0F),
    primaryContainer = Color(0xFF12233F),
    onPrimaryContainer = Color(0xFF9CC2FF),
    secondary = Color(0xFFF2F3F5),
    onSecondary = Color(0xFF0B0C0F),
    secondaryContainer = Color(0xFF26301A),
    onSecondaryContainer = Color(0xFFC9F25C),
    tertiary = Color(0xFFA8E600),
    onTertiary = Color(0xFF17181C),
    error = Color(0xFFFF8A80),
    background = Color(0xFF0B0C0F),
    onBackground = Color(0xFFF2F3F5),
    surface = Color(0xFF0B0C0F),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF1E2026),
    onSurfaceVariant = Color(0xFFA3A8AD),
    surfaceContainer = Color(0xFF17181C),
    surfaceContainerHigh = Color(0xFF1E2026),
    surfaceContainerHighest = Color(0xFF2A2D33),
    outline = Color(0xFF6B7075),
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
