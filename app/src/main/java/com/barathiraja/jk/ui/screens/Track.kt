package com.barathiraja.jk.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.JkCard
import com.barathiraja.jk.ui.components.Ring
import com.barathiraja.jk.ui.components.SectionTitle
import com.barathiraja.jk.ui.components.WaterBottle
import com.barathiraja.jk.ui.theme.Aqua
import com.barathiraja.jk.ui.theme.Ember
import com.barathiraja.jk.ui.theme.Leaf
import com.barathiraja.jk.ui.theme.Sun
import com.barathiraja.jk.ui.theme.Violet

@Composable
fun TrackScreen(vm: JkViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val water by vm.waterToday.collectAsStateWithLifecycle()
    val steps by vm.stepsToday.collectAsStateWithLifecycle()
    val fast by vm.activeFast.collectAsStateWithLifecycle()
    val food by vm.foodToday.collectAsStateWithLifecycle()

    val context = LocalContext.current
    fun hasActivityPerm() = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    var stepPerm by remember { mutableStateOf(hasActivityPerm()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        stepPerm = ok
        if (ok) vm.startSteps()
    }

    TabScreen("Health", subtitle = "Daily habits") {
        // Water
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaterBottle(water / settings.waterGoalGlasses.toFloat(), Modifier.width(64.dp).height(110.dp))
                    Spacer(Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Hydration", style = MaterialTheme.typography.titleMedium)
                        Text("%.2f L · $water of ${settings.waterGoalGlasses} glasses".format(water * 0.25f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(onClick = { vm.addWater(-1) }, enabled = water > 0) { Icon(Icons.Filled.Remove, "Remove glass") }
                            FilledTonalIconButton(onClick = { vm.addWater(1) }) { Icon(Icons.Filled.Add, "Add glass") }
                        }
                        if (water >= settings.waterGoalGlasses) {
                            Text("Goal reached 💧", color = Aqua, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Steps
        item {
            JkCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(steps / settings.stepGoal.toFloat(), Leaf, size = 84.dp) {
                        Text("%,d".format(steps), style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Steps", style = MaterialTheme.typography.titleMedium)
                        when {
                            !vm.stepSensorAvailable -> Text("This device has no step counter sensor.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            !stepPerm -> {
                                Text("Allow activity access to count steps.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = { permLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }) { Text("Enable") }
                            }
                            else -> {
                                Text("%.2f km · %d kcal · goal %,d".format(Health.stepKm(steps, profile.heightCm),
                                    Health.stepCalories(steps, profile.weightKg), settings.stepGoal),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedButton(onClick = { nav.navigate(Routes.WALK) }) {
                                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start a walk")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diet
        item {
            val eaten = food.sumOf { it.kcal }
            val target = Health.targetCalories(profile)
            JkCard(Modifier.fillMaxWidth(), onClick = { nav.navigate(Routes.DIET) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Restaurant, Ember)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Diet & calories", style = MaterialTheme.typography.titleMedium)
                        Text("$eaten eaten · ${(target - eaten).coerceAtLeast(0)} kcal left",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { eaten / target.toFloat() }, Modifier.fillMaxWidth(), color = Ember)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                }
            }
        }

        item { SectionTitle("Mind & body") }
        item {
            NavRow(Icons.Outlined.Timer, "Intermittent fasting",
                if (fast != null) "Fast in progress — tap to view" else "Start a 13–20 hour fast", Sun) { nav.navigate(Routes.FASTING) }
        }
        item { NavRow(Icons.Outlined.SelfImprovement, "Meditate", "Guided sessions with calming sounds", Violet) { nav.navigate(Routes.MEDITATE) } }
        item { NavRow(Icons.Outlined.Air, "Breathing", "Box, 4-7-8 and relax patterns", Aqua) { nav.navigate(Routes.BREATHING) } }

        item { SectionTitle("Tools & learning") }
        item { NavRow(Icons.Outlined.Calculate, "Health calculators", "BMI, BMR, TDEE, ideal weight, macros", Leaf) { nav.navigate(Routes.CALCULATORS) } }
        item { NavRow(Icons.Outlined.WatchLater, "Stopwatch & interval timer", "Laps, Tabata and custom intervals", Ember) { nav.navigate(Routes.TOOLS) } }
        item { NavRow(Icons.AutoMirrored.Outlined.MenuBook, "Guides & articles", "Training, nutrition and recovery basics", Sun) { nav.navigate(Routes.ARTICLES) } }
    }
}

@Composable
fun IconTile(icon: ImageVector, color: Color) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = color) }
}

@Composable
fun NavRow(icon: ImageVector, title: String, sub: String, color: Color, onClick: () -> Unit) {
    JkCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, color)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}
