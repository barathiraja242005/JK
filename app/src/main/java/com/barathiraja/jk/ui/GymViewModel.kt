package com.barathiraja.jk.ui

import android.content.Context
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.barathiraja.jk.AppContainer
import com.barathiraja.jk.data.ExerciseRepo
import com.barathiraja.jk.data.WorkoutSession
import com.barathiraja.jk.domain.Health
import com.barathiraja.jk.domain.TrainingEngine
import com.barathiraja.jk.gym.AssignStatus
import com.barathiraja.jk.gym.AssignedExercise
import com.barathiraja.jk.gym.Assignment
import com.barathiraja.jk.gym.GivenAward
import com.barathiraja.jk.gym.Gym
import com.barathiraja.jk.gym.GymException
import com.barathiraja.jk.gym.MonthAwards
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.PartialSave
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.gym.Template
import com.barathiraja.jk.ui.gym.plural
import com.google.firebase.auth.FirebaseUser
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Where the signed-in user stands with the gym; drives which screens JkRoot shows. */
sealed interface GymState {
    /** Firebase isn't configured on this build: JK runs as a personal app. */
    data object Disabled : GymState
    data object Loading : GymState
    data object SignedOut : GymState
    data class NoGym(val user: FirebaseUser) : GymState
    data class Pending(val gym: Gym?) : GymState
    data class Removed(val gym: Gym?) : GymState
    data class Ready(val gym: Gym, val me: Person) : GymState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class GymViewModel(private val c: AppContainer) : ViewModel() {
    private val repo get() = c.gym
    val today: Long get() = LocalDate.now().toEpochDay()
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
    private val authKnown = MutableStateFlow(!c.gymEnabled)
    private val user: StateFlow<FirebaseUser?> =
        (if (c.gymEnabled) c.auth.user.onEach { authKnown.value = true } else flowOf(null)).stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    private fun <T> perGym(empty: T, f: (String) -> Flow<T>): StateFlow<T> =
        activeGymId.flatMapLatest { id -> if (id.isNullOrEmpty()) flowOf(empty) else f(id) }.stateIn(viewModelScope, SharingStarted.Eagerly, empty)

    val gym: StateFlow<Gym?> = gymId.flatMapLatest { if (it.isNullOrEmpty()) flowOf(null) else repo.gym(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val people: StateFlow<List<Person>> = perGym(emptyList()) { repo.people(it) }
    val awards: StateFlow<List<MonthAwards>> = perGym(emptyList()) { repo.awards(it) }
    /** Awards the owner gave by hand, newest first. */
    val givenAwards: StateFlow<List<GivenAward>> =
        perGym(emptyList()) { id -> repo.givenAwards(id) }.mapState { l -> l.sortedByDescending { it.givenAt } }

    /** Last month (for awards and "most improved") through the next two months (scheduled workouts). */
    val assignments: StateFlow<List<Assignment>> = perGym(emptyList()) {
        repo.assignments(it, month.minusMonths(1).atDay(1).toEpochDay(), today + 62)
    }

    private fun <T, R> StateFlow<T>.mapState(f: (T) -> R): StateFlow<R> = map(f).stateIn(viewModelScope, SharingStarted.Eagerly, f(value))

    val templates: StateFlow<List<Template>> = combine(activeGymId, user) { g, u -> g to u?.uid }.flatMapLatest { (g, uid) ->
        if (g.isNullOrEmpty() || uid == null) flowOf(emptyList()) else repo.templates(g, uid)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val state: StateFlow<GymState> = combine(authKnown, user, gymId, gym, myRecord) { known, u, g, gymDoc, me ->
        when {
            !c.gymEnabled -> GymState.Disabled
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, if (c.gymEnabled) GymState.Loading else GymState.Disabled)

    val me: StateFlow<Person?> = state.map { (it as? GymState.Ready)?.me }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ---------- derived ----------

    val monthScores: StateFlow<Map<String, Scoring.MemberScore>> = assignments.map {
        Scoring.memberScores(it, Scoring.monthRange(month), today)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val memberRanking: StateFlow<List<Scoring.MemberScore>> = combine(monthScores, people) { s, p ->
        val active = p.filter { it.role == Role.MEMBER && it.active }.map { it.uid }.toSet()
        // Members with nothing assigned yet still appear, at the bottom.
        val all = s.filterKeys { it in active } + (active - s.keys).associateWith { Scoring.MemberScore(it, 0, 0, 0, 0, 0.0) }
        Scoring.memberRanking(all)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trainerRanking: StateFlow<List<Scoring.TrainerScore>> = combine(people, assignments) { p, a ->
        Scoring.trainerScores(p, a, Scoring.monthRange(month), today)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The owner's home screen numbers: turnout, quiet members, this month, trainers. */
    val ownerDigest: StateFlow<OwnerStats.Digest?> = combine(people, assignments) { p, a ->
        OwnerStats.digest(p, a, today, month, month.minusMonths(1).atDay(1).toEpochDay())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The signed-in member's workout for today, if their trainer assigned one. */
    val myToday: StateFlow<Assignment?> = combine(me, assignments) { m, a ->
        if (m == null || m.role != Role.MEMBER) null
        else a.filter { it.memberUid == m.uid && it.epochDay == today }.minByOrNull { if (it.done) 1 else 0 }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun person(uid: String?): Person? = people.value.firstOrNull { it.uid == uid }

    /** A member's current trainer; null when they have none or their trainer has left the gym. */
    fun trainerOf(member: Person): Person? = person(member.trainerUid)?.takeIf { it.active }

    private val _ranksTab = MutableStateFlow(0)
    /** Which section the Ranks tab shows (0 members, 1 trainers, 2 awards); kept here so other screens can open it. */
    val ranksTab: StateFlow<Int> = _ranksTab
    fun showRanksTab(index: Int) { _ranksTab.value = index }

    init {
        // After sign-in: bring back this account's profile and settings if the phone doesn't have them.
        viewModelScope.launch {
            user.collect { u ->
                if (u == null || c.prefs.profile.value.onboarded) return@collect
                restoring.value = true
                runCatching { repo.loadBackup(u.uid) }.getOrNull()?.let { c.prefs.importBackup(it) }
                restoring.value = false
            }
        }
        // Keep the backup current (debounced) while signed in and set up.
        viewModelScope.launch {
            combine(user, c.prefs.profile, c.prefs.settings, c.prefs.training) { u, p, _, _ -> u?.uid.takeIf { p.onboarded } }
                .debounce(3_000)
                .collect { uid -> if (uid != null && !restoring.value) runCatching { repo.saveBackup(uid, c.prefs.exportBackup()) } }
        }
        // Tidy names saved before tidyName existed (e.g. "_S. Janarthanan_").
        viewModelScope.launch {
            state.collect { s ->
                if (s is GymState.Ready && s.me.name != tidyName(s.me.name)) runCatching { repo.setName(s.gym.id, s.me.uid, tidyName(s.me.name)) }
            }
        }
        // The owner's phone saves last month's awards once they're missing (see saveLastMonthAwards).
        viewModelScope.launch {
            combine(state, awards) { s, aw -> s to aw }.collect { (s, aw) ->
                if (s is GymState.Ready && s.me.role == Role.OWNER) saveLastMonthAwards(s.gym.id, aw)
            }
        }
    }

    /** The month whose awards were last saved (or found saved) by this phone, so the check runs once per month. */
    private var awardsSavedFor: String? = null

    /**
     * Saves last month's automatic awards, if nobody has yet. Waits one grace day into the new month so members can
     * finish logging, and works from fresh server data covering last month and the one before it (for "most
     * improved"), because the saved result can never be changed.
     */
    private suspend fun saveLastMonthAwards(gymId: String, saved: List<MonthAwards>) {
        val last = month.minusMonths(1)
        val key = last.toString()
        if (awardsSavedFor == key) return
        if (saved.any { it.month == key }) { awardsSavedFor = key; return }
        if (LocalDate.now().dayOfMonth < AWARDS_GRACE_DAYS + 1) return
        try {
            val from = last.minusMonths(1).atDay(1).toEpochDay()
            val list = repo.fetchAssignments(gymId, from, last.atEndOfMonth().toEpochDay())
            val winners = Scoring.awards(repo.fetchPeople(gymId), list, last)
            if (winners.isNotEmpty()) repo.saveAwards(gymId, key, winners)
            awardsSavedFor = key
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline or refused: try again the next time the gym data changes.
            Log.w("GymViewModel", "Saving $key awards failed", e)
        }
    }

    // ---------- actions ----------

    /**
     * Runs one Firestore action: shows the busy state, then [success] or a plain-words error as a message.
     * Firestore keeps writes made offline and sends them later, but the call only returns once the server confirms;
     * when [queuedOk] and the server hasn't answered in [OFFLINE_WAIT_MS], the user is told it's saved on the phone.
     * Joining and signing in need the server, so they pass false and keep waiting.
     */
    private fun act(success: String? = null, queuedOk: Boolean = true, block: suspend () -> Unit) = viewModelScope.launch {
        busy.value = true
        try {
            if (queuedOk) {
                val finished = withTimeoutOrNull(OFFLINE_WAIT_MS) { block() } != null
                _message.value = if (finished) success else "Saved on this phone. It will sync when you're back online."
            } else {
                block()
                _message.value = success
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _message.value = friendly(e)
        } finally {
            busy.value = false
        }
    }

    private fun friendly(e: Exception): String = when {
        e is GymException -> e.message.orEmpty()
        e is PartialSave -> "Only ${e.saved} of ${e.total} were saved. Check your connection and assign the rest again."
        e is GetCredentialCancellationException -> "Sign-in cancelled"
        e is NoCredentialException -> "No Google account found on this phone"
        e is com.google.firebase.firestore.FirebaseFirestoreException &&
            e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to do that"
        e is com.google.firebase.FirebaseNetworkException -> "No internet connection"
        else -> e.message ?: "Something went wrong"
    }

    private val gymIdOrThrow get() = gymId.value?.takeIf { it.isNotEmpty() } ?: error("Not in a gym")
    private val uid get() = user.value?.uid ?: error("Not signed in")
    private val myName get() = tidyName(user.value?.displayName ?: c.prefs.profile.value.name).ifBlank { "JK user" }
    private val myPhoto get() = user.value?.photoUrl?.toString()

    fun signIn(activity: Context) = act(queuedOk = false) {
        c.auth.signIn(activity)
    }

    /** Signs out and removes this account's data from the phone so the next person starts clean. */
    fun signOut() = act(queuedOk = false) {
        c.auth.signOut()
        withContext(Dispatchers.IO) { c.db.clearAllTables() }
        c.prefs.clearAll()
    }

    fun createGym(name: String) = act("Gym created!", queuedOk = false) { repo.createGym(name.trim(), uid, myName, myPhoto) }
    fun joinAsTrainer(code: String) = act(queuedOk = false) { repo.joinAsTrainer(code, uid, myName, myPhoto) }
    fun joinAsMember(code: String) = act("Welcome to the gym!", queuedOk = false) { repo.joinAsMember(code, uid, myName, myPhoto) }
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
        val list = members.flatMap { m -> days.map { d -> Assignment("", uid, m, title.trim().ifBlank { "Workout" }, d, clean, trainerNote = note.trim()) } }
        val n = list.size
        act("Assigned ${plural(n, "workout")}") { repo.assign(gymIdOrThrow, list); onDone() }
    }

    fun deleteAssignment(a: Assignment) = act("Workout removed") { repo.deleteAssignment(gymIdOrThrow, a.id) }
    fun verify(a: Assignment, verified: Boolean, note: String) = act(if (verified) "Verified ✓" else "Saved") {
        repo.verify(gymIdOrThrow, a.id, verified, note.trim())
    }

    fun saveTemplate(title: String, exercises: List<AssignedExercise>) = act("Saved as template") {
        repo.saveTemplate(gymIdOrThrow, Template("", uid, title.trim().ifBlank { "Workout" }, exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = false) }) }))
    }
    fun giveAward(title: String, emoji: String, person: Person, note: String, onDone: () -> Unit) {
        act("${emoji} ${title.trim()} given to ${person.firstName}") {
            repo.giveAward(gymIdOrThrow, GivenAward("", title.trim(), emoji, person.uid, note.trim(), month.toString(), System.currentTimeMillis()))
            onDone()
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

    /** Member progress; the first completion also logs a local session so streaks and Progress count it. */
    fun saveProgress(a: Assignment) {
        val now = System.currentTimeMillis()
        val allDone = a.exercises.isNotEmpty() && a.exercises.all { it.done }
        val anyDone = a.setsDone > 0
        val updated = a.copy(
            status = when { allDone -> AssignStatus.DONE; anyDone -> AssignStatus.IN_PROGRESS; else -> AssignStatus.ASSIGNED },
            startedAt = a.startedAt ?: now.takeIf { anyDone },
            completedAt = if (allDone) a.completedAt ?: now else null,
        )
        val workoutId = "gym:${a.id}"
        act {
            repo.saveProgress(gymIdOrThrow, updated)
            // Mirror it in the local history once the save went through: one session per workout, removed if undone.
            when {
                allDone && a.completedAt == null -> {
                    val restSec = c.prefs.training.value.restSec
                    val sec = updated.exercises.sumOf { TrainingEngine.estimateSec(it.sets, restSec) }
                    val kcal = updated.exercises.sumOf {
                        Health.caloriesBurned(ExerciseRepo.get(it.exerciseId)?.met ?: 5f, c.prefs.profile.value.weightKg, TrainingEngine.estimateSec(it.sets, restSec))
                    }
                    c.dao.replaceSession(WorkoutSession(workoutId = workoutId, title = a.title, finishedAt = now, epochDay = today, durationSec = sec, calories = kcal))
                }
                !allDone && a.completedAt != null -> c.dao.deleteSessionsFor(workoutId)
            }
        }
    }

    companion object {
        /** How long a write waits for the server before telling the user it's saved on the phone. */
        private const val OFFLINE_WAIT_MS = 8_000L
        /** Days into a new month before last month's awards are saved, so late logs still count. */
        private const val AWARDS_GRACE_DAYS = 1

        /**
         * "_S. Janarthanan_" -> "S. Janarthanan", "KOUNDAR BARATHIRAJA" -> "Koundar Barathiraja",
         * "Barathiraja K 2023-2027" -> "Barathiraja K" (college accounts add the batch years).
         */
        fun tidyName(raw: String): String {
            val trimmed = raw.trim().trim('_', '.', '-', ' ').replace(Regex("\\s+"), " ")
                .replace(Regex("[\\s(\\[]*\\d{4}\\s*[-–]\\s*\\d{2,4}[)\\]]*$"), "")
                .replace(Regex("\\s+\\d+$"), "")
                .trim().trim('_', '.', '-', ' ')
            return if (trimmed.any { it.isLowerCase() }) trimmed
            else trimmed.split(' ').joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.titlecase() } }
        }

        fun factory(c: AppContainer) = viewModelFactory { initializer { GymViewModel(c) } }
    }
}
