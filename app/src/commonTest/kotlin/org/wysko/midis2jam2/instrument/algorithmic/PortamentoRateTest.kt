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

package org.wysko.midis2jam2.instrument.algorithmic

import org.wysko.midis2jam2.util.NumberSmoother
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/** Protects the FluidSynth-style portamento glide time and the framerate independence of smoothing. */
class PortamentoRateTest {

    @Test
    fun `the glide time grows with the portamento time and the interval`() {
        val times = (1..127).map { portamentoGlideSeconds(it, 12) }
        assertTrue(times.zipWithNext().all { (a, b) -> a <= b || b < 1.0 }, "Longer portamento times glide longer")
        assertEquals(0.0, portamentoGlideSeconds(0, 24), 1e-9)
        assertEquals(2 * portamentoGlideSeconds(78, 12), portamentoGlideSeconds(78, 24), 1e-9)
        assertEquals(portamentoGlideSeconds(78, 24), portamentoGlideSeconds(78, -24), 1e-9)
    }

    @Test
    fun `smoothing reaches the same value regardless of frame rate`() {
        val fast = NumberSmoother(0f, 10.0).also { s -> repeat(120) { s.tick(8.milliseconds) { 1f } } }
        val slow = NumberSmoother(0f, 10.0).also { s -> repeat(24) { s.tick(40.milliseconds) { 1f } } }
        assertEquals(fast.value, slow.value, 0.001f)
    }
}
