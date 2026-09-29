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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The playback keys while recording.
 *
 * A recording's audio is rendered separately, straight through, so pausing or seeking the picture would leave it out
 * of step with the sound for the rest of the video. The keys must do nothing.
 */
class RecordingInputTest {

    @Test
    @Spec("record.input.playback-locked")
    fun `play, pause and seek keys are ignored while recording`() {
        HeadlessPerformance.start(MidiFixtures.theWholeBand(), isRecording = true).use { performance ->
            val input = InputHarness(performance)
            input.frames(SETTLE_FRAMES)
            val playback = assertNotNull(performance.app.stateManager.getState(PlaybackManager::class.java))

            input.tap(KeyInput.KEY_SPACE)
            assertTrue(playback.isPlaying, "Space paused a recording")

            val before = playback.time
            input.tap(KeyInput.KEY_RIGHT)
            input.tap(KeyInput.KEY_HOME)
            val after = playback.time
            assertTrue(
                after >= before && after - before < 5.seconds,
                "Seek keys moved a recording's clock from $before to $after"
            )
            assertTrue(performance.sequencer.seeks.isEmpty(), "A recording was asked to seek")
            performance.throwIfEngineFailed()
        }
    }

    private companion object {
        const val SETTLE_FRAMES = 12
    }
}
