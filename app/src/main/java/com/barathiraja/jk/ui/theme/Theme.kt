package com.barathiraja.jk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.barathiraja.jk.R
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.data.ThemeMode

val Ember = Color(0xFFFF5A1F)
val EmberDeep = Color(0xFFD9400A)
val Aqua = Color(0xFF16B4D8)
val Leaf = Color(0xFF2FBF71)
val Violet = Color(0xFF8B6CF6)
val Sun = Color(0xFFFFB020)

private val Light = lightColorScheme(
    primary = EmberDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3D6),
    onPrimaryContainer = Color(0xFF3A1100),
    secondary = Color(0xFF0E7C94),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F3FB),
    onSecondaryContainer = Color(0xFF00313C),
    tertiary = Color(0xFF1E8C52),
    background = Color(0xFFF7F6F4),
    onBackground = Color(0xFF15161A),
    surface = Color(0xFFF7F6F4),
    onSurface = Color(0xFF15161A),
    surfaceVariant = Color(0xFFECEAE6),
    onSurfaceVariant = Color(0xFF5B5D63),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0EEEA),
    outline = Color(0xFFC9C6C0),
    outlineVariant = Color(0xFFE2DFDA),
)

private val Dark = darkColorScheme(
    primary = Ember,
    onPrimary = Color(0xFF1A0700),
    primaryContainer = Color(0xFF4A1A06),
    onPrimaryContainer = Color(0xFFFFD9C9),
    secondary = Aqua,
    onSecondary = Color(0xFF00242D),
    secondaryContainer = Color(0xFF07404D),
    onSecondaryContainer = Color(0xFFC9F1FA),
    tertiary = Leaf,
    background = Color(0xFF111214),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF111214),
    onSurface = Color(0xFFEDEDEF),
    surfaceVariant = Color(0xFF26282C),
    onSurfaceVariant = Color(0xFFA3A6AD),
    surfaceContainer = Color(0xFF1B1C1F),
    surfaceContainerHigh = Color(0xFF232529),
    outline = Color(0xFF45474D),
    outlineVariant = Color(0xFF2E3034),
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700), inter(800), inter(900))

private val base = Typography().let { t ->
    fun TextStyle.i() = copy(fontFamily = Inter)
    Typography(
        displayLarge = t.displayLarge.i(), displayMedium = t.displayMedium.i(), displaySmall = t.displaySmall.i(),
        headlineLarge = t.headlineLarge.i(), headlineMedium = t.headlineMedium.i(), headlineSmall = t.headlineSmall.i(),
        titleLarge = t.titleLarge.i(), titleMedium = t.titleMedium.i(), titleSmall = t.titleSmall.i(),
        bodyLarge = t.bodyLarge.i(), bodyMedium = t.bodyMedium.i(), bodySmall = t.bodySmall.i(),
        labelLarge = t.labelLarge.i(), labelMedium = t.labelMedium.i(), labelSmall = t.labelSmall.i(),
    )
}
private val JkType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val Numeric = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Black, fontSize = 28.sp, letterSpacing = (-0.5).sp)

@Composable
fun JkTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = JkType, content = content)
}
