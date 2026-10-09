package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.ui.components.BackScreen
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.barathiraja.jk.data.Articles
import com.barathiraja.jk.data.BodyPhoto
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Pill
import com.barathiraja.jk.ui.components.SectionTitle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ArticlesScreen(nav: NavHostController) {
    BackScreen("Guides", onBack = { nav.popBackStack() }) {
        items(Articles.all, key = { it.id }) { a ->
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.article(a.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.AutoMirrored.Outlined.MenuBook)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                        Text("${a.category} · ${a.minutes} min read", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(id: String, nav: NavHostController) {
    val a = Articles.byId(id) ?: return
    BackScreen(a.category, onBack = { nav.popBackStack() }) {
        item { Text(a.title, style = MaterialTheme.typography.headlineSmall) }
        item { Pill("${a.minutes} min read", MaterialTheme.colorScheme.primary) }
        items(a.body) { p -> Text(p, style = MaterialTheme.typography.bodyLarge) }
        item {
            Text("General guidance only — not medical advice.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

private val photoFmt = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Progress photos stored as persistable URIs; compare first vs latest with a drag slider. */
@Composable
fun PhotosScreen(vm: JkViewModel, nav: NavHostController) {
    val photos by vm.photos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleting by remember { mutableStateOf<BodyPhoto?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.addPhoto(uri.toString())
        }
    }
    deleting?.let { p ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Remove photo?") },
            text = { Text("It's removed from JK only, not from your gallery.") },
            confirmButton = { TextButton(onClick = { vm.deletePhoto(p.id); deleting = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }

    BackScreen("Transformation", onBack = { nav.popBackStack() }) {
        item {
            Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text("Add today's progress photo")
            }
        }
        if (photos.size >= 2) {
            item { SectionTitle("Before / after") }
            item { CompareSlider(photos.first(), photos.last()) }
        } else {
            item {
                Text("Take a photo in the same spot and lighting every couple of weeks. Once you have two, compare them side by side here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (photos.isNotEmpty()) item { SectionTitle("All photos (${photos.size})") }
        items(photos.reversed(), key = { it.id }) { p ->
            JkCard(Modifier.fillMaxWidth()) {
                AsyncImage(p.uri, null, Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalDate.ofEpochDay(p.epochDay).format(photoFmt) + (p.weightKg?.let { " · %.1f kg".format(it) } ?: ""),
                        Modifier.weight(1f))
                    IconButton(onClick = { deleting = p }) { Icon(Icons.Outlined.Delete, "Remove") }
                }
            }
        }
    }
}

@Composable
private fun CompareSlider(before: BodyPhoto, after: BodyPhoto) {
    var split by remember { mutableFloatStateOf(0.5f) }
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(16.dp))) {
            AsyncImage(after.uri, "After", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            val full = maxWidth
            Box(Modifier.fillMaxHeight().width(full * split).clipToBounds()) {
                AsyncImage(before.uri, "Before", Modifier.width(full).fillMaxHeight(), contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterStart)
            }
            Box(Modifier.padding(start = (full * split - 1.dp).coerceAtLeast(0.dp)).width(2.dp).fillMaxHeight()
                .background(Color.White))
        }
        Slider(split, { split = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Before · " + LocalDate.ofEpochDay(before.epochDay).format(photoFmt), style = MaterialTheme.typography.labelMedium)
            Text("After · " + LocalDate.ofEpochDay(after.epochDay).format(photoFmt), style = MaterialTheme.typography.labelMedium)
        }
    }
}
