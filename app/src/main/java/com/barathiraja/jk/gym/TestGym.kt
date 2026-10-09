package com.barathiraja.jk.gym

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Test mode: a whole made-up gym kept in memory, so every screen can be checked with a realistic amount of data —
 * an owner, five trainers (plus one waiting and one removed), about forty members, last month and this month of
 * workouts (done, half done, missed, verified), next week's plan, monthly awards and hand-given awards.
 * Everything works (assign, log sets, verify, give awards, approve, remove, join with a code) but nothing leaves the
 * phone, and it starts fresh every time test mode is turned on. [viewAs] switches whose eyes the app sees it through.
 */
class TestGym(private val today: LocalDate = LocalDate.now()) : GymBackend {

    /** Who to look at the test gym as. */
    enum class Viewer(val label: String, val uid: String) {
        OWNER("Owner", OWNER_UID),
        TRAINER("Trainer", "t-arjun"),
        MEMBER("Member", "m-0"),
        WAITING("New trainer (waiting)", "t-sanjay"),
        NEWCOMER("Newcomer (no gym yet)", NEWCOMER_UID),
    }

    private val rnd = Random(42)
    private val gymState = MutableStateFlow(Gym(GYM_ID, "Iron Temple Fitness", OWNER_UID, "TESTGM"))
    private val peopleState = MutableStateFlow<Map<String, Person>>(emptyMap())
    private val assignmentsState = MutableStateFlow<Map<String, Assignment>>(emptyMap())
    private val templatesState = MutableStateFlow<Map<String, Template>>(emptyMap())
    private val awardsState = MutableStateFlow<Map<String, MonthAwards>>(emptyMap())
    private val givenState = MutableStateFlow<Map<String, GivenAward>>(emptyMap())
    /** Which gym each account belongs to (users/{uid}.gymId). */
    private val usersState = MutableStateFlow<Map<String, String?>>(emptyMap())
    /** code -> (type, trainerUid) */
    private val codes = mutableMapOf<String, Pair<String, String?>>("TESTGM" to ("gym" to null))
    private var nextId = 0
    private fun newId() = "x${nextId++}"

    val viewer = MutableStateFlow(Viewer.OWNER)

    /** The pretend signed-in account; follows [viewer]. */
    val account: Flow<Account?> = combine(viewer, peopleState) { v, people ->
        Account(v.uid, people[v.uid]?.name ?: if (v == Viewer.NEWCOMER) "Nila Newcomer" else v.label, null)
    }.distinctUntilChanged()

    fun viewAs(v: Viewer) { viewer.value = v }

    init { seed() }

    // ---------- reads ----------

    override fun userGymId(uid: String): Flow<String?> = usersState.map { it[uid] }.distinctUntilChanged()
    override fun gym(gymId: String): Flow<Gym?> = gymState.map { g -> g.takeIf { it.id == gymId } }
    override fun person(gymId: String, uid: String): Flow<Person?> = peopleState.map { it[uid] }.distinctUntilChanged()
    override fun people(gymId: String): Flow<List<Person>> = peopleState.map { it.values.toList() }
    override fun assignments(gymId: String, from: Long, to: Long): Flow<List<Assignment>> =
        assignmentsState.map { m -> m.values.filter { it.epochDay in from..to } }
    override fun assignmentsOf(gymId: String, field: String, uid: String, from: Long, to: Long): Flow<List<Assignment>> =
        assignments(gymId, from, to).map { l -> l.filter { (if (field == "memberUid") it.memberUid else it.trainerUid) == uid } }
    override fun templates(gymId: String, trainerUid: String): Flow<List<Template>> =
        templatesState.map { m -> m.values.filter { it.trainerUid == trainerUid } }
    override fun awards(gymId: String): Flow<List<MonthAwards>> = awardsState.map { it.values.toList() }
    override fun givenAwards(gymId: String): Flow<List<GivenAward>> = givenState.map { it.values.toList() }

    // ---------- joining ----------

    override suspend fun createGym(name: String, uid: String, displayName: String, photo: String?): String {
        throw GymException("Test mode has one gym already. Join it with code TESTGM (trainer) or a trainer's code (member).")
    }

    override suspend fun joinAsTrainer(gymCode: String, uid: String, displayName: String, photo: String?) {
        val c = codes[gymCode.trim().uppercase()]
        if (c?.first != "gym") throw GymException("No gym found with that code. In test mode the gym code is TESTGM.")
        val code = freeCode()
        codes[code] = "trainer" to uid
        peopleState.update { it + (uid to Person(uid, displayName, photo, Role.TRAINER, PersonStatus.PENDING, trainerCode = code, joinedAt = now())) }
        usersState.update { it + (uid to GYM_ID) }
    }

    override suspend fun joinAsMember(trainerCode: String, uid: String, displayName: String, photo: String?) {
        val c = codes[trainerCode.trim().uppercase()]
        if (c?.first != "trainer") throw GymException("No trainer found with that code. Ask your trainer for it again.")
        val trainer = peopleState.value[c.second]
        if (trainer?.active != true) throw GymException("Couldn't join with this code. The trainer may not be approved yet, or you're already in this gym.")
        peopleState.update { it + (uid to Person(uid, displayName, photo, Role.MEMBER, PersonStatus.ACTIVE, trainerUid = trainer.uid, joinedAt = now())) }
        usersState.update { it + (uid to GYM_ID) }
    }

    override suspend fun setName(gymId: String, uid: String, name: String) = editPerson(uid) { it.copy(name = name) }
    override suspend fun setStatus(gymId: String, uid: String, status: PersonStatus) = editPerson(uid) { it.copy(status = status) }

    override suspend fun setTrainer(gymId: String, memberUids: List<String>, trainerUid: String?) {
        memberUids.forEach { editPerson(it) { p -> p.copy(trainerUid = trainerUid) } }
    }

    override suspend fun removeFromGym(gymId: String, uid: String, members: List<String>, moveTo: String?) {
        members.forEach { editPerson(it) { p -> p.copy(trainerUid = moveTo) } }
        editPerson(uid) { it.copy(status = PersonStatus.REMOVED) }
    }

    override suspend fun leave(gymId: String, uid: String) {
        usersState.update { it + (uid to null) }
        editPerson(uid) { it.copy(status = PersonStatus.REMOVED) }
    }

    override suspend fun newTrainerCode(gymId: String, uid: String, oldCode: String?): String {
        val code = freeCode()
        oldCode?.let { codes.remove(it) }
        codes[code] = "trainer" to uid
        editPerson(uid) { it.copy(trainerCode = code) }
        return code
    }

    override suspend fun deleteAccount(gymId: String?, uid: String) {
        usersState.update { it + (uid to null) }
        editPerson(uid) { it.copy(name = "Former member", photoUrl = null, status = PersonStatus.REMOVED) }
    }

    override suspend fun loadBackup(uid: String): Map<String, String>? = null
    override suspend fun saveBackup(uid: String, prefs: Map<String, String>) {}

    // ---------- workouts and awards ----------

    override suspend fun assign(gymId: String, list: List<Assignment>): Int {
        assignmentsState.update { m -> m + list.map { a -> newId().let { it to a.copy(id = it) } } }
        return list.size
    }

    override suspend fun saveProgress(gymId: String, a: Assignment) = editAssignment(a.id) {
        it.copy(exercises = a.exercises, status = a.status, startedAt = a.startedAt, completedAt = a.completedAt, memberNote = a.memberNote)
    }

    override suspend fun verify(gymId: String, id: String, verified: Boolean, note: String) =
        editAssignment(id) { it.copy(verified = verified, trainerNote = note) }

    override suspend fun deleteAssignment(gymId: String, id: String) { assignmentsState.update { it - id } }

    override suspend fun saveTemplate(gymId: String, t: Template) {
        val id = t.id.ifBlank { newId() }
        templatesState.update { it + (id to t.copy(id = id)) }
    }

    override suspend fun deleteTemplate(gymId: String, id: String) { templatesState.update { it - id } }

    override suspend fun saveAwards(gymId: String, month: String, winners: Map<Award, String>) {
        awardsState.update { if (month in it) it else it + (month to MonthAwards(month, winners)) }
    }

    override suspend fun fetchPeople(gymId: String): List<Person> = peopleState.value.values.toList()
    override suspend fun fetchAssignments(gymId: String, from: Long, to: Long): List<Assignment> =
        assignmentsState.value.values.filter { it.epochDay in from..to }

    override suspend fun giveAward(gymId: String, a: GivenAward) {
        val id = newId()
        givenState.update { it + (id to a.copy(id = id)) }
    }

    override suspend fun editGivenAward(gymId: String, id: String, title: String, note: String) {
        givenState.update { m -> m[id]?.let { m + (id to it.copy(title = title, note = note)) } ?: m }
    }

    override suspend fun deleteGivenAward(gymId: String, id: String) { givenState.update { it - id } }
    override suspend fun renameGym(gymId: String, name: String) { gymState.update { it.copy(name = name) } }

    // ---------- helpers ----------

    private fun editPerson(uid: String, f: (Person) -> Person) {
        peopleState.update { m -> m[uid]?.let { m + (uid to f(it)) } ?: m }
    }

    private fun editAssignment(id: String, f: (Assignment) -> Assignment) {
        assignmentsState.update { m -> m[id]?.let { m + (id to f(it)) } ?: m }
    }

    private fun freeCode(): String {
        while (true) {
            val code = (1..6).map { CODE_CHARS[rnd.nextInt(CODE_CHARS.length)] }.joinToString("")
            if (code !in codes) return code
        }
    }

    private fun now() = System.currentTimeMillis()
    private fun millisOf(day: LocalDate, hour: Int, minute: Int = 0) =
        day.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ---------- the made-up gym ----------

    /** A trainer: how many of their members' finished workouts they check, and how many members they have. */
    private class Coach(val uid: String, val name: String, val code: String, val checks: Double, val members: Int)

    private fun seed() {
        val coaches = listOf(
            Coach("t-arjun", "Arjun Mehta", "ARJUN7", 0.85, 10),
            Coach("t-priya", "Priya Nair", "PRIYA4", 0.7, 9),
            Coach("t-karthik", "Karthik Raja", "KARTK8", 0.5, 8),
            Coach("t-divya", "Divya Shankar", "DIVYA3", 0.6, 6),
            Coach("t-vikram", "Vikram Singh", "VIKRM5", 0.2, 3),
        )
        val people = mutableMapOf<String, Person>()
        val users = mutableMapOf<String, String?>()
        fun add(p: Person) { people[p.uid] = p; users[p.uid] = GYM_ID }

        val start = today.minusMonths(1).withDayOfMonth(1)
        add(Person(OWNER_UID, "Ravi Shankar", null, Role.OWNER, PersonStatus.ACTIVE, joinedAt = millisOf(start.minusMonths(6), 9)))
        coaches.forEachIndexed { i, c ->
            codes[c.code] = "trainer" to c.uid
            add(Person(c.uid, c.name, null, Role.TRAINER, PersonStatus.ACTIVE, trainerCode = c.code, joinedAt = millisOf(start.minusMonths(5L - i), 10)))
        }
        codes["SANJY2"] = "trainer" to "t-sanjay"
        add(Person("t-sanjay", "Sanjay Murthy", null, Role.TRAINER, PersonStatus.PENDING, trainerCode = "SANJY2", joinedAt = millisOf(today, 8)))
        codes["RAHUL6"] = "trainer" to "t-rahul"
        add(Person("t-rahul", "Rahul Verma", null, Role.TRAINER, PersonStatus.REMOVED, trainerCode = "RAHUL6", joinedAt = millisOf(start.minusMonths(4), 10)))

        // Members in coach order; the last two lost their trainer when Rahul was removed.
        val owners = coaches.flatMap { c -> List(c.members) { c.uid } } + listOf<String?>(null, null)
        val members = MEMBER_NAMES.take(owners.size).mapIndexed { i, name ->
            val joined = when {
                i == 9 || i == 18 -> today.minusDays(1) // brand new: nothing assigned yet
                else -> start.minusDays(rnd.nextLong(0, 120))
            }
            Person("m-$i", name, null, Role.MEMBER, PersonStatus.ACTIVE, trainerUid = owners[i], joinedAt = millisOf(joined, 18))
        }
        members.forEach(::add)
        add(Person("m-gone", "Kiran Kumar", null, Role.MEMBER, PersonStatus.REMOVED, trainerUid = "t-priya", joinedAt = millisOf(start.minusMonths(2), 18)))
        users[NEWCOMER_UID] = null

        // Workouts from the first of last month to a week ahead, Monday to Saturday, following the trainer's plan.
        val plan = PlanLibrary.pushPullLegs
        val assignments = mutableMapOf<String, Assignment>()
        members.forEachIndexed { i, m ->
            if (i == 9 || i == 18) return@forEachIndexed
            val coach = coaches.firstOrNull { it.uid == m.trainerUid }
            // Member 0 is the one you see in Member view: keen, with today's workout still to do.
            val keen = if (i == 0) 0.85 else listOf(0.95, 0.85, 0.7, 0.55, 0.35, 0.15)[i % 6] * (0.85 + rnd.nextDouble() * 0.3)
            val strength = 0.7f + rnd.nextFloat() * 0.8f
            val daysPerWeek = listOf(6, 5, 4, 3)[i % 4]
            val trainerUid = m.trainerUid ?: "t-rahul"
            var planIndex = i % plan.size
            var day = start
            // Members without a trainer stopped getting workouts two weeks ago.
            val last = if (m.trainerUid == null) today.minusDays(14) else today.plusDays(6)
            while (!day.isAfter(last)) {
                val dow = day.dayOfWeek
                val trains = dow != DayOfWeek.SUNDAY && (dow.value - 1) < daysPerWeek
                if (trains) {
                    val pd = plan[planIndex++ % plan.size]
                    val weeks = (day.toEpochDay() - start.toEpochDay()) / 7
                    val past = day.isBefore(today)
                    val isToday = day == today
                    val roll = rnd.nextDouble()
                    val outcome = when {
                        isToday && i == 0 -> 0 // still to do
                        isToday -> if (roll < keen * 0.6) 2 else if (roll < keen * 0.6 + 0.15) 1 else 0
                        past -> if (roll < keen) 2 else if (roll < keen + 0.08) 1 else 0
                        else -> 0
                    }
                    val exercises = pd.exercises.map { e ->
                        val base = baseKg(e.bodyPart) * strength + weeks * 1.25f
                        val kg = (Math.round(base / 2.5f) * 2.5f).coerceAtLeast(2.5f)
                        e.copy(sets = e.sets.mapIndexed { si, s -> s.copy(weightKg = kg, done = false).let { it.copy(reps = it.reps - (si / 2)) } })
                    }
                    val done = when (outcome) {
                        2 -> exercises.map { e -> e.copy(sets = e.sets.map { it.copy(done = true) }) }
                        1 -> exercises.mapIndexed { ei, e -> e.copy(sets = e.sets.map { it.copy(done = ei < exercises.size / 2) }) }
                        else -> exercises
                    }
                    val startAt = millisOf(day, 6 + rnd.nextInt(14), rnd.nextInt(60))
                    val id = newId()
                    val finished = outcome == 2
                    assignments[id] = Assignment(
                        id = id, trainerUid = trainerUid, memberUid = m.uid, title = pd.title, epochDay = day.toEpochDay(),
                        exercises = done,
                        status = when (outcome) { 2 -> AssignStatus.DONE; 1 -> AssignStatus.IN_PROGRESS; else -> AssignStatus.ASSIGNED },
                        startedAt = startAt.takeIf { outcome > 0 }, completedAt = (startAt + 55 * 60_000L).takeIf { finished },
                        verified = finished && past && rnd.nextDouble() < (coach?.checks ?: 0.0),
                        trainerNote = when {
                            finished && past && rnd.nextDouble() < 0.12 -> TRAINER_NOTES[rnd.nextInt(TRAINER_NOTES.size)]
                            !past && rnd.nextDouble() < 0.2 -> pd.note
                            else -> ""
                        },
                        memberNote = if (finished && rnd.nextDouble() < 0.1) MEMBER_NOTES[rnd.nextInt(MEMBER_NOTES.size)] else "",
                    )
                }
                day = day.plusDays(1)
            }
        }

        // Saved templates for the trainer you see in Trainer view.
        val templates = plan.take(3).associate { pd ->
            val id = newId()
            id to Template(id, "t-arjun", "${pd.title} (my version)", pd.exercises)
        }

        // Last month's awards, worked out the same way the owner's phone does; the month before, a fixed set.
        val lastMonth = YearMonth.from(today).minusMonths(1)
        val twoAgo = lastMonth.minusMonths(1)
        val awards = mutableMapOf(
            twoAgo.toString() to MonthAwards(twoAgo.toString(), mapOf(
                Award.BEST_MEMBER to "m-1", Award.BEST_TRAINER to "t-priya", Award.MOST_CONSISTENT to "m-12",
                Award.MOST_IMPROVED to "m-21", Award.IRON_LIFTER to "m-6",
            )),
        )
        val computed = Scoring.awards(people.values.toList(), assignments.values.toList(), lastMonth)
        if (computed.isNotEmpty()) awards[lastMonth.toString()] = MonthAwards(lastMonth.toString(), computed)

        val given = listOf(
            GivenAward("", "Comeback of the month", "🚀", "m-4", "Free protein shake at the counter", lastMonth.toString(), millisOf(start.plusDays(28), 19)),
            GivenAward("", "Coach's favourite", "⭐", "m-0", "", lastMonth.toString(), millisOf(start.plusDays(29), 19)),
            GivenAward("", "Best attitude", "😊", "t-divya", "One month free membership", twoAgo.toString(), millisOf(start.minusDays(2), 19)),
            GivenAward("", "Early bird", "🌅", "m-2", "Gym T-shirt", YearMonth.from(today).toString(), millisOf(today, 7)),
        ).associate { a -> newId().let { it to a.copy(id = it) } }

        peopleState.value = people
        usersState.value = users
        assignmentsState.value = assignments
        templatesState.value = templates
        awardsState.value = awards
        givenState.value = given
    }

    private fun baseKg(bodyPart: String): Float = when (bodyPart) {
        "LEGS" -> 60f; "BACK" -> 40f; "CHEST" -> 30f; "SHOULDER" -> 15f; "BICEPS", "TRICEPS" -> 12f; else -> 10f
    }

    companion object {
        const val GYM_ID = "test-gym"
        const val OWNER_UID = "t-owner"
        const val NEWCOMER_UID = "u-newcomer"
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        private val MEMBER_NAMES = listOf(
            "Meera Iyer", "Rohan Das", "Ananya Gupta", "Aditya Rao", "Kavya Reddy", "Siddharth Menon", "Nisha Patel",
            "Harish Kumar", "Lakshmi Narayanan", "Farhan Sheikh", "Sneha Joshi", "Deepak Pillai", "Pooja Bhat",
            "Naveen Krishnan", "Swathi Ramesh", "Gokul Prasad", "Ishita Sharma", "Manoj Varma", "Revathi S.",
            "Tarun Bose", "Aarav Kapoor", "Divya Lakshmi", "Ganesh Moorthy", "Yamini Rao", "Imran Khan",
            "Keerthana Murali", "Venkatanarayanan Subramaniam Iyer", "Abhishek Jain", "Bhavana Reddy", "Suresh Babu",
            "Nandini Ravi", "Anjali M.", "Prakash Shetty", "Ritu Saxena", "Zoya Ali", "Mohan Raj", "Asha Thomas",
            "Joel D'Souza",
        )
        private val TRAINER_NOTES = listOf(
            "Great form today, keep the elbows tucked.", "Add 2.5 kg next week.", "Slow down the lowering part.",
            "Nice work, you hit every rep!", "Rest a little longer between sets.",
        )
        private val MEMBER_NOTES = listOf(
            "Shoulder felt a bit tight.", "Felt strong today!", "Gym was crowded, swapped the machine.", "Short on time, rushed the last set.",
        )
    }
}
