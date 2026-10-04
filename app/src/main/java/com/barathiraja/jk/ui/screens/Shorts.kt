package com.barathiraja.jk.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.cap
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.ExerciseDemo
import com.barathiraja.jk.ui.components.openUrl
import com.barathiraja.jk.ui.components.shareText
import com.barathiraja.jk.ui.theme.Ember
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Vertical, full-screen feed of animated exercise demos (the "shorts" experience). */
@Composable
fun ShortsScreen(vm: JkViewModel, nav: NavHostController) {
    // Offline exercises first so the feed works without internet, then a shuffled mix of the rest.
    val seed = rememberSaveable { Random.nextInt() }
    val feed = remember(seed) {
        val all = ExerciseRepo.all()
        val (local, remote) = all.partition { it.id in ExerciseRepo.bundled }
        local.shuffled(Random(seed)) + remote.shuffled(Random(seed)).take(300)
    }
    val pager = rememberPagerState { feed.size }
    val flags by vm.flags.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
            val ex = feed[page]
            val liked = flags[ex.id]?.liked == true
            val saved = flags[ex.id]?.saved == true
            val heart = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()

            Box(
                Modifier.fillMaxSize().pointerInput(ex.id) {
                    detectTapGestures(onDoubleTap = {
                        if (!liked) vm.toggleLike(ex.id)
                        scope.launch {
                            heart.snapTo(0.3f)
                            heart.animateTo(1.2f, spring(Spring.DampingRatioMediumBouncy))
                            heart.animateTo(0f, tween(400, delayMillis = 300))
                        }
                    })
                },
            ) {
                ExerciseDemo(ex, Modifier.fillMaxSize(), animate = pager.currentPage == page, frameMs = 900,
                    contentScale = ContentScale.Fit)
                // Scrims so white text stays readable over bright photos.
                Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(300.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))

                Icon(Icons.Filled.Favorite, null, tint = Ember,
                    modifier = Modifier.align(Alignment.Center).size(120.dp).scale(heart.value).graphicsLayer { alpha = heart.value.coerceAtMost(1f) })

                Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 88.dp, bottom = 24.dp)) {
                    Text(ex.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text("${ex.muscles} · ${ex.equipment.cap()} · ${ex.level.cap()}", color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(ex.instructions.firstOrNull().orEmpty(), color = Color.White.copy(alpha = 0.9f), maxLines = 3,
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    AssistChip(
                        onClick = { nav.navigate(Routes.exercise(ex.id)) },
                        label = { Text("View full guide") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.18f), labelColor = Color.White),
                        border = null,
                    )
                }

                Column(
                    Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SideAction(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (liked) "Liked" else "Like",
                        if (liked) Ember else Color.White) { vm.toggleLike(ex.id) }
                    SideAction(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, if (saved) "Saved" else "Save",
                        Color.White) { vm.toggleSave(ex.id) }
                    SideAction(Icons.Filled.PlayCircle, "Tutorial", Color.White) { context.openUrl(ex.tutorialUrl) }
                    SideAction(Icons.AutoMirrored.Filled.Send, "Share", Color.White) {
                        context.shareText("Try the ${ex.name} (${ex.muscles}) — I found it on JK fitness.\n${ex.tutorialUrl}")
                    }
                }
            }
        }
        Text("Shorts", Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(16.dp),
            style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.35f), androidx.compose.foundation.shape.CircleShape)) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
