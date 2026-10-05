package com.barathiraja.jk.gym

import com.barathiraja.jk.data.SetSpec
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await

/**
 * Firestore layout (see firestore.rules):
 *   users/{uid}                    { gymId }
 *   codes/{CODE}                   { gymId, type: gym|trainer, trainerUid? }
 *   gyms/{gymId}                   { name, ownerUid, gymCode }
 *   gyms/{gymId}/people/{uid}      Person
 *   gyms/{gymId}/assignments/{id}  Assignment
 *   gyms/{gymId}/templates/{id}    Template
 *   gyms/{gymId}/awards/{yyyy-MM}  { winners: {AWARD: uid} }
 *   gyms/{gymId}/given/{id}        GivenAward (owner hands these out)
 */
class GymRepo {
    private val db = FirebaseFirestore.getInstance()
    private fun gymDoc(gymId: String) = db.collection("gyms").document(gymId)
    private fun peopleCol(gymId: String) = gymDoc(gymId).collection("people")
    private fun assignmentsCol(gymId: String) = gymDoc(gymId).collection("assignments")
    private fun templatesCol(gymId: String) = gymDoc(gymId).collection("templates")
    private fun awardsCol(gymId: String) = gymDoc(gymId).collection("awards")
    private fun givenCol(gymId: String) = gymDoc(gymId).collection("given")

    // ---------- live reads ----------

    fun userGymId(uid: String): Flow<String?> = db.collection("users").document(uid).listen { it.getString("gymId") }

    fun gym(gymId: String): Flow<Gym?> = gymDoc(gymId).listen { d ->
        Gym(d.id, d.getString("name").orEmpty(), d.getString("ownerUid").orEmpty(), d.getString("gymCode").orEmpty())
    }

    /** Your own record; readable even while a trainer is still waiting for approval. */
    fun person(gymId: String, uid: String): Flow<Person?> = peopleCol(gymId).document(uid).listen(::toPerson)

    fun people(gymId: String): Flow<List<Person>> = peopleCol(gymId).listenAll(::toPerson)

    /** All assignments in the gym between two days (inclusive); one range filter needs no composite index. */
    fun assignments(gymId: String, from: Long, to: Long): Flow<List<Assignment>> =
        assignmentsCol(gymId).whereGreaterThanOrEqualTo("epochDay", from).whereLessThanOrEqualTo("epochDay", to).listenAll(::toAssignment)

    fun templates(gymId: String, trainerUid: String): Flow<List<Template>> =
        templatesCol(gymId).whereEqualTo("trainerUid", trainerUid).listenAll { d ->
            Template(d.id, d.getString("trainerUid").orEmpty(), d.getString("title").orEmpty(), exercisesOf(d))
        }

    fun awards(gymId: String): Flow<List<MonthAwards>> = awardsCol(gymId).listenAll { d ->
        @Suppress("UNCHECKED_CAST")
        val w = (d.get("winners") as? Map<String, String>).orEmpty()
        MonthAwards(d.id, w.mapNotNull { (k, v) -> Award.entries.firstOrNull { it.name == k }?.let { it to v } }.toMap())
    }

    fun givenAwards(gymId: String): Flow<List<GivenAward>> = givenCol(gymId).listenAll { d ->
        GivenAward(d.id, d.getString("title").orEmpty(), d.getString("emoji").orEmpty(), d.getString("uid").orEmpty(),
            d.getString("note").orEmpty(), d.getString("month").orEmpty(), d.getLong("givenAt") ?: 0)
    }

    // ---------- joining ----------

    suspend fun createGym(name: String, uid: String, displayName: String, photo: String?): String {
        val gym = db.collection("gyms").document()
        val code = freeCode()
        db.batch()
            .set(gym, mapOf("name" to name, "ownerUid" to uid, "gymCode" to code, "createdAt" to System.currentTimeMillis()))
            .set(peopleCol(gym.id).document(uid), personMap(displayName, photo, Role.OWNER, PersonStatus.ACTIVE))
            .set(db.collection("codes").document(code), mapOf("gymId" to gym.id, "type" to "gym"))
            .set(db.collection("users").document(uid), mapOf("gymId" to gym.id), SetOptions.merge())
            .commit().await()
        return gym.id
    }

    /** Trainers join with the gym code and wait for the owner; their member code is created now so it never changes. */
    suspend fun joinAsTrainer(gymCode: String, uid: String, displayName: String, photo: String?) {
        val c = lookup(gymCode, "gym")
        val gymId = c.getString("gymId")!!
        val trainerCode = freeCode()
        db.batch()
            .set(peopleCol(gymId).document(uid), personMap(displayName, photo, Role.TRAINER, PersonStatus.PENDING) + ("trainerCode" to trainerCode))
            .set(db.collection("codes").document(trainerCode), mapOf("gymId" to gymId, "type" to "trainer", "trainerUid" to uid))
            .set(db.collection("users").document(uid), mapOf("gymId" to gymId), SetOptions.merge())
            .commit().await()
    }

    suspend fun joinAsMember(trainerCode: String, uid: String, displayName: String, photo: String?) {
        val c = lookup(trainerCode, "trainer")
        val gymId = c.getString("gymId")!!
        val trainerUid = c.getString("trainerUid")!!
        try {
            db.batch()
                .set(peopleCol(gymId).document(uid), personMap(displayName, photo, Role.MEMBER, PersonStatus.ACTIVE) + ("trainerUid" to trainerUid))
                .set(db.collection("users").document(uid), mapOf("gymId" to gymId), SetOptions.merge())
                .commit().await()
        } catch (e: FirebaseFirestoreException) {
            // The rules only allow joining trainers the owner has approved.
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) throw GymException("This trainer hasn't been approved by the gym owner yet.")
            throw e
        }
    }

    suspend fun setName(gymId: String, uid: String, name: String) {
        peopleCol(gymId).document(uid).update("name", name).await()
    }

    suspend fun setStatus(gymId: String, uid: String, status: PersonStatus) {
        peopleCol(gymId).document(uid).update("status", status.name.lowercase()).await()
    }

    /** Owner: moves members to another trainer ([trainerUid] null = no trainer for now). */
    suspend fun setTrainer(gymId: String, memberUids: List<String>, trainerUid: String?) {
        if (memberUids.isEmpty()) return
        val batch = db.batch()
        memberUids.forEach { batch.update(peopleCol(gymId).document(it), "trainerUid", trainerUid) }
        batch.commit().await()
    }

    /** Owner: takes someone out of the gym; a trainer's members move to [moveTo] in the same write. */
    suspend fun removeFromGym(gymId: String, uid: String, members: List<String>, moveTo: String?) {
        val batch = db.batch()
        members.forEach { batch.update(peopleCol(gymId).document(it), "trainerUid", moveTo) }
        batch.update(peopleCol(gymId).document(uid), "status", PersonStatus.REMOVED.name.lowercase())
        batch.commit().await()
    }

    /** Leaving only unlinks the account from the gym; the gym keeps the history and the user keeps their backup. */
    suspend fun leave(uid: String) {
        db.collection("users").document(uid).update("gymId", FieldValue.delete()).await()
    }

    /** Private per-account backup of profile and settings (users/{uid}.prefs), so signing in again restores them. */
    suspend fun loadBackup(uid: String): Map<String, String>? {
        @Suppress("UNCHECKED_CAST")
        return db.collection("users").document(uid).get().await().get("prefs") as? Map<String, String>
    }

    suspend fun saveBackup(uid: String, prefs: Map<String, String>) {
        db.collection("users").document(uid).set(mapOf("prefs" to prefs), SetOptions.merge()).await()
    }

    // ---------- workouts ----------

    suspend fun assign(gymId: String, list: List<Assignment>) {
        list.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(assignmentsCol(gymId).document(), assignmentMap(it)) }
            batch.commit().await()
        }
    }

    /** Member progress: sets ticked, status and timestamps. */
    suspend fun saveProgress(gymId: String, a: Assignment) {
        assignmentsCol(gymId).document(a.id).update(
            mapOf(
                "exercises" to a.exercises.map(::exerciseMap),
                "status" to a.status.name.lowercase(),
                "startedAt" to a.startedAt,
                "completedAt" to a.completedAt,
                "memberNote" to a.memberNote,
            )
        ).await()
    }

    suspend fun verify(gymId: String, id: String, verified: Boolean, note: String) {
        assignmentsCol(gymId).document(id).update(mapOf("verified" to verified, "trainerNote" to note)).await()
    }

    suspend fun deleteAssignment(gymId: String, id: String) {
        assignmentsCol(gymId).document(id).delete().await()
    }

    suspend fun saveTemplate(gymId: String, t: Template) {
        val doc = if (t.id.isBlank()) templatesCol(gymId).document() else templatesCol(gymId).document(t.id)
        doc.set(mapOf("trainerUid" to t.trainerUid, "title" to t.title, "exercises" to t.exercises.map(::exerciseMap))).await()
    }

    suspend fun deleteTemplate(gymId: String, id: String) {
        templatesCol(gymId).document(id).delete().await()
    }

    /** Monthly awards are written once; later attempts are refused by the rules and ignored. */
    suspend fun saveAwards(gymId: String, month: String, winners: Map<Award, String>) {
        runCatching {
            db.runTransaction { tx ->
                val ref = awardsCol(gymId).document(month)
                if (!tx.get(ref).exists()) tx.set(ref, mapOf("winners" to winners.mapKeys { it.key.name }, "savedAt" to System.currentTimeMillis()))
            }.await()
        }
    }

    suspend fun giveAward(gymId: String, a: GivenAward) {
        givenCol(gymId).add(mapOf("title" to a.title, "emoji" to a.emoji, "uid" to a.uid, "note" to a.note,
            "month" to a.month, "givenAt" to a.givenAt)).await()
    }

    suspend fun editGivenAward(gymId: String, id: String, title: String, note: String) {
        givenCol(gymId).document(id).update(mapOf("title" to title, "note" to note)).await()
    }

    suspend fun deleteGivenAward(gymId: String, id: String) {
        givenCol(gymId).document(id).delete().await()
    }

    suspend fun renameGym(gymId: String, name: String) {
        gymDoc(gymId).update("name", name).await()
    }

    // ---------- helpers ----------

    private suspend fun lookup(code: String, type: String): DocumentSnapshot {
        val c = db.collection("codes").document(code.trim().uppercase()).get().await()
        if (!c.exists() || c.getString("type") != type) {
            throw GymException(if (type == "gym") "No gym found with that code. Check it with the gym owner." else "No trainer found with that code. Ask your trainer for it again.")
        }
        return c
    }

    private suspend fun freeCode(): String {
        repeat(10) {
            val code = (1..6).map { CODE_CHARS.random() }.joinToString("")
            if (!db.collection("codes").document(code).get().await().exists()) return code
        }
        throw GymException("Couldn't create a code, please try again")
    }

    private fun personMap(name: String, photo: String?, role: Role, status: PersonStatus) = mapOf(
        "name" to name, "photoUrl" to photo, "role" to role.name.lowercase(), "status" to status.name.lowercase(),
        "joinedAt" to System.currentTimeMillis(),
    )

    private fun exerciseMap(e: AssignedExercise) = mapOf(
        "exerciseId" to e.exerciseId, "bodyPart" to e.bodyPart,
        "sets" to e.sets.map { mapOf("reps" to it.reps, "weightKg" to it.weightKg.toDouble(), "seconds" to it.seconds, "done" to it.done) },
    )

    private fun assignmentMap(a: Assignment) = mapOf(
        "trainerUid" to a.trainerUid, "memberUid" to a.memberUid, "title" to a.title, "epochDay" to a.epochDay,
        "exercises" to a.exercises.map(::exerciseMap), "status" to a.status.name.lowercase(),
        "startedAt" to a.startedAt, "completedAt" to a.completedAt, "verified" to a.verified,
        "trainerNote" to a.trainerNote, "memberNote" to a.memberNote, "createdAt" to System.currentTimeMillis(),
    )

    private fun toPerson(d: DocumentSnapshot) = Person(
        uid = d.id, name = d.getString("name").orEmpty(), photoUrl = d.getString("photoUrl"),
        role = enumOf(d.getString("role"), Role.MEMBER), status = enumOf(d.getString("status"), PersonStatus.PENDING),
        trainerUid = d.getString("trainerUid"), trainerCode = d.getString("trainerCode"), joinedAt = d.getLong("joinedAt") ?: 0,
    )

    private fun toAssignment(d: DocumentSnapshot) = Assignment(
        id = d.id, trainerUid = d.getString("trainerUid").orEmpty(), memberUid = d.getString("memberUid").orEmpty(),
        title = d.getString("title").orEmpty(), epochDay = d.getLong("epochDay") ?: 0, exercises = exercisesOf(d),
        status = enumOf(d.getString("status"), AssignStatus.ASSIGNED), startedAt = d.getLong("startedAt"),
        completedAt = d.getLong("completedAt"), verified = d.getBoolean("verified") ?: false,
        trainerNote = d.getString("trainerNote").orEmpty(), memberNote = d.getString("memberNote").orEmpty(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun exercisesOf(d: DocumentSnapshot): List<AssignedExercise> =
        (d.get("exercises") as? List<Map<String, Any?>>).orEmpty().map { e ->
            AssignedExercise(
                e["exerciseId"] as? String ?: "", e["bodyPart"] as? String ?: "",
                (e["sets"] as? List<Map<String, Any?>>).orEmpty().map { s ->
                    SetSpec((s["reps"] as? Number)?.toInt() ?: 0, (s["weightKg"] as? Number)?.toFloat() ?: 0f,
                        (s["seconds"] as? Number)?.toInt() ?: 0, s["done"] as? Boolean ?: false)
                },
            )
        }

    private inline fun <reified E : Enum<E>> enumOf(s: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name.equals(s, ignoreCase = true) } ?: default

    private fun <T> com.google.firebase.firestore.DocumentReference.listen(map: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
        // INCLUDE so we also hear the server's confirmation, which changes only metadata.
        val reg = addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) { trySend(null); return@addSnapshotListener }
            // Wait for the server to confirm local writes: rules only see committed data, so acting on an
            // unconfirmed "you're in the gym" would start gym listeners that the server then refuses.
            if (snap?.metadata?.hasPendingWrites() == true) return@addSnapshotListener
            trySend(snap?.takeIf { it.exists() }?.let(map))
        }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    /** A refused listener is dead in Firestore, so retry a few times (e.g. right after joining or approval). */
    private fun <T> Query.listenAll(map: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val reg = addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.documents?.map(map).orEmpty())
        }
        awaitClose { reg.remove() }
    }.retryWhen { _, attempt ->
        if (attempt < 6) { delay(1000L * (attempt + 1)); true } else false
    }.catch { emit(emptyList()) }

    private companion object {
        const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O, 1/I
    }
}

class GymException(message: String) : Exception(message)
