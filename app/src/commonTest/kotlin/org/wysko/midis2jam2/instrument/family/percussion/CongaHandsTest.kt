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
package org.wysko.midis2jam2.instrument.family.percussion

import org.wysko.midis2jam2.instrument.family.percussion.CongaHand.LEFT
import org.wysko.midis2jam2.instrument.family.percussion.CongaHand.RIGHT
import org.wysko.midis2jam2.instrument.family.percussion.CongaStroke.LOW
import org.wysko.midis2jam2.instrument.family.percussion.CongaStroke.MUTE
import org.wysko.midis2jam2.instrument.family.percussion.CongaStroke.OPEN_HIGH
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Which hand plays each conga mute. Each conga has one hand, and a mute can be played by either; a hand must never be
 * asked to strike twice at once, and fast mutes should be shared between the hands rather than piled on one.
 */
@Spec("instrument.congas.mute-either-hand")
class CongaHandsTest {

    @Test
    fun `open strokes are always played by their conga's hand`() {
        val hits = listOf(hit(0.0, OPEN_HIGH), hit(0.1, LOW), hit(0.2, OPEN_HIGH), hit(0.3, LOW))

        assertEquals(listOf(LEFT, RIGHT, LEFT, RIGHT), CongaHands.assign(hits))
    }

    @Test
    fun `occasional mutes are played on the high conga`() {
        val hits = listOf(hit(0.0, MUTE), hit(1.0, MUTE), hit(2.0, MUTE))

        assertEquals(listOf(LEFT, LEFT, LEFT), CongaHands.assign(hits))
    }

    @Test
    fun `a mute struck with an open high stroke goes to the right hand`() {
        val hits = listOf(hit(0.0, OPEN_HIGH), hit(0.0, MUTE))

        assertEquals(listOf(LEFT, RIGHT), CongaHands.assign(hits))
    }

    @Test
    fun `a mute struck with a low stroke goes to the left hand`() {
        val hits = listOf(hit(0.5, MUTE), hit(0.5, LOW))

        assertEquals(listOf(LEFT, RIGHT), CongaHands.assign(hits))
    }

    @Test
    fun `fast mutes alternate hands`() {
        val hits = (0 until 8).map { hit(it * 0.1, MUTE) }

        val hands = CongaHands.assign(hits)

        hands.zipWithNext().forEach { (a, b) -> assertTrue(a != b, "Fast mutes should alternate, but got $hands") }
    }

    @Test
    fun `mutes between fast open high strokes go to the right hand`() {
        val hits = (0 until 8).map { hit(it * 0.125, if (it % 2 == 0) OPEN_HIGH else MUTE) }

        val hands = CongaHands.assign(hits)

        assertEquals(List(4) { listOf(LEFT, RIGHT) }.flatten(), hands)
    }

    @Test
    fun `assignments are returned in the order the hits were given`() {
        val hits = listOf(hit(0.0, LOW), hit(0.0, MUTE), hit(-1.0, OPEN_HIGH))

        assertEquals(listOf(RIGHT, LEFT, LEFT), CongaHands.assign(hits))
    }

    @Test
    fun `a hand stays put between strikes and arrives at the next position before striking`() {
        val last = 0.0 to false
        val next = 1.0 to true

        assertEquals(0.0, CongaHands.muteBlend(0.5, last, next))
        assertEquals(1.0, CongaHands.muteBlend(0.99, last, next))
        assertEquals(1.0, CongaHands.muteBlend(1.0, last, next))
        assertEquals(1.0, CongaHands.muteBlend(2.0, next, null))
    }

    private fun hit(time: Double, stroke: CongaStroke) = CongaHit(time, stroke)
}
