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
 * Red, black, yellow on white: the "Ignite" palette.
 *
 * Why these four, and how much of each (60-30-10):
 *  - White (60%): page and cards. Space and calm lower the effort of reading, which matters most for an older
 *    owner scanning numbers. The page is a faint grey so white cards stand out without borders.
 *  - Black and charcoal (30%): black for text (19.8:1 on white, the clearest there is); charcoal, a softer
 *    shade of black, for the bottom bar and the one dark "hero" card per page. Both read as strength and
 *    authority, the core of gym culture; charcoal keeps the big dark areas from feeling heavy.
 *  - Red (about 7%): only the main action on a screen and "needs attention". Red raises arousal and draws the eye
 *    first (it is the colour people spot fastest), so it is kept for what the user should do or look at next.
 *    Using it sparingly keeps it meaningful: one red thing per view stands out (the isolation effect).
 *  - Yellow (about 3%): reward and celebration: awards, the top spot, done ticks. Yellow is the brightest hue and
 *    reads as optimism and achievement; black on yellow is the high-visibility pairing of sports kit and signage
 *    (12.6:1). Yellow is never used for text on white (too faint); it is a fill with black on it.
 *
 * Every text colour is at least 4.5:1 on its surface (WCAG AA); white on red is 5.2:1. Status is never shown by
 * colour alone; a word always goes with it. In dark mode red text lightens to coral so it stays readable, and red
 * buttons carry black text there.
 */
private object Palette {
    var dark by mutableStateOf(false)
}

/** A colour that follows light/dark mode. */
internal fun pick(light: Long, dark: Long) = if (Palette.dark) Color(dark) else Color(light)

/** Brand red as a fill (buttons, the open tab). White text on it is 5.2:1. */
val Red: Color = Color(0xFFD7141E)
/** Signal yellow, for fills only (awards, top spot, done). Always black text on it. */
val Yellow: Color = Color(0xFFFFC629)
/** Near-black ink, for text. */
val Ink: Color = Color(0xFF0A0A0A)
/** Charcoal: hero cards and the bottom bar. White text on it is 15:1. */
val Charcoal: Color = Color(0xFF232529)

/** Solid fill for hero cards; white text on it in both modes. */
val HeroBlue: Color get() = pick(0xFF232529, 0xFF2A2C31)

/** The accent for text, icons and progress: red on white, coral on black. */
val Ember: Color get() = pick(0xFFD7141E, 0xFFFF5A5F)
/** Good / done: ink, with a tick or a word next to it. */
val Leaf: Color get() = pick(0xFF0A0A0A, 0xFFF2F2F2)
/** Watch / in progress: dark gold on white, yellow on black. */
val Sun: Color get() = pick(0xFF8A6100, 0xFFFFC629)
/** Bad / missed. */
val Alert: Color get() = pick(0xFFB3121B, 0xFFFF8A8E)
/** Information, water: the accent. */
val Aqua: Color get() = pick(0xFFD7141E, 0xFFFF5A5F)
/** Calm: meditation, sleep. A quiet grey, so calm screens stay calm. */
val Violet: Color get() = pick(0xFF5C5C5C, 0xFFA3A3A3)

private val Light = lightColorScheme(
    primary = Color(0xFFD7141E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE4E4),
    onPrimaryContainer = Color(0xFF9E0F16),
    secondary = Color(0xFF0A0A0A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF2C7),
    onSecondaryContainer = Color(0xFF0A0A0A),
    tertiary = Color(0xFFFFC629),
    onTertiary = Color(0xFF0A0A0A),
    error = Color(0xFFB3121B),
    background = Color(0xFFF6F6F4),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFF6F6F4),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFEFEFEC),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F1EE),
    surfaceContainerHighest = Color(0xFFE6E6E3),
    outline = Color(0xFF8F8F8C),
    outlineVariant = Color(0xFFE6E6E3),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF5A5F),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF2A1214),
    onPrimaryContainer = Color(0xFFFFB4B6),
    secondary = Color(0xFFF2F2F2),
    onSecondary = Color(0xFF0A0A0A),
    secondaryContainer = Color(0xFF2A2410),
    onSecondaryContainer = Color(0xFFFFD966),
    tertiary = Color(0xFFFFC629),
    onTertiary = Color(0xFF0A0A0A),
    error = Color(0xFFFF8A8E),
    background = Color(0xFF0B0B0B),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0B0B0B),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFA3A3A3),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1F1F1F),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    outline = Color(0xFF6E6E6E),
    outlineVariant = Color(0xFF262626),
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
