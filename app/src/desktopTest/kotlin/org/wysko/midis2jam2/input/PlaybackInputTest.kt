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

import com.jme3.input.KeyInput
import org.wysko.midis2jam2.manager.PlaybackManager
import org.wysko.midis2jam2.testing.HeadlessPerformance
import org.wysko.midis2jam2.testing.InputHarness
import org.wysko.midis2jam2.testing.MidiFixtures
import org.wysko.midis2jam2.testing.Spec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The playback keys, checked against the times the documentation quotes.
 *
 * Playback is paused before each measurement so the clock holds still; otherwise the engine's
 * own frame loop would be advancing it while the assertion is being made.
 */
class PlaybackInputTest {

    @Test
    @Spec("playback.play-pause")
    fun `space toggles play and pause`() {
        withPlayback { _, input, playback ->
            assertTrue(playback.isPlaying, "A performance should begin playing")

            input.tap(KeyInput.KEY_SPACE)
            assertTrue(!playback.isPlaying, "Space did not pause playback")

            input.tap(KeyInput.KEY_SPACE)
            assertTrue(playback.isPlaying, "Space did not resume playback")
        }
    }

    @Test
    @Spec("playback.seek.forward")
    fun `the right arrow seeks forward by ten seconds`() {
        withPlayback { performance, input, playback ->
            input.tap(KeyInput.KEY_SPACE)
            performance.onEngineThread { playback.seek(20.seconds) }

            val before = playback.time
            input.tap(KeyInput.KEY_RIGHT)

            assertEquals(before + 10.seconds, playback.time, "The right arrow should seek forward ten seconds")
        }
    }

    @Test
    @Spec("playback.seek.backward")
    fun `the left arrow seeks backward by ten seconds`() {
        withPlayback { performance, input, playback ->
            input.tap(KeyInput.KEY_SPACE)
            performance.onEngineThread { playback.seek(20.seconds) }

            val before = playback.time
            input.tap(KeyInput.KEY_LEFT)

            assertEquals(before - 10.seconds, playback.time, "The left arrow should seek backward ten seconds")
        }
    }

    @Test
    fun `seeking backward never goes past the beginning`() {
        withPlayback { performance, input, playback ->
            input.tap(KeyInput.KEY_SPACE)
            performance.onEngineThread { playback.seek(Duration.ZERO) }

            input.tap(KeyInput.KEY_LEFT)

            assertTrue(
                playback.time >= Duration.ZERO,
                "Seeking backward from the start went to ${playback.time}"
            )
        }
    }

    @Test
    @Spec("playback.restart")
    fun `the home key restarts the song`() {
        withPlayback { performance, input, playback ->
            input.tap(KeyInput.KEY_SPACE)
            performance.onEngineThread { playback.seek(30.seconds) }

            input.tap(KeyInput.KEY_HOME)

            assertEquals(Duration.ZERO, playback.time, "Home should return playback to the beginning")
        }
    }

    @Test
    fun `seeking tells the sequencer where to go`() {
        withPlayback { performance, input, playback ->
            input.tap(KeyInput.KEY_SPACE)
            performance.onEngineThread { playback.seek(20.seconds) }
            performance.sequencer.seeks.clear()

            input.tap(KeyInput.KEY_RIGHT)

            assertTrue(
                performance.sequencer.seeks.isNotEmpty(),
                "Seeking moved the animation clock but never told the sequencer, so the " +
                    "sound would drift out of step with the picture"
            )
            assertEquals(30.seconds, performance.sequencer.seeks.last())
        }
    }

    @Test
    @Spec("playback.seek.resumes")
    fun `playback carries on from where a seek lands`() {
        withPlayback { performance, input, playback ->
            // The test sequencer finishes moving before it even returns, the quickest a sequencer can. Playback
            // must not mistake that for a seek that never finishes.
            performance.onEngineThread { playback.seek(5.seconds) }
            input.frames(SETTLE_FRAMES)

            assertTrue(playback.isPlaying, "Seeking shouldn't pause playback")
            assertTrue(
                playback.time > 5.seconds,
                "After seeking to 5 s, playback should carry on from there, but it is stuck at ${playback.time}"
            )
        }
    }

    @Test
    fun `pausing stops the sequencer as well as the clock`() {
        withPlayback { _, input, _ ->
            input.tap(KeyInput.KEY_SPACE)
            input.frames(4)
        }
    }

    private companion object {

        const val SETTLE_FRAMES = 12

        fun withPlayback(block: (HeadlessPerformance, InputHarness, PlaybackManager) -> Unit) {
            HeadlessPerformance.start(MidiFixtures.theWholeBand()).use { performance ->
                val input = InputHarness(performance)
                input.frames(SETTLE_FRAMES)

                val playback = assertNotNull(
                    performance.app.stateManager.getState(PlaybackManager::class.java),
                    "No playback manager was attached"
                )
                block(performance, input, playback)
                performance.throwIfEngineFailed()
            }
        }
    }
}
