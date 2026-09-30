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

package org.wysko.midis2jam2.input

import org.wysko.midis2jam2.testing.withControls
import org.wysko.midis2jam2.testing.withCamera
import com.jme3.input.KeyInput
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.InputHarness
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The Shift and Ctrl speed modifiers for the free camera.
 *
 * These stopped working at some point and had to be restored by hand. The failure mode is
 * quiet: the camera still moves, just always at the same speed, so nothing looks broken
 * unless you already know how fast it should have been.
 *
 * Frame timing comes from the wall clock, so distances are compared as ratios with generous
 * bounds. That is enough to catch a modifier that does nothing, which is what went wrong.
 */
class CameraSpeedModifierTest {

    @Test
    @Spec("camera.freecam.speed.default", "camera.freecam.speed.fast", "camera.freecam.speed.slow")
    fun `the modifier keys change how fast the free camera moves`() {
        withFreeCamera { input ->
            val normal = input.distanceTravelledHoldingForward()
            val fast = input.distanceTravelledHoldingForward(modifier = KeyInput.KEY_LSHIFT)
            val slow = input.distanceTravelledHoldingForward(modifier = KeyInput.KEY_LCONTROL)

            assertTrue(normal > 0f, "The camera did not move at all at the normal speed")
            assertTrue(
                fast > normal * FAST_AT_LEAST,
                "Holding Shift should move the camera faster, but it went $fast against $normal at normal speed"
            )
            assertTrue(
                slow < normal * SLOW_AT_MOST,
                "Holding Ctrl should move the camera slower, but it went $slow against $normal at normal speed"
            )
        }
    }

    @Test
    @Spec("camera.freecam.speed.release-restores-default")
    fun `releasing a modifier restores the normal speed`() {
        withFreeCamera { input ->
            val normal = input.distanceTravelledHoldingForward()

            // Hold and release the modifier, then move again with nothing held.
            input.press(KeyInput.KEY_LSHIFT)
            input.release(KeyInput.KEY_LSHIFT)
            val afterRelease = input.distanceTravelledHoldingForward()

            assertTrue(
                afterRelease < normal * FAST_AT_LEAST,
                "The fast speed outlived the Shift key: $afterRelease against $normal before it was pressed"
            )
        }
    }

    @Test
    @Spec("camera.freecam.speed.sticky-toggle")
    fun `sticky modifiers stay on after the key is released`() {
        val sticky = AppSettings()
            .withCamera { copy(isSmoothFreecam = false) }
            .withControls { copy(isSpeedModifierKeysSticky = true) }

        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = sticky).use { performance ->
            val input = InputHarness(performance)
            input.frames(SETTLE_FRAMES)

            val normal = input.distanceTravelledHoldingForward()

            // In sticky mode the modifier toggles on release and then stays on.
            input.tap(KeyInput.KEY_LSHIFT)
            val whileSticky = input.distanceTravelledHoldingForward()

            assertTrue(
                whileSticky > normal * FAST_AT_LEAST,
                "A sticky Shift should keep the camera fast after release, but it went " +
                    "$whileSticky against $normal"
            )

            // Tapping the same modifier again turns it back off.
            input.tap(KeyInput.KEY_LSHIFT)
            val afterSecondTap = input.distanceTravelledHoldingForward()

            assertTrue(
                afterSecondTap < whileSticky * SLOW_AT_MOST + normal,
                "Tapping a sticky modifier a second time should turn it off, but the camera " +
                    "still moved $afterSecondTap"
            )
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `the other modifier cancels a sticky one`() {
        val sticky = AppSettings()
            .withCamera { copy(isSmoothFreecam = false) }
            .withControls { copy(isSpeedModifierKeysSticky = true) }

        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = sticky).use { performance ->
            val input = InputHarness(performance)
            input.frames(SETTLE_FRAMES)

            val normal = input.distanceTravelledHoldingForward()

            input.tap(KeyInput.KEY_LSHIFT)
            input.tap(KeyInput.KEY_LCONTROL)
            val afterSwitching = input.distanceTravelledHoldingForward()

            assertTrue(
                afterSwitching < normal,
                "Tapping Ctrl after a sticky Shift should slow the camera down, but it moved " +
                    "$afterSwitching against $normal at the normal speed"
            )
        }
    }

    private companion object {

        const val SETTLE_FRAMES = 12
        const val HOLD_FRAMES = 20

        /** Fast is twice the normal speed in the app; half that is a safe floor. */
        const val FAST_AT_LEAST = 1.4f

        /** Slow is a tenth of the normal speed; half of normal is a safe ceiling. */
        const val SLOW_AT_MOST = 0.5f

        fun withFreeCamera(block: (InputHarness) -> Unit) {
            val settings = AppSettings().withCamera { copy(isSmoothFreecam = false) }
            HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = settings)
                .use { performance ->
                    val input = InputHarness(performance)
                    input.frames(SETTLE_FRAMES)
                    block(input)
                    performance.throwIfEngineFailed()
                }
        }

        /** Holds the forward key for a fixed number of frames and reports how far the camera went. */
        fun InputHarness.distanceTravelledHoldingForward(modifier: Int? = null): Float {
            val before = cameraPose()
            modifier?.let { press(it) }
            press(KeyInput.KEY_W)
            frames(HOLD_FRAMES)
            release(KeyInput.KEY_W)
            modifier?.let { release(it) }
            return before.distanceTo(cameraPose())
        }
    }
}
