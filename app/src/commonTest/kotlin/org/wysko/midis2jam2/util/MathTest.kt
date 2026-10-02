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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** The interpolation helpers every animator is built on. */
class MathTest {

    @Test
    fun `lerp returns the endpoints at t equals zero and one`() {
        assertEquals(10.0, dLerp(10, 20, 0), TOLERANCE_D)
        assertEquals(20.0, dLerp(10, 20, 1), TOLERANCE_D)
        assertEquals(10f, fLerp(10, 20, 0), TOLERANCE_F)
        assertEquals(20f, fLerp(10, 20, 1), TOLERANCE_F)
    }

    @Test
    fun `lerp interpolates linearly`() {
        assertEquals(15.0, dLerp(10, 20, 0.5), TOLERANCE_D)
        assertEquals(12.5f, fLerp(10, 20, 0.25), TOLERANCE_F)
    }

    @Test
    fun `lerp extrapolates beyond the endpoints`() {
        assertEquals(30.0, dLerp(10, 20, 2), TOLERANCE_D)
        assertEquals(0f, fLerp(10, 20, -1), TOLERANCE_F)
    }

    @Test
    fun `interpTo snaps to the target when it is already there`() {
        assertEquals(5f, interpTo(5f, 5f, 0.016f, 10f), TOLERANCE_F)
    }

    @Test
    fun `interpTo snaps to the target when the speed is not positive`() {
        assertEquals(100f, interpTo(0f, 100f, 0.016f, 0f), TOLERANCE_F)
        assertEquals(100f, interpTo(0f, 100f, 0.016f, -5f), TOLERANCE_F)
    }

    @Test
    fun `interpTo moves toward the target without overshooting`() {
        val result = interpTo(0f, 100f, 0.1f, 5f)
        assertTrue(result > 0f && result < 100f, "Expected a value between the endpoints, got $result")
    }

    @Test
    fun `interpTo never passes the target even with a huge step`() {
        // The alpha is clamped to one, so a large delta lands exactly on the target.
        assertEquals(100f, interpTo(0f, 100f, 10f, 10f), TOLERANCE_F)
    }

    @Test
    fun `mapRangeClamped maps the input range onto the output range`() {
        assertEquals(0f, mapRangeClamped(0, 0, 10, 0, 100), TOLERANCE_F)
        assertEquals(50f, mapRangeClamped(5, 0, 10, 0, 100), TOLERANCE_F)
        assertEquals(100f, mapRangeClamped(10, 0, 10, 0, 100), TOLERANCE_F)
    }

    @Test
    fun `mapRangeClamped clamps outside the input range`() {
        assertEquals(0f, mapRangeClamped(-5, 0, 10, 0, 100), TOLERANCE_F)
        assertEquals(100f, mapRangeClamped(15, 0, 10, 0, 100), TOLERANCE_F)
    }

    @Test
    fun `mapRangeClamped clamps correctly when the output range is inverted`() {
        assertEquals(100f, mapRangeClamped(-5, 0, 10, 100, 0), TOLERANCE_F)
        assertEquals(0f, mapRangeClamped(15, 0, 10, 100, 0), TOLERANCE_F)
    }

    @Test
    fun `mapRangeClamped returns the output minimum for a degenerate input range`() {
        assertEquals(7f, mapRangeClamped(5, 3, 3, 7, 9), TOLERANCE_F)
    }

    @Test
    fun `easeOut spans zero to one and is monotonic`() {
        assertEquals(0f, easeOut(0f), TOLERANCE_F)
        assertEquals(1f, easeOut(1f), TOLERANCE_F)

        var previous = easeOut(0f)
        for (step in 1..20) {
            val current = easeOut(step / 20f)
            assertTrue(current >= previous, "easeOut decreased between steps ${step - 1} and $step")
            previous = current
        }
    }

    @Test
    fun `noteNumberToPitch names every pitch class`() {
        val expected = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        assertEquals(expected, (0 until PITCHES).map { noteNumberToPitch(it) })
    }

    @Test
    fun `noteNumberToPitch wraps by octave`() {
        // Middle C and the C an octave above share a pitch class.
        assertEquals(noteNumberToPitch(60), noteNumberToPitch(72))
        assertEquals("A", noteNumberToPitch(69))
    }

    @Test
    fun `noteNumberToPitch rejects a negative note number`() {
        assertFailsWith<IllegalStateException> { noteNumberToPitch(-1) }
    }

    private companion object {
        const val TOLERANCE_D = 1.0e-9
        const val TOLERANCE_F = 1.0e-5f
    }
}
