package com.barathiraja.jk.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WorkoutSession::class, WaterDay::class, WeightEntry::class, Fast::class, StepDay::class,
        ExerciseFlag::class, CustomWorkout::class, ChallengeDay::class, FoodEntry::class, BodyPhoto::class,
        WalkSession::class, MindSession::class, SplitDay::class, PlanDay::class, PlanItem::class, SetLog::class,
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class JkDatabase : RoomDatabase() {
    abstract fun dao(): JkDao

    companion object {
        fun create(context: Context): JkDatabase =
            Room.databaseBuilder(context, JkDatabase::class.java, "jk.db")
                .build()
    }
}
