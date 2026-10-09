package com.barathiraja.jk.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.theme.HeroFill
import com.barathiraja.jk.ui.theme.Plex
import com.barathiraja.jk.ui.theme.pick

/*
 * JK's look, built on the app's red / black / yellow / white palette (see Theme.kt for the reasoning).
 * One red thing per screen: the action the owner should take next. Black carries text and the one hero card.
 * Yellow marks reward and the top spot. Everything else is white space, so the owner's eye has few places to go.
 * Status is always a word in a chip ([Tone]); the colour only backs the word up.
 */
internal object Jk {
    val Paper: Color @Composable @ReadOnlyComposable get() = pick(0xFFF6F6F4, 0xFF0B0B0B)
    /** Plain cards on the paper. */
    val Card: Color @Composable @ReadOnlyComposable get() = pick(0xFFFFFFFF, 0xFF161616)
    /** A panel or strip inside a card. */
    val Well: Color @Composable @ReadOnlyComposable get() = pick(0xFFF1F1EE, 0xFF1F1F1F)
    /** Text, icons and lines on paper and cards. */
    val Ink: Color @Composable @ReadOnlyComposable get() = pick(0xFF0A0A0A, 0xFFF2F2F2)
    val OnInk: Color @Composable @ReadOnlyComposable get() = pick(0xFFFFFFFF, 0xFF0A0A0A)
    /** The charcoal hero card; white text on it in both modes. */
    val Hero: Color @Composable @ReadOnlyComposable get() = HeroFill
    /** Text on yellow, in both modes. */
    val Black = Color(0xFF0A0A0A)
    /** Red as a fill: the main action. White text on it (5.2:1). */
    val Red = Color(0xFFD7141E)
    /** Red as text or an icon on paper and cards. */
    val RedText: Color @Composable @ReadOnlyComposable get() = pick(0xFFD7141E, 0xFFFF5A5F)
    val Yellow = Color(0xFFFFC629)
    val Muted: Color @Composable @ReadOnlyComposable get() = pick(0xFF5C5C5C, 0xFFA3A3A3)
    val Line: Color @Composable @ReadOnlyComposable get() = pick(0xFFE6E6E3, 0xFF262626)
    /** Red text on a dark card (both modes): lighter than [Red] so it reads on charcoal. */
    val RedOnDark = Color(0xFFFF5A5F)
    /** On the hero card. */
    val OnDarkMuted = Color(0xFFA3A3A3)
    val OnDarkSoft = Color(0xFFD4D4D4)
    val DarkTrack = Color(0xFF44474D)
    val DarkStrip: Color @Composable @ReadOnlyComposable get() = pick(0xFF33363B, 0xFF383B40)
}

/**
 * A status word's look. [TOP]: yellow, the best. [GOOD]: quiet grey, all fine (the usual case, so it stays calm).
 * [WARN]: pale yellow, worth a look. [BAD]: pale red, needs the owner. [NONE]: outlined, nothing yet.
 */
internal enum class Tone {
    TOP, GOOD, WARN, BAD, NONE;

    val fill: Color @Composable @ReadOnlyComposable get() = when (this) {
        TOP -> Jk.Yellow
        GOOD -> pick(0xFFEFEFEC, 0xFF262626)
        WARN -> pick(0xFFFFF2C7, 0xFF2A2410)
        BAD -> pick(0xFFFDE4E4, 0xFF2A1214)
        NONE -> Color.Transparent
    }
    val ink: Color @Composable @ReadOnlyComposable get() = when (this) {
        TOP -> Jk.Black
        GOOD -> Jk.Ink
        WARN -> pick(0xFF0A0A0A, 0xFFFFD966)
        BAD -> pick(0xFF9E0F16, 0xFFFFB4B6)
        NONE -> pick(0xFF3D3D3D, 0xFFD4D4D4)
    }
}

internal fun plex(size: TextUnit, weight: FontWeight = FontWeight.Normal, line: TextUnit = TextUnit.Unspecified, tracking: TextUnit = 0.sp) =
    TextStyle(fontFamily = Plex, fontSize = size, fontWeight = weight, lineHeight = line, letterSpacing = tracking)
