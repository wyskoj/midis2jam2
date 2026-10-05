/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.strings.bowing

import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bow must lie on the string being played and clear the others, move without jumps, and behave the same
 * wherever playback is sought to.
 */
class BowMotionTest {
    private val x = doubleArrayOf(-0.8, -0.3, 0.3, 0.8)
    private val z = doubleArrayOf(0.47, 0.58, 0.58, 0.47)
    private val contacts = StringContactMap(x, z)

    private fun heightAt(line: ContactLine, string: Int) = line.z0 + line.slope * x[string]

    @Test
    @Spec("instrument.strings.bow.string-contact")
    fun `the bow line touches the sounded string and clears every other`() {
        for (played in 0..3) {
            val line = contacts.lineFor(setOf(played))
            assertEquals(z[played], heightAt(line, played), 1e-9)
            for (other in (0..3) - played) {
                assertTrue(heightAt(line, other) >= z[other] - 1e-9, "String $other pokes through when playing $played")
            }
        }
    }

    @Test
    @Spec("instrument.strings.bow.string-contact")
    fun `a double stop touches both strings and clears the rest`() {
        for (low in 0..2) {
            val line = contacts.lineFor(setOf(low, low + 1))
            assertEquals(z[low], heightAt(line, low), 1e-9)
            assertEquals(z[low + 1], heightAt(line, low + 1), 1e-9)
            for (other in (0..3) - setOf(low, low + 1)) {
                assertTrue(heightAt(line, other) >= z[other] - 1e-9)
            }
        }
    }

    private val plan = BowingPlanner.plan(
        listOf(BowNote(1.0, 1.5, 0), BowNote(1.5, 2.0, 3), BowNote(5.0, 5.5, 1)),
    )
    private val motion = BowMotion(plan, contacts)

    @Test
    fun `the bow is on the string by the time a note starts`() {
        val onFirst = motion.poseAt(1.0).line
        assertEquals(contacts.lineFor(setOf(0)).slope, onFirst.slope, 1e-9)
        val onSecond = motion.poseAt(2.0 - 0.45).line
        assertEquals(contacts.lineFor(setOf(3)).slope, onSecond.slope, 1e-9)
    }

    @Test
    @Spec("instrument.strings.bow.string-contact")
    fun `the bow tilts gradually across a string crossing`() {
        val before = motion.poseAt(1.45).line.slope
        val during = motion.poseAt(1.48).line.slope
        val after = motion.poseAt(1.5).line.slope
        assertTrue(during > before && during < after || during < before && during > after, "No gradual tilt")
    }

    @Test
    @Spec("instrument.strings.bow.lifts")
    fun `the bow is down while playing and lifted in long rests and after the end`() {
        assertEquals(0.0, motion.poseAt(1.2).lift, 1e-9)
        assertEquals(1.0, motion.poseAt(3.5).lift, 1e-9)
        assertEquals(1.0, motion.poseAt(30.0).lift, 1e-9)
        assertEquals(1.0, motion.poseAt(0.0).lift, 1e-9)
    }

    @Test
    fun `the bow position never jumps`() {
        var previous = motion.poseAt(0.0).position
        var t = 0.0
        while (t < 7.0) {
            t += 0.005
            val p = motion.poseAt(t).position
            assertTrue(kotlin.math.abs(p - previous) < 0.05, "Bow jumped from $previous to $p at $t s")
            previous = p
        }
    }

    @Test
    fun `an empty part leaves the bow resting`() {
        assertEquals(1.0, BowMotion(BowingPlanner.plan(emptyList()), contacts).poseAt(2.0).lift)
    }
}
