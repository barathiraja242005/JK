package com.barathiraja.jk

import com.barathiraja.jk.ui.gym.parseCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.barathiraja.jk.ui.gym.shortStep

/** A coach's cue becomes a rep range, a swap and short tips, so members never read a paragraph. */
class CueTest {
    @Test fun planCueSplitsIntoRepsSwapAndTips() {
        val c = parseCue("3–4 sets of 6–8. Or Hack Squat. Brace the core, squat under control, push through the whole foot.")
        assertEquals("6–8", c.reps)
        assertEquals("Hack Squat", c.swap)
        assertEquals(listOf("Brace the core", "Squat under control", "Push through the whole foot"), c.tips)
    }

    @Test fun trainerFreeTextBecomesTips() {
        val c = parseCue("Slow on the way down. Keep elbows tucked")
        assertNull(c.reps); assertNull(c.swap)
        assertEquals(listOf("Slow on the way down", "Keep elbows tucked"), c.tips)
    }

    @Test fun emptyCueHasNothing() {
        assertEquals(0, parseCue("").tips.size)
    }
}

class ShortStepTest {
    @Test fun stepsAreCutToTheAction() {
        assertEquals("Slowly lower the bar by bending the knees and hips",
            shortStep("Begin to slowly lower the bar by bending the knees and hips as you maintain a straight posture with the head up. Continue down."))
        assertEquals("Hold on to the bar using both arms at each side",
            shortStep("Hold on to the bar using both arms at each side and lift it off the rack by first pushing with your legs and at the same time straightening your torso."))
        assertEquals("Raise the bar as you exhale by pushing the floor",
            shortStep("Begin to raise the bar as you exhale by pushing the floor with the heel of your foot as you straighten the legs again and go back to the starting position."))
        assertEquals("Step away from the rack and position your legs",
            shortStep("Step away from the rack and position your legs using a shoulder width medium stance with the toes slightly pointed out."))
        assertNull(shortStep("Repeat for the recommended amount of repetitions."))
    }
}
