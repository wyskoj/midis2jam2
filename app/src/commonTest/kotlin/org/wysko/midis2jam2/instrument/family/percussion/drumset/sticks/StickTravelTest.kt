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

import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletKeyframe
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.MalletPath
import org.wysko.midis2jam2.instrument.family.chromaticpercussion.mallets.Point3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How a stick times its move between two pieces: on a long gap it waits on the piece it struck and sets off only in
 * time to get to the next one, rather than drifting the whole way, and a longer move sets off sooner. On a short gap
 * it moves as it always has.
 */
class StickTravelTest {

    @Test
    fun `on a long gap the stick waits, then travels in time to arrive`() {
        val keyframes = listOf(MalletKeyframe(0.0, SNARE), MalletKeyframe(3.0, TOM))
        val travel = StickTravel(keyframes, SPEED, MIN_TRAVEL)
        val path = MalletPath(keyframes)
        val arrive = MalletPath.defaultTiming(keyframes[0], keyframes[1]).arrive

        assertEquals(SNARE, path.positionAt(travel.pathTime(1.5), lifted = false), "Halfway through, still waiting")
        val departs = arrive - 10.0 / SPEED
        assertEquals(SNARE, path.positionAt(travel.pathTime(departs - 0.01), lifted = false), "Just before setting off")
        assertTrue(path.positionAt(travel.pathTime(departs + 0.05), lifted = false).x > 0.0, "Under way")
        assertEquals(arrive, travel.pathTime(arrive), 1e-9, "Over the tom when the wind-up starts")
        assertEquals(2.9, travel.pathTime(2.9), 1e-9, "Winding up as usual")
    }

    @Test
    fun `a longer move sets off sooner`() {
        val near = StickTravel(listOf(MalletKeyframe(0.0, SNARE), MalletKeyframe(3.0, TOM)), SPEED, MIN_TRAVEL)
        val far = StickTravel(listOf(MalletKeyframe(0.0, SNARE), MalletKeyframe(3.0, FAR)), SPEED, MIN_TRAVEL)
        val time = 2.4
        assertEquals(0.0, near.pathTime(time), 1e-9, "The near move hasn't started yet")
        assertTrue(far.pathTime(time) > 0.0, "The far move has")
    }

    @Test
    fun `on a short gap the stick moves as it always has`() {
        val travel = StickTravel(listOf(MalletKeyframe(0.0, SNARE), MalletKeyframe(0.25, TOM)), SPEED, MIN_TRAVEL)
        for (time in listOf(0.05, 0.1, 0.2)) assertEquals(time, travel.pathTime(time), 1e-9)
    }

    @Test
    fun `before the first strike and after the last, time is unchanged`() {
        val travel = StickTravel(listOf(MalletKeyframe(1.0, SNARE), MalletKeyframe(5.0, TOM)), SPEED, MIN_TRAVEL)
        assertEquals(0.5, travel.pathTime(0.5))
        assertEquals(6.0, travel.pathTime(6.0))
    }

    private companion object {
        const val SPEED = 40.0
        const val MIN_TRAVEL = 0.2
        val SNARE = Point3(0.0, 0.0, 0.0)
        val TOM = Point3(10.0, 0.0, 0.0)
        val FAR = Point3(40.0, 0.0, 0.0)
    }
}
