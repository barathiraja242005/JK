package com.barathiraja.jk.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.data.BodyPart
import com.barathiraja.jk.domain.TrainingEngine
import com.barathiraja.jk.ui.TrainingViewModel
import com.barathiraja.jk.ui.components.BodyMap
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.SectionTitle
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDaysScreen(tvm: TrainingViewModel, nav: NavHostController) {
    val split by tvm.split.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<DayOfWeek?>(null) }

    editing?.let { dow ->
        var picked by remember(dow) { mutableStateOf(split[dow].orEmpty().toSet()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Select Body Parts · ${dow.getDisplayName(TextStyle.FULL, Locale.getDefault())}") },
            text = {
                Column {
                    BodyPart.entries.forEach { p ->
                        Row(Modifier.fillMaxWidth().clickable { picked = if (p in picked) picked - p else picked + p },
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(p in picked, { picked = if (p in picked) picked - p else picked + p })
                            Text(p.label.uppercase())
                        }
                    }
                    Text("Leave everything unticked to make it a rest day.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Keep the canonical body-part order.
                    tvm.setSplitDay(dow, BodyPart.entries.filter { it in picked }); editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }

    BackScreen("Edit Workout Days", onBack = { nav.popBackStack() }) {
        item {
            Text("Tap the pencil to choose which body parts you train each day. Upcoming workouts update automatically; finished ones are kept.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(DayOfWeek.entries) { dow ->
            val parts = split[dow].orEmpty()
            JkCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = RoundedCornerShape(10.dp)) {
                        Text(dow.getDisplayName(TextStyle.FULL, Locale.getDefault()), Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (parts.isEmpty()) Text("Rest day 😴", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        parts.forEach { AssistChip(onClick = { editing = dow }, label = { Text(it.label.uppercase()) }) }
                    }
                    FilledTonalIconButton(onClick = { editing = dow }) { Icon(Icons.Filled.Edit, "Edit ${dow.name}") }
                }
            }
        }
    }
}

private val histFmt = DateTimeFormatter.ofPattern("EEE, d MMM").withZone(ZoneId.systemDefault())

@Composable
fun TrainingHistoryScreen(tvm: TrainingViewModel, nav: NavHostController) {
    val done by tvm.completedDays.collectAsStateWithLifecycle()
    BackScreen("Workout History", onBack = { nav.popBackStack() }) {
        if (done.isEmpty()) {
            item { Text("No workout history yet. Complete today's plan and it shows up here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item { SectionTitle("Completed workouts: ${done.size}") }
        }
        items(done, key = { it.epochDay }) { d ->
            val parts = BodyPart.parseList(d.parts)
            JkCard(Modifier.fillMaxWidth(), onClick = { tvm.select(d.epochDay); nav.popBackStack() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BodyMap(parts, height = 70.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(TrainingEngine.title(parts), style = MaterialTheme.typography.titleMedium)
                        Text(LocalDate.ofEpochDay(d.epochDay).format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        d.completedAt?.let {
                            Text("Finished ${histFmt.format(Instant.ofEpochMilli(it))}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
