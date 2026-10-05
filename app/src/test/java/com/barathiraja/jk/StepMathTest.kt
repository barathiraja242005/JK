package com.barathiraja.jk

import com.barathiraja.jk.data.StepDay
import com.barathiraja.jk.steps.StepState
import com.barathiraja.jk.steps.nextSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepMathTest {
    private val day = 20_000L

    @Test fun firstReadingStartsAtZero() {
        val u = nextSteps(StepState.NONE, day, 5_000f)
        assertEquals(0, u.steps)
        assertEquals(StepState(day, 5_000f, 5_000f), u.state)
        assertNull(u.finished)
    }

    @Test fun countsWithinTheDay() {
        val u = nextSteps(StepState(day, 1_000f, 1_500f), day, 3_200f)
        assertEquals(2_200, u.steps)
        assertNull(u.finished)
    }

    @Test fun rolloverKeepsStepsWalkedAroundMidnight() {
        // Last reading yesterday: 2,000 steps (3,000 - 1,000). 400 more walked before the first reading today.
        val u = nextSteps(StepState(day, 1_000f, 3_000f), day + 1, 3_400f)
        assertEquals(StepDay(day, 2_000), u.finished)
        assertEquals(400, u.steps)
        assertEquals(3_000f, u.state.baseline)
    }

    @Test fun rebootDuringTheDayKeepsTodaysSteps() {
        val u = nextSteps(StepState(day, 1_000f, 4_000f), day, 250f)
        assertEquals(3_250, u.steps)
        // The next reading continues from there.
        assertEquals(3_300, nextSteps(u.state, day, 300f).steps)
    }

    @Test fun rebootOvernightCountsStepsSinceBootAsToday() {
        val u = nextSteps(StepState(day, 1_000f, 4_000f), day + 1, 600f)
        assertEquals(StepDay(day, 3_000), u.finished)
        assertEquals(600, u.steps)
    }

    @Test fun afterAGapStepsAreNotDumpedOnToday() {
        val u = nextSteps(StepState(day, 1_000f, 4_000f), day + 3, 9_000f)
        assertEquals(StepDay(day, 3_000), u.finished)
        assertEquals(0, u.steps)
    }

    @Test fun oldPrefsWithoutLastTotalRebaseOnNewDay() {
        val u = nextSteps(StepState(day, 1_000f, -1f), day + 1, 2_000f)
        assertNull(u.finished)
        assertEquals(0, u.steps)
    }
}
