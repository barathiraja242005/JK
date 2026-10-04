package com.barathiraja.jk.ui.gym

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.theme.Plex

/*
 * The owner's look: warm off-white paper, black ink, one mustard accent and soft pastels for people.
 * Cards carry a curved cut-out in their top-right corner that holds a round button. Status colours
 * always sit next to a word. Every text colour here is at least 4.5:1 on the surface it is used on.
 */
internal object Owner {
    val Paper = Color(0xFFEEECE7)
    val Ink = Color(0xFF000000)
    val Mustard = Color(0xFFF0D68C)
    val Cream = Color(0xFFF8E9C0)
    val CardCream = Color(0xFFFFF9EA)
    val Lavender = Color(0xFFE4DCF4)
    val Mint = Color(0xFFCDE5DF)
    val Coral = Color(0xFFF2CDC7)
    val Butter = Color(0xFFF3E3B6)
    val Muted = Color(0xFF5A5A57)
    val Warm = Color(0xFF3D3A33)
    val Faint = Color(0xFFA3A3A0)
    val Line = Color(0xFFE5E3DE)
    val OnDarkMuted = Color(0xFFABABAB)
    val OnDarkSoft = Color(0xFFD4D4D2)
    val DarkTrack = Color(0xFF2A2A2A)
    val DarkStrip = Color(0xFF1A1A1A)
    val Good = Color(0xFF0F766E)
    val Behind = Color(0xFFC2410C)

    private val pastels = listOf(Lavender, Mint, Butter, Coral)
    fun pastel(key: String): Color = pastels[Math.floorMod(key.hashCode(), pastels.size)]
}

internal fun plex(size: TextUnit, weight: FontWeight = FontWeight.Normal, line: TextUnit = TextUnit.Unspecified, tracking: TextUnit = 0.sp) =
    androidx.compose.ui.text.TextStyle(fontFamily = Plex, fontSize = size, fontWeight = weight, lineHeight = line, letterSpacing = tracking)

/**
 * A rounded card with a square bite out of its top-right corner. The bite's inner corner is concave and the two
 * corners it makes on the card are rounded, so the outline reads as one smooth curve. Being a real shape, whatever
 * is behind the card shows through the bite.
 */
internal class NotchedShape(
    private val radius: Dp = 28.dp,
    private val notch: Dp = 68.dp,
    private val inner: Dp = 26.dp,
    private val smooth: Dp = 22.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val r = radius.toPx(); val n = notch.toPx(); val r1 = inner.toPx(); val r2 = smooth.toPx()
        val w = size.width; val h = size.height
        Outline.Generic(Path().apply {
            moveTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            lineTo(w - n - r2, 0f)
            arcTo(Rect(w - n - 2 * r2, 0f, w - n, 2 * r2), -90f, 90f, false)
            lineTo(w - n, n - r1)
            arcTo(Rect(w - n, n - 2 * r1, w - n + 2 * r1, n), 180f, -90f, false)
            lineTo(w - r2, n)
            arcTo(Rect(w - 2 * r2, n, w, n + 2 * r2), -90f, 90f, false)
            lineTo(w, h - r)
            arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            close()
        })
    }
}

/**
 * A folder tab that grows out of the panel under it: rounded on top, with concave flares at the bottom so it
 * blends into the panel. [left]/[right] turn each flare off when the tab sits on the panel's edge.
 */
internal class FolderTabShape(
    private val radius: Dp = 22.dp,
    private val flare: Dp = 20.dp,
    private val left: Boolean = true,
    private val right: Boolean = true,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val r = radius.toPx(); val f = flare.toPx()
        val w = size.width; val h = size.height
        val lf = if (left) f else 0f; val rf = if (right) f else 0f
        Outline.Generic(Path().apply {
            moveTo(0f, h)
            if (left) arcTo(Rect(-f, h - 2 * f, f, h), 90f, -90f, false)
            lineTo(lf, r)
            arcTo(Rect(lf, 0f, lf + 2 * r, 2 * r), 180f, 90f, false)
            lineTo(w - rf - r, 0f)
            arcTo(Rect(w - rf - 2 * r, 0f, w - rf, 2 * r), -90f, 90f, false)
            if (right) {
                lineTo(w - f, h - f)
                arcTo(Rect(w - f, h - 2 * f, w + f, h), 180f, -90f, false)
            } else lineTo(w, h)
            close()
        })
    }
}

/** Round photo or initial on a pastel, with a fine black ring. */
@Composable
internal fun OwnerAvatar(photoUrl: String?, name: String, key: String, size: Dp = 48.dp, fill: Color = Owner.pastel(key)) {
    Box(
        Modifier.size(size).clip(CircleShape).background(fill).border(1.5.dp, Owner.Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            coil3.compose.AsyncImage(photoUrl, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Text(name.take(1).uppercase().ifBlank { "J" }, style = plex((size.value * 0.38f).sp, FontWeight.SemiBold), color = Owner.Ink)
        }
    }
}

/** A small rounded label: always a word, never colour alone. */
@Composable
internal fun OwnerChip(text: String, fill: Color, ink: Color = Owner.Ink, modifier: Modifier = Modifier) {
    Text(
        text, modifier.clip(RoundedCornerShape(50)).background(fill).padding(horizontal = 12.dp, vertical = 5.dp),
        style = plex(13.sp, FontWeight.SemiBold), color = ink, maxLines = 1,
    )
}

private val easeOut = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** 0→1 once, when first shown, so rings and bars fill in. */
@Composable
internal fun rememberFillIn(key: Any, delayMs: Int = 300): Float {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { a.animateTo(1f, tween(1000, delayMs, easeOut)) }
    return a.value
}

/** A progress ring that fills to [fraction] when it first appears. */
@Composable
internal fun OwnerRing(fraction: Float, size: Dp, stroke: Dp, track: Color, color: Color, key: Any, center: @Composable () -> Unit) {
    val k = rememberFillIn(key)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val box = Size(this.size.width - s, this.size.height - s)
            val tl = androidx.compose.ui.geometry.Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, tl, box, style = Stroke(s))
            val sweep = 360f * fraction.coerceIn(0f, 1f) * k
            if (sweep > 0f) drawArc(color, -90f, sweep, false, tl, box, style = Stroke(s, cap = StrokeCap.Round))
        }
        center()
    }
}

/** A thin rounded bar that fills to [fraction] when it first appears. */
@Composable
internal fun OwnerBar(fraction: Float, track: Color, color: Color, key: Any, height: Dp = 8.dp) {
    val k = rememberFillIn(key, 400)
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f) * k).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}
