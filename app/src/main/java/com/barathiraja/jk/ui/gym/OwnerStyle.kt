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
 * The owner's look, built on the app's red / black / yellow / white palette (see Theme.kt for the reasoning).
 * One red thing per screen: the action the owner should take next. Black carries text and the one hero card.
 * Yellow marks reward and the top spot. Everything else is white space, so the owner's eye has few places to go.
 * Status is always a word in a chip ([Tone]); the colour only backs the word up.
 */
internal object Owner {
    val Paper get() = pick(0xFFF6F6F4, 0xFF0B0B0B)
    /** Plain cards on the paper. */
    val Card get() = pick(0xFFFFFFFF, 0xFF161616)
    /** A panel or strip inside a card. */
    val Well get() = pick(0xFFF1F1EE, 0xFF1F1F1F)
    /** Text, icons and lines on paper and cards. */
    val Ink get() = pick(0xFF0A0A0A, 0xFFF2F2F2)
    val OnInk get() = pick(0xFFFFFFFF, 0xFF0A0A0A)
    /** The dark hero card; white text on it in both modes. */
    val Hero get() = pick(0xFF0A0A0A, 0xFF1C1C1C)
    /** Text on yellow, in both modes. */
    val Black = Color(0xFF0A0A0A)
    /** Red as a fill: the main action. White text on it (5.2:1). */
    val Red = Color(0xFFD7141E)
    /** Red as text or an icon on paper and cards. */
    val RedText get() = pick(0xFFD7141E, 0xFFFF5A5F)
    val Yellow = Color(0xFFFFC629)
    val Muted get() = pick(0xFF5C5C5C, 0xFFA3A3A3)
    val Line get() = pick(0xFFE6E6E3, 0xFF262626)
    /** On the hero card. */
    val OnDarkMuted = Color(0xFFA3A3A3)
    val OnDarkSoft = Color(0xFFD4D4D4)
    val DarkTrack = Color(0xFF333333)
    val DarkStrip get() = pick(0xFF1F1F1F, 0xFF2A2A2A)
}

/**
 * A status word's look. [TOP]: yellow, the best. [GOOD]: quiet grey, all fine (the usual case, so it stays calm).
 * [WARN]: pale yellow, worth a look. [BAD]: pale red, needs the owner. [NONE]: outlined, nothing yet.
 */
internal enum class Tone {
    TOP, GOOD, WARN, BAD, NONE;

    val fill: Color get() = when (this) {
        TOP -> Owner.Yellow
        GOOD -> pick(0xFFEFEFEC, 0xFF262626)
        WARN -> pick(0xFFFFF2C7, 0xFF2A2410)
        BAD -> pick(0xFFFDE4E4, 0xFF2A1214)
        NONE -> Color.Transparent
    }
    val ink: Color get() = when (this) {
        TOP -> Owner.Black
        GOOD -> Owner.Ink
        WARN -> pick(0xFF0A0A0A, 0xFFFFD966)
        BAD -> pick(0xFF9E0F16, 0xFFFFB4B6)
        NONE -> pick(0xFF3D3D3D, 0xFFD4D4D4)
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
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(50),
        modifier = modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).background(if (enabled) com.barathiraja.jk.ui.theme.Gradients.Red else androidx.compose.ui.graphics.SolidColor(Tone.GOOD.fill)),
        color = Color.Transparent, contentColor = if (enabled) Color.White else Owner.Muted,
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
    val fill = if (onDark) Owner.DarkStrip else Tone.GOOD.fill
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
internal fun OwnerBar(fraction: Float, track: Color, color: Color, key: Any, height: Dp = 8.dp, brush: androidx.compose.ui.graphics.Brush? = null) {
    val k = rememberFillIn(key, 400)
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f) * k).fillMaxHeight().clip(RoundedCornerShape(50)).background(brush ?: androidx.compose.ui.graphics.SolidColor(color)))
    }
}
