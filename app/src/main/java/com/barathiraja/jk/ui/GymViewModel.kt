package com.barathiraja.jk.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.barathiraja.jk.AppContainer
import com.barathiraja.jk.gym.Account
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.GivenAward
import com.barathiraja.jk.gym.GymBackend
import com.barathiraja.jk.gym.TestGym
import com.barathiraja.jk.gym.Gym
import com.barathiraja.jk.gym.GymException
import com.barathiraja.jk.gym.MonthAwards
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.gym.friendlyMessage
import com.barathiraja.jk.gym.tidyName
import com.barathiraja.jk.gym.plural
import com.barathiraja.jk.gym.AwardsJob
import com.barathiraja.jk.gym.HistorySync
import com.barathiraja.jk.gym.Template
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Where the signed-in user stands with the gym; drives which screens JkRoot shows. */
sealed interface GymState {
    /** Firebase isn't configured on this build: JK runs as a personal app. */
    data object Loading : GymState
    data object SignedOut : GymState
    data class NoGym(val user: Account) : GymState
    data class Pending(val gym: Gym?) : GymState
    data class Removed(val gym: Gym?) : GymState
    data class Ready(val gym: Gym, val me: Person) : GymState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GymViewModel(private val c: AppContainer) : ViewModel() {
    /** True when the gym screens run on the made-up test gym instead of the real one. */
    val testMode: Boolean = c.testMode
    private val repo: GymBackend get() = if (testMode) c.testGym else c.gym

    /** Test mode: whose eyes the test gym is seen through. */
    val testViewer: StateFlow<TestGym.Viewer> get() = c.testGym.viewer
    fun viewTestGymAs(v: TestGym.Viewer) { if (testMode) { clearMessage(); c.testGym.viewAs(v) } }
    fun setTestMode(on: Boolean) = c.setTestMode(on)
    val today: Long get() = c.currentDay.value
    private val month get() = YearMonth.now()
    val restSec: Int get() = c.prefs.training.value.restSec.coerceAtLeast(15)

    /** True while this account's saved profile is being fetched after sign-in (JkRoot waits before onboarding). */
    val restoring = MutableStateFlow(false)

    private val _message = MutableStateFlow<String?>(null)
    /** Short error/confirmation messages for a snackbar. */
    val message: StateFlow<String?> = _message

    /** Shows [text] in the snackbar. */
    fun showMessage(text: String) { _message.value = text }
    /** Called once the snackbar has shown the message (or a screen wants an old error gone). */
    fun clearMessage() { _message.value = null }
    val busy = MutableStateFlow(false)

    /** Becomes true once Firebase has told us whether someone is signed in. */
    private val authKnown = MutableStateFlow(false)
    private val user: StateFlow<Account?> = when {
        testMode -> c.testGym.account.onEach { authKnown.value = true }
        c.gymEnabled -> c.auth.user.map { u -> u?.let { Account(it.uid, it.displayName.orEmpty(), it.photoUrl?.toString()) } }
            .onEach { authKnown.value = true }
        // A build without google-services.json can't sign in; it stops at the sign-in screen, which says so.
        else -> flowOf(null).onEach { authKnown.value = true }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null = still loading, "" = signed in but not in a gym. */
    private val gymId: StateFlow<String?> = user.flatMapLatest { u ->
        if (u == null) flowOf("") else repo.userGymId(u.uid).map { it.orEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val myRecord: StateFlow<Person?> = combine(gymId, user) { g, u -> g to u?.uid }.flatMapLatest { (g, uid) ->
        if (g.isNullOrEmpty() || uid == null) flowOf(null) else repo.person(g, uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The gym id once this account is an active member of it; gym-wide listeners restart on approval. */
    private val activeGymId: StateFlow<String?> = combine(gymId, myRecord) { g, m -> if (m?.active == true) g else "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /**
     * Gym data listeners stop a few seconds after no screen (or job) needs them, so a phone in a pocket isn't
     * billed for every set ticked in the gym.
     */
    private val whileUsed = SharingStarted.WhileSubscribed(5_000)

    private fun <T> perGym(empty: T, f: (String) -> Flow<T>): StateFlow<T> =
        activeGymId.flatMapLatest { id -> if (id.isNullOrEmpty()) flowOf(empty) else f(id) }.stateIn(viewModelScope, whileUsed, empty)

    val gym: StateFlow<Gym?> = gymId.flatMapLatest { if (it.isNullOrEmpty()) flowOf(null) else repo.gym(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val people: StateFlow<List<Person>> = perGym(emptyList()) { repo.people(it) }
    val awards: StateFlow<List<MonthAwards>> = perGym(emptyList()) { repo.awards(it) }
    /** Awards the owner gave by hand, newest first. */
    val givenAwards: StateFlow<List<GivenAward>> =
        perGym(emptyList()) { id -> repo.givenAwards(id) }.mapState { l -> l.sortedByDescending { it.givenAt } }

    /**
     * The workouts this person works with, last month (for "most improved" and history) through the next two
     * months (scheduled ones): the whole gym for the owner, their own members' for a trainer, their own for a member.
     * Reading only that keeps the gym on Firestore's free daily reads. Restarts at midnight so the window moves.
     */
    val assignments: StateFlow<List<Assignment>> = combine(activeGymId, myRecord.map { it?.uid to it?.role }.distinctUntilChanged(), c.currentDay) {
        g, who, day -> Triple(g, who, day)
    }.distinctUntilChanged().flatMapLatest { (g, who, day) ->
        val (uid, role) = who
        val from = LocalDate.ofEpochDay(day).minusMonths(1).withDayOfMonth(1).toEpochDay()
        val to = day + SCHEDULE_DAYS
        when {
            g.isNullOrEmpty() || uid == null -> flowOf(emptyList())
            role == Role.OWNER -> repo.assignments(g, from, to)
            role == Role.TRAINER -> repo.assignmentsOf(g, "trainerUid", uid, from, to)
            else -> repo.assignmentsOf(g, "memberUid", uid, from, to)
        }
    }.stateIn(viewModelScope, whileUsed, emptyList())

    /**
     * Everyone's workouts this month up to today: all the leaderboards need. Future weeks and last month are left
     * out, so members and trainers read far less than the whole gym. The owner already has it all in [assignments].
     */
    private val gymMonth: StateFlow<List<Assignment>> = combine(activeGymId, myRecord.map { it?.role }.distinctUntilChanged(), c.currentDay) {
        g, role, day -> Triple(g, role, day)
    }.distinctUntilChanged().flatMapLatest { (g, role, day) ->
        val start = LocalDate.ofEpochDay(day).withDayOfMonth(1).toEpochDay()
        when {
            g.isNullOrEmpty() -> flowOf(emptyList())
            role == Role.OWNER -> assignments.map { l -> l.filter { it.epochDay in start..day } }
            else -> repo.assignments(g, start, day)
        }
    }.stateIn(viewModelScope, whileUsed, emptyList())

    private fun <T, R> StateFlow<T>.mapState(f: (T) -> R): StateFlow<R> = map(f).stateIn(viewModelScope, whileUsed, f(value))

    val templates: StateFlow<List<Template>> = combine(activeGymId, user) { g, u -> g to u?.uid }.flatMapLatest { (g, uid) ->
        if (g.isNullOrEmpty() || uid == null) flowOf(emptyList()) else repo.templates(g, uid)
    }.stateIn(viewModelScope, whileUsed, emptyList())

    val state: StateFlow<GymState> = combine(authKnown, user, gymId, gym, myRecord) { known, u, g, gymDoc, me ->
        when {
            !known -> GymState.Loading
            u == null -> GymState.SignedOut
            g == null -> GymState.Loading
            g.isEmpty() -> GymState.NoGym(u)
            else -> {
                when {
                    gymDoc == null || me == null -> GymState.Loading
                    me.status == PersonStatus.PENDING -> GymState.Pending(gymDoc)
                    me.status == PersonStatus.REMOVED -> GymState.Removed(gymDoc)
                    else -> GymState.Ready(gymDoc, me)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GymState.Loading)

    val me: StateFlow<Person?> = state.map { (it as? GymState.Ready)?.me }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ---------- derived ----------

    // Scores are worked out off the main thread: they go over every workout in the gym this month.
    val monthScores: StateFlow<Map<String, Scoring.MemberScore>> = gymMonth.map {
        Scoring.memberScores(it, Scoring.monthRange(month), today)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, whileUsed, emptyMap())

    val memberRanking: StateFlow<List<Scoring.MemberScore>> = combine(monthScores, people) { s, p ->
        val active = p.filter { it.role == Role.MEMBER && it.active }.map { it.uid }.toSet()
        // Members with nothing assigned yet still appear, at the bottom.
        val all = s.filterKeys { it in active } + (active - s.keys).associateWith { Scoring.MemberScore(it, 0, 0, 0, 0, 0.0) }
        Scoring.memberRanking(all)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, whileUsed, emptyList())

    val trainerRanking: StateFlow<List<Scoring.TrainerScore>> = combine(people, gymMonth) { p, a ->
        Scoring.trainerScores(p, a, Scoring.monthRange(month), today)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, whileUsed, emptyList())

    /**
     * Turnout, quiet members, this month, trainers: the owner's home, and (over their own members) a trainer's.
     * Members never see it, so their phones don't work it out.
     */
    val ownerDigest: StateFlow<OwnerStats.Digest?> = combine(people, assignments, myRecord) { p, a, m ->
        if (m == null || m.role == Role.MEMBER) null else OwnerStats.digest(p, a, today, month, month.minusMonths(1).atDay(1).toEpochDay())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, whileUsed, null)

    fun person(uid: String?): Person? = people.value.firstOrNull { it.uid == uid }

    /** A member's current trainer; null when they have none or their trainer has left the gym. */
    fun trainerOf(member: Person): Person? = person(member.trainerUid)?.takeIf { it.active }

    private val history = HistorySync(c.dao)
    private val awardsJob = AwardsJob({ c.prefs.awardsCheckedMonth }, { c.prefs.awardsCheckedMonth = it })

    private val _ranksTab = MutableStateFlow(0)
    /** Which section the Ranks tab shows (0 members, 1 trainers, 2 awards); kept here so other screens can open it. */
    val ranksTab: StateFlow<Int> = _ranksTab
    fun showRanksTab(index: Int) { _ranksTab.value = index }

    init {
        // After sign-in: bring back this account's profile and settings if the phone doesn't have them.
        viewModelScope.launch {
            user.collect { u ->
                if (u == null || testMode || c.prefs.profile.value.onboarded) return@collect
                restoring.value = true
                runCatching { repo.loadBackup(u.uid) }.getOrNull()?.let { c.prefs.importBackup(it) }
                restoring.value = false
            }
        }
        // Keep the backup current (debounced) while signed in and set up.
        viewModelScope.launch {
            combine(user, c.prefs.profile, c.prefs.settings, c.prefs.training) { u, p, _, _ -> u?.uid.takeIf { p.onboarded && !testMode } }
                .debounce(3_000)
                .collect { uid -> if (uid != null && !restoring.value) runCatching { repo.saveBackup(uid, c.prefs.exportBackup()) } }
        }
        // Tidy names saved before tidyName existed (e.g. "_S. Janarthanan_").
        viewModelScope.launch {
            state.collect { s ->
                if (s is GymState.Ready && s.me.name != tidyName(s.me.name)) runCatching { repo.setName(s.gym.id, s.me.uid, tidyName(s.me.name)) }
            }
        }
        // A member's finished gym workouts count in this phone's history (streak, calories, Progress).
        viewModelScope.launch {
            combine(me, assignments) { m, a -> if (m?.role == Role.MEMBER && !testMode) a.filter { it.memberUid == m.uid } else null }
                .filterNotNull().distinctUntilChanged().debounce(500)
                .collect { mine ->
                    val windowStart = LocalDate.ofEpochDay(today).minusMonths(1).withDayOfMonth(1).toEpochDay()
                    runCatching { history.sync(mine, windowStart, c.prefs.training.value.restSec, c.prefs.profile.value.weightKg) }
                        .onFailure { Log.w("GymViewModel", "History sync failed", it) }
                }
        }
        // The owner's phone saves last month's awards once they're missing (see AwardsJob).
        viewModelScope.launch {
            combine(state, awards) { s, aw -> s to aw }.collect { (s, aw) ->
                if (s is GymState.Ready && s.me.role == Role.OWNER) awardsJob.saveLastMonthIfDue(repo, s.gym.id, aw, LocalDate.ofEpochDay(today))
            }
        }
    }

    // ---------- actions ----------

    /**
     * Runs one Firestore action: shows the busy state, then [success] or a plain-words error as a message.
     * Firestore keeps writes made offline and sends them later, but the call only returns once the server confirms;
     * when [queuedOk] and the server hasn't answered in [OFFLINE_WAIT_MS], the user is told it's saved on the phone.
     * The write itself runs in the app's scope, so giving up the wait never cancels it half-way (a long batch of
     * assignments would otherwise lose its later chunks offline). [then] runs once it's saved or queued, not on error.
     * Joining and signing in need the server, so they pass false and keep waiting. [quiet] skips the busy state and
     * the messages (for saves that happen on every tap, like ticking a set); errors are still shown.
     */
    private fun act(
        success: String? = null, queuedOk: Boolean = true, quiet: Boolean = false, then: (() -> Unit)? = null,
        block: suspend () -> Unit,
    ) = viewModelScope.launch {
        if (!quiet) busy.value = true
        try {
            if (queuedOk) {
                val write = c.appScope.async { block() }
                val finished = withTimeoutOrNull(OFFLINE_WAIT_MS) { write.await() } != null
                if (!finished) write.invokeOnCompletion { e -> if (e is Exception && e !is CancellationException) _message.value = friendlyMessage(e) }
                if (!quiet) _message.value = if (finished) success else "Saved on this phone. It will sync when you're back online."
            } else {
                block()
                _message.value = success
            }
            then?.invoke()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _message.value = friendlyMessage(e)
        } finally {
            if (!quiet) busy.value = false
        }
    }

    private val gymIdOrThrow get() = gymId.value?.takeIf { it.isNotEmpty() } ?: error("Not in a gym")
    private val uid get() = user.value?.uid ?: error("Not signed in")
    private val myName get() = tidyName(user.value?.displayName ?: c.prefs.profile.value.name).ifBlank { "JK user" }
    private val myPhoto get() = user.value?.photoUrl

    fun signIn(activity: Context) = act(queuedOk = false) {
        c.auth.signIn(activity)
    }

    /** Signs out and removes this account's data from the phone so the next person starts clean. */
    fun signOut() = act(queuedOk = false) {
        // In test mode "sign out" just leaves test mode: the real account and this phone's data stay as they were.
        if (testMode) { setTestMode(false); return@act }
        c.auth.signOut()
        c.wipePhone()
        c.restart()
    }

    /**
     * Deletes the account: its gym record loses the name and photo and is marked as left, the private backup is
     * deleted, then the Firebase account, then everything on this phone. The owner runs the gym, so can't do this here.
     */
    fun deleteAccount() = act(queuedOk = false) {
        if (me.value?.role == Role.OWNER) throw GymException("The owner's account runs the gym, so it can't be deleted from the app.")
        repo.deleteAccount(gymId.value?.takeIf { it.isNotEmpty() }, uid)
        if (testMode) { setTestMode(false); return@act }
        c.auth.deleteUser()
        c.wipePhone()
        c.restart()
    }

    /** Trainer: a new member code; the old one stops working, members who already joined stay. */
    fun newMemberCode() = act("New member code ready. The old one no longer works.") {
        repo.newTrainerCode(gymIdOrThrow, uid, me.value?.trainerCode)
    }

    fun createGym(name: String) = act("Gym created!", queuedOk = false) { repo.createGym(name.trim(), uid, myName, myPhoto) }
    fun joinAsTrainer(code: String) = act(queuedOk = false) { repo.joinAsTrainer(code, uid, myName, myPhoto) }
    /**
     * A new member always fills in their details (age, height, weight, goal) after joining, even on a phone that
     * already had a profile: from a restored backup, or from being a trainer or owner here before.
     */
    fun joinAsMember(code: String) = act("Welcome to the gym!", queuedOk = false) {
        repo.joinAsMember(code, uid, myName, myPhoto)
        c.prefs.saveProfile(c.prefs.profile.value.copy(onboarded = false))
    }
    fun leaveGym() = act(queuedOk = false) { repo.leave(gymIdOrThrow, uid) }

    fun approve(p: Person) {
        act("${p.firstName} is now a trainer") { repo.setStatus(gymIdOrThrow, p.uid, PersonStatus.ACTIVE) }
    }
    fun reject(p: Person) {
        act { repo.setStatus(gymIdOrThrow, p.uid, PersonStatus.REMOVED) }
    }
    fun removeMember(p: Person) = act("${p.firstName} removed") { repo.setStatus(gymIdOrThrow, p.uid, PersonStatus.REMOVED) }

    /** Active members a trainer looks after. */
    fun membersOf(trainerUid: String): List<Person> = people.value.filter { it.role == Role.MEMBER && it.active && it.trainerUid == trainerUid }

    /** Owner: moves one member to another trainer. */
    fun changeTrainer(member: Person, trainer: Person) {
        val done = "${member.firstName} now trains with ${trainer.firstName}"
        act(done) { repo.setTrainer(gymIdOrThrow, listOf(member.uid), trainer.uid) }
    }

    /**
     * Owner: takes a trainer or member out of the gym. A trainer's members move to [moveTo] (null leaves them
     * without a trainer until the owner picks one). Their past workouts stay in the records.
     */
    fun removeFromGym(p: Person, moveTo: Person?) {
        val theirs = if (p.role == Role.TRAINER) membersOf(p.uid).map { it.uid }.toSet() else emptySet()
        val done = "${p.firstName} removed from the gym"
        act(done) { repo.removeFromGym(gymIdOrThrow, p.uid, theirs.toList(), moveTo?.uid) }
    }

    /** Your own display name, as everyone in the gym sees it. */
    fun renameMe(name: String) {
        val n = tidyName(name)
        if (n.isBlank()) return
        val p = me.value ?: return
        act("Name saved") { repo.setName(gymIdOrThrow, p.uid, n) }
    }

    /** Creates one assignment per member per day; [note] reaches the member as the coach's note. */
    fun assign(title: String, members: Collection<String>, days: Collection<Long>, exercises: List<AssignedExercise>, note: String = "", onDone: () -> Unit) {
        val clean = exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = false) }) }
        val n = members.size * days.size
        act("Assigned ${plural(n, "workout")}", then = onDone) {
            val list = members.flatMap { m -> days.map { d -> Assignment("", uid, m, title.trim().ifBlank { "Workout" }, d, clean, trainerNote = note.trim()) } }
            repo.assign(gymIdOrThrow, list)
        }
    }

    fun deleteAssignment(a: Assignment) = act("Workout removed") { repo.deleteAssignment(gymIdOrThrow, a.id) }
    fun verify(a: Assignment, verified: Boolean, note: String) = act(if (verified) "Verified ✓" else "Saved") {
        repo.verify(gymIdOrThrow, a.id, verified, note.trim())
    }

    fun saveTemplate(title: String, exercises: List<AssignedExercise>) = act("Saved as template") {
        repo.saveTemplate(gymIdOrThrow, Template("", uid, title.trim().ifBlank { "Workout" }, exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = false) }) }))
    }
    fun giveAward(title: String, emoji: String, person: Person, note: String, onDone: () -> Unit) {
        act("${emoji} ${title.trim()} given to ${person.firstName}", then = onDone) {
            repo.giveAward(gymIdOrThrow, GivenAward("", title.trim(), emoji, person.uid, note.trim(), month.toString(), System.currentTimeMillis()))
        }
    }
    fun editGivenAward(a: GivenAward, title: String, note: String) {
        val t = title.trim(); val n = note.trim()
        act("Award updated") { repo.editGivenAward(gymIdOrThrow, a.id, t, n) }
    }
    fun removeGivenAward(a: GivenAward) {
        act("Award removed") { repo.deleteGivenAward(gymIdOrThrow, a.id) }
    }
    fun renameGym(name: String) = act("Gym name saved") { repo.renameGym(gymIdOrThrow, name.trim()) }

    fun deleteTemplate(t: Template) = act { repo.deleteTemplate(gymIdOrThrow, t.id) }

    /** Member progress. Finished workouts reach the local history through [syncGymSessions]. */
    fun saveProgress(a: Assignment) {
        val now = System.currentTimeMillis()
        val allDone = a.exercises.isNotEmpty() && a.exercises.all { it.done }
        val anyDone = a.setsDone > 0
        val updated = a.copy(
            status = when { allDone -> AssignStatus.DONE; anyDone -> AssignStatus.IN_PROGRESS; else -> AssignStatus.ASSIGNED },
            startedAt = a.startedAt ?: now.takeIf { anyDone },
            completedAt = if (allDone) a.completedAt ?: now else null,
        )
        act(quiet = true) { repo.saveProgress(gymIdOrThrow, updated) }
    }

    companion object {
        /** How long a write waits for the server before telling the user it's saved on the phone. */
        private const val OFFLINE_WAIT_MS = 8_000L
        /** How far ahead scheduled workouts are loaded (about two months). */
        const val SCHEDULE_DAYS = 62L

        fun factory(c: AppContainer) = viewModelFactory { initializer { GymViewModel(c) } }
    }
}
