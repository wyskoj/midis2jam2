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
package org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks

import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.Point3
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.HandProfile.Companion.LEFT
import org.wysko.midis2jam2.instrument.family.percussion.drumset.sticks.HandProfile.Companion.RIGHT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The planner that decides which of a drummer's two sticks strikes each hit. These pin down how a right-handed
 * drummer playing crossed looks: the right hand keeps time on the hi-hat while the left plays the snare, fast hits
 * alternate hands, and the right hand comes down to the floor tom.
 */
class DrumStickPlannerTest {

    @Test
    fun `in a rock groove the right hand plays the hi-hat and the left hand the snare`() {
        val hits = buildList {
            repeat(16) { i ->
                add(StickHit(i * 0.25, HI_HAT, "hat$i"))
                if (i % 4 == 2) add(StickHit(i * 0.25, SNARE, "snare$i"))
            }
        }
        val plan = DrumStickPlanner.plan(hits, HOME)

        assertTrue(plan.unassigned.isEmpty())
        assertTrue(plan.perHand[RIGHT].all { it.target == HI_HAT }, "Right hand: ${plan.perHand[RIGHT].ids()}")
        assertTrue(plan.perHand[LEFT].all { it.target == SNARE }, "Left hand: ${plan.perHand[LEFT].ids()}")
    }

    @Test
    fun `a fast snare roll alternates hands`() {
        val plan = DrumStickPlanner.plan(List(16) { StickHit(it * 0.125, SNARE, it) }, HOME)
        assertAlternates(plan)
    }

    @Test
    fun `a fill down the toms alternates hands and lands with the right hand on the floor tom`() {
        val fill = listOf(HIGH_TOM, HIGH_TOM, HIGH_MID_TOM, HIGH_MID_TOM, LOW_MID_TOM, LOW_MID_TOM, FLOOR_TOM)
        val plan = DrumStickPlanner.plan(fill.mapIndexed { i, tom -> StickHit(i * 0.125, tom, i) }, HOME)

        assertAlternates(plan)
        val floor = plan.perHand.flatMapIndexed { hand, hits -> hits.filter { it.target == FLOOR_TOM }.map { hand } }
        assertTrue(RIGHT in floor, "The right hand should play the floor tom, got hands $floor")
    }

    @Test
    fun `a crash with the snare is split between the hands`() {
        val plan = DrumStickPlanner.plan(listOf(StickHit(0.0, CRASH, "crash"), StickHit(0.0, SNARE, "snare")), HOME)
        assertEquals(1, plan.perHand[LEFT].size)
        assertEquals(1, plan.perHand[RIGHT].size)
    }

    @Test
    fun `when three pieces are struck at once the hi-hat goes without a stick`() {
        val hits = listOf(StickHit(0.0, CRASH, "crash"), StickHit(0.0, SNARE, "snare"), StickHit(0.0, HI_HAT, "hat"))
        val plan = DrumStickPlanner.plan(hits, HOME)

        assertEquals(setOf("crash", "snare"), plan.perHand.flatten().map { it.source }.toSet())
        assertEquals(listOf("hat"), plan.unassigned.map { it.source })
    }

    @Test
    fun `planning is deterministic`() {
        val hits = List(32) { i -> StickHit(i * 0.1, listOf(SNARE, HI_HAT, HIGH_TOM, CRASH, RIDE)[(i * 7) % 5], i) }
        assertEquals(DrumStickPlanner.plan(hits, HOME), DrumStickPlanner.plan(hits, HOME))
    }

    @Test
    fun `an empty part plans nothing`() {
        val plan = DrumStickPlanner.plan(emptyList<StickHit<Int>>(), HOME)
        assertEquals(listOf(emptyList(), emptyList()), plan.perHand)
        assertTrue(plan.unassigned.isEmpty())
    }

    private companion object {
        // Roughly where the standard kit's pieces are, in the kit's space.
        val SNARE = DrumTarget("snare", Point3(-11.0, 17.0, -72.0), HandProfile.SNARE)
        val HI_HAT = DrumTarget("hi_hat", Point3(-19.0, 24.0, -72.0), HandProfile.HI_HAT)
        val HIGH_TOM = DrumTarget("tom_high", Point3(-15.0, 30.0, -78.0), HandProfile.RACK_TOM)
        val HIGH_MID_TOM = DrumTarget("tom_high_mid", Point3(-9.0, 32.0, -82.0), HandProfile.RACK_TOM)
        val LOW_MID_TOM = DrumTarget("tom_low_mid", Point3(0.0, 33.0, -85.0), HandProfile.RACK_TOM)
        val FLOOR_TOM = DrumTarget("tom_low_floor", Point3(20.0, 21.0, -60.0), HandProfile.FLOOR_TOM)
        val CRASH = DrumTarget("cymbal_crash_1", Point3(-18.0, 50.0, -88.0), HandProfile.LEFT_CYMBAL)
        val RIDE = DrumTarget("ride_ride_1_edge", Point3(22.0, 45.0, -76.0), HandProfile.RIDE)

        val HOME = listOf(SNARE.position, HI_HAT.position)

        fun List<StickHit<*>>.ids() = map { it.target.id }

        /** Asserts every hit of [plan] is struck by the other hand from the hit before. */
        fun assertAlternates(plan: StickPlan<Int>) {
            val hands = plan.perHand
                .flatMapIndexed { hand, hits -> hits.map { it.time to hand } }
                .sortedBy { it.first }
                .map { it.second }
            assertTrue(hands.zipWithNext().all { (a, b) -> a != b }, "Expected the hands to alternate, got $hands")
        }
    }
}
