package com.barathiraja.jk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role as A11yRole
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.barathiraja.jk.data.Place
import com.barathiraja.jk.data.Profile
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.ui.components.LocalNavBarInset
import com.barathiraja.jk.ui.gym.AssignScreen
import com.barathiraja.jk.ui.gym.AssignedSessionScreen
import com.barathiraja.jk.ui.gym.ChooseRoleScreen
import com.barathiraja.jk.ui.gym.GiveAwardScreen
import com.barathiraja.jk.ui.gym.GymLoading
import com.barathiraja.jk.ui.gym.MemberDetailScreen
import com.barathiraja.jk.ui.gym.MemberGymScreen
import com.barathiraja.jk.ui.gym.OwnerAwardsScreen
import com.barathiraja.jk.ui.gym.Owner
import com.barathiraja.jk.ui.gym.OwnerHomeScreen
import com.barathiraja.jk.ui.gym.OwnerMeScreen
import com.barathiraja.jk.ui.gym.OwnerPeopleScreen
import com.barathiraja.jk.ui.gym.RanksScreen
import com.barathiraja.jk.ui.gym.SignInScreen
import com.barathiraja.jk.ui.gym.TrainerDetailScreen
import com.barathiraja.jk.ui.gym.TrainerMembersScreen
import com.barathiraja.jk.ui.gym.WaitingScreen
import com.barathiraja.jk.ui.screens.ArticleScreen
import com.barathiraja.jk.ui.screens.ArticlesScreen
import com.barathiraja.jk.ui.screens.BreathingScreen
import com.barathiraja.jk.ui.screens.BuilderScreen
import com.barathiraja.jk.ui.screens.CalculatorsScreen
import com.barathiraja.jk.ui.screens.ChallengeDetailScreen
import com.barathiraja.jk.ui.screens.DietScreen
import com.barathiraja.jk.ui.screens.EditDaysScreen
import com.barathiraja.jk.ui.screens.EditProfileScreen
import com.barathiraja.jk.ui.screens.ExerciseDetailScreen
import com.barathiraja.jk.ui.screens.ExerciseSessionScreen
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
import com.barathiraja.jk.ui.screens.TrainingHistoryScreen
import com.barathiraja.jk.ui.screens.TrainingSetupScreen
import com.barathiraja.jk.ui.screens.WalkScreen
import com.barathiraja.jk.ui.screens.WorkoutDetailScreen
import com.barathiraja.jk.ui.screens.WorkoutPlayerScreen
import com.barathiraja.jk.ui.screens.WorkoutsScreen
import com.barathiraja.jk.ui.theme.Charcoal
import com.barathiraja.jk.ui.theme.HeroFill
import com.barathiraja.jk.ui.theme.LocalDarkTheme
import com.barathiraja.jk.ui.theme.Red

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
    // Gym
    const val GYM = "gym"
    const val MEMBERS = "members"
    const val RANKS = "ranks"
    const val ASSIGN_TAB = "assignTab"
    const val ASSIGN = "assign/{id}"
    const val GYM_MEMBER = "gymMember/{id}"
    const val GYM_TRAINER = "gymTrainer/{id}"
    const val GIVE_AWARD = "giveAward"
    const val ASSIGNED = "assigned/{id}"
    const val OWNER_PEOPLE = "ownerPeople"
    const val OWNER_AWARDS = "ownerAwards"
    fun assign(memberUid: String) = "assign/${memberUid.ifBlank { "-" }}"
    fun gymMember(uid: String) = "gymMember/$uid"
    fun gymTrainer(uid: String) = "gymTrainer/$uid"
    fun assigned(id: String) = "assigned/$id"
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

private val soloTabs = listOf(
    Tab(Routes.TODAY, "Today", Icons.Outlined.WbSunny),
    Tab(Routes.WORKOUTS, "Train", Icons.Outlined.FitnessCenter),
    Tab(Routes.SHORTS, "Shorts", Icons.Outlined.SlowMotionVideo),
    Tab(Routes.TRACK, "Health", Icons.Outlined.FavoriteBorder),
    Tab(Routes.PROGRESS, "Progress", Icons.Outlined.Insights),
)
private val memberTabs = listOf(
    Tab(Routes.TODAY, "Today", Icons.Outlined.WbSunny),
    Tab(Routes.WORKOUTS, "Train", Icons.Outlined.FitnessCenter),
    Tab(Routes.GYM, "Gym", Icons.Outlined.Groups),
    Tab(Routes.TRACK, "Health", Icons.Outlined.FavoriteBorder),
    Tab(Routes.PROGRESS, "Progress", Icons.Outlined.Insights),
)
private val trainerTabs = listOf(
    Tab(Routes.MEMBERS, "Members", Icons.Outlined.Groups),
    Tab(Routes.ASSIGN_TAB, "Assign", Icons.Outlined.AddTask),
    Tab(Routes.RANKS, "Ranks", Icons.Outlined.EmojiEvents),
    Tab(Routes.WORKOUTS, "Train", Icons.Outlined.FitnessCenter),
    Tab(Routes.PROFILE, "Me", Icons.Outlined.Person),
)
/** The owner gets one tab per job, each thing in one place: today, people, awards, and their gym and settings. */
private val ownerTabs = listOf(
    Tab(Routes.GYM, "Home", Icons.Outlined.Home),
    Tab(Routes.OWNER_PEOPLE, "People", Icons.Outlined.Groups),
    Tab(Routes.OWNER_AWARDS, "Awards", Icons.Outlined.EmojiEvents),
    Tab(Routes.PROFILE, "Me", Icons.Outlined.Person),
)

/**
 * The floating bottom bar: a charcoal bar whose top edge dips smoothly under the open tab, with a raised red button
 * resting in the dip that shows that tab's icon. Black anchors the screen; the one red circle says "you are here".
 * Picking another tab slides the dip and the button across to it; the open tab's label sits in the dip under the
 * button in white, the others show icon over label in light grey (7:1 on black).
 * Nothing is painted behind it, so pages scroll under it.
 */
@Composable
private fun DipBottomBar(tabs: List<Tab>, route: String?, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    // In dark mode the bar lifts a step off the black page (the hero fill) so it still reads as a bar.
    val barFill = if (LocalDarkTheme.current) HeroFill else Charcoal
    val selected = tabs.indexOfFirst { it.route == route }.coerceAtLeast(0)
    val pos by animateFloatAsState(
        selected.toFloat(), spring(dampingRatio = 0.78f, stiffness = 380f), label = "dip")
    // Fewer tabs leave room for a bigger button and a wider, deeper dip.
    val roomy = tabs.size <= 3
    val button = if (roomy) 52.dp else 48.dp
    val hw = if (roomy) 64.dp else 46.dp
    val depth = if (roomy) 32.dp else 28.dp
    val rise = if (roomy) 18.dp else 16.dp
    val inner = 18.dp
    // Full screen width, flush with the bottom edge: the bar's white runs down behind the system gesture area.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    BoxWithConstraints(
        modifier.fillMaxWidth().height(BAR_HEIGHT + rise + navInset),
    ) {
        val slot = (maxWidth - inner * 2) / tabs.size
        val cx = inner + slot * (pos + 0.5f)
        val barShape = DipShape(radius = 28.dp, center = cx, dipHalfWidth = hw, dipDepth = depth, roundBottom = false)
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(BAR_HEIGHT + navInset)
                .shadow(24.dp, barShape, ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.22f))
                .clip(barShape).background(barFill).padding(start = inner, end = inner, bottom = navInset),
        ) {
            tabs.forEachIndexed { i, tab -> SideTab(tab, i == selected, Modifier.weight(1f).fillMaxHeight(), onSelect) }
        }
        val tab = tabs[selected]
        // Decoration for the open tab: the tab row under it already is the accessible control, so this is hidden.
        Surface(
            shape = CircleShape, color = Red, contentColor = Color.White, shadowElevation = 8.dp,
            modifier = Modifier.offset(x = cx - button / 2).size(button).clearAndSetSemantics { },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(tab.icon, null, Modifier.size(if (roomy) 22.dp else 20.dp))
            }
        }
    }
}

private val BAR_HEIGHT = 60.dp

/** One slot: icon over label in light grey; when open, just its label in white, low in the dip under the button. */
@Composable
private fun SideTab(tab: Tab, on: Boolean, modifier: Modifier, onSelect: (String) -> Unit) {
    val idle = Owner.OnDarkMuted
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .selectable(selected = on, role = A11yRole.Tab) { onSelect(tab.route) }
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        AnimatedVisibility(!on,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()) {
            Icon(tab.icon, null, tint = idle, modifier = Modifier.size(21.dp))
        }
        Text(tab.label, style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 11.sp), color = if (on) Color.White else idle, maxLines = 1,
            modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Rounded bar with a smooth dip centred in its top edge: two S-curves meeting at the dip's floor, so the
 * outline reads as one curve around the button that sits in it.
 */
private class DipShape(
    private val radius: Dp,
    private val center: Dp,
    private val dipHalfWidth: Dp,
    private val dipDepth: Dp,
    private val roundBottom: Boolean = true,
) : Shape {
    override fun createOutline(
        size: Size, layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = with(density) {
        val d = dipDepth.toPx()
        val w = size.width; val h = size.height; val cx = center.toPx()
        // Near an end the dip narrows and the top corner tightens, so the outline never runs past the bar's edge.
        val edge = 12.dp.toPx()
        val hw = dipHalfWidth.toPx().coerceAtMost(cx - edge).coerceAtMost(w - cx - edge)
        val rl = radius.toPx().coerceAtMost(cx - hw)
        val rr = radius.toPx().coerceAtMost(w - cx - hw)
        val r = radius.toPx()
        Outline.Generic(Path().apply {
            moveTo(0f, rl)
            if (rl > 0f) arcTo(Rect(0f, 0f, 2 * rl, 2 * rl), 180f, 90f, false)
            lineTo(cx - hw, 0f)
            // Shoulder eases off the top edge, then the wall curves round into a flat floor under the button.
            cubicTo(cx - hw * 0.5f, 0f, cx - hw * 0.5f, d, cx, d)
            cubicTo(cx + hw * 0.5f, d, cx + hw * 0.5f, 0f, cx + hw, 0f)
            lineTo(w - rr, 0f)
            if (rr > 0f) arcTo(Rect(w - 2 * rr, 0f, w, 2 * rr), 270f, 90f, false)
            if (roundBottom) {
                lineTo(w, h - r)
                arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
                lineTo(r, h)
                arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            } else {
                lineTo(w, h)
                lineTo(0f, h)
            }
            close()
        })
    }
}

private fun NavBackStackEntry.arg(name: String) = arguments?.getString(name).orEmpty()

@Composable
fun JkRoot(vm: JkViewModel, tvm: TrainingViewModel, gvm: GymViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val training by tvm.prefs.collectAsStateWithLifecycle()
    val gymState by gvm.state.collectAsStateWithLifecycle()
    var pendingProfile by remember { mutableStateOf<Profile?>(null) }

    // Gym gate: everyone signs in and joins a gym first (only builds without Firebase run solo).
    val solo = gymState == GymState.Disabled
    if (!solo) when (val s = gymState) {
        GymState.Loading -> { GymLoading(); return }
        GymState.SignedOut -> { SignInScreen(gvm); return }
        is GymState.NoGym -> { ChooseRoleScreen(gvm, s.user.displayName.orEmpty()); return }
        is GymState.Pending -> { WaitingScreen(gvm, s.gym, removed = false); return }
        is GymState.Removed -> { WaitingScreen(gvm, s.gym, removed = true); return }
        else -> {}
    }
    val me = (gymState as? GymState.Ready)?.me
    val role = me?.role
    val restoring by gvm.restoring.collectAsStateWithLifecycle()
    if (restoring && !profile.onboarded) { GymLoading(); return }
    // Owners and trainers skip the body-stats onboarding; their name comes from Google.
    if (me != null && role != Role.MEMBER && !profile.onboarded) {
        LaunchedEffect(me.uid) { vm.saveProfile(profile.copy(onboarded = true, name = me.name), logWeight = false) }
        GymLoading()
        return
    }
    if (!profile.onboarded) {
        val p = pendingProfile
        if (p == null) {
            // Gym members start with their Google name and "Gym" picked.
            val initial = if (me != null) Profile(name = me.name, place = Place.GYM)
                else Profile()
            OnboardingScreen(initial, onDone = { pendingProfile = it })
        } else {
            // Step 2 of onboarding: training preferences, then the plan is generated.
            TrainingSetupScreen(training, p.place, p.goal, onFinish = { t, place, goal ->
                vm.saveProfile(p.copy(onboarded = true, place = place, goal = goal))
                tvm.setup(t)
            }, onBack = { pendingProfile = null })
        }
        return
    }

    val tabs = when (role) { Role.OWNER -> ownerTabs; Role.TRAINER -> trainerTabs; Role.MEMBER -> memberTabs; null -> soloTabs }
    val gymVm = gvm.takeIf { gymState != GymState.Disabled }
    key(role) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }
    val message by gvm.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); gvm.clearMessage() }
    }

    // The bar floats over the screens (nothing painted behind it); screens pad their lists by LocalNavBarInset.
    val barInset = if (showBar) 88.dp + WindowInsets.navigationBars
        .asPaddingValues().calculateBottomPadding() else 0.dp
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = barInset)) },
        contentWindowInsets = WindowInsets(0),
    ) { _ ->
      Box(Modifier.fillMaxSize()) {
      CompositionLocalProvider(LocalNavBarInset provides barInset) {
        NavHost(nav, startDestination = tabs.first().route, modifier = Modifier.fillMaxSize()) {
            composable(Routes.TODAY) { TodayScreen(vm, tvm, nav, gvm.takeIf { role == Role.MEMBER }) }
            composable(Routes.GYM) { if (role == Role.OWNER) OwnerHomeScreen(gvm, nav) else MemberGymScreen(gvm, nav) }
            composable(Routes.MEMBERS) { TrainerMembersScreen(gvm, nav) }
            composable(Routes.RANKS) { RanksScreen(gvm, nav) }
            composable(Routes.ASSIGN_TAB) { AssignScreen("", gvm, nav) }
            composable(Routes.ASSIGN) { AssignScreen(it.arg("id").takeIf { a -> a != "-" }.orEmpty(), gvm, nav) }
            composable(Routes.GYM_MEMBER) { MemberDetailScreen(it.arg("id"), gvm, nav) }
            composable(Routes.GYM_TRAINER) { TrainerDetailScreen(it.arg("id"), gvm, nav) }
            composable(Routes.OWNER_PEOPLE) { OwnerPeopleScreen(gvm, nav) }
            composable(Routes.OWNER_AWARDS) { OwnerAwardsScreen(gvm, nav) }
            composable(Routes.GIVE_AWARD) { GiveAwardScreen(gvm, nav) }
            composable(Routes.ASSIGNED) { AssignedSessionScreen(it.arg("id"), gvm, nav) }
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
            composable(Routes.PROFILE) { if (role == Role.OWNER) OwnerMeScreen(vm, gvm, nav) else ProfileScreen(vm, nav, gymVm) }
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
        if (showBar) DipBottomBar(tabs, route, Modifier.align(Alignment.BottomCenter)) { nav.switchTab(it) }
      }
    }
    }
}

fun NavHostController.switchTab(route: String) {
    val start = graph.findStartDestination()
    // Going "home": pop back to it. Navigating to it with restoreState would bring back the tab we just saved.
    if (start.route == route) {
        popBackStack(start.id, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(start.id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
