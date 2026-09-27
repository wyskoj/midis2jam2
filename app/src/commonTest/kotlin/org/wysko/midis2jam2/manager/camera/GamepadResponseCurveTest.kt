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

package org.wysko.midis2jam2.manager.camera

import com.jme3.renderer.Camera
import org.wysko.midis2jam2.testing.Spec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The shaping applied to a gamepad stick before it moves the camera.
 *
 * A stick rests slightly off centre and never quite reaches its corners, so the raw value is
 * passed through a deadzone and a curve. Getting this wrong is the difference between a
 * camera that drifts on its own and one that cannot be nudged gently.
 */
class GamepadResponseCurveTest {

    @Test
    @Spec("gamepad.deadzone")
    fun `a stick resting near centre produces no movement`() {
        listOf(0f, 0.05f, -0.05f, 0.19f, -0.19f).forEach {
            assertEquals(0f, curve(it), "A deviation of $it is inside the deadzone and should be ignored")
        }
    }

    @Test
    fun `a stick pushed past the deadzone produces movement`() {
        assertTrue(curve(0.5f) > 0f, "Half deflection produced no movement")
        assertTrue(curve(-0.5f) < 0f, "Half deflection produced no movement")
    }

    @Test
    fun `the curve preserves the direction the stick was pushed`() {
        listOf(0.3f, 0.5f, 0.8f, 1f).forEach {
            assertTrue(curve(it) > 0f, "Pushing the stick one way should move the camera that way")
            assertTrue(curve(-it) < 0f, "Pushing the stick the other way should reverse the movement")
            assertEquals(
                abs(curve(it)),
                abs(curve(-it)),
                TOLERANCE,
                "The curve should be symmetric about centre, but $it and ${-it} differ"
            )
        }
    }

    @Test
    fun `full deflection gives full speed`() {
        assertEquals(1f, curve(1f), TOLERANCE)
        assertEquals(-1f, curve(-1f), TOLERANCE)
    }

    @Test
    fun `a value beyond full deflection is clamped rather than amplified`() {
        assertEquals(1f, curve(1.5f), TOLERANCE)
        assertEquals(-1f, curve(-1.5f), TOLERANCE)
    }

    @Test
    fun `the response rises without dipping as the stick is pushed further`() {
        var previous = 0f
        var step = STICK_DEADZONE
        while (step <= 1f) {
            val current = curve(step)
            assertTrue(current >= previous, "The response dipped between ${step - 0.05f} and $step")
            previous = current
            step += 0.05f
        }
    }

    @Test
    @Spec("gamepad.response-curve")
    fun `small deflections are gentler than a straight line would be`() {
        // The whole point of the curve: just past the deadzone, the camera should crawl.
        val justPastDeadzone = STICK_DEADZONE + 0.1f
        val linear = (justPastDeadzone - STICK_DEADZONE) / (1f - STICK_DEADZONE)

        assertTrue(
            curve(justPastDeadzone) < linear,
            "A small push should be gentler than a linear response, for fine control"
        )
    }

    private companion object {
        const val TOLERANCE = 1.0e-5f

        /** A camera is all the stick shaping needs; no window and no real gamepad. */
        val subject = ExtendedJoystickFlyByCamera(Camera(WIDTH, HEIGHT))

        const val WIDTH = 640
        const val HEIGHT = 480

        fun curve(deviation: Float): Float = subject.applyDeadzoneAndCurve(deviation)
    }
}
