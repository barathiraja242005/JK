package com.barathiraja.jk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** Plain card on the page: white (dark grey in dark mode), large rounds. [onClick] makes the whole card a button. */
@Composable
fun JkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val color = MaterialTheme.colorScheme.surfaceContainer
    val ink = MaterialTheme.colorScheme.onSurface
    // The non-clickable overload, so a plain card isn't announced as a disabled button.
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, contentColor = ink) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, contentColor = ink) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 10.dp, bottom = 4.dp))
}

/** Circular progress ring with centred content; [track] defaults to the theme's quiet grey. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp,
    track: Color = Color.Unspecified,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    val trackColor = track.takeOrElse { MaterialTheme.colorScheme.surfaceVariant }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun StatRing(label: String, value: String, sub: String, progress: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Ring(progress, color, size = 92.dp, stroke = 9.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Simple vertical bar chart; [goal] draws a dashed-looking target line. */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    highlightLast: Boolean = true,
) {
    val maxV = max(values.maxOrNull() ?: 0f, goal ?: 0f).coerceAtLeast(1f)
    val muted = color.copy(alpha = 0.35f)
    val guide = MaterialTheme.colorScheme.outline
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = slot * 0.5f
            values.forEachIndexed { i, v ->
                val h = (v / maxV) * size.height
                val c = if (highlightLast && i == values.lastIndex) color else muted
                drawRoundRect(
                    c,
                    topLeft = Offset(i * slot + (slot - barW) / 2, size.height - h),
                    size = Size(barW, h.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(barW / 3),
                )
            }
            goal?.let {
                val y = size.height - (it / maxV) * size.height
                var x = 0f
                while (x < size.width) {
                    drawLine(guide, Offset(x, y), Offset(x + 8f, y), strokeWidth = 2f)
                    x += 16f
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun LineChart(points: List<Float>, color: Color, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val minV = points.min()
    val maxV = points.max()
    val range = (maxV - minV).coerceAtLeast(1f)
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val pad = 8.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        fun pt(i: Int) = Offset(
            pad + w * i / (points.size - 1),
            pad + h - ((points[i] - minV) / range) * h,
        )
        val line = Path().apply {
            moveTo(pt(0).x, pt(0).y)
            for (i in 1 until points.size) lineTo(pt(i).x, pt(i).y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pt(points.lastIndex).x, size.height)
            lineTo(pt(0).x, size.height)
            close()
        }
        drawPath(fill, color.copy(alpha = 0.12f))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        points.indices.forEach { drawCircle(color, 4.dp.toPx(), pt(it)) }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
