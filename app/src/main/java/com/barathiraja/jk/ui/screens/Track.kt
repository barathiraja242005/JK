package com.barathiraja.jk.ui.screens

import com.barathiraja.jk.steps.rememberStepPermission
import androidx.compose.foundation.clickable
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.barathiraja.jk.ui.theme.Jk
import com.barathiraja.jk.ui.components.ProgressBar
import com.barathiraja.jk.ui.components.CardBox
import com.barathiraja.jk.ui.components.Heading
import com.barathiraja.jk.ui.components.JkPage
import com.barathiraja.jk.ui.components.ProgressRing
import com.barathiraja.jk.ui.components.PageTitle
import com.barathiraja.jk.ui.components.PlainButton
import com.barathiraja.jk.ui.theme.plex
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.ui.JkViewModel
import com.barathiraja.jk.ui.Routes
import com.barathiraja.jk.ui.components.WaterBottle

@Composable
fun TrackScreen(vm: JkViewModel, nav: NavHostController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val water by vm.waterToday.collectAsStateWithLifecycle()
    val steps by vm.stepsToday.collectAsStateWithLifecycle()
    val fast by vm.activeFast.collectAsStateWithLifecycle()
    val food by vm.foodToday.collectAsStateWithLifecycle()

    val stepPerm = rememberStepPermission { vm.startSteps() }

    JkPage {
        item { PageTitle("Health", "Water, steps and food for today, and tools for rest and recovery.") }
        // Water
        item {
            CardBox(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WaterBottle(water / settings.waterGoalGlasses.toFloat(), Modifier.width(56.dp).height(96.dp))
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Water", style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
                        Text("$water of ${settings.waterGoalGlasses} glasses · %.2f L".format(water * 0.25f),
                            style = plex(14.sp), color = Jk.Muted)
                        if (water >= settings.waterGoalGlasses) Text("Goal reached", style = plex(14.sp, FontWeight.SemiBold), color = Jk.RedText)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = { vm.addWater(-1) }, enabled = water > 0,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Jk.Well, contentColor = Jk.Ink)) {
                            Icon(Icons.Filled.Remove, "Remove glass")
                        }
                        FilledTonalIconButton(onClick = { vm.addWater(1) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Jk.Yellow, contentColor = Jk.Black)) {
                            Icon(Icons.Filled.Add, "Add glass")
                        }
                    }
                }
            }
        }

        // Steps
        item {
            CardBox(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val frac = steps / settings.stepGoal.toFloat()
                    ProgressRing(frac.coerceIn(0f, 1f), 80.dp, 7.dp, Jk.Well, if (frac >= 1f) Jk.Yellow else Jk.Ink, key = "steps") {
                        Text("%,d".format(steps), style = plex(15.sp, FontWeight.Bold), color = Jk.Ink, maxLines = 1)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Steps", style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
                        when {
                            !vm.stepSensorAvailable -> Text("This phone has no step counter.", style = plex(14.sp), color = Jk.Muted)
                            !stepPerm.granted -> {
                                Text("Allow activity access so JK can count your steps.", style = plex(14.sp, line = 19.sp), color = Jk.Muted)
                                PlainButton("Allow", stepPerm.ask, Modifier.padding(top = 8.dp))
                            }
                            else -> {
                                Text("%.2f km · %d kcal · goal %,d".format(Health.stepKm(steps, profile.heightCm),
                                    Health.stepCalories(steps, profile.weightKg), settings.stepGoal), style = plex(14.sp), color = Jk.Muted)
                                Text("Counted while JK is open.", style = plex(12.sp), color = Jk.Muted)
                                PlainButton("Start a walk", { nav.navigate(Routes.WALK) }, Modifier.padding(top = 8.dp),
                                    icon = Icons.AutoMirrored.Filled.DirectionsWalk)
                            }
                        }
                    }
                }
            }
        }

        // Food
        item {
            val eaten = food.sumOf { it.kcal }
            val target = Health.targetCalories(profile)
            CardBox(padding = 16.dp, onClick = { nav.navigate(Routes.DIET) }, onClickLabel = "Open diet") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Food", style = plex(17.sp, FontWeight.Bold), color = Jk.Ink)
                            Text(if (eaten == 0) "Nothing logged yet · aim for $target kcal" else "$eaten eaten · ${(target - eaten).coerceAtLeast(0)} kcal left",
                                style = plex(14.sp), color = Jk.Muted)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Jk.Muted)
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(eaten / target.toFloat(), Jk.Well, if (eaten > target) Jk.Red else Jk.Ink, key = "food")
                }
            }
        }

        item { Heading("Rest & recovery") }
        item {
            LinkGroup(listOf(
                Link(Icons.Outlined.Timer, "Intermittent fasting", if (fast != null) "Fast in progress, tap to view" else "Start a 13–20 hour fast") { nav.navigate(Routes.FASTING) },
                Link(Icons.Outlined.SelfImprovement, "Meditate", "Guided sessions with calming sounds") { nav.navigate(Routes.MEDITATE) },
                Link(Icons.Outlined.Air, "Breathing", "Box, 4-7-8 and relax patterns") { nav.navigate(Routes.BREATHING) },
            ))
        }
        item { Heading("Tools & reading") }
        item {
            LinkGroup(listOf(
                Link(Icons.Outlined.Calculate, "Health calculators", "BMI, BMR, TDEE, ideal weight, macros") { nav.navigate(Routes.CALCULATORS) },
                Link(Icons.Outlined.WatchLater, "Stopwatch & interval timer", "Laps, Tabata and custom intervals") { nav.navigate(Routes.TOOLS) },
                Link(Icons.AutoMirrored.Outlined.MenuBook, "Guides & articles", "Training, nutrition and recovery basics") { nav.navigate(Routes.ARTICLES) },
            ))
        }
    }
}

class Link(val icon: ImageVector, val title: String, val sub: String, val onClick: () -> Unit)

/** Several links in one card, one row each with a thin line between. */
@Composable
fun LinkGroup(links: List<Link>) {
    CardBox(padding = 0.dp) {
        Column {
            links.forEachIndexed { i, l ->
                if (i > 0) Box(Modifier.padding(start = 72.dp).fillMaxWidth().height(1.dp).background(Jk.Line))
                Row(Modifier.fillMaxWidth().clickable(onClick = l.onClick).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Jk.Well), contentAlignment = Alignment.Center) {
                        Icon(l.icon, null, Modifier.size(22.dp), tint = Jk.Ink)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(l.title, style = plex(16.sp, FontWeight.SemiBold), color = Jk.Ink)
                        Text(l.sub, style = plex(13.sp, line = 18.sp), color = Jk.Muted)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Jk.Muted)
                }
            }
        }
    }
}

@Composable
/** An icon on a quiet square, the same as the rows in [LinkGroup]. */
fun IconTile(icon: ImageVector) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Jk.Well),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Jk.Ink) }
}

@Composable
fun NavRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    LinkGroup(listOf(Link(icon, title, sub, onClick)))
}
