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
package org.wysko.midis2jam2.instrument.algorithmic

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The extra lift a stick gets as it winds up to strike, which gives a hit its weight. What matters on screen: the
 * stick is down on the drum at the moment it strikes, rises before the downswing rather than after it, hits harder
 * look bigger, and quick hits don't make it jump.
 */
class WindUpRiseTest {

    @Test
    fun `the stick is down at the moment it strikes and at rest`() {
        assertEquals(0.0, rise(time = NEXT), 1e-9)
        assertEquals(0.0, rise(time = NEXT - 1.0), 1e-9, "Long before the wind-up the stick is at rest")
        assertEquals(0.0, rise(time = NEXT, next = null), 1e-9, "With nothing to strike there's no rise")
    }

    @Test
    fun `the stick peaks before the downswing and drops into the strike`() {
        val samples = (0..100).map { NEXT - ANTICIPATION + it * ANTICIPATION / 100 }
        val peakTime = samples.maxBy { rise(it) }
        val peakWindUp = 1 - (NEXT - peakTime) / ANTICIPATION
        assertTrue(abs(peakWindUp - 0.4) < 0.02, "The rise should peak 40% into the wind-up, got $peakWindUp")
        assertTrue(rise(peakTime) > 0.9 * HEIGHT, "The peak should come close to the full height")

        val beforeStrike = rise(NEXT - 0.02)
        assertTrue(beforeStrike < 0.3 * HEIGHT, "The stick should be most of the way down just before the strike")
    }

    @Test
    fun `louder strikes rise higher`() {
        val time = NEXT - 0.4 * ANTICIPATION
        assertTrue(rise(time, velocity = 127) > rise(time, velocity = 40))
        assertEquals(0.5, rise(time, velocity = 0) / rise(time, velocity = 127), 1e-9, "The softest rises half as high")
    }

    @Test
    fun `quick strikes rise less, without jumping up straight after a strike`() {
        val last = NEXT - 0.1
        assertEquals(0.0, rise(time = last, last = last), 1e-9, "Right after a strike the stick is still down")
        val quick = (0..10).maxOf { rise(time = last + it * 0.01, last = last) }
        val slow = (0..100).maxOf { rise(time = NEXT - 1.0 + it * 0.01, last = NEXT - 1.0) }
        assertTrue(quick < slow, "A strike 0.1 s after the last should rise less ($quick) than a slow one ($slow)")
    }

    @Test
    fun `no height means no rise`() {
        assertEquals(0.0, windUpRise(NEXT - 0.1, NEXT, null, 127, ANTICIPATION, 0.0))
    }

    private companion object {
        const val NEXT = 10.0
        const val ANTICIPATION = 0.22
        const val HEIGHT = 3.0

        fun rise(time: Double, next: Double? = NEXT, last: Double? = null, velocity: Byte = 127) =
            windUpRise(time, next, last, velocity, ANTICIPATION, HEIGHT)
    }
}
