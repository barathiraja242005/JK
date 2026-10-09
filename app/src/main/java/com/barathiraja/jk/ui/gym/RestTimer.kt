package com.barathiraja.jk.ui.gym

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import com.barathiraja.jk.ui.components.ProgressRing
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.theme.plex

/**
 * What every workout screen shares: the rest countdown between sets and the beeps. While the screen is open it stays
 * on. [start] begins a rest of [restSec]; the last three seconds beep short and zero beeps long.
 */
class WorkoutClock internal constructor(private val left: MutableIntState, val restSec: Int, private val tone: ToneGenerator?) {
    val resting: Int get() = left.intValue
    fun start() { left.intValue = restSec }
    fun add(sec: Int) { left.intValue += sec }
    fun skip() { left.intValue = 0 }
    /** Short beep (a countdown's last seconds) or long beep (time's up). */
    fun beep(long: Boolean = false) {
        tone?.startTone(if (long) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, if (long) 300 else 120)
    }
}

@Composable
fun rememberWorkoutClock(restSec: Int): WorkoutClock {
    val left = rememberSaveable { mutableIntStateOf(0) }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    val clock = remember(restSec, tone) { WorkoutClock(left, restSec.coerceAtLeast(1), tone) }
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false; tone?.release() }
    }
    LaunchedEffect(left.intValue > 0) {
        while (left.intValue > 0) {
            delay(1000)
            left.intValue--
            if (left.intValue in 1..3) clock.beep()
            if (left.intValue == 0) clock.beep(long = true)
        }
    }
    return clock
}

/** The rest countdown over the bottom of a workout: charcoal bar, yellow ring, white text (15:1), yellow Skip (10:1). */
@Composable
fun RestBar(clock: WorkoutClock, next: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(clock.resting > 0, modifier = modifier) {
        val onSurface = MaterialTheme.colorScheme.inverseOnSurface
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.inverseSurface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProgressRing(clock.resting / clock.restSec.toFloat(), 64.dp, 6.dp, Jk.DarkTrack, Jk.Yellow, key = "rest", animate = false) {
                Text("${clock.resting}", style = plex(17.sp, FontWeight.SemiBold), color = onSurface)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Rest", style = plex(17.sp, FontWeight.SemiBold), color = onSurface)
                if (next != null) Text(next, style = plex(13.sp), color = onSurface.copy(alpha = 0.75f))
            }
            TextButton(onClick = { clock.add(15) }) { Text("+15s", style = plex(15.sp, FontWeight.SemiBold), color = onSurface) }
            TextButton(onClick = { clock.skip() }) { Text("Skip", style = plex(15.sp, FontWeight.SemiBold), color = Jk.Yellow) }
        }
    }
}
