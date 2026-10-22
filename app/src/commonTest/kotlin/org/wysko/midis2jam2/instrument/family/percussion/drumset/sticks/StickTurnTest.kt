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

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How a stick turns from one piece's angle to another's as it travels: it ends exactly at each piece's angle, and on
 * the way it points along the shortest arc between them, instead of sweeping round as a straight slerp would.
 */
class StickTurnTest {

    @Test
    fun `the stick starts and ends at each piece's angle`() {
        assertSame(FROM, StickTurn.between(FROM, TO, 0f, AXIS))
        assertSame(TO, StickTurn.between(FROM, TO, 1f, AXIS))
    }

    @Test
    fun `the stick points along the shortest arc between the two pieces`() {
        val start = FROM.mult(AXIS)
        val end = TO.mult(AXIS)
        val total = start.angleBetween(end)
        for (step in 1..9) {
            val t = step / 10f
            val pointing = StickTurn.between(FROM, TO, t, AXIS).mult(AXIS)
            assertEquals(t * total, start.angleBetween(pointing), 1e-3f, "At $t, along the arc")
            assertEquals(0f, start.cross(end).normalize().dot(pointing), 1e-3f, "At $t, in the plane of the arc")
        }
    }

    @Test
    fun `the tip travels less than it would turning straight there`() {
        fun tipTravel(turn: (Float) -> Quaternion) =
            (0 until 100).sumOf { turn((it + 1) / 100f).mult(AXIS).distance(turn(it / 100f).mult(AXIS)).toDouble() }

        val slerp = tipTravel { t -> Quaternion().apply { slerp(FROM, TO, t) } }
        val turn = tipTravel { t -> StickTurn.between(FROM, TO, t, AXIS) }
        assertTrue(turn < slerp * 0.9, "The tip should travel less ($turn) than a slerp's ($slerp)")
    }

    private companion object {
        /** The stick lies along Z, tilted up as it is at rest. */
        val AXIS: Vector3f = Quaternion().fromAngles(50 * FastMath.DEG_TO_RAD, 0f, 0f).mult(Vector3f.UNIT_Z)

        /** Roughly the snare's angle and a floor tom's, which are turned a long way from each other. */
        val FROM: Quaternion = Quaternion().fromAngles(0.17f, 1.4f, -0.17f)
        val TO: Quaternion = Quaternion().fromAngles(0.26f, -1.57f, 0f)

        fun assertSame(expected: Quaternion, actual: Quaternion) {
            val dot = expected.dot(actual)
            assertEquals(1f, kotlin.math.abs(dot), 1e-4f, "Expected $expected, got $actual")
        }
    }
}
