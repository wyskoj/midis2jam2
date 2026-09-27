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

package org.wysko.midis2jam2.datastructure

import kotlin.test.Test
import kotlin.test.assertEquals

class RunningAverageTest {

    @Test
    fun `it starts at its initial value`() {
        assertEquals(5.0, RunningAverage(3, 5)(), TOLERANCE)
    }

    @Test
    fun `it averages the values it holds`() {
        // The initial value counts as one of the elements: (0 + 2 + 4) / 3.
        val average = RunningAverage(4, 0)
        average += 2
        average += 4
        assertEquals(2.0, average(), TOLERANCE)
    }

    @Test
    fun `it evicts the oldest value once it is full`() {
        val average = RunningAverage(2, 100)
        average += 0
        average += 0
        // The initial 100 has been pushed out of the window.
        assertEquals(0.0, average(), TOLERANCE)
    }

    @Test
    fun `it never holds more than its size`() {
        val average = RunningAverage(3, 0)
        repeat(100) { average += 9 }
        assertEquals(9.0, average(), TOLERANCE)
    }

    @Test
    fun `it mixes numeric types`() {
        val average = RunningAverage(4, 0)
        average += 1
        average += 2.0
        average += 3f
        assertEquals(1.5, average(), TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-9
    }
}
