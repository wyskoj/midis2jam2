/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The planner that decides which of a few mallets strikes each note of a mallet part. These pin down the things a
 * viewer would notice if they broke: every note gets struck, mallets only cross a little, fast notes alternate hands,
 * slow ones don't, and notes far apart go to different hands.
 */
class MalletPlannerTest {

    @Test
    fun `every note is struck by exactly one mallet or listed as unassigned`() {
        val hits = melody()
        val plan = MalletPlanner.plan(hits, 2, ::x)

        val struck = plan.perMallet.flatten() + plan.unassigned
        assertEquals(hits.size, struck.size, "Every hit should appear once in the plan")
        assertEquals(hits.map { it.source }.toSet(), struck.map { it.source }.toSet())
    }

    @Test
    fun `a fast descending run alternates hands`() {
        val run = listOf(84, 83, 81, 79, 77, 76, 74, 72, 71, 69, 67, 65)
        val plan = MalletPlanner.plan(run.mapIndexed { i, note -> MalletHit(i * 0.2, note, i) }, 2, ::x)
        assertAlternates(plan)
    }

    @Test
    fun `a fast ascending run alternates hands`() {
        val run = listOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79)
        val plan = MalletPlanner.plan(run.mapIndexed { i, note -> MalletHit(i * 0.2, note, i) }, 2, ::x)
        assertAlternates(plan)
    }

    @Test
    fun `a slow run doesn't swap hands on every note`() {
        val run = listOf(84, 83, 81, 79, 77, 76, 74, 72)
        val hits = run.mapIndexed { i, note -> MalletHit(i * 0.6, note, i) }
        val plan = MalletPlanner.plan(hits, 2, ::x)
        val owners = hits.map { hit -> plan.perMallet.indexOfFirst { hit in it } }
        val swaps = owners.zipWithNext().count { (a, b) -> a != b }
        assertTrue(swaps <= 1, "Notes 0.6 s apart should mostly stay on one mallet, got $owners")
    }

    @Test
    fun `mallets never cross further than the limit`() {
        val limit = MalletCostParams().maxCross
        for (mallets in listOf(2, 3, 4)) {
            val plan = MalletPlanner.plan(melody(), mallets, ::x)
            val byTime = plan.perMallet
                .flatMapIndexed { m, hits -> hits.map { m to it } }
                .groupBy { it.second.time }
                .entries.sortedBy { it.key }
            val position = arrayOfNulls<Double>(mallets)
            for ((time, struck) in byTime.map { it.key to it.value }) {
                // A chord's notes are all struck at once, so check the order after the whole chord.
                struck.forEach { (m, hit) -> position[m] = x(hit.note) }
                val placed = position.withIndex().filter { it.value != null }
                for (left in placed) {
                    for (right in placed.filter { it.index > left.index }) {
                        if (left.value!! - right.value!! > limit + 1e-9) {
                            fail("With $mallets mallets, mallet ${left.index} is too far right of ${right.index} at ${time}s")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `a fast repeated note alternates between mallets`() {
        val hits = List(8) { MalletHit(it * 0.1, 60, it) }
        val plan = MalletPlanner.plan(hits, 2, ::x)

        val owner = hits.map { hit -> plan.perMallet.indexOfFirst { hit in it } }
        owner.zipWithNext().forEach { (a, b) ->
            assertTrue(a != b, "Repeated notes 0.1 s apart should alternate mallets, got $owner")
        }
    }

    @Test
    fun `a slow repeated note stays on one mallet`() {
        val hits = List(4) { MalletHit(it * 1.0, 60, it) }
        val plan = MalletPlanner.plan(hits, 2, ::x)
        assertTrue(plan.perMallet.any { it.size == 4 }, "Slow repeats shouldn't swap hands, got ${plan.perMallet}")
    }

    @Test
    fun `a wide two-note chord is split between the left and right mallets`() {
        val hits = listOf(MalletHit(0.0, 40, "low"), MalletHit(0.0, 90, "high"))
        val plan = MalletPlanner.plan(hits, 2, ::x)
        assertEquals(listOf("low"), plan.perMallet[0].map { it.source })
        assertEquals(listOf("high"), plan.perMallet[1].map { it.source })
    }

    @Test
    fun `notes leaping between the ends of the keyboard go to different hands`() {
        val hits = List(8) { MalletHit(it * 0.25, if (it % 2 == 0) 40 else 90, it) }
        val plan = MalletPlanner.plan(hits, 2, ::x)
        assertTrue(plan.perMallet[0].all { it.note == 40 }, "Left mallet should take the low notes: ${plan.perMallet}")
        assertTrue(plan.perMallet[1].all { it.note == 90 }, "Right mallet should take the high notes: ${plan.perMallet}")
    }

    @Test
    fun `a chord bigger than the mallets keeps its outer notes`() {
        val hits = listOf(48, 55, 60, 67).map { MalletHit(0.0, it, it) }

        val two = MalletPlanner.plan(hits, 2, ::x)
        assertEquals(listOf(48, 67), two.perMallet.flatten().map { it.note }.sorted())
        assertEquals(listOf(55, 60), two.unassigned.map { it.note }.sorted())

        val four = MalletPlanner.plan(hits, 4, ::x)
        assertEquals(listOf(48, 55, 60, 67), four.perMallet.map { it.single().note })
        assertTrue(four.unassigned.isEmpty())
    }

    @Test
    fun `a note doubled within a chord is struck once`() {
        val hits = listOf(MalletHit(0.0, 60, "a"), MalletHit(0.0, 60, "b"))
        val plan = MalletPlanner.plan(hits, 2, ::x)
        assertEquals(1, plan.perMallet.flatten().size)
        assertEquals(1, plan.unassigned.size)
    }

    @Test
    fun `a single mallet plays every note`() {
        val hits = List(10) { MalletHit(it * 0.2, 50 + (it * 7) % 30, it) }
        val plan = MalletPlanner.plan(hits, 1, ::x)
        assertEquals(hits.size, plan.perMallet.single().size)
    }

    @Test
    fun `planning is deterministic`() {
        assertEquals(MalletPlanner.plan(melody(), 2, ::x), MalletPlanner.plan(melody(), 2, ::x))
    }

    @Test
    fun `an empty part plans nothing`() {
        val plan = MalletPlanner.plan(emptyList<MalletHit<Int>>(), 2, ::x)
        assertEquals(listOf(emptyList(), emptyList()), plan.perMallet)
        assertTrue(plan.unassigned.isEmpty())
    }

    private companion object {
        /** Asserts every note of [plan] is struck by the other mallet from the note before. */
        fun assertAlternates(plan: MalletPlan<Int>) {
            val owners = plan.perMallet
                .flatMapIndexed { m, hits -> hits.map { it.time to m } }
                .sortedBy { it.first }
                .map { it.second }
            assertTrue(plan.unassigned.isEmpty())
            assertTrue(owners.zipWithNext().all { (a, b) -> a != b }, "Expected the mallets to alternate, got $owners")
        }

        /** Roughly where a note's bar sits: about 0.78 units per semitone, like the real instrument. */
        fun x(note: Int): Double = (note - 64) * 0.78

        /** A made-up but busy part: runs, leaps, repeats and two-note chords. */
        fun melody(): List<MalletHit<Int>> {
            val notes = listOf(60, 62, 64, 65, 67, 69, 71, 72, 48, 84, 48, 84, 60, 60, 60, 60, 55, 79, 57, 77)
            val hits = notes.mapIndexed { i, note -> MalletHit(i * 0.125, note, i) }
            val chords = listOf(40 to 88, 52 to 64, 70 to 71).mapIndexed { i, (a, b) ->
                listOf(MalletHit(3.0 + i * 0.5, a, 100 + 2 * i), MalletHit(3.0 + i * 0.5, b, 101 + 2 * i))
            }.flatten()
            return hits + chords
        }
    }
}
