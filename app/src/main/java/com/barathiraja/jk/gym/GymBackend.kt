package com.barathiraja.jk.gym

import kotlinx.coroutines.flow.Flow

/** The signed-in account: a Google account, or a made-up person in test mode. */
data class Account(val uid: String, val displayName: String, val photoUrl: String?)

/** Everything the app reads and writes about the gym: Firestore ([GymRepo]) or the in-memory test gym ([TestGym]). */
interface GymBackend {
    fun userGymId(uid: String): Flow<String?>
    fun gym(gymId: String): Flow<Gym?>
    fun person(gymId: String, uid: String): Flow<Person?>
    fun people(gymId: String): Flow<List<Person>>
    fun assignments(gymId: String, from: Long, to: Long): Flow<List<Assignment>>
    /** Assignments between two days where [field] ("memberUid" or "trainerUid") is [uid]. */
    fun assignmentsOf(gymId: String, field: String, uid: String, from: Long, to: Long): Flow<List<Assignment>>
    fun templates(gymId: String, trainerUid: String): Flow<List<Template>>
    fun awards(gymId: String): Flow<List<MonthAwards>>
    fun givenAwards(gymId: String): Flow<List<GivenAward>>

    suspend fun createGym(name: String, uid: String, displayName: String, photo: String?): String
    suspend fun joinAsTrainer(gymCode: String, uid: String, displayName: String, photo: String?)
    suspend fun joinAsMember(trainerCode: String, uid: String, displayName: String, photo: String?)
    suspend fun setName(gymId: String, uid: String, name: String)
    suspend fun setStatus(gymId: String, uid: String, status: PersonStatus)
    suspend fun setTrainer(gymId: String, memberUids: List<String>, trainerUid: String?)
    suspend fun removeFromGym(gymId: String, uid: String, members: List<String>, moveTo: String?)
    suspend fun leave(gymId: String, uid: String)
    /** A trainer's new member code; the old one stops letting people join. Returns the new code. */
    suspend fun newTrainerCode(gymId: String, uid: String, oldCode: String?): String
    /** Removes the account from the gym (name cleared, marked as left) and deletes its private backup. */
    suspend fun deleteAccount(gymId: String?, uid: String)
    suspend fun loadBackup(uid: String): Map<String, String>?
    suspend fun saveBackup(uid: String, prefs: Map<String, String>)

    suspend fun assign(gymId: String, list: List<Assignment>): Int
    suspend fun saveProgress(gymId: String, a: Assignment)
    suspend fun verify(gymId: String, id: String, verified: Boolean, note: String)
    suspend fun deleteAssignment(gymId: String, id: String)
    suspend fun saveTemplate(gymId: String, t: Template)
    suspend fun deleteTemplate(gymId: String, id: String)
    suspend fun saveAwards(gymId: String, month: String, winners: Map<Award, String>)
    suspend fun fetchPeople(gymId: String): List<Person>
    suspend fun fetchAssignments(gymId: String, from: Long, to: Long): List<Assignment>
    suspend fun giveAward(gymId: String, a: GivenAward)
    suspend fun editGivenAward(gymId: String, id: String, title: String, note: String)
    suspend fun deleteGivenAward(gymId: String, id: String)
    suspend fun renameGym(gymId: String, name: String)
}
