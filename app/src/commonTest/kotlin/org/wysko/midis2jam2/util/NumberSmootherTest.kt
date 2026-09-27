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

package org.wysko.midis2jam2.util

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The smoother behind camera and lyric motion. Its overshoot clamp is the part that matters:
 * without it, a large frame delta would fling the value past its target.
 */
class NumberSmootherTest {

    @Test
    fun `a smoothness of zero snaps straight to the target`() {
        val smoother = NumberSmoother(0f, 0.0)
        assertEquals(100f, smoother.tick(FRAME) { 100f }, TOLERANCE)
        assertEquals(100f, smoother.value, TOLERANCE)
    }

    @Test
    fun `the value converges on a steady target`() {
        val smoother = NumberSmoother(0f, 5.0)
        repeat(300) { smoother.tick(FRAME) { 100f } }
        assertEquals(100f, smoother.value, 0.5f)
    }

    @Test
    fun `the value approaches the target monotonically`() {
        val smoother = NumberSmoother(0f, 5.0)
        var previous = smoother.value
        repeat(50) {
            val current = smoother.tick(FRAME) { 100f }
            assertTrue(current >= previous, "The smoothed value moved away from its target")
            previous = current
        }
    }

    @Test
    fun `a large frame delta does not overshoot the target`() {
        // A stalled frame must not fling the value past where it was heading.
        val smoother = NumberSmoother(0f, 50.0)
        val result = smoother.tick(10.seconds) { 100f }
        assertTrue(result <= 100f, "Overshot the target: $result")
        assertEquals(100f, result, TOLERANCE)
    }

    @Test
    fun `a large frame delta does not overshoot a descending target`() {
        val smoother = NumberSmoother(100f, 50.0)
        val result = smoother.tick(10.seconds) { 0f }
        assertTrue(result >= 0f, "Overshot the target: $result")
        assertEquals(0f, result, TOLERANCE)
    }

    @Test
    fun `snap sets the value immediately`() {
        val smoother = NumberSmoother(0f, 5.0)
        smoother.snap(42f)
        assertEquals(42f, smoother.value, TOLERANCE)
    }

    @Test
    fun `it tracks a moving target without diverging`() {
        val smoother = NumberSmoother(0f, 10.0)
        var target = 0f
        repeat(200) {
            target += 1f
            smoother.tick(FRAME) { target }
        }
        assertTrue(
            abs(smoother.value - target) < 20f,
            "The smoother fell too far behind a moving target: ${smoother.value} vs $target"
        )
    }

    @Test
    fun `a negative smoothness is rejected`() {
        assertFailsWith<IllegalArgumentException> { NumberSmoother(0f, 1.0).smoothness = -1.0 }
    }

    private companion object {
        val FRAME = (1.0 / 60.0).seconds
        const val TOLERANCE = 1.0e-4f
    }
}
