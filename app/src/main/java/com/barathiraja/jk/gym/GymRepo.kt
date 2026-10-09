package com.barathiraja.jk.gym

import android.util.Log
import com.barathiraja.jk.data.SetSpec
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

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
class GymRepo : GymBackend {
    private val db = FirebaseFirestore.getInstance()
    private fun gymDoc(gymId: String) = db.collection("gyms").document(gymId)
    private fun peopleCol(gymId: String) = gymDoc(gymId).collection("people")
    private fun assignmentsCol(gymId: String) = gymDoc(gymId).collection("assignments")
    private fun templatesCol(gymId: String) = gymDoc(gymId).collection("templates")
    private fun awardsCol(gymId: String) = gymDoc(gymId).collection("awards")
    private fun givenCol(gymId: String) = gymDoc(gymId).collection("given")

    // ---------- live reads ----------

    override fun userGymId(uid: String): Flow<String?> = db.collection("users").document(uid).listen { it.getString("gymId") }

    override fun gym(gymId: String): Flow<Gym?> = gymDoc(gymId).listen { d ->
        Gym(d.id, d.getString("name").orEmpty(), d.getString("ownerUid").orEmpty(), d.getString("gymCode").orEmpty())
    }

    /** Your own record; readable even while a trainer is still waiting for approval. */
    override fun person(gymId: String, uid: String): Flow<Person?> = peopleCol(gymId).document(uid).listen(::toPerson)

    override fun people(gymId: String): Flow<List<Person>> = peopleCol(gymId).listenAll(::toPerson)

    /** All assignments in the gym between two days (inclusive); one range filter needs no composite index. */
    override fun assignments(gymId: String, from: Long, to: Long): Flow<List<Assignment>> =
        assignmentsCol(gymId).whereGreaterThanOrEqualTo("epochDay", from).whereLessThanOrEqualTo("epochDay", to).listenAll(::toAssignment)

    /**
     * One person's assignments (a member's own, or a trainer's members'), so a phone reads only what it shows rather
     * than the whole gym. Needs a composite index on [field] + epochDay (see firestore.indexes.json); until that index
     * exists Firestore refuses the query, and this falls back to the gym-wide range filtered on the phone.
     */
    override fun assignmentsOf(gymId: String, field: String, uid: String, from: Long, to: Long): Flow<List<Assignment>> {
        val inRange = { q: Query -> q.whereGreaterThanOrEqualTo("epochDay", from).whereLessThanOrEqualTo("epochDay", to) }
        return inRange(assignmentsCol(gymId).whereEqualTo(field, uid)).snapshots(::toAssignment)
            .catch { e ->
                if (e !is FirebaseFirestoreException || e.code != FirebaseFirestoreException.Code.FAILED_PRECONDITION) throw e
                Log.e("GymRepo", "Missing Firestore index for assignments by $field; reading the whole gym instead", e)
                emitAll(inRange(assignmentsCol(gymId)).snapshots(::toAssignment)
                    .map { l -> l.filter { (if (field == "memberUid") it.memberUid else it.trainerUid) == uid } })
            }
            .retryWithBackoff()
    }

    override fun templates(gymId: String, trainerUid: String): Flow<List<Template>> =
        templatesCol(gymId).whereEqualTo("trainerUid", trainerUid).listenAll { d ->
            Template(d.id, d.getString("trainerUid").orEmpty(), d.getString("title").orEmpty(), exercisesOf(d))
        }

    override fun awards(gymId: String): Flow<List<MonthAwards>> = awardsCol(gymId).listenAll { d ->
        @Suppress("UNCHECKED_CAST")
        val w = (d.get("winners") as? Map<String, String>).orEmpty()
        MonthAwards(d.id, w.mapNotNull { (k, v) -> Award.entries.firstOrNull { it.name == k }?.let { it to v } }.toMap())
    }

    override fun givenAwards(gymId: String): Flow<List<GivenAward>> = givenCol(gymId).listenAll { d ->
        GivenAward(d.id, d.getString("title").orEmpty(), d.getString("emoji").orEmpty(), d.getString("uid").orEmpty(),
            d.getString("note").orEmpty(), d.getString("month").orEmpty(), d.getLong("givenAt") ?: 0)
    }

    // ---------- joining ----------

    override suspend fun createGym(name: String, uid: String, displayName: String, photo: String?): String {
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
    override suspend fun joinAsTrainer(gymCode: String, uid: String, displayName: String, photo: String?) {
        val c = lookup(gymCode, "gym")
        val gymId = c.getString("gymId")!!
        val trainerCode = freeCode()
        joinBatch {
            db.batch()
                // joinCode (the gym code) lets the rules check they were invited.
                .set(peopleCol(gymId).document(uid), personMap(displayName, photo, Role.TRAINER, PersonStatus.PENDING) +
                    mapOf("trainerCode" to trainerCode, "joinCode" to c.id))
                .set(db.collection("codes").document(trainerCode), mapOf("gymId" to gymId, "type" to "trainer", "trainerUid" to uid))
                .set(db.collection("users").document(uid), mapOf("gymId" to gymId), SetOptions.merge())
        }
    }

    override suspend fun joinAsMember(trainerCode: String, uid: String, displayName: String, photo: String?) {
        val c = lookup(trainerCode, "trainer")
        val gymId = c.getString("gymId")!!
        val trainerUid = c.getString("trainerUid")!!
        joinBatch {
            // joinCode lets the rules check the member really had the code (codes can't be listed).
            db.batch()
                .set(peopleCol(gymId).document(uid),
                    personMap(displayName, photo, Role.MEMBER, PersonStatus.ACTIVE) + mapOf("trainerUid" to trainerUid, "joinCode" to c.id))
                .set(db.collection("users").document(uid), mapOf("gymId" to gymId), SetOptions.merge())
        }
    }

    /** Commits a join; the rules refuse joining an unapproved trainer or coming back while not removed. */
    private suspend fun joinBatch(build: () -> com.google.firebase.firestore.WriteBatch) {
        try {
            build().commit().await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw GymException("Couldn't join with this code. The trainer may not be approved yet, or you're already in this gym.")
            }
            throw e
        }
    }

    override suspend fun setName(gymId: String, uid: String, name: String) {
        peopleCol(gymId).document(uid).update("name", name.take(MAX_NAME)).await()
    }

    override suspend fun setStatus(gymId: String, uid: String, status: PersonStatus) {
        peopleCol(gymId).document(uid).update("status", status.name.lowercase()).await()
    }

    /** Owner: moves members to another trainer ([trainerUid] null = no trainer for now). */
    override suspend fun setTrainer(gymId: String, memberUids: List<String>, trainerUid: String?) {
        if (memberUids.isEmpty()) return
        val batch = db.batch()
        memberUids.forEach { batch.update(peopleCol(gymId).document(it), "trainerUid", trainerUid) }
        batch.commit().await()
    }

    /** Owner: takes someone out of the gym; a trainer's members move to [moveTo] in the same write. */
    override suspend fun removeFromGym(gymId: String, uid: String, members: List<String>, moveTo: String?) {
        val batch = db.batch()
        members.forEach { batch.update(peopleCol(gymId).document(it), "trainerUid", moveTo) }
        batch.update(peopleCol(gymId).document(uid), "status", PersonStatus.REMOVED.name.lowercase())
        batch.commit().await()
    }

    /**
     * Leaving unlinks the account and marks them removed, so they drop out of rankings and lose access. The gym keeps
     * their history and they keep their backup. leftSelf tells the rules it was their choice, so they may join again
     * with a code (someone the gym removed can't).
     */
    override suspend fun leave(gymId: String, uid: String) {
        db.batch()
            .update(db.collection("users").document(uid), "gymId", FieldValue.delete())
            .update(peopleCol(gymId).document(uid), mapOf("status" to PersonStatus.REMOVED.name.lowercase(), "leftSelf" to true))
            .commit().await()
    }

    override suspend fun newTrainerCode(gymId: String, uid: String, oldCode: String?): String {
        val code = freeCode()
        db.batch()
            .set(db.collection("codes").document(code), mapOf("gymId" to gymId, "type" to "trainer", "trainerUid" to uid))
            .update(peopleCol(gymId).document(uid), "trainerCode", code)
            .apply { if (!oldCode.isNullOrBlank()) update(db.collection("codes").document(oldCode), "active", false) }
            .commit().await()
        return code
    }

    /**
     * Deleting an account: the gym record keeps its workouts (they're part of other people's rankings) but loses the
     * name and photo and is marked as left; the private backup and the account's gym link are deleted.
     */
    override suspend fun deleteAccount(gymId: String?, uid: String) {
        if (gymId != null) {
            peopleCol(gymId).document(uid).update(mapOf("name" to "Former member", "photoUrl" to null)).await()
            peopleCol(gymId).document(uid).update(mapOf("status" to PersonStatus.REMOVED.name.lowercase(), "leftSelf" to true)).await()
        }
        db.collection("users").document(uid).delete().await()
    }

    /** Private per-account backup of profile and settings (users/{uid}.prefs), so signing in again restores them. */
    override suspend fun loadBackup(uid: String): Map<String, String>? {
        @Suppress("UNCHECKED_CAST")
        return db.collection("users").document(uid).get().await().get("prefs") as? Map<String, String>
    }

    override suspend fun saveBackup(uid: String, prefs: Map<String, String>) {
        db.collection("users").document(uid).set(mapOf("prefs" to prefs), SetOptions.merge()).await()
    }

    // ---------- workouts ----------

    /**
     * Writes assignments in small batches: the rules read each member's record, and a batch may only make
     * [RULE_READS_PER_BATCH] such reads. Returns how many were saved; throws [PartialSave] if a later batch fails.
     */
    override suspend fun assign(gymId: String, list: List<Assignment>): Int {
        var saved = 0
        list.chunked(RULE_READS_PER_BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(assignmentsCol(gymId).document(), assignmentMap(it)) }
            try {
                batch.commit().await()
            } catch (e: Exception) {
                if (saved > 0) throw PartialSave(saved, list.size, e)
                throw e
            }
            saved += chunk.size
        }
        return saved
    }

    /** Member progress: sets ticked, status and timestamps. */
    override suspend fun saveProgress(gymId: String, a: Assignment) {
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

    override suspend fun verify(gymId: String, id: String, verified: Boolean, note: String) {
        assignmentsCol(gymId).document(id).update(mapOf("verified" to verified, "trainerNote" to note)).await()
    }

    override suspend fun deleteAssignment(gymId: String, id: String) {
        assignmentsCol(gymId).document(id).delete().await()
    }

    override suspend fun saveTemplate(gymId: String, t: Template) {
        val doc = if (t.id.isBlank()) templatesCol(gymId).document() else templatesCol(gymId).document(t.id)
        doc.set(mapOf("trainerUid" to t.trainerUid, "title" to t.title, "exercises" to t.exercises.map(::exerciseMap))).await()
    }

    override suspend fun deleteTemplate(gymId: String, id: String) {
        templatesCol(gymId).document(id).delete().await()
    }

    /** Monthly awards are written once (only by the owner); an existing month is left as it is. */
    override suspend fun saveAwards(gymId: String, month: String, winners: Map<Award, String>) {
        db.runTransaction { tx ->
            val ref = awardsCol(gymId).document(month)
            if (!tx.get(ref).exists()) tx.set(ref, mapOf("winners" to winners.mapKeys { it.key.name }, "savedAt" to System.currentTimeMillis()))
        }.await()
    }

    /** Everyone in the gym, straight from the server (not the phone's cache), for decisions that are saved for good. */
    override suspend fun fetchPeople(gymId: String): List<Person> = peopleCol(gymId).get(Source.SERVER).await().documents.map(::toPerson)

    /** Assignments between two days from the server, for the same reason. */
    override suspend fun fetchAssignments(gymId: String, from: Long, to: Long): List<Assignment> =
        assignmentsCol(gymId).whereGreaterThanOrEqualTo("epochDay", from).whereLessThanOrEqualTo("epochDay", to)
            .get(Source.SERVER).await().documents.map(::toAssignment)

    override suspend fun giveAward(gymId: String, a: GivenAward) {
        givenCol(gymId).add(mapOf("title" to a.title, "emoji" to a.emoji, "uid" to a.uid, "note" to a.note,
            "month" to a.month, "givenAt" to a.givenAt)).await()
    }

    override suspend fun editGivenAward(gymId: String, id: String, title: String, note: String) {
        givenCol(gymId).document(id).update(mapOf("title" to title, "note" to note)).await()
    }

    override suspend fun deleteGivenAward(gymId: String, id: String) {
        givenCol(gymId).document(id).delete().await()
    }

    override suspend fun renameGym(gymId: String, name: String) {
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
        // The rules only accept a short name and a Google profile photo.
        "name" to name.take(MAX_NAME), "photoUrl" to photo?.takeIf { GOOGLE_PHOTO.matches(it) && it.length <= 500 }, "role" to role.name.lowercase(), "status" to status.name.lowercase(),
        "joinedAt" to System.currentTimeMillis(),
    )

    private fun exerciseMap(e: AssignedExercise) = mapOf(
        "exerciseId" to e.exerciseId, "bodyPart" to e.bodyPart, "cue" to e.cue,
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
                cue = e["cue"] as? String ?: "",
            )
        }

    private inline fun <reified E : Enum<E>> enumOf(s: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name.equals(s, ignoreCase = true) } ?: default

    private fun <T> com.google.firebase.firestore.DocumentReference.listen(map: (DocumentSnapshot) -> T?): Flow<T?> = callbackFlow {
        // INCLUDE so we also hear the server's confirmation, which changes only metadata.
        val reg = addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            // An error is not "the document is gone": close and retry, so a hiccup never shows the join screen.
            if (err != null) { close(err); return@addSnapshotListener }
            // Wait for the server to confirm local writes: rules only see committed data, so acting on an
            // unconfirmed "you're in the gym" would start gym listeners that the server then refuses.
            if (snap?.metadata?.hasPendingWrites() == true) return@addSnapshotListener
            trySend(snap?.takeIf { it.exists() }?.let(map))
        }
        awaitClose { reg.remove() }
    }.retryWithBackoff().distinctUntilChanged()

    /** A refused listener is dead in Firestore, so retry (e.g. right after joining or approval). */
    private fun <T> Query.listenAll(map: (DocumentSnapshot) -> T): Flow<List<T>> = snapshots(map).retryWithBackoff()

    /** A query's results as they change; ends with the error if Firestore refuses it. */
    private fun <T> Query.snapshots(map: (DocumentSnapshot) -> T): Flow<List<T>> = callbackFlow {
        val reg = addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.documents?.map(map).orEmpty())
        }
        awaitClose { reg.remove() }
    }

    /** Keeps a listener alive: retries after 1s, 2s, 4s… up to [MAX_RETRY_DELAY_MS], for as long as it is collected. */
    private fun <T> Flow<T>.retryWithBackoff(): Flow<T> = retryWhen { e, attempt ->
        if (e is CancellationException) return@retryWhen false
        delay((1000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(MAX_RETRY_DELAY_MS))
        true
    }

    private companion object {
        const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O, 1/I
        /** Firestore rules allow 20 document reads per batch; each assignment needs one, plus a little headroom. */
        const val RULE_READS_PER_BATCH = 15
        const val MAX_RETRY_DELAY_MS = 30_000L
        const val MAX_NAME = 60
        val GOOGLE_PHOTO = Regex("https://[a-z0-9-]+[.]googleusercontent[.]com/.*")
    }
}

/** Something the user can fix, with a message written for them. */
class GymException(message: String) : Exception(message)

/** Only [saved] of [total] items were written before [cause] stopped the rest. */
class PartialSave(val saved: Int, val total: Int, cause: Throwable) : Exception(cause)
