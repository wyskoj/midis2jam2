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
package org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The timing of roaming mallets' moves around each other. On screen, two mallets passing through each other is the
 * glitch to avoid: either the one in the way gets out of it in time, or the other jumps over it.
 */
class MalletChoreographerTest {

    @Test
    fun `in a descending run played hand to hand, the leading mallet gets out of the way`() {
        // R strikes C5, L strikes B4, R strikes A4, L strikes G4 ... a quarter of a second apart.
        val run = listOf(72, 71, 69, 67, 65, 64, 62, 60)
        val keyframes = listOf(
            run.withIndex().filter { it.index % 2 == 1 }.map { frame(it.index * 0.25, it.value, LEFT) },
            run.withIndex().filter { it.index % 2 == 0 }.map { frame(it.index * 0.25, it.value, RIGHT) },
        )

        val paths = MalletChoreographer.choreograph(keyframes)

        assertTrue(paths.all { path -> path.timings.all { it.jumps.isEmpty() } }, "Nothing should need to jump")
        var time = 0.0
        while (time <= run.size * 0.25) {
            val left = paths[0].positionAt(time).x
            val right = paths[1].positionAt(time).x
            assertTrue(left < right, "At ${time}s the left mallet (x=$left) is not left of the right one (x=$right)")
            time += 0.005
        }
    }

    @Test
    fun `a mallet that must pass over a still one jumps over it`() {
        // L sits on E4 the whole time; R reaches past it to C4 and back.
        val keyframes = listOf(
            listOf(frame(0.0, 64, LEFT), frame(2.0, 64, LEFT)),
            listOf(frame(0.0, 67, RIGHT), frame(0.5, 60, RIGHT), frame(1.0, 67, RIGHT)),
        )

        val paths = MalletChoreographer.choreograph(keyframes)

        assertTrue(paths[0].timings.all { it.jumps.isEmpty() }, "The still mallet shouldn't jump")
        val jumps = paths[1].timings.flatMap { it.jumps }
        assertEquals(2, jumps.size, "The moving mallet should jump on the way over and on the way back")

        for (jump in jumps) {
            val lifted = paths[1].positionAt(jump)
            val flat = paths[1].positionAt(jump, lifted = false)
            assertTrue(lifted.y - flat.y > 2.0, "At $jump s the mallet should be well clear, but it's at $lifted")
        }
    }

    @Test
    fun `choreography never moves a mallet off the bar it strikes`() {
        val run = listOf(72, 71, 69, 67, 65, 64, 62, 60)
        val keyframes = listOf(
            run.withIndex().filter { it.index % 2 == 1 }.map { frame(it.index * 0.1, it.value, LEFT) },
            run.withIndex().filter { it.index % 2 == 0 }.map { frame(it.index * 0.1, it.value, RIGHT) },
        )

        val paths = MalletChoreographer.choreograph(keyframes)

        keyframes.zip(paths).forEach { (frames, path) ->
            frames.forEach { assertEquals(it.point, path.positionAt(it.time)) }
        }
    }

    @Test
    fun `mallets that never meet keep the usual timing`() {
        val keyframes = listOf(
            listOf(frame(0.0, 40, LEFT), frame(0.5, 43, LEFT)),
            listOf(frame(0.0, 80, RIGHT), frame(0.5, 84, RIGHT)),
        )

        val paths = MalletChoreographer.choreograph(keyframes)

        keyframes.zip(paths).forEach { (frames, path) ->
            assertEquals(MalletPath.defaultTimings(frames), path.timings)
        }
    }

    private companion object {
        const val LEFT = -0.175
        const val RIGHT = 0.175

        /** A strike on [note] at [time], offset to one side of the bar like the real mallets are. */
        fun frame(time: Double, note: Int, side: Double) =
            MalletKeyframe(time, Point3((note - 64) * 0.78 + side, 1.35, 0.0))
    }
}
