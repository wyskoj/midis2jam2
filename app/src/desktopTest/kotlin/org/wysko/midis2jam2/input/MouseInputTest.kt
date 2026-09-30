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

import org.wysko.midis2jam2.testing.withCamera
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.InputHarness
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Mouse control of the free camera, and the debug overlay.
 *
 * Mouse events reach the engine the same way key presses do, so these drive the real bindings
 * the free camera registers rather than calling its handlers directly.
 */
class MouseInputTest {

    @Test
    @Spec("camera.freecam.mouse.drag-rotates")
    fun `dragging with the left button rotates the camera`() {
        withCamera { performance, input ->
            val before = input.cameraPose()

            input.dragMouse(dx = 120, dy = 0)
            input.frames(SETTLE_FRAMES)

            val after = input.cameraPose()
            assertTrue(
                before.angleTo(after) > ROTATION_TOLERANCE,
                "Dragging should have turned the camera, but it still faces ${after.direction}"
            )
            performance.throwIfEngineFailed()
        }
    }

    @Test
    fun `dragging vertically rotates the camera too`() {
        withCamera { _, input ->
            val before = input.cameraPose()

            input.dragMouse(dx = 0, dy = 90)
            input.frames(SETTLE_FRAMES)

            assertTrue(
                before.angleTo(input.cameraPose()) > ROTATION_TOLERANCE,
                "Dragging up and down should have turned the camera"
            )
        }
    }

    @Test
    fun `moving the mouse without holding a button does not rotate the camera`() {
        withCamera { _, input ->
            val before = input.cameraPose()

            input.moveMouse(dx = 150, dy = 120)
            input.frames(SETTLE_FRAMES)

            assertTrue(
                before.angleTo(input.cameraPose()) < ROTATION_TOLERANCE,
                "The camera turned without the button being held down"
            )
        }
    }

    @Test
    @Spec("camera.freecam.mouse.scroll-zooms")
    fun `scrolling the wheel zooms the camera`() {
        withCamera { performance, input ->
            val before = input.cameraPose()

            input.scroll(clicks = 4)
            input.frames(SETTLE_FRAMES)
            val zoomedIn = input.cameraPose()

            assertTrue(
                zoomedIn.fieldOfView != before.fieldOfView,
                "Scrolling should have zoomed the camera, but the field of view is still " +
                    "${zoomedIn.fieldOfView}"
            )

            // Scrolling the other way should head back toward where it started.
            input.scroll(clicks = -4)
            input.frames(SETTLE_FRAMES)
            val zoomedBack = input.cameraPose()

            assertTrue(
                kotlin.math.abs(zoomedBack.fieldOfView - before.fieldOfView) <
                    kotlin.math.abs(zoomedIn.fieldOfView - before.fieldOfView),
                "Scrolling back should undo the zoom, but the field of view went from " +
                    "${before.fieldOfView} to ${zoomedIn.fieldOfView} to ${zoomedBack.fieldOfView}"
            )
            performance.throwIfEngineFailed()
        }
    }

    /**
     * Regression test for #410: zooming out far enough pushed the free camera's field of view past
     * 180 degrees, where the engine can no longer represent it, and the next camera update crashed
     * with "Field of view must be greater than 0".
     */
    @Test
    fun `zooming out as far as the wheel goes does not crash the free camera`() {
        withCamera(smooth = false) { performance, input -> zoomOutToTheLimit(performance, input) }
    }

    /** As above, for smooth freecam, which eases the field of view toward the target instead. */
    @Test
    fun `zooming out as far as the wheel goes does not crash the smooth free camera`() {
        withCamera(smooth = true) { performance, input -> zoomOutToTheLimit(performance, input) }
    }

    private fun zoomOutToTheLimit(performance: HeadlessPerformance, input: InputHarness) {
        val before = input.cameraPose().fieldOfView
        repeat(ZOOM_OUT_CLICKS) { input.scroll(clicks = -1) }
        input.frames(ZOOM_CATCH_UP_FRAMES)

        performance.throwIfEngineFailed()
        val after = input.cameraPose().fieldOfView
        assertTrue(after > before, "Scrolling this way should zoom out, but the field of view went from $before to $after")
        assertTrue(
            after < MAX_FIELD_OF_VIEW,
            "After scrolling out $ZOOM_OUT_CLICKS clicks the field of view should still be a usable angle, but it is $after"
        )
    }

    private companion object {

        const val SETTLE_FRAMES = 12
        const val ROTATION_TOLERANCE = 0.01f
        const val MAX_FIELD_OF_VIEW = 180f

        /** Enough clicks to take the default field of view well past [MAX_FIELD_OF_VIEW]. */
        const val ZOOM_OUT_CLICKS = 150

        /** Frames for smooth freecam to catch up with a zoom; it covers a few percent per frame. */
        const val ZOOM_CATCH_UP_FRAMES = 180

        fun withCamera(smooth: Boolean = false, block: (HeadlessPerformance, InputHarness) -> Unit) {
            val settings = AppSettings().withCamera { copy(isSmoothFreecam = smooth) }
            HeadlessPerformance.start(MidiFixtures.theWholeBand(), settings = settings)
                .use { performance ->
                    val input = InputHarness(performance)
                    input.frames(SETTLE_FRAMES)
                    block(performance, input)
                }
        }
    }
}
