package com.barathiraja.jk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Space the floating bottom bar covers at the bottom of the screen. Set while the bar is showing (0 otherwise);
 * scrolling screens add it to their bottom padding so their last item can scroll clear of the bar.
 */
val LocalNavBarInset = compositionLocalOf { 0.dp }

/** Tab-level screen: large title then lazy content, padded below the status bar. */
@Composable
fun TabScreen(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp + LocalNavBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(title, style = MaterialTheme.typography.headlineMedium)
                }
                action?.invoke()
            }
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackScreenBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            RoundBack(onBack)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/** Pushed screen: back button and title in a top bar, then the content as one scrolling list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackScreen(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                RoundBack(onBack)
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp + LocalNavBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Round white back button, the same one the owner's pages use. 48dp so it's easy to hit. */
@Composable
fun RoundBack(onBack: () -> Unit) {
    Surface(
        onClick = onBack, shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(22.dp))
        }
    }
}

fun formatDuration(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/**
 * Round profile picture, falling back to the user's initial. With [onClick] it opens the profile; the touch area
 * is at least 48dp even when the picture is smaller.
 */
@Composable
fun Avatar(uri: String?, name: String, size: Dp, onClick: (() -> Unit)? = null) {
    val tap = if (onClick != null) {
        Modifier.minimumInteractiveComponentSize().clip(CircleShape)
            .clickable(onClickLabel = "Open profile", role = Role.Button, onClick = onClick)
    } else Modifier
    Box(tap, contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            if (uri != null) {
                AsyncImage(uri, "Profile photo", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else {
                Text(name.take(1).uppercase().ifBlank { "J" }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (size > 60.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium)
            }
        }
    }
}
