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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Which hits two sticks share a piece on, so they can move aside instead of striking one on top of the other: only
 * the same piece, only when both hands are on it close together.
 */
class SharedHitsTest {

    @Test
    fun `an alternating roll on one drum is shared by both hands`() {
        val left = List(4) { StickHit(it * 0.25, SNARE, it) }
        val right = List(4) { StickHit(it * 0.25 + 0.125, SNARE, it) }
        assertEquals(listOf(List(4) { true }, List(4) { true }), sharedHits(listOf(left, right), WINDOW))
    }

    @Test
    fun `hands on different pieces share nothing`() {
        val left = List(4) { StickHit(it * 0.25, SNARE, it) }
        val right = List(4) { StickHit(it * 0.25, HI_HAT, it) }
        assertEquals(listOf(List(4) { false }, List(4) { false }), sharedHits(listOf(left, right), WINDOW))
    }

    @Test
    fun `hits on the same piece far apart in time are not shared`() {
        val left = listOf(StickHit(0.0, SNARE, 0), StickHit(5.0, SNARE, 1))
        val right = listOf(StickHit(2.0, SNARE, 0), StickHit(5.2, SNARE, 1))
        assertEquals(listOf(listOf(false, true), listOf(false, true)), sharedHits(listOf(left, right), WINDOW))
    }

    private companion object {
        const val WINDOW = 0.5
        val SNARE = DrumTarget("snare", Point3(0.0, 0.0, 0.0), HandProfile.SNARE)
        val HI_HAT = DrumTarget("hi_hat", Point3(-10.0, 0.0, 0.0), HandProfile.HI_HAT)
    }
}
