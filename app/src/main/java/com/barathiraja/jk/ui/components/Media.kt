package com.barathiraja.jk.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.barathiraja.jk.data.Exercise
import com.barathiraja.jk.ui.theme.Aqua
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Leaf
import com.barathiraja.jk.ui.theme.Sun
import com.barathiraja.jk.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated exercise demo: alternates the start and end position photos, which reads like a
 * slow GIF of the movement. Set [animate] false for a still thumbnail.
 */
@Composable
fun ExerciseDemo(
    exercise: Exercise,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    frameMs: Long = 1100,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var frame by remember(exercise.id) { mutableIntStateOf(0) }
    if (animate) {
        LaunchedEffect(exercise.id) {
            while (true) { delay(frameMs); frame = 1 - frame }
        }
    }
    Box(modifier.background(Color.White), contentAlignment = Alignment.Center) {
        Crossfade(frame, label = "demo") { f ->
            AsyncImage(
                model = exercise.image(f),
                contentDescription = exercise.name,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Burst of falling confetti; drawn on a Canvas so no animation assets are needed. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 120) {
    val colors = listOf(Ember, Sun, Leaf, Aqua, Violet, Color.White)
    val pieces = remember {
        List(count) {
            floatArrayOf(
                Random.nextFloat(),                 // x
                -Random.nextFloat() * 0.6f,         // y start
                0.15f + Random.nextFloat() * 0.35f, // fall speed (screen heights per second)
                Random.nextFloat() * 360f,          // rotation
                (Random.nextFloat() - 0.5f) * 540f, // spin speed
                Random.nextFloat() * colors.size,   // color index
                (Random.nextFloat() - 0.5f) * 0.12f, // drift
            )
        }
    }
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = ((it - start) / 1_000_000).toInt() }
    }
    Canvas(modifier.fillMaxSize()) {
        val sec = t / 1000f
        pieces.forEach { p ->
            val y = (p[1] + p[2] * sec) * size.height
            if (y > size.height + 20) return@forEach
            val x = (p[0] + p[6] * sin(sec * 2 + p[0] * 10)) * size.width
            rotate(p[3] + p[4] * sec, Offset(x, y)) {
                drawRect(colors[p[5].toInt() % colors.size], Offset(x - 6, y - 10), Size(12f, 20f))
            }
        }
    }
}

/** Water bottle that fills to [fraction] with an animated wave surface. */
@Composable
fun WaterBottle(fraction: Float, modifier: Modifier = Modifier, color: Color = Aqua) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    val level by androidx.compose.animation.core.animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800), label = "level")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val neckW = w * 0.4f
        val neckH = h * 0.12f
        val r = w * 0.18f
        val bottle = Path().apply {
            moveTo((w - neckW) / 2, 0f)
            lineTo((w + neckW) / 2, 0f)
            lineTo((w + neckW) / 2, neckH)
            quadraticTo(w, neckH, w, neckH + r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            lineTo(0f, neckH + r)
            quadraticTo(0f, neckH, (w - neckW) / 2, neckH)
            close()
        }
        val top = h - (h - neckH) * level
        val wave = Path().apply {
            moveTo(0f, h)
            lineTo(0f, top)
            var x = 0f
            while (x <= w) {
                lineTo(x, top + sin(x / w * 2 * PI.toFloat() * 1.5f + phase) * h * 0.02f)
                x += 4f
            }
            lineTo(w, h)
            close()
        }
        drawPath(bottle, color.copy(alpha = 0.12f))
        if (level > 0f) {
            clipPath(bottle) {
                drawPath(wave, color.copy(alpha = 0.85f))
            }
        }
        drawPath(bottle, color, style = Stroke(width = 3f))
    }
}

fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}
