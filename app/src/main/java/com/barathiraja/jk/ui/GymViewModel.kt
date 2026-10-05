package com.barathiraja.jk.ui

import android.content.Context
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
import com.barathiraja.jk.gym.DemoData
import com.barathiraja.jk.gym.GivenAward
import com.barathiraja.jk.gym.Gym
import com.barathiraja.jk.gym.MonthAwards
import com.barathiraja.jk.gym.OwnerStats
import com.barathiraja.jk.gym.Person
import com.barathiraja.jk.gym.PersonStatus
import com.barathiraja.jk.gym.Role
import com.barathiraja.jk.gym.Scoring
import com.barathiraja.jk.gym.Template
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.LocalDate
import java.time.YearMonth

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

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class GymViewModel(private val c: AppContainer) : ViewModel() {
    private val repo get() = c.gym
    val today: Long get() = LocalDate.now().toEpochDay()
    private val month get() = YearMonth.now()
    val restSec: Int get() = c.prefs.training.value.restSec.coerceAtLeast(15)

    /** True while this account's saved profile is being fetched after sign-in (JkRoot waits before onboarding). */
    val restoring = MutableStateFlow(false)

    /** Short error/confirmation messages for a snackbar. */
    val message = MutableStateFlow<String?>(null)
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
    /** Sample gym shown instead of the real one while set (owner's Me → Demo data). Never saved anywhere. */
    val demo = MutableStateFlow<DemoData.Gym?>(null)
    private fun <T> orDemo(real: StateFlow<T>, pick: (DemoData.Gym) -> T): StateFlow<T> =
        combine(real, demo) { r, d -> if (d == null) r else pick(d) }.stateIn(viewModelScope, SharingStarted.Eagerly, real.value)

    val people: StateFlow<List<Person>> = orDemo(perGym(emptyList()) { repo.people(it) }) { it.people }
    val awards: StateFlow<List<MonthAwards>> = orDemo(perGym(emptyList()) { repo.awards(it) }) { it.awards }
    /** Awards the owner gave by hand, newest first. */
    val givenAwards: StateFlow<List<GivenAward>> =
        orDemo(perGym(emptyList()) { id -> repo.givenAwards(id) }) { it.given }.mapState { l -> l.sortedByDescending { it.givenAt } }

    /** Last month (for awards and "most improved") through the next two months (scheduled workouts). */
    val assignments: StateFlow<List<Assignment>> = orDemo(perGym(emptyList()) {
        repo.assignments(it, month.minusMonths(1).atDay(1).toEpochDay(), today + 62)
    }) { it.assignments }

    private fun <T, R> StateFlow<T>.mapState(f: (T) -> R): StateFlow<R> = map(f).stateIn(viewModelScope, SharingStarted.Eagerly, f(value))

    fun setDemo(on: Boolean) {
        val owner = me.value
        demo.value = if (on && owner != null) DemoData.build(owner, today) else null
    }

    /** Applies an owner action to the demo gym instead of Firestore. */
    private fun editDemo(success: String, f: (DemoData.Gym) -> DemoData.Gym) {
        demo.value?.let { demo.value = f(it); message.value = "$success (demo)" }
    }

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

    /** Which section the Ranks tab shows (0 members, 1 trainers, 2 awards); kept here so other screens can open it. */
    val ranksTab = MutableStateFlow(0)

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
        // Save last month's awards once, from whichever phone opens first after the month ends.
        viewModelScope.launch {
            combine(state, assignments, awards) { s, a, aw -> Triple(s, a, aw) }.collect { (s, a, aw) ->
                // Never save awards computed from demo data.
                if (s !is GymState.Ready || demo.value != null) return@collect
                val last = month.minusMonths(1)
                val key = last.toString()
                if (aw.any { it.month == key } || a.none { it.epochDay in Scoring.monthRange(last) }) return@collect
                val winners = Scoring.awards(people.value, a, last)
                if (winners.isNotEmpty()) repo.saveAwards(s.gym.id, key, winners)
            }
        }
    }

    // ---------- actions ----------

    private fun act(success: String? = null, block: suspend () -> Unit): kotlinx.coroutines.Job {
        if (demo.value != null) {
            message.value = "Demo data is on, so this isn't saved"
            return kotlinx.coroutines.Job().apply { complete() }
        }
        return launchAct(success, block)
    }

    private fun launchAct(success: String?, block: suspend () -> Unit) = viewModelScope.launch {
        busy.value = true
        try {
            block()
            if (success != null) message.value = success
        } catch (e: Exception) {
            message.value = friendly(e)
        } finally {
            busy.value = false
        }
    }

    private fun friendly(e: Exception): String = when {
        e is com.barathiraja.jk.gym.GymException -> e.message.orEmpty()
        e is androidx.credentials.exceptions.GetCredentialCancellationException -> "Sign-in cancelled"
        e is androidx.credentials.exceptions.NoCredentialException -> "No Google account found on this phone"
        e is com.google.firebase.firestore.FirebaseFirestoreException &&
            e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> "You don't have permission to do that"
        e is com.google.firebase.FirebaseNetworkException -> "No internet connection"
        else -> e.message ?: "Something went wrong"
    }

    private val gymIdOrThrow get() = gymId.value?.takeIf { it.isNotEmpty() } ?: error("Not in a gym")
    private val uid get() = user.value?.uid ?: error("Not signed in")
    private val myName get() = tidyName(user.value?.displayName ?: c.prefs.profile.value.name).ifBlank { "JK user" }
    private val myPhoto get() = user.value?.photoUrl?.toString()

    fun signIn(activity: Context) = act {
        c.auth.signIn(activity)
    }

    /** Signs out and removes this account's data from the phone so the next person starts clean. */
    fun signOut() = launchAct(null) {
        demo.value = null
        c.auth.signOut()
        withContext(Dispatchers.IO) { c.db.clearAllTables() }
        c.prefs.clearAll()
    }

    fun createGym(name: String) = act("Gym created!") { repo.createGym(name.trim(), uid, myName, myPhoto) }
    fun joinAsTrainer(code: String) = act { repo.joinAsTrainer(code, uid, myName, myPhoto) }
    fun joinAsMember(code: String) = act("Welcome to the gym!") { repo.joinAsMember(code, uid, myName, myPhoto) }
    fun leaveGym() = act { repo.leave(uid) }

    fun approve(p: Person) {
        if (demo.value != null) return editDemo("${p.firstName} is now a trainer") { g -> g.copy(people = g.people.map { if (it.uid == p.uid) it.copy(status = PersonStatus.ACTIVE) else it }) }
        act("${p.firstName} is now a trainer") { repo.setStatus(gymIdOrThrow, p.uid, PersonStatus.ACTIVE) }
    }
    fun reject(p: Person) {
        if (demo.value != null) return editDemo("${p.firstName} rejected") { g -> g.copy(people = g.people.map { if (it.uid == p.uid) it.copy(status = PersonStatus.REMOVED) else it }) }
        act { repo.setStatus(gymIdOrThrow, p.uid, PersonStatus.REMOVED) }
    }
    fun removeMember(p: Person) = act("${p.firstName} removed") { repo.setStatus(gymIdOrThrow, p.uid, PersonStatus.REMOVED) }

    /** Active members a trainer looks after. */
    fun membersOf(trainerUid: String): List<Person> = people.value.filter { it.role == Role.MEMBER && it.active && it.trainerUid == trainerUid }

    /** Owner: moves one member to another trainer. */
    fun changeTrainer(member: Person, trainer: Person) {
        val done = "${member.firstName} now trains with ${trainer.firstName}"
        if (demo.value != null) return editDemo(done) { g -> g.copy(people = g.people.map { if (it.uid == member.uid) it.copy(trainerUid = trainer.uid) else it }) }
        act(done) { repo.setTrainer(gymIdOrThrow, listOf(member.uid), trainer.uid) }
    }

    /**
     * Owner: takes a trainer or member out of the gym. A trainer's members move to [moveTo] (null leaves them
     * without a trainer until the owner picks one). Their past workouts stay in the records.
     */
    fun removeFromGym(p: Person, moveTo: Person?) {
        val theirs = if (p.role == Role.TRAINER) membersOf(p.uid).map { it.uid }.toSet() else emptySet()
        val done = "${p.firstName} removed from the gym"
        if (demo.value != null) return editDemo(done) { g ->
            g.copy(people = g.people.map {
                when {
                    it.uid == p.uid -> it.copy(status = PersonStatus.REMOVED)
                    it.uid in theirs -> it.copy(trainerUid = moveTo?.uid)
                    else -> it
                }
            })
        }
        act(done) { repo.removeFromGym(gymIdOrThrow, p.uid, theirs.toList(), moveTo?.uid) }
    }

    /** Your own display name, as everyone in the gym sees it. */
    fun renameMe(name: String) {
        val n = tidyName(name)
        if (n.isBlank()) return
        if (demo.value != null) { message.value = "Demo data is on, so this isn't saved"; return }
        val p = me.value ?: return
        launchAct("Name saved") { repo.setName(gymIdOrThrow, p.uid, n) }
    }

    /** Creates one assignment per member per day. */
    fun assign(title: String, members: Collection<String>, days: Collection<Long>, exercises: List<AssignedExercise>, onDone: () -> Unit) {
        val clean = exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = false) }) }
        val list = members.flatMap { m -> days.map { d -> Assignment("", uid, m, title.trim().ifBlank { "Workout" }, d, clean) } }
        val n = list.size
        act("Assigned $n workout${if (n == 1) "" else "s"}") { repo.assign(gymIdOrThrow, list); onDone() }
    }

    fun deleteAssignment(a: Assignment) = act("Workout removed") { repo.deleteAssignment(gymIdOrThrow, a.id) }
    fun verify(a: Assignment, verified: Boolean, note: String) = act(if (verified) "Verified ✓" else "Saved") {
        repo.verify(gymIdOrThrow, a.id, verified, note.trim())
    }

    fun saveTemplate(title: String, exercises: List<AssignedExercise>) = act("Saved as template") {
        repo.saveTemplate(gymIdOrThrow, Template("", uid, title.trim().ifBlank { "Workout" }, exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = false) }) }))
    }
    fun giveAward(title: String, emoji: String, person: Person, note: String, onDone: () -> Unit) {
        if (demo.value != null) {
            val a = GivenAward("demo-g${System.nanoTime()}", title.trim(), emoji, person.uid, note.trim(), month.toString(), System.currentTimeMillis())
            editDemo("$emoji ${title.trim()} given to ${person.firstName}") { g -> g.copy(given = g.given + a) }
            return onDone()
        }
        act("${emoji} ${title.trim()} given to ${person.firstName}") {
            repo.giveAward(gymIdOrThrow, GivenAward("", title.trim(), emoji, person.uid, note.trim(), month.toString(), System.currentTimeMillis()))
            onDone()
        }
    }
    fun editGivenAward(a: GivenAward, title: String, note: String) {
        val t = title.trim(); val n = note.trim()
        if (demo.value != null) return editDemo("Award updated") { g -> g.copy(given = g.given.map { if (it.id == a.id) it.copy(title = t, note = n) else it }) }
        act("Award updated") { repo.editGivenAward(gymIdOrThrow, a.id, t, n) }
    }
    fun removeGivenAward(a: GivenAward) {
        if (demo.value != null) return editDemo("Award removed") { g -> g.copy(given = g.given.filter { it.id != a.id }) }
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
        val firstCompletion = allDone && a.completedAt == null
        act { repo.saveProgress(gymIdOrThrow, updated) }
        if (firstCompletion) viewModelScope.launch {
            val restSec = c.prefs.training.value.restSec
            val sec = updated.exercises.sumOf { TrainingEngine.estimateSec(it.sets, restSec) }
            val kcal = updated.exercises.sumOf {
                Health.caloriesBurned(ExerciseRepo.get(it.exerciseId)?.met ?: 5f, c.prefs.profile.value.weightKg, TrainingEngine.estimateSec(it.sets, restSec))
            }
            c.dao.insertSession(WorkoutSession(workoutId = "gym:${a.id}", title = a.title, finishedAt = now, epochDay = today, durationSec = sec, calories = kcal))
        }
    }

    companion object {
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
