package com.barathiraja.jk.gym

import android.util.Log
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException

/**
 * Saves last month's automatic awards from the owner's phone, if nobody has yet. Waits a grace day into the new month
 * so members can finish logging, and works from fresh server data covering last month and the one before it (for
 * "most improved"), because the saved result can never be changed. Checked once a month (remembered across
 * restarts), and after a failure it waits before reading two months of workouts from the server again.
 */
class AwardsJob(
    /** The last month whose awards were found saved (kept across restarts). */
    private val checkedMonth: () -> String?,
    private val setCheckedMonth: (String) -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastTry = 0L

    suspend fun saveLastMonthIfDue(repo: GymBackend, gymId: String, saved: List<MonthAwards>, today: LocalDate) {
        val last = YearMonth.from(today).minusMonths(1)
        val key = last.toString()
        if (checkedMonth() == key) return
        if (saved.any { it.month == key }) { setCheckedMonth(key); return }
        if (today.dayOfMonth < GRACE_DAYS + 1) return
        if (now() - lastTry < RETRY_MS) return
        lastTry = now()
        try {
            val from = last.minusMonths(1).atDay(1).toEpochDay()
            val list = repo.fetchAssignments(gymId, from, last.atEndOfMonth().toEpochDay())
            val winners = Scoring.awards(repo.fetchPeople(gymId), list, last)
            if (winners.isNotEmpty()) repo.saveAwards(gymId, key, winners)
            setCheckedMonth(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { Log.w("AwardsJob", "Saving $key awards failed", e) }
        }
    }

    private companion object {
        /** Days into a new month before last month's awards are saved, so late logs still count. */
        const val GRACE_DAYS = 1
        /** How long to wait before trying again after a failure. */
        const val RETRY_MS = 30 * 60_000L
    }
}
