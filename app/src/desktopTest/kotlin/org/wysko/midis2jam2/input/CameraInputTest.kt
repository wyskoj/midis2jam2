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
import com.jme3.input.KeyInput
import com.jme3.math.Vector3f
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.manager.camera.CameraAngleCategory
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.CameraStateListener
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.InputHarness
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Drives the camera with simulated key presses, the way the documentation says a user does.
 *
 * Camera control is the part of the app most often touched and most easily broken: three of
 * the last four commits before this suite existed were fixes for camera input regressions
 * that shipped unnoticed.
 */
class CameraInputTest {

    @Test
    @Spec(
        "camera.freecam.key.forward",
        "camera.freecam.key.backward",
        "camera.freecam.key.left",
        "camera.freecam.key.right",
        "camera.freecam.key.up",
        "camera.freecam.key.down",
    )
    fun `the movement keys move the free camera the way the documentation says`() {
        withCamera { performance, input ->
            val origin = input.cameraPose()

            // Each key is checked against the camera's own axes, so the assertion holds
            // wherever the camera happens to be pointing.
            val forward = origin.direction.normalize()
            val left = performance.onEngineThread { performance.app.camera.left.clone() }.normalize()
            val up = performance.onEngineThread { performance.app.camera.up.clone() }.normalize()

            assertMovesAlong(input, KeyInput.KEY_W, forward, "W", "forward")
            assertMovesAlong(input, KeyInput.KEY_S, forward.negate(), "S", "backward")
            assertMovesAlong(input, KeyInput.KEY_A, left, "A", "left")
            assertMovesAlong(input, KeyInput.KEY_D, left.negate(), "D", "right")
            assertMovesAlong(input, KeyInput.KEY_Q, up, "Q", "up")
            assertMovesAlong(input, KeyInput.KEY_Z, up.negate(), "Z", "down")
        }
    }

    @Test
    @Spec(
        "camera.freecam.angle.1",
        "camera.freecam.angle.2",
        "camera.freecam.angle.3",
        "camera.freecam.angle.4",
        "camera.freecam.angle.5",
        "camera.freecam.angle.6",
    )
    fun `each camera key jumps to its predefined position`() {
        withCamera { _, input ->
            (1..6).forEach { category ->
                // Pressing the key for the category the camera is already on advances to that
                // category's alternate view, so each check starts from a different category.
                input.moveAwayFrom(category)

                val expected = expectedLocation(category, index = 0)
                input.tap(cameraKeyFor(category))
                input.frames(SETTLE_FRAMES)

                val actual = input.cameraPose().location
                assertTrue(
                    actual.distance(expected) < POSITION_TOLERANCE,
                    "Camera key $category should move the camera to $expected, but it went to $actual"
                )
            }
        }
    }

    @Test
    @Spec("camera.freecam.angle.repeat-press-alternates")
    fun `pressing the same camera key again switches to the alternate view`() {
        withCamera { _, input ->
            val category = CameraAngleCategory.categories.first { it.angles.size > 1 }

            // Arrive at the category from elsewhere, so the first press shows its main view.
            input.moveAwayFrom(category.category)

            input.tap(cameraKeyFor(category.category))
            input.frames(SETTLE_FRAMES)
            val first = input.cameraPose().location

            input.tap(cameraKeyFor(category.category))
            input.frames(SETTLE_FRAMES)
            val second = input.cameraPose().location

            assertTrue(
                first.distance(second) > POSITION_TOLERANCE,
                "Pressing camera key ${category.category} twice left the camera at $first"
            )
            assertTrue(
                second.distance(expectedLocation(category.category, index = 1)) < POSITION_TOLERANCE,
                "The second press should show the alternate view, but the camera is at $second"
            )
        }
    }

    @Test
    @Spec("camera.freecam.reset")
    fun `the reset key returns the camera to the default position`() {
        withCamera { _, input ->
            input.tap(KeyInput.KEY_4)
            input.frames(SETTLE_FRAMES)

            input.tap(KeyInput.KEY_GRAVE)
            input.frames(SETTLE_FRAMES)

            val actual = input.cameraPose().location
            val default = expectedLocation(category = 1, index = 0)
            assertTrue(
                actual.distance(default) < POSITION_TOLERANCE,
                "The reset key should return the camera to $default, but it is at $actual"
            )
        }
    }

    @Test
    @Spec("camera.modes.three", "camera.autocam.activate", "camera.slidecam.activate")
    fun `the mode keys switch between the three camera modes`() {
        withCamera { performance, input ->
            val observed = RecordingCameraStateListener()
            performance.onEngineThread {
                performance.app.stateManager.getState(CameraManager::class.java)
                    ?.registerCameraStateListener(observed)
            }

            input.tap(KeyInput.KEY_0)
            input.frames(SETTLE_FRAMES)
            assertTrue(observed.autoCamera > 0, "Key 0 did not activate the auto-cam")

            input.tap(KeyInput.KEY_9)
            input.frames(SETTLE_FRAMES)
            assertTrue(observed.rotatingCamera > 0, "Key 9 did not activate the slide camera")

            input.tap(KeyInput.KEY_1)
            input.frames(SETTLE_FRAMES)
            assertTrue(observed.freeCamera > 0, "A camera key did not return to the free camera")
        }
    }

    @Test
    @Spec("camera.autocam.repeat-press-advances")
    fun `pressing the auto-cam key again moves it somewhere else`() {
        withCamera { performance, input ->
            val observed = RecordingCameraStateListener()
            performance.onEngineThread {
                performance.app.stateManager.getState(CameraManager::class.java)
                    ?.registerCameraStateListener(observed)
            }

            input.tap(KeyInput.KEY_0)
            input.frames(SETTLE_FRAMES)
            val first = observed.autoCamera

            input.tap(KeyInput.KEY_0)
            input.frames(SETTLE_FRAMES)

            assertTrue(
                observed.autoCamera > first,
                "Pressing the auto-cam key again should move it, but nothing happened"
            )
        }
    }

    @Test
    @Spec("camera.slidecam.rotates-around-stage")
    fun `the slide camera keeps moving around the stage on its own`() {
        // A long song: the performance ends itself once the music runs out, and this test
        // watches the camera for several seconds.
        withLongSong { performance, input ->
            input.tap(KeyInput.KEY_9)

            // The slide camera eases in over several seconds before it is travelling at its
            // own pace, so the samples are taken after that, spaced by real time.
            input.letTimePass(SLIDE_SETTLE_MILLIS)
            val first = input.cameraPose()
            input.letTimePass(SLIDE_OBSERVATION_MILLIS)
            val second = input.cameraPose()
            input.letTimePass(SLIDE_OBSERVATION_MILLIS)
            val third = input.cameraPose()

            assertTrue(
                first.distanceTo(second) > SLIDE_MOVEMENT_TOLERANCE,
                "The slide camera should be circling the stage, but it has not moved"
            )
            assertTrue(
                second.distanceTo(third) > SLIDE_MOVEMENT_TOLERANCE,
                "The slide camera stopped after its opening move"
            )
            performance.throwIfEngineFailed()
        }
    }

    @Test
    @Spec("camera.autocam.highlights-playing-instruments")
    fun `the auto-cam moves itself as the music plays`() {
        withLongSong { performance, input ->
            input.tap(KeyInput.KEY_0)
            input.frames(SETTLE_FRAMES)

            val start = input.cameraPose()
            input.letTimePass()
            val later = input.cameraPose()

            assertTrue(
                start.distanceTo(later) > POSITION_TOLERANCE || start.angleTo(later) > ANGLE_TOLERANCE,
                "The auto-cam should follow the performance on its own, but the view never changed"
            )
            performance.throwIfEngineFailed()
        }
    }

    @Test
    @Spec("camera.settings.smooth-freecam")
    fun `smooth freecam eases the camera into position instead of snapping`() {
        val smooth = AppSettings().withCamera { copy(isSmoothFreecam = true) }

        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = smooth).use { performance ->
            val input = InputHarness(performance)
            input.frames(SETTLE_FRAMES)

            input.tap(KeyInput.KEY_4)
            input.frames(2)
            val justAfter = input.cameraPose().location

            input.frames(SETTLE_FRAMES * 4)
            val settled = input.cameraPose().location

            // With smoothing on, the camera is still travelling a frame or two after the key.
            assertTrue(
                justAfter.distance(settled) > POSITION_TOLERANCE,
                "With smooth freecam on, the camera should ease into position, but it arrived immediately"
            )
        }
    }

    @Test
    @Spec("camera.settings.start-autocam-with-song")
    fun `the auto-cam can be set to start with the song`() {
        val autoStart = AppSettings()
            .withCamera { copy(isStartAutocamWithSong = true, isSmoothFreecam = false) }

        HeadlessPerformance.start(MidiFixtures.theWholeBand(), settings = autoStart).use { performance ->
            val input = InputHarness(performance)
            input.frames(SETTLE_FRAMES)

            val start = input.cameraPose()
            input.letTimePass(AUTO_CAM_GUARANTEED_MOVE_MILLIS)
            val later = input.cameraPose()

            // The auto-cam is in charge from the first frame, so the view changes on its own.
            assertTrue(
                start.distanceTo(later) > POSITION_TOLERANCE || start.angleTo(later) > ANGLE_TOLERANCE,
                "With the auto-cam set to start with the song, the camera never moved by itself"
            )
        }
    }

    @Test
    @Spec("camera.settings.classic-autocam")
    fun `the classic auto-cam can be selected`() {
        val classic = AppSettings()
            .withCamera { copy(isClassicAutoCam = true, isSmoothFreecam = false) }

        HeadlessPerformance.start(MidiFixtures.theWholeBand(), settings = classic).use { performance ->
            val input = InputHarness(performance)
            input.frames(SETTLE_FRAMES)

            val observed = RecordingCameraStateListener()
            performance.onEngineThread {
                performance.app.stateManager.getState(CameraManager::class.java)
                    ?.registerCameraStateListener(observed)
            }

            input.tap(KeyInput.KEY_0)
            input.frames(SETTLE_FRAMES)

            assertTrue(observed.autoCamera > 0, "The classic auto-cam could not be activated")
            performance.throwIfEngineFailed()
        }
    }

    @Test
    @Spec("graphics.fov")
    fun `the field of view setting is applied to the camera`() {
        val narrow = AppSettings()
            .withCamera { copy(defaultFieldOfView = 30f, isSmoothFreecam = false) }

        HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = narrow).use { performance ->
            val input = InputHarness(performance)
            // A camera key hands control to the free camera, which owns the field of view.
            input.tap(KeyInput.KEY_1)
            input.frames(SETTLE_FRAMES)

            assertEquals(30f, input.cameraPose().fieldOfView, FOV_TOLERANCE)
        }
    }

    private companion object {

        /** Frames to let the camera arrive, generously, before asserting where it is. */
        const val SETTLE_FRAMES = 12

        const val POSITION_TOLERANCE = 0.5f
        const val ANGLE_TOLERANCE = 0.01f
        const val FOV_TOLERANCE = 0.01f

        /** How long a movement key is held before the distance is measured. */
        const val HOLD_FRAMES = 15

        /**
         * Runs [block] against a performance with camera smoothing off, so that a camera key
         * lands exactly on its configured position rather than easing towards it.
         */
        fun withCamera(block: (HeadlessPerformance, InputHarness) -> Unit) {
            val settings = AppSettings().withCamera { copy(isSmoothFreecam = false) }
            HeadlessPerformance.start(MidiFixtures.singleProgram(program = 0), settings = settings)
                .use { performance ->
                    val input = InputHarness(performance)
                    input.frames(SETTLE_FRAMES)
                    block(performance, input)
                    performance.throwIfEngineFailed()
                }
        }

        /**
         * Moves the camera to some category other than [category].
         *
         * Pressing a camera key while already on that category advances to its alternate
         * view, so a test that wants the main view has to approach from somewhere else.
         */
        fun InputHarness.moveAwayFrom(category: Int) {
            val other = (1..6).first { it != category }
            tap(cameraKeyFor(other))
            frames(SETTLE_FRAMES)
        }

        /** How long to wait when a test needs the engine's own clock to advance. */
        const val OBSERVATION_MILLIS = 700L

        /**
         * Long enough to make the standard auto-cam's first move deterministic.
         *
         * [StandardAutoCamPlugin] picks its first target at random: a 25% chance of a different
         * stage angle (always visible movement), or else an instrument angle - which, during the
         * 2-second intro ([org.wysko.midis2jam2.manager.INTRO]), has nothing visible to pick from
         * and silently falls back to the camera's own starting angle. A short wait would only
         * ever catch that first roll, so on the 75% of runs where it lands on the fallback, the
         * camera never appears to move - flaky by design, not by accident. Waiting out the plugin's
         * full re-roll cycle (finishing whatever move is underway, up to 3 seconds at its
         * one-third-per-second pace, then its 3-second waiting period) lands well past the intro,
         * by which point the drum kit alone is continuously visible, so the next roll - stage or
         * instrument - is guaranteed to land somewhere else.
         */
        const val AUTO_CAM_GUARANTEED_MOVE_MILLIS = 8000L

        /** The slide camera eases in slowly; give it time to reach its travelling pace. */
        const val SLIDE_SETTLE_MILLIS = 3000L
        const val SLIDE_OBSERVATION_MILLIS = 1500L

        /** The slide camera moves deliberately slowly, so it covers little ground per second. */
        const val SLIDE_MOVEMENT_TOLERANCE = 0.2f

        /**
         * Lets real time pass on the engine thread.
         *
         * Queued work can drain several items in a single frame, so counting queued calls is
         * not a way to measure elapsed time. Cameras that move by themselves are driven by
         * the engine clock, so these tests wait on that instead.
         */
        fun InputHarness.letTimePass(millis: Long = OBSERVATION_MILLIS) {
            frames(1)
            Thread.sleep(millis)
            frames(1)
        }

        /** Like [withCamera], but with a song long enough to watch the camera for a while. */
        fun withLongSong(block: (HeadlessPerformance, InputHarness) -> Unit) {
            val settings = AppSettings().withCamera { copy(isSmoothFreecam = false) }
            HeadlessPerformance.start(MidiFixtures.theWholeBand(), settings = settings)
                .use { performance ->
                    val input = InputHarness(performance)
                    input.frames(SETTLE_FRAMES)
                    block(performance, input)
                }
        }

        fun cameraKeyFor(category: Int): Int = when (category) {
            1 -> KeyInput.KEY_1
            2 -> KeyInput.KEY_2
            3 -> KeyInput.KEY_3
            4 -> KeyInput.KEY_4
            5 -> KeyInput.KEY_5
            6 -> KeyInput.KEY_6
            else -> error("There is no camera key for category $category")
        }

        /** Where the bundled camera angles say category [category] should put the camera. */
        fun expectedLocation(category: Int, index: Int): Vector3f {
            val angles = assertNotNull(
                CameraAngleCategory.categories.firstOrNull { it.category == category },
                "camera_angles.yaml has no category $category"
            ).angles
            return angles[index].location
        }

        /** Holds [key] and asserts the camera travelled along [axis]. */
        fun assertMovesAlong(input: InputHarness, key: Int, axis: Vector3f, keyName: String, meaning: String) {
            val before = input.cameraPose()
            input.press(key)
            input.frames(HOLD_FRAMES)
            input.release(key)
            val after = input.cameraPose()

            val travelled = after.location.subtract(before.location)
            assertTrue(
                travelled.length() > POSITION_TOLERANCE,
                "Holding $keyName did not move the camera at all"
            )
            assertTrue(
                travelled.normalize().dot(axis) > MOVEMENT_AGREEMENT,
                "$keyName should move the camera $meaning (along $axis), but it moved $travelled"
            )
        }

        /** How closely the travel direction must agree with the expected axis. */
        const val MOVEMENT_AGREEMENT = 0.9f
    }

    /** Records which camera modes were switched to, in order. */
    private class RecordingCameraStateListener : CameraStateListener {
        var freeCamera = 0
            private set
        var autoCamera = 0
            private set
        var rotatingCamera = 0
            private set

        override fun onFreeCameraEnabled() {
            freeCamera++
        }

        override fun onAutoCameraEnabled() {
            autoCamera++
        }

        override fun onRotatingCameraEnabled() {
            rotatingCamera++
        }
    }
}
