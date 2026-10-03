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

import com.jme3.math.Vector3f
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Box3
import org.wysko.midis2jam2.manager.camera.cinematic.framing.CameraPose
import org.wysko.midis2jam2.manager.camera.cinematic.framing.FramingSolver
import org.wysko.midis2jam2.manager.camera.cinematic.framing.ShotClarity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Whether a shot reads: can the viewer tell what it is about, and does a cut into it look like a cut.
 *
 * The camera uses these judgements to move away from a planned angle when another instrument stands in front of
 * the subject, or when the cut from the previous shot would barely change the picture.
 */
class ShotClarityTest {

    @Test
    fun `a subject in plain view is fully prominent`() {
        assertEquals(1f, ShotClarity.prominence(pose, SUBJECT, emptyList(), ASPECT), 1e-4f)

        val behind = Box3(Vector3f(0f, 20f, -80f), Vector3f(30f, 30f, 5f))
        assertEquals(
            1f,
            ShotClarity.prominence(pose, SUBJECT, listOf(behind), ASPECT),
            1e-4f,
            "An instrument behind the subject doesn't hide it"
        )
    }

    @Test
    fun `a nearer instrument in front of the subject hides it`() {
        val inFront = Box3(Vector3f(0f, 20f, 30f), Vector3f(30f, 30f, 2f))
        assertEquals(0f, ShotClarity.prominence(pose, SUBJECT, listOf(inFront), ASPECT), 1e-4f)

        val halfInFront = Box3(Vector3f(-20f, 20f, 30f), Vector3f(20f, 40f, 2f))
        val partly = ShotClarity.prominence(pose, SUBJECT, listOf(halfInFront), ASPECT)
        assertTrue(partly in 0.2f..0.8f, "An instrument covering part of the subject should hide part of it: $partly")
    }

    @Test
    fun `a cut that barely turns or moves the camera is a jump`() {
        val target = SUBJECT.center
        val nudged = CameraPose(
            pose.location.add(3f, 0f, 0f),
            FramingSolver.aim(pose.location.add(3f, 0f, 0f), target.add(4f, 0f, 0f), FOV, ASPECT),
            FOV,
        )
        assertFalse(ShotClarity.isDistinctCut(pose, nudged, target), "A small nudge is a jump cut")

        val turned = FramingSolver.place(SUBJECT, 35f, 10f, FOV, ASPECT, composition)
        assertTrue(ShotClarity.isDistinctCut(pose, turned, target), "A 35-degree change of angle is a cut")

        val closer = FramingSolver.place(SUBJECT, 0f, 10f, FOV, ASPECT, composition, distanceScale = 0.45f)
        assertTrue(ShotClarity.isDistinctCut(pose, closer, target), "Moving in to half the distance is a cut")
    }

    private companion object {
        const val FOV = 45f
        const val ASPECT = 16f / 9f
        val SUBJECT = Box3(Vector3f(0f, 20f, 0f), Vector3f(8f, 8f, 8f))
        val composition = org.wysko.midis2jam2.manager.camera.cinematic.framing.Composition(fill = 0.6f)
        val pose: CameraPose = FramingSolver.place(SUBJECT, 0f, 10f, FOV, ASPECT, composition)
    }
}
