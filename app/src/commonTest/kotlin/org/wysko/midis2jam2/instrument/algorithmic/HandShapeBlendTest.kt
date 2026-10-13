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

import org.wysko.midis2jam2.instrument.algorithmic.HandShapeBlend.MAX_TRANSITION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Protects the timing of the move between hand shapes: the fingers must arrive exactly as the next note starts (so a
 * sounding note always shows its own fingering), and never start moving before the previous note began.
 */
class HandShapeBlendTest {

    @Test
    fun `the hand reaches the next shape exactly as the note starts`() {
        val next = 1.seconds
        assertEquals(1f, HandShapeBlend.progress(next, next, 0.seconds, 1.seconds))
        assertEquals(1f, HandShapeBlend.progress(next + 10.milliseconds, next, 0.seconds, 1.seconds))
    }

    @Test
    fun `the hand holds its shape until the move begins`() {
        val next = 1.seconds
        assertEquals(0f, HandShapeBlend.progress(next - MAX_TRANSITION, next, 0.seconds, 1.seconds))
        assertEquals(0f, HandShapeBlend.progress(0.5.seconds, next, 0.seconds, 1.seconds))
    }

    @Test
    fun `the move eases in and out and only goes forward`() {
        val next = 1.seconds
        val samples = (0..90).map { HandShapeBlend.progress(next - MAX_TRANSITION + it.milliseconds, next, 0.seconds, 1.seconds) }
        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a }, "The move should never go backwards: $samples")
        assertEquals(0.5f, HandShapeBlend.progress(next - MAX_TRANSITION / 2, next, 0.seconds, 1.seconds), 1e-4f)
    }

    @Test
    fun `a long rest still moves for no longer than the longest transition`() {
        assertEquals(MAX_TRANSITION, HandShapeBlend.transition(10.seconds, 0.seconds, 1.seconds))
        assertEquals(MAX_TRANSITION, HandShapeBlend.transition(10.seconds, null, null))
    }

    @Test
    fun `quick legato notes move within the tail of the previous note`() {
        // A 100 ms note followed straight away by the next: the move may take 30 ms of it.
        assertEquals(30.milliseconds, HandShapeBlend.transition(100.milliseconds, 0.seconds, 100.milliseconds))
    }

    @Test
    fun `overlapping notes never move before the previous note started`() {
        val transition = HandShapeBlend.transition(20.milliseconds, 0.seconds, 200.milliseconds)
        assertTrue(transition <= 20.milliseconds, "The move started before the previous note: $transition")
    }

    @Test
    fun `notes that start together snap`() {
        assertEquals(0f, HandShapeBlend.progress(0.seconds - 1.milliseconds, 0.seconds, 0.seconds, 1.seconds))
        assertEquals(1f, HandShapeBlend.progress(0.seconds, 0.seconds, 0.seconds, 1.seconds))
    }
}
