package com.barathiraja.jk

import com.barathiraja.jk.data.Block
import com.barathiraja.jk.data.Catalog
import com.barathiraja.jk.data.Foods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {
    private val assets = File("src/main/assets")

    @Test fun blocksRoundTrip() {
        val blocks = listOf(Block("Pushups", reps = 12), Block("Plank", seconds = 45))
        assertEquals(blocks, Block.decodeAll(Block.encodeAll(blocks)))
    }

    @Test fun decodeIgnoresGarbage() {
        assertEquals(listOf(Block("Plank", seconds = 30)), Block.decodeAll("Plank:s:30;;broken;X:r:abc"))
    }

    @Test fun challengeIdsResolve() {
        for (c in Catalog.challenges) {
            for (d in 1..c.days) {
                val w = c.plan(d)
                if (d % 7 == 0) assertNull("${c.id} day $d should rest", w)
                else {
                    assertNotNull(w)
                    assertEquals(w!!.id, Catalog.resolve("challenge:${c.id}:$d")?.id)
                    assertTrue(w.blocks.all { it.reps > 0 || it.seconds > 0 })
                }
            }
        }
    }

    @Test fun everyReferencedExerciseExistsAndIsBundled() {
        // org.json is stubbed in local unit tests, so pull ids out with a regex.
        val ids = Regex("\"id\":\"([^\"]+)\"").findAll(File(assets, "exercises.json").readText()).map { it.groupValues[1] }.toSet()
        assertTrue(ids.size > 800)
        for (id in Catalog.referencedIds()) {
            assertTrue("$id missing from database", id in ids)
            assertTrue("$id photo 0 not bundled", File(assets, "exercise_images/$id/0.jpg").exists())
            assertTrue("$id photo 1 not bundled", File(assets, "exercise_images/$id/1.jpg").exists())
        }
    }

    @Test fun foodNamesUniqueAndSane() {
        assertEquals(Foods.all.size, Foods.all.map { it.name }.toSet().size)
        assertTrue(Foods.all.all { it.kcal in 0..1500 })
        assertTrue(Foods.search("dosa", vegOnly = true).isNotEmpty())
        assertTrue(Foods.search("chicken", vegOnly = true).isEmpty())
    }
}
