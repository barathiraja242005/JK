package com.barathiraja.jk.ui.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.components.LocalNavBarInset
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.Tone
import com.barathiraja.jk.ui.theme.plex

/** A tab or page shell: paper background, status-bar padding and room for the bottom bar. */
@Composable
internal fun JkPage(spacing: Dp = 10.dp, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(Jk.Paper).windowInsetsPadding(WindowInsets.statusBars).imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** A tab's title and the one sentence that says what the page is for. */
@Composable
internal fun PageTitle(title: String, explain: String) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
        Text(title, style = plex(24.sp, FontWeight.Bold, line = 30.sp, tracking = (-0.4).sp), color = Jk.Ink)
        Text(explain, style = plex(15.sp, line = 21.sp), color = Jk.Muted, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Section title with one plain sentence under it. */
@Composable
internal fun Heading(title: String, explain: String? = null) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 0.dp)) {
        Text(title, style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
        if (explain != null) Text(explain, style = plex(14.sp, line = 19.sp), color = Jk.Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The screen's main action: a red pill, white words, an optional icon after them. 56dp tall. */
@Composable
internal fun RedButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(50),
        color = if (enabled) Jk.Red else Tone.GOOD.fill, contentColor = if (enabled) Color.White else Jk.Muted,
    ) {
        Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
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
internal fun PlainButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, ink: Color = Jk.Ink) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(50),
        color = Jk.Card, contentColor = ink, border = BorderStroke(1.5.dp, Jk.Line),
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
internal fun CardBox(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onClickLabel: String? = null, padding: Dp = 18.dp,
                          content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Jk.Card)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .padding(padding),
    ) { content() }
}

/** One tab of [SegmentedTabs]: its label, an optional count, and whether it can be picked right now. */
internal data class SegmentTab(val label: String, val count: Int? = null, val enabled: Boolean = true)

/**
 * The app's one switch between views (Members | Trainers, Members | Trainers | Awards, theme…): a white pill track
 * ([track]: grey inside a white card) with the open tab filled black and its count on yellow. Every tab is a 48dp target announced as a tab.
 */
@Composable
internal fun SegmentedTabs(
    tabs: List<SegmentTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, track: Color = Jk.Card,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(track).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            val on = i == selected
            Surface(
                onClick = { onSelect(i) }, enabled = tab.enabled && !on,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { role = Role.Tab; this.selected = on },
                shape = RoundedCornerShape(50),
                color = if (on) Jk.Ink else Color.Transparent,
                contentColor = when { on -> Jk.OnInk; tab.enabled -> Jk.Ink; else -> Jk.Muted },
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(tab.label, style = plex(15.sp, FontWeight.SemiBold), maxLines = 1)
                    if (tab.count != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.heightIn(min = 22.dp).clip(RoundedCornerShape(50)).background(if (on) Jk.Yellow else Jk.Well)
                            .padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            Text("${tab.count}", style = plex(12.sp, FontWeight.SemiBold), color = if (on) Jk.Black else Jk.Ink)
                        }
                    }
                }
            }
        }
    }
}

/** Round photo, or the first letter on grey. */
@Composable
internal fun PersonAvatar(photoUrl: String?, name: String, size: Dp = 48.dp, onDark: Boolean = false) {
    val fill = if (onDark) Jk.DarkStrip else Tone.GOOD.fill
    Box(Modifier.size(size).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) {
        if (photoUrl != null) {
            coil3.compose.AsyncImage(photoUrl, null, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Text(name.trim().take(1).uppercase().ifBlank { "J" }, style = plex((size.value * 0.4f).sp, FontWeight.SemiBold),
                color = if (onDark) Color.White else Jk.Ink)
        }
    }
}

/** A small rounded label: always a word, never colour alone. [onDark] adds a fine ring so a black chip shows on black. */
@Composable
internal fun StatusChip(text: String, tone: Tone, modifier: Modifier = Modifier, onDark: Boolean = false, small: Boolean = false, lines: Int = 1) {
    // A one-line chip is a pill; free text (a gift, a note) may wrap, so it gets softer corners instead.
    val shape = if (lines > 1) RoundedCornerShape(12.dp) else RoundedCornerShape(50)
    Text(
        text,
        modifier.clip(shape).background(tone.fill)
            .then(if (tone == Tone.NONE) Modifier.border(1.dp, if (onDark) Jk.OnDarkMuted else Jk.Line, shape) else Modifier)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 2.dp else 4.dp),
        style = plex(if (small) 11.sp else 12.sp, FontWeight.SemiBold), color = if (onDark && tone == Tone.NONE) Color.White else tone.ink,
        maxLines = lines, overflow = TextOverflow.Ellipsis,
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

/** A progress ring that fills to [fraction] when it first appears ([animate] false: drawn as is, e.g. a countdown). */
@Composable
internal fun ProgressRing(fraction: Float, size: Dp, stroke: Dp, track: Color, color: Color, key: Any, animate: Boolean = true,
                       center: @Composable () -> Unit) {
    val k = if (animate) rememberFillIn(key) else 1f
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            val box = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, tl, box, style = Stroke(s))
            val sweep = 360f * fraction.coerceIn(0f, 1f) * k
            if (sweep > 0f) drawArc(color, -90f, sweep, false, tl, box, style = Stroke(s, cap = StrokeCap.Round))
        }
        center()
    }
}

/** A thin rounded bar that fills to [fraction] when it first appears. */
@Composable
internal fun ProgressBar(fraction: Float, track: Color, color: Color, key: Any, height: Dp = 8.dp) {
    val k = rememberFillIn(key, 400)
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f) * k).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}
