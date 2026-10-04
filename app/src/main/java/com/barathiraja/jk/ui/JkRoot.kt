package com.barathiraja.jk.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.barathiraja.jk.ui.screens.EditDaysScreen
import com.barathiraja.jk.ui.screens.ExerciseSessionScreen
import com.barathiraja.jk.ui.screens.TrainingHistoryScreen
import com.barathiraja.jk.ui.screens.TrainingSetupScreen
import com.barathiraja.jk.ui.screens.ArticleScreen
import com.barathiraja.jk.ui.screens.ArticlesScreen
import com.barathiraja.jk.ui.screens.BreathingScreen
import com.barathiraja.jk.ui.screens.BuilderScreen
import com.barathiraja.jk.ui.screens.CalculatorsScreen
import com.barathiraja.jk.ui.screens.ChallengeDetailScreen
import com.barathiraja.jk.ui.screens.DietScreen
import com.barathiraja.jk.ui.screens.EditProfileScreen
import com.barathiraja.jk.ui.screens.ExerciseDetailScreen
import com.barathiraja.jk.ui.screens.FastingScreen
import com.barathiraja.jk.ui.screens.HelpScreen
import com.barathiraja.jk.ui.screens.MeditateScreen
import com.barathiraja.jk.ui.screens.MeditationPlayerScreen
import com.barathiraja.jk.ui.screens.OnboardingScreen
import com.barathiraja.jk.ui.screens.PhotosScreen
import com.barathiraja.jk.ui.screens.ProfileScreen
import com.barathiraja.jk.ui.screens.ProgressScreen
import com.barathiraja.jk.ui.screens.ShortsScreen
import com.barathiraja.jk.ui.screens.TodayScreen
import com.barathiraja.jk.ui.screens.ToolsScreen
import com.barathiraja.jk.ui.screens.TrackScreen
import com.barathiraja.jk.ui.screens.WalkScreen
import com.barathiraja.jk.ui.screens.WorkoutDetailScreen
import com.barathiraja.jk.ui.screens.WorkoutPlayerScreen
import com.barathiraja.jk.ui.screens.WorkoutsScreen

object Routes {
    const val TODAY = "today"
    const val WORKOUTS = "workouts"
    const val SHORTS = "shorts"
    const val TRACK = "track"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val WORKOUT = "workout/{id}"
    const val PLAYER = "player/{id}"
    const val EXERCISE = "exercise/{id}"
    const val CHALLENGE = "challenge/{id}"
    const val BUILDER = "builder/{id}"
    const val MEDITATION = "meditation/{id}"
    const val ARTICLE = "article/{id}"
    const val FASTING = "fasting"
    const val BREATHING = "breathing"
    const val CALCULATORS = "calculators"
    const val EDIT_PROFILE = "editProfile"
    const val DIET = "diet"
    const val MEDITATE = "meditate"
    const val WALK = "walk"
    const val TOOLS = "tools"
    const val ARTICLES = "articles"
    const val PHOTOS = "photos"
    const val HELP = "help"
    const val SESSION = "session/{id}"
    const val EDIT_DAYS = "editDays"
    const val TRAIN_SETUP = "trainSetup"
    const val TRAIN_HISTORY = "trainHistory"
    fun session(id: Long) = "session/$id"
    fun workout(id: String) = "workout/$id"
    fun player(id: String) = "player/$id"
    fun exercise(id: String) = "exercise/$id"
    fun challenge(id: String) = "challenge/$id"
    fun builder(id: Long) = "builder/$id"
    fun meditation(id: String) = "meditation/$id"
    fun article(id: String) = "article/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, "Today", Icons.Outlined.WbSunny),
    Tab(Routes.WORKOUTS, "Train", Icons.Outlined.FitnessCenter),
    Tab(Routes.SHORTS, "Shorts", Icons.Outlined.SlowMotionVideo),
    Tab(Routes.TRACK, "Health", Icons.Outlined.FavoriteBorder),
    Tab(Routes.PROGRESS, "Progress", Icons.Outlined.Insights),
)

private fun NavBackStackEntry.arg(name: String) = arguments?.getString(name).orEmpty()

@Composable
fun JkRoot(vm: JkViewModel, tvm: TrainingViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val training by tvm.prefs.collectAsStateWithLifecycle()
    var pendingProfile by remember { mutableStateOf<com.barathiraja.jk.data.Profile?>(null) }
    if (!profile.onboarded) {
        val p = pendingProfile
        if (p == null) {
            OnboardingScreen(onDone = { pendingProfile = it })
        } else {
            // Step 2 of onboarding: training preferences, then the plan is generated.
            TrainingSetupScreen(training, p.place, p.goal, onFinish = { t, place, goal ->
                vm.saveProfile(p.copy(onboarded = true, place = place, goal = goal))
                tvm.setup(t)
            }, onBack = { pendingProfile = null })
        }
        return
    }

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.TODAY, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Routes.TODAY) { TodayScreen(vm, tvm, nav) }
            composable(Routes.WORKOUTS) { WorkoutsScreen(vm, tvm, nav) }
            composable(Routes.SESSION) { ExerciseSessionScreen(it.arg("id").toLongOrNull() ?: 0L, tvm, nav) }
            composable(Routes.EDIT_DAYS) { EditDaysScreen(tvm, nav) }
            composable(Routes.TRAIN_HISTORY) { TrainingHistoryScreen(tvm, nav) }
            composable(Routes.TRAIN_SETUP) {
                TrainingSetupScreen(training, profile.place, profile.goal, onFinish = { t, place, goal ->
                    vm.saveProfile(profile.copy(place = place, goal = goal), logWeight = false)
                    tvm.setup(t)
                    nav.popBackStack()
                }, onBack = { nav.popBackStack() })
            }
            composable(Routes.SHORTS) { ShortsScreen(vm, nav) }
            composable(Routes.TRACK) { TrackScreen(vm, nav) }
            composable(Routes.PROGRESS) { ProgressScreen(vm, nav) }
            composable(Routes.PROFILE) { ProfileScreen(vm, nav) }
            composable(Routes.WORKOUT) { WorkoutDetailScreen(it.arg("id"), vm, nav) }
            composable(Routes.PLAYER) { WorkoutPlayerScreen(it.arg("id"), vm, nav) }
            composable(Routes.EXERCISE) { ExerciseDetailScreen(it.arg("id"), vm, nav) }
            composable(Routes.CHALLENGE) { ChallengeDetailScreen(it.arg("id"), vm, nav) }
            composable(Routes.BUILDER) { BuilderScreen(it.arg("id").toLongOrNull() ?: 0L, vm, nav) }
            composable(Routes.MEDITATION) { MeditationPlayerScreen(it.arg("id"), vm, nav) }
            composable(Routes.ARTICLE) { ArticleScreen(it.arg("id"), nav) }
            composable(Routes.FASTING) { FastingScreen(vm, nav) }
            composable(Routes.BREATHING) { BreathingScreen(nav) }
            composable(Routes.CALCULATORS) { CalculatorsScreen(vm, nav) }
            composable(Routes.DIET) { DietScreen(vm, nav) }
            composable(Routes.MEDITATE) { MeditateScreen(vm, nav) }
            composable(Routes.WALK) { WalkScreen(vm, nav) }
            composable(Routes.TOOLS) { ToolsScreen(nav) }
            composable(Routes.ARTICLES) { ArticlesScreen(nav) }
            composable(Routes.PHOTOS) { PhotosScreen(vm, nav) }
            composable(Routes.HELP) { HelpScreen(nav) }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(profile, onSave = { vm.saveProfile(it); nav.popBackStack() }, onBack = { nav.popBackStack() })
            }
        }
    }
}

fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
