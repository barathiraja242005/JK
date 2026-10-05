package com.barathiraja.jk.ui.gym

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.components.LocalNavBarInset
import com.barathiraja.jk.ui.theme.Plex
import com.barathiraja.jk.ui.theme.pick

/*
 * The owner's look, built on the app's athletic red palette (see Theme.kt for the reasoning). One red thing per
 * screen: the action the owner should take next. Charcoal carries text and the one hero card; bright red marks the
 * key number on it. Green, amber and red only ever mean a state. Status is always a word in a chip ([Tone]).
 */
internal object Owner {
    val Paper get() = pick(0xFFFAFAFA, 0xFF0B0C0F)
    /** Cards on the page. */
    val Card get() = pick(0xFFFFFFFF, 0xFF17181C)
    /** A panel or strip inside a card. */
    val Well get() = pick(0xFFF0F1F3, 0xFF1F2126)
    /** Text, icons and lines on the page and cards. */
    val Ink get() = pick(0xFF0B0C0F, 0xFFF5F5F6)
    val OnInk get() = pick(0xFFFFFFFF, 0xFF0B0C0F)
    /** The dark hero card; white text on it in both modes. */
    val Hero get() = pick(0xFF17181C, 0xFF1F2126)
    /** Dark text on bright fills, in both modes. */
    val Black = Color(0xFF0B0C0F)
    /** Deep red as a fill: the main action. White text on it (4.8:1). */
    val Red = Color(0xFFD92D20)
    /** Removing, rejecting, signing out: the error red, as text or an icon. */
    val RedText get() = pick(0xFFDC2626, 0xFFF87171)
    /** Bright red: emphasis on the hero card (5.4:1 on charcoal). */
    val Accent = Color(0xFFFF4B4B)
    /** Ticks and "done": success green fill with a dark icon. */
    val Success = Color(0xFF22C55E)
    /** Waiting and medals: amber fill with dark text. */
    val Amber = Color(0xFFF59E0B)
    val Muted get() = pick(0xFF667085, 0xFF98A2B3)
    val Line get() = pick(0xFFE1E3E6, 0xFF2A2D33)
    /** On the hero card. */
    val OnDarkMuted = Color(0xFF98A2B3)
    val OnDarkSoft = Color(0xFFD0D5DD)
    val DarkTrack = Color(0xFF33363C)
    val DarkStrip get() = pick(0xFF25272D, 0xFF2A2D33)
}

/**
 * A status word's look. [TOP]: red tint, the best. [GOOD]: green tint, all fine. [WARN]: amber tint, worth a look.
 * [BAD]: error tint, needs the owner. [NONE]: outlined, nothing yet.
 */
internal enum class Tone {
    TOP, GOOD, WARN, BAD, NONE;

    val fill: Color get() = when (this) {
        TOP -> pick(0xFFFDECEA, 0xFF2D1214)
        GOOD -> pick(0xFFDCFCE7, 0xFF0F2A1A)
        WARN -> pick(0xFFFEF3C7, 0xFF2D2110)
        BAD -> pick(0xFFFEE2E2, 0xFF2D1214)
        NONE -> Color.Transparent
    }
    val ink: Color get() = when (this) {
        TOP -> pick(0xFFB42318, 0xFFFF6B6B)
        GOOD -> pick(0xFF15803D, 0xFF4ADE80)
        WARN -> pick(0xFF92400E, 0xFFFBBF24)
        BAD -> pick(0xFFB91C1C, 0xFFF87171)
        NONE -> pick(0xFF667085, 0xFFD0D5DD)
    }
}

internal fun plex(size: TextUnit, weight: FontWeight = FontWeight.Normal, line: TextUnit = TextUnit.Unspecified, tracking: TextUnit = 0.sp) =
    androidx.compose.ui.text.TextStyle(fontFamily = Plex, fontSize = size, fontWeight = weight, lineHeight = line, letterSpacing = tracking)

/** Owner page shell: paper background, status-bar padding and room for the bottom bar. */
@Composable
internal fun OwnerPage(spacing: Dp = 10.dp, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Owner.Paper).windowInsetsPadding(WindowInsets.statusBars).imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** A tab's title and the one sentence that says what the page is for. */
@Composable
internal fun PageTitle(title: String, explain: String) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
        Text(title, style = plex(24.sp, FontWeight.Bold, line = 30.sp, tracking = (-0.4).sp), color = Owner.Ink)
        Text(explain, style = plex(15.sp, line = 21.sp), color = Owner.Muted, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Section title with one plain sentence under it. */
@Composable
internal fun OwnerHeading(title: String, explain: String? = null) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 0.dp)) {
        Text(title, style = plex(17.sp, FontWeight.Bold), color = Owner.Ink)
        if (explain != null) Text(explain, style = plex(14.sp, line = 19.sp), color = Owner.Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The screen's main action: a red pill, white words, an optional icon after them. 56dp tall. */
@Composable
internal fun RedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(50),
        color = if (enabled) Owner.Red else Owner.Line, contentColor = if (enabled) Color.White else Owner.Muted,
    ) {
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = plex(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (icon != null) {
                Spacer(Modifier.width(10.dp))
                Icon(icon, null, Modifier.size(20.dp))
            }
        }
    }
}

/** A second action next to a red one: outlined, ink words. */
@Composable
internal fun PlainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, ink: Color = Owner.Ink) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(50),
        color = Owner.Card, contentColor = ink, border = BorderStroke(1.5.dp, Owner.Line),
    ) {
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = plex(15.sp, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A white card with large rounds; [onClick] makes the whole card a button. */
@Composable
internal fun OwnerCardBox(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onClickLabel: String? = null, padding: Dp = 18.dp,
                          content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Owner.Card)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .padding(padding),
    ) { content() }
}

/** Round photo, or the first letter on grey. */
@Composable
internal fun OwnerAvatar(photoUrl: String?, name: String, size: Dp = 48.dp, onDark: Boolean = false) {
    val fill = if (onDark) Owner.DarkStrip else Owner.Well
    Box(Modifier.size(size).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
        if (photoUrl != null) {
            coil3.compose.AsyncImage(photoUrl, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Text(name.trim().take(1).uppercase().ifBlank { "J" }, style = plex((size.value * 0.4f).sp, FontWeight.SemiBold),
                color = if (onDark) Color.White else Owner.Ink)
        }
    }
}

/** A small rounded label: always a word, never colour alone. [onDark] adds a fine ring so a black chip shows on black. */
@Composable
internal fun OwnerChip(text: String, tone: Tone, modifier: Modifier = Modifier, onDark: Boolean = false, small: Boolean = false) {
    Text(
        text,
        modifier.clip(RoundedCornerShape(50)).background(tone.fill)
            .then(if (tone == Tone.NONE) Modifier.border(1.dp, if (onDark) Owner.OnDarkMuted else Owner.Line, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
        style = plex(if (small) 11.sp else 12.sp, FontWeight.SemiBold), color = if (onDark && tone == Tone.NONE) Color.White else tone.ink, maxLines = 1,
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
