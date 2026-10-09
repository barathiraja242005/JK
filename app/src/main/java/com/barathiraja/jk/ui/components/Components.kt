package com.barathiraja.jk.ui.components

import com.barathiraja.jk.ui.theme.Jk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

/**
 * A card holding a column of content: [CardBox] (the one card of the app) with [padding] inside. [onClick] makes the
 * whole card a button.
 */
@Composable
fun JkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardBox(modifier, onClick = onClick, padding = padding) { Column(content = content) }
}

/** A section heading inside a page; the same [Heading] the gym screens use. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Box(modifier) { Heading(text) }
}

/** Circular progress ring with centred content: the app's one [ProgressRing], drawn as given (no fill-in). */
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
    Box(modifier) {
        ProgressRing(progress, size, stroke, track.takeOrElse { Jk.Well }, color, key = Unit, animate = false, center = content)
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
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}

@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
