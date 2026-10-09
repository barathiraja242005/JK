package com.barathiraja.jk.data

import androidx.room.Dao
import com.barathiraja.jk.gym.SessionStore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

@Dao
interface JkDao : SessionStore {
    // Workouts
    @Insert
    suspend fun insertSession(session: WorkoutSession)

    /** Removes the sessions logged for one workout (e.g. "gym:{id}"), so re-finishing it never counts twice. */
    @Query("DELETE FROM workout_sessions WHERE workoutId = :workoutId")
    override suspend fun deleteSessionsFor(workoutId: String)

    /** Logs [session] as the only session for its workout. */
    @Transaction
    override suspend fun replaceSession(session: WorkoutSession) {
        deleteSessionsFor(session.workoutId)
        insertSession(session)
    }

    /** Sessions mirrored from the gym's workouts ("gym:{assignment id}"). */
    @Query("SELECT * FROM workout_sessions WHERE workoutId LIKE 'gym:%'")
    override suspend fun gymSessions(): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions ORDER BY finishedAt DESC")
    fun sessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE epochDay >= :fromDay ORDER BY finishedAt DESC")
    fun sessionsSince(fromDay: Long): Flow<List<WorkoutSession>>

    // Water
    @Upsert
    suspend fun upsertWater(day: WaterDay)

    @Query("SELECT * FROM water_days WHERE epochDay = :day")
    fun water(day: Long): Flow<WaterDay?>

    @Query("SELECT * FROM water_days WHERE epochDay >= :fromDay ORDER BY epochDay")
    fun waterSince(fromDay: Long): Flow<List<WaterDay>>

    @Query("SELECT * FROM water_days WHERE epochDay = :day")
    suspend fun waterNow(day: Long): WaterDay?

    /** Adds [delta] glasses in one transaction, so quick taps never overwrite each other. */
    @Transaction
    suspend fun addWater(day: Long, delta: Int, max: Int) {
        val current = waterNow(day)?.glasses ?: 0
        upsertWater(WaterDay(day, (current + delta).coerceIn(0, max)))
    }

    // Weight
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries ORDER BY epochDay")
    fun weights(): Flow<List<WeightEntry>>

    @Query("DELETE FROM weight_entries WHERE epochDay = :day")
    suspend fun deleteWeight(day: Long)

    // Fasting
    @Insert
    suspend fun insertFast(fast: Fast): Long

    @Update
    suspend fun updateFast(fast: Fast)

    @Query("SELECT * FROM fasts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeFast(): Flow<Fast?>

    @Query("SELECT * FROM fasts WHERE endedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 20")
    fun pastFasts(): Flow<List<Fast>>

    // Steps
    @Upsert
    suspend fun upsertSteps(day: StepDay)

    @Query("SELECT * FROM step_days WHERE epochDay >= :fromDay ORDER BY epochDay")
    fun stepsSince(fromDay: Long): Flow<List<StepDay>>

    // Exercise flags
    @Upsert
    suspend fun upsertFlag(flag: ExerciseFlag)

    @Query("SELECT * FROM exercise_flags WHERE exerciseId = :id")
    suspend fun flag(id: String): ExerciseFlag?

    @Query("SELECT * FROM exercise_flags")
    fun flags(): Flow<List<ExerciseFlag>>

    // Custom workouts
    @Upsert
    suspend fun upsertCustom(w: CustomWorkout): Long

    @Query("SELECT * FROM custom_workouts ORDER BY createdAt DESC")
    fun customWorkouts(): Flow<List<CustomWorkout>>

    @Query("SELECT * FROM custom_workouts WHERE id = :id")
    suspend fun customWorkout(id: Long): CustomWorkout?

    @Query("DELETE FROM custom_workouts WHERE id = :id")
    suspend fun deleteCustom(id: Long)

    // Challenges
    @Upsert
    suspend fun markChallengeDay(day: ChallengeDay)

    @Query("SELECT * FROM challenge_days")
    fun challengeDays(): Flow<List<ChallengeDay>>

    @Query("DELETE FROM challenge_days WHERE challengeId = :id")
    suspend fun resetChallenge(id: String)

    // Food
    @Insert
    suspend fun insertFood(e: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFood(id: Long)

    @Query("SELECT * FROM food_entries WHERE epochDay = :day ORDER BY id")
    fun foodOn(day: Long): Flow<List<FoodEntry>>

    // Body photos
    @Insert
    suspend fun insertPhoto(p: BodyPhoto)

    @Query("DELETE FROM body_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("SELECT * FROM body_photos ORDER BY epochDay")
    fun photos(): Flow<List<BodyPhoto>>

    // Walks & mindfulness
    @Insert
    suspend fun insertWalk(w: WalkSession)

    @Query("SELECT * FROM walk_sessions ORDER BY startedAt DESC LIMIT 50")
    fun walks(): Flow<List<WalkSession>>

    @Insert
    suspend fun insertMind(m: MindSession)

    @Query("SELECT * FROM mind_sessions ORDER BY id DESC LIMIT 50")
    fun mindSessions(): Flow<List<MindSession>>

    // ---- Training ----
    @Upsert
    suspend fun upsertSplit(days: List<SplitDay>)

    @Query("SELECT * FROM split_days ORDER BY dayOfWeek")
    fun split(): Flow<List<SplitDay>>

    @Query("SELECT * FROM split_days ORDER BY dayOfWeek")
    suspend fun splitNow(): List<SplitDay>

    @Upsert
    suspend fun upsertPlanDay(day: PlanDay)

    @Query("SELECT * FROM plan_days WHERE epochDay = :day")
    suspend fun planDay(day: Long): PlanDay?

    @Query("SELECT * FROM plan_days WHERE epochDay = :day")
    fun planDayFlow(day: Long): Flow<PlanDay?>

    @Query("SELECT * FROM plan_days WHERE epochDay BETWEEN :from AND :to")
    fun planDaysBetween(from: Long, to: Long): Flow<List<PlanDay>>

    @Query("SELECT * FROM plan_days WHERE completedAt IS NOT NULL ORDER BY epochDay DESC LIMIT 60")
    fun completedDays(): Flow<List<PlanDay>>

    @Query("DELETE FROM plan_days WHERE epochDay >= :fromDay AND completedAt IS NULL")
    suspend fun clearFuturePlans(fromDay: Long)

    @Query("SELECT * FROM plan_items WHERE epochDay = :day ORDER BY position")
    fun planItems(day: Long): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE epochDay = :day ORDER BY position")
    suspend fun planItemsNow(day: Long): List<PlanItem>

    @Insert
    suspend fun insertPlanItems(items: List<PlanItem>)

    @Update
    suspend fun updatePlanItem(item: PlanItem)

    @Query("SELECT * FROM plan_items WHERE id = :id")
    suspend fun planItem(id: Long): PlanItem?

    @Query("DELETE FROM plan_items WHERE id = :id")
    suspend fun deletePlanItem(id: Long)

    @Query("DELETE FROM plan_items WHERE epochDay = :day")
    suspend fun clearPlanItems(day: Long)

    @Query("DELETE FROM plan_items WHERE epochDay >= :fromDay AND epochDay NOT IN (SELECT epochDay FROM plan_days WHERE completedAt IS NOT NULL)")
    suspend fun clearFutureItems(fromDay: Long)

    @Insert
    suspend fun insertSetLog(log: SetLog)

    @Query("DELETE FROM set_logs WHERE epochDay = :day AND exerciseId = :exerciseId AND setIndex = :setIndex")
    suspend fun deleteSetLog(day: Long, exerciseId: String, setIndex: Int)

    /** Logs a set as the only row for its day, exercise and index, so a double tap never logs it twice. */
    @Transaction
    suspend fun replaceSetLog(log: SetLog) {
        deleteSetLog(log.epochDay, log.exerciseId, log.setIndex)
        insertSetLog(log)
    }

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY loggedAt DESC LIMIT 60")
    suspend fun setLogs(exerciseId: String): List<SetLog>

    @Query("SELECT * FROM set_logs WHERE exerciseId = :exerciseId ORDER BY loggedAt DESC LIMIT 60")
    fun setLogsFlow(exerciseId: String): Flow<List<SetLog>>
}
