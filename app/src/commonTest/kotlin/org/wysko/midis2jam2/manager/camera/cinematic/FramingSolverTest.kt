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

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Box3
import org.wysko.midis2jam2.manager.camera.cinematic.framing.CameraPose
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Composition
import org.wysko.midis2jam2.manager.camera.cinematic.framing.FramingSolver
import org.wysko.midis2jam2.manager.camera.cinematic.framing.Occlusion
import org.wysko.midis2jam2.manager.camera.cinematic.framing.StageEnvelope
import com.jme3.math.FastMath
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The geometry that turns "frame the guitarist in a close-up from the left" into a camera position.
 *
 * The cinematic camera has no table of hand-placed angles: every shot is solved from the subject's bounds. If
 * this geometry is wrong, subjects are cropped, off-centre or behind the camera, for every shot of every song.
 */
class FramingSolverTest {

    @Test
    fun `every corner of the subject lands inside its share of the frame`() {
        compositions.forEach { composition ->
            angles.forEach { (yaw, pitch) ->
                val pose = FramingSolver.place(SUBJECT, yaw, pitch, FOV, ASPECT, composition)
                val limitX = composition.fill * (1 - abs(composition.anchorX))
                val limitY = composition.fill * (1 - abs(composition.anchorY))
                var tightest = 0f

                SUBJECT.corners.forEach { corner ->
                    val ndc = assertNotNull(
                        FramingSolver.project(corner, pose, ASPECT),
                        "A corner of the subject ended up behind the camera ($composition, yaw $yaw, pitch $pitch)"
                    )
                    val dx = abs(ndc.x - composition.anchorX) / limitX
                    val dy = abs(ndc.y - composition.anchorY) / limitY
                    assertTrue(
                        dx <= 1 + TOLERANCE && dy <= 1 + TOLERANCE,
                        "A corner at $ndc spills outside its share of the frame ($composition, yaw $yaw, pitch $pitch)"
                    )
                    tightest = maxOf(tightest, dx, dy)
                }
                assertEquals(
                    1f,
                    tightest,
                    0.01f,
                    "The subject should fill its share of the frame snugly ($composition, yaw $yaw, pitch $pitch)"
                )
            }
        }
    }

    @Test
    fun `the subject's centre lands on the composition's anchor`() {
        compositions.forEach { composition ->
            angles.forEach { (yaw, pitch) ->
                val pose = FramingSolver.place(SUBJECT, yaw, pitch, FOV, ASPECT, composition, distanceScale = 1.3f)
                val ndc = assertNotNull(FramingSolver.project(SUBJECT.center, pose, ASPECT))
                assertEquals(composition.anchorX, ndc.x, TOLERANCE, "Horizontal anchor missed at yaw $yaw")
                assertEquals(composition.anchorY, ndc.y, TOLERANCE, "Vertical anchor missed at pitch $pitch")
            }
        }
    }

    @Test
    fun `a yaw of zero films from the audience and positive pitch looks down`() {
        val pose = FramingSolver.place(SUBJECT, 0f, 20f, FOV, ASPECT, Composition())

        assertTrue(pose.location.z > SUBJECT.max.z, "A straight-on shot should stand on the audience (+Z) side")
        assertTrue(pose.location.y > SUBJECT.center.y, "A camera looking down should stand above its subject")
        assertTrue(pose.forward.y < 0f, "A positive pitch should look down")
    }

    @Test
    fun `a wider lens stands closer for the same framing`() {
        val wide = FramingSolver.place(SUBJECT, 15f, 10f, 70f, ASPECT, Composition(fill = 0.8f))
        val tele = FramingSolver.place(SUBJECT, 15f, 10f, 25f, ASPECT, Composition(fill = 0.8f))

        assertTrue(
            wide.location.distance(SUBJECT.center) < tele.location.distance(SUBJECT.center),
            "A wide lens should get close and a long lens stand back"
        )
    }

    @Test
    fun `aiming from anywhere puts the target on the anchor`() {
        val target = Vector3f(10f, 30f, -20f)
        listOf(
            Vector3f(0f, 50f, 100f),
            Vector3f(-80f, 10f, 40f),
            Vector3f(60f, 90f, 10f),
        ).forEach { eye ->
            listOf(0f to 0f, 1 / 3f to 0f, -1 / 3f to 0.2f).forEach { (ax, ay) ->
                val rotation = FramingSolver.aim(eye, target, FOV, ASPECT, ax, ay)
                val ndc = assertNotNull(FramingSolver.project(target, CameraPose(eye, rotation, FOV), ASPECT))
                assertEquals(ax, ndc.x, 0.01f, "From $eye the target should sit at x = $ax")
                assertEquals(ay, ndc.y, 0.01f, "From $eye the target should sit at y = $ay")
                assertEquals(0f, rotation.getRotationColumn(0).y, 1e-3f, "Aiming should never roll the camera")
            }
        }
    }

    @Test
    fun `yaw and pitch survive a round trip through a direction`() {
        angles.forEach { (yaw, pitch) ->
            val (y, p) = FramingSolver.anglesOf(FramingSolver.forward(yaw, pitch))
            assertEquals(yaw, y, 1e-3f)
            assertEquals(pitch, p, 1e-3f)
        }
    }

    @Test
    fun `an instrument between the camera and its subject blocks the view`() {
        val eye = Vector3f(0f, 20f, 100f)
        val blocker = Box3(Vector3f(0f, 20f, 50f), Vector3f(30f, 30f, 5f))

        assertEquals(0f, Occlusion.clearFraction(eye, SUBJECT, listOf(blocker)), "The view should be fully blocked")
        assertEquals(1f, Occlusion.clearFraction(eye, SUBJECT, emptyList()), "Nothing is in the way")

        val beside = Box3(Vector3f(200f, 20f, 50f), Vector3f(10f, 10f, 10f))
        assertEquals(1f, Occlusion.clearFraction(eye, SUBJECT, listOf(beside)), "A box off to the side is not in the way")
    }

    @Test
    fun `a camera inside another instrument is pushed out through the nearest face`() {
        val box = Box3(Vector3f(0f, 10f, 0f), Vector3f(10f, 10f, 10f))

        val pushed = box.pushOutside(Vector3f(2f, 12f, 8f), margin = 0.5f)
        assertEquals(Vector3f(2f, 12f, 10.5f), pushed, "The nearest face is the front (+Z), two units away")
        assertFalse(box.contains(pushed), "The camera should end up outside the instrument")

        val outside = Vector3f(30f, 10f, 0f)
        assertEquals(outside, box.pushOutside(outside, margin = 0.5f), "A camera already outside stays put")
    }

    @Test
    fun `a turned box contains, and pushes out, along its own axes`() {
        val box = Box3(Vector3f(0f, 10f, 0f), Vector3f(20f, 5f, 2f), TURNED)

        // Along the box's own length, at 45 degrees across the world's axes.
        val alongLength = TURNED.mult(Vector3f(18f, 0f, 0f)).addLocal(box.center)
        assertTrue(box.contains(alongLength), "A point along the box's length should be inside it")
        // Inside the world-squared box around it, but off the side of the box itself.
        val offTheSide = TURNED.mult(Vector3f(0f, 0f, 5f)).addLocal(box.center)
        assertTrue(box.aligned.contains(offTheSide), "The world-squared box should take in the empty corner")
        assertFalse(box.contains(offTheSide), "A point off the side of a turned box is not inside it")

        val pushed = box.pushOutside(TURNED.mult(Vector3f(5f, 1f, 1.5f)).addLocal(box.center), margin = 0.5f)
        val expected = TURNED.mult(Vector3f(5f, 1f, 2.5f)).addLocal(box.center)
        assertTrue(pushed.distance(expected) < TOLERANCE, "The nearest face is the box's own front: $pushed")
    }

    @Test
    fun `a box fitted to a turned instrument turns with it`() {
        val instrument = Box3(Vector3f(10f, 20f, -30f), Vector3f(25f, 4f, 6f), TURNED)
        val squared = instrument.aligned
        val fitted = assertNotNull(Box3.fit(instrument.corners))

        instrument.corners.forEach {
            assertTrue(fitted.contains(it, margin = TOLERANCE), "The fitted box should hold every corner")
        }
        assertTrue(fitted.isUpright, "An instrument turned only about the vertical should get an upright box")
        val volume = fitted.extent.x * fitted.extent.y * fitted.extent.z
        assertEquals(25f * 4f * 6f, volume, 25f * 4f * 6f * 0.02f, "The fitted box should fit the instrument snugly")
        assertTrue(
            volume < squared.extent.x * squared.extent.y * squared.extent.z * 0.5f,
            "A turned box should be far snugger than one squared to the world"
        )
    }

    @Test
    fun `a sight line past the empty corner of a turned instrument is clear`() {
        // A long, thin instrument at 45 degrees, and a sight line running alongside it, through the empty corner of
        // the box squared to the world around it.
        val blocker = Box3(Vector3f(0f, 20f, 50f), Vector3f(30f, 30f, 2f), TURNED)
        val eye = blocker.toWorld(Vector3f(60f, 0f, 10f))
        val target = blocker.toWorld(Vector3f(-60f, 0f, 10f))

        assertTrue(Occlusion.segmentHits(eye, target, blocker.aligned), "The squared box should be in the way")
        assertFalse(Occlusion.segmentHits(eye, target, blocker), "The instrument itself should not be in the way")
    }

    @Test
    fun `the envelope keeps the camera on the audience side and off the floor`() {
        val envelope = StageEnvelope(Box3(Vector3f(0f, 30f, 0f), Vector3f(80f, 30f, 60f)))

        assertTrue(envelope.allows(Vector3f(0f, 60f, 150f)), "Straight out front is always allowed")
        val behind = Vector3f(0f, 60f, -150f)
        assertFalse(envelope.allows(behind), "Behind the band breaks the 180-degree rule")
        val fixed = envelope.constrain(behind)
        assertTrue(envelope.allows(fixed), "Constraining should produce an allowed location, but gave $fixed")

        val underground = envelope.constrain(Vector3f(20f, -40f, 120f))
        assertTrue(underground.y > envelope.floorY, "The camera should be lifted above the floor")
    }

    private companion object {
        const val FOV = 45f
        const val ASPECT = 16f / 9f
        const val TOLERANCE = 1e-3f

        val SUBJECT = Box3(Vector3f(5f, 20f, -10f), Vector3f(12f, 8f, 6f))

        /** Turned 45 degrees about the vertical, the way the piano stands on stage. */
        val TURNED: Quaternion = Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y)

        val compositions = listOf(
            Composition(0f, 0f, 0.6f),
            Composition(1 / 3f, 0f, 0.85f),
            Composition(-1 / 3f, 0.1f, 0.4f),
            Composition(0f, 0f, 0.95f),
        )

        val angles = listOf(0f to 0f, 30f to 15f, -45f to 35f, 60f to -10f, 10f to 70f)
    }
}
