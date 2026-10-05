package com.barathiraja.jk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.ui.theme.Accent

/**
 * Simplified front/back body silhouettes with the targeted muscle groups highlighted.
 * Drawn in a 100×220 virtual grid so it scales to any size.
 */
@Composable
fun BodyMap(parts: Collection<BodyPart>, modifier: Modifier = Modifier, height: Dp = 150.dp, highlight: Color = Accent) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val skin = MaterialTheme.colorScheme.outlineVariant
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(true, false).forEach { front ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(height * 100f / 220f, height)) {
                    val s = size.width / 100f
                    fun c(p: BodyPart?) = if (p != null && p in parts) highlight else base
                    body(s, skin)
                    if (front) {
                        region(s, 30f, 46f, 40f, 22f, c(BodyPart.CHEST))            // chest
                        region(s, 36f, 70f, 28f, 36f, c(BodyPart.ABDOMEN))          // abs
                        region(s, 16f, 44f, 14f, 14f, c(BodyPart.SHOULDER), 7f)     // delts
                        region(s, 70f, 44f, 14f, 14f, c(BodyPart.SHOULDER), 7f)
                        region(s, 13f, 60f, 11f, 26f, c(BodyPart.BICEPS), 5f)      // biceps
                        region(s, 76f, 60f, 11f, 26f, c(BodyPart.BICEPS), 5f)
                        region(s, 30f, 112f, 18f, 46f, c(BodyPart.LEGS), 8f)       // quads
                        region(s, 52f, 112f, 18f, 46f, c(BodyPart.LEGS), 8f)
                    } else {
                        region(s, 30f, 44f, 40f, 16f, c(BodyPart.BACK))             // traps / upper back
                        region(s, 30f, 62f, 40f, 30f, c(BodyPart.BACK), 10f)        // lats
                        region(s, 16f, 44f, 14f, 14f, c(BodyPart.SHOULDER), 7f)
                        region(s, 70f, 44f, 14f, 14f, c(BodyPart.SHOULDER), 7f)
                        region(s, 13f, 60f, 11f, 26f, c(BodyPart.TRICEPS), 5f)     // triceps
                        region(s, 76f, 60f, 11f, 26f, c(BodyPart.TRICEPS), 5f)
                        region(s, 32f, 96f, 36f, 16f, c(BodyPart.LEGS), 8f)        // glutes
                        region(s, 30f, 114f, 18f, 40f, c(BodyPart.LEGS), 8f)       // hamstrings
                        region(s, 52f, 114f, 18f, 40f, c(BodyPart.LEGS), 8f)
                        region(s, 31f, 166f, 15f, 30f, c(BodyPart.LEGS), 7f)       // calves
                        region(s, 54f, 166f, 15f, 30f, c(BodyPart.LEGS), 7f)
                    }
                    if (BodyPart.CARDIO in parts) drawCircle(highlight.copy(alpha = 0.9f), 5f * s, Offset(57f * s, 54f * s)) // heart
                }
                Text(if (front) "Front" else "Back", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun DrawScope.region(s: Float, x: Float, y: Float, w: Float, h: Float, color: Color, r: Float = 6f) =
    drawRoundRect(color, Offset(x * s, y * s), Size(w * s, h * s), CornerRadius(r * s))

/** Neutral silhouette behind the muscle regions. */
private fun DrawScope.body(s: Float, color: Color) {
    drawCircle(color, 12f * s, Offset(50f * s, 18f * s))                         // head
    drawRoundRect(color, Offset(44f * s, 28f * s), Size(12f * s, 10f * s))        // neck
    drawRoundRect(color, Offset(24f * s, 38f * s), Size(52f * s, 72f * s), CornerRadius(14f * s)) // torso
    drawRoundRect(color, Offset(10f * s, 42f * s), Size(16f * s, 74f * s), CornerRadius(8f * s))  // arms
    drawRoundRect(color, Offset(74f * s, 42f * s), Size(16f * s, 74f * s), CornerRadius(8f * s))
    drawRoundRect(color, Offset(28f * s, 104f * s), Size(21f * s, 110f * s), CornerRadius(10f * s)) // legs
    drawRoundRect(color, Offset(51f * s, 104f * s), Size(21f * s, 110f * s), CornerRadius(10f * s))
}
