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

package org.wysko.midis2jam2.manager.camera.cinematic

import org.wysko.midis2jam2.manager.camera.cinematic.planning.Move
import org.wysko.midis2jam2.manager.camera.cinematic.planning.RigFrame
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How the camera rig moves during a shot.
 *
 * A move that eases in starts every shot from a standstill, which reads as the camera waking up the moment the
 * viewer arrives. A move that covers the same ground however short the shot whips round in a short one. Both make
 * the edit feel mechanical, so every move must travel at a steady, gentle pace, and only the very last shot may
 * come to rest.
 */
class MoveTest {

    @Test
    @Spec("camera.cinematic.moving-at-cut")
    fun `a move is travelling at full speed from the first frame of its shot`() = forEachMovingShot { move, length ->
        val atCut = speed(move, length, 0.0)
        val midway = speed(move, length, length / 2)
        assertEquals(midway, atCut, midway * 0.01f + 1e-5f, "$move over $length s starts slower than it travels")
    }

    @Test
    @Spec("camera.cinematic.steady-moves")
    fun `no move turns the camera quickly, however short the shot`() = forEachMovingShot { move, length ->
        var t = 0.0
        while (t < length) {
            val a = move.rig(YAW, PITCH, t, length)
            val b = move.rig(YAW, PITCH, t + STEP, length)
            val turn = (abs(b.yaw - a.yaw) + abs(b.pitch - a.pitch)) / STEP.toFloat()
            assertTrue(turn <= MAX_TURN, "$move over $length s turns at $turn degrees a second")
            t += STEP
        }
    }

    @Test
    fun `a longer shot covers more ground, but never beyond the move's reach`() {
        Move.entries.filter { it != Move.Static }.forEach { move ->
            val short = span(move, 2.0)
            val long = span(move, 8.0)
            val epic = span(move, 60.0)
            assertTrue(long > short, "$move should cover more ground in a long shot than a short one")
            assertEquals(span(move, 120.0), epic, 1e-4f, "$move should stop growing once it reaches its limit")
        }
    }

    @Test
    fun `the planned framing sits in the middle of the move`() = forEachMovingShot { move, length ->
        val start = move.rig(YAW, PITCH, 0.0, length)
        val end = move.rig(YAW, PITCH, length, length)
        val middle = move.rig(YAW, PITCH, length / 2, length)
        listOf(RigFrame::yaw, RigFrame::lateral, RigFrame::vertical).forEach { part ->
            assertEquals(
                (part(start) + part(end)) / 2,
                part(middle),
                1e-3f,
                "$move over $length s should pass through its planned framing halfway"
            )
        }
    }

    @Test
    fun `the last shot of the film comes to rest`() = forEachMovingShot { move, length ->
        val atCut = speed(move, length, 0.0, settles = true)
        val atEnd = speed(move, length, length - STEP, settles = true)
        assertTrue(atCut > 0f, "$move should still be moving when the closing shot begins")
        assertTrue(atEnd < atCut * 0.05f, "$move should have slowed to a stop by the end of the closing shot")
    }

    /** How much the rig is changing [elapsed] seconds into the shot, in its own units per second. */
    private fun speed(move: Move, length: Double, elapsed: Double, settles: Boolean = false): Float {
        val a = move.rig(YAW, PITCH, elapsed, length, settles)
        val b = move.rig(YAW, PITCH, elapsed + STEP, length, settles)
        return distance(a, b) / STEP.toFloat()
    }

    /** How far the rig travels over a whole shot of [length] seconds. */
    private fun span(move: Move, length: Double): Float =
        distance(move.rig(YAW, PITCH, 0.0, length), move.rig(YAW, PITCH, length, length))

    private fun distance(a: RigFrame, b: RigFrame): Float =
        abs(b.yaw - a.yaw) + abs(b.pitch - a.pitch) + abs(b.distanceScale - a.distanceScale) * 100 +
            abs(b.lateral - a.lateral) * 100 + abs(b.vertical - a.vertical) * 100

    private fun forEachMovingShot(block: (Move, Double) -> Unit) =
        Move.entries.filter { it != Move.Static }.forEach { move -> LENGTHS.forEach { block(move, it) } }

    private companion object {
        const val YAW = 15f
        const val PITCH = 10f
        const val STEP = 1.0 / 60

        /** The fastest any move may swing the camera's direction, in degrees per second. */
        const val MAX_TURN = 6f

        val LENGTHS = listOf(1.0, 1.5, 3.0, 6.0, 10.0)
    }
}
