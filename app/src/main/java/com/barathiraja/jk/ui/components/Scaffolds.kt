package com.barathiraja.jk.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

/**
 * Space the floating bottom bar covers at the bottom of the screen. Set while the bar is showing (0 otherwise);
 * scrolling screens add it to their bottom padding so their last item can scroll clear of the bar.
 */
val LocalNavBarInset = androidx.compose.runtime.compositionLocalOf { 0.dp }

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
            androidx.compose.foundation.layout.Row(
                Modifier.padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
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
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        navigationIcon = {
            RoundBack(onBack)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackScreen(title: String, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
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

/** Round white back button, the same one the owner's pages use. */
@Composable
fun RoundBack(onBack: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onBack, shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(44.dp),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
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

/** Round profile picture, falling back to the user's initial. */
@Composable
fun Avatar(uri: String?, name: String, size: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    val m = Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    androidx.compose.foundation.layout.Box(m, contentAlignment = androidx.compose.ui.Alignment.Center) {
        if (uri != null) {
            coil3.compose.AsyncImage(uri, "Profile photo", Modifier.matchParentSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop)
        } else {
            Text(name.take(1).uppercase().ifBlank { "J" }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = if (size > 60.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium)
        }
    }
}
