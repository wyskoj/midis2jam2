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

package org.wysko.midis2jam2

import com.jme3.app.Application
import com.jme3.app.state.AppStateManager
import com.jme3.input.KeyInput
import com.jme3.input.controls.KeyTrigger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.gervill.JwRealTimeSequencer
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.SynthesizerConfiguration
import org.wysko.midis2jam2.starter.configuration.getType
import org.wysko.midis2jam2.util.ErrorHandling.errorDisp
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.KeyMap
import org.wysko.midis2jam2.world.background.BackgroundController
import org.wysko.midis2jam2.world.background.BackgroundImageFormatException
import org.wysko.midis2jam2.world.camera.CameraAngle.Companion.preventCameraFromLeaving
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [Midis2jam2] for desktop. This is so that we can access `javax` classes.
 *
 * @property sequencer The sequencer responsible for playing the MIDI file.
 * @property midiFile The MIDI file that this instance is playing.
 * @property onClose A function to call when the application is closed.
 * @property synthesizer The synthesizer responsible for playing the MIDI file, if any.
 * @param configs A collection of the application's configurations.
 */
class DesktopPlaylistMidis2jam2(
    override val fileName: String,
    val sequencer: Sequencer,
    val midiFile: TimeBasedSequence,
    val onFinish: () -> Unit,
    val onClose: () -> Unit,
    val synthesizer: Synthesizer?,
    configs: Collection<Configuration>,
) : Midis2jam2(midiFile, fileName, configs) {

    private var isSequencerStarted: Boolean = false
    private var skippedFrames = 0
    private var isFadeStarted = false

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        logger().debug("Initializing application...")

        KeyMap.registerMappings(app, this)
        try {
            BackgroundController.configureBackground(
                context = this@DesktopPlaylistMidis2jam2,
                config = configs.getType(BackgroundConfiguration::class),
                root = root,
            )
        } catch (e: BackgroundImageFormatException) {
            exit()
            logger().errorDisp(e.message ?: "There was an error loading the images for the background.", e)
        } catch (e: IllegalArgumentException) {
            exit()
            logger().errorDisp(
                message = when (e.message) {
                    "Image width and height must be the same" -> "The background image(s) must be square."
                    else -> e.message ?: "There was an error loading the images for the background."
                },
                exception = e
            )
        }
        logger().debug("Application initialized")
    }

    override fun update(tpf: Float) {
        super.update(tpf)

        val delta = tpf.toDouble().seconds

        startSequencerIfNeeded()
        startFadeIfNeeded()

        instruments.forEach { it.tick(time, delta) }

        if (time >= sequence.duration) {
            if (!isSongFinished) endTime = time
            isSongFinished = true
        }

        handleSongCompletionAndLoopOrExit()

        // This is a hack to prevent the first few frames from updating the timeSinceStart variable.
        if (skippedFrames++ < 3) {
            return
        }

        tickControllers(delta)

        if (sequencer.isOpen && !paused) {
            time += delta
        }
    }

    override fun cleanup() {
        super.cleanup()
        app.inputManager.removeListener(this)
    }

    override fun exit() {
        logger().debug("Exiting...")
        if (sequencer.isOpen) {
            sequencer.stop()
        }
        app.run {
            stateManager.detach(this@DesktopPlaylistMidis2jam2)
            stop(false)
        }
        onClose()
        logger().debug("Exit complete")
    }

    override fun seek(time: Duration) {
        logger().debug("Seeking to time: $time")
        this.time = time
        sequencer.microsecondPosition = time.coerceAtLeast(ZERO).inWholeMicroseconds
        collectors.forEach { it.seek(time) }
    }

    override fun togglePause() {
        super.togglePause()
        if (paused) {
            sequencer.stop()
        } else {
            sequencer.start()
        }
    }

    private fun tickControllers(delta: Duration) {
        fakeShadowsController?.tick()
        standController.tick()
        lyricController?.tick(time, delta)
        hudController.tick(time, fadeFilter.value)
        flyByCamera.tick(delta)
        autocamController.tick(time, delta)
        slideCamController.tick(time, delta)
        preventCameraFromLeaving(app.camera)
        drumSetVisibilityManager.tick(time)
    }

    private fun handleSongCompletionAndLoopOrExit() {
        if (isSongFinished && time >= endTime + 3.seconds) {
            onFinish()
        }
    }

    private fun startFadeIfNeeded() {
        if (!isFadeStarted && time > (-1.5).seconds) {
            fadeFilter.fadeIn()
            isFadeStarted = true
        }
    }

    private fun startSequencerIfNeeded() {
        if (!isSequencerStarted && time > ZERO) {
            sequencer.start()
            isSequencerStarted = true
            setSynthEffects()
        }
    }

    private fun setSynthEffects() {
        synthesizer?.let {
            if (!configs.getType(SynthesizerConfiguration::class).isReverbEnabled) {
                synthesizer.channels.forEach { it.controlChange(91, 0) }
            }
            if (!configs.getType(SynthesizerConfiguration::class).isChorusEnabled) {
                synthesizer.channels.forEach { it.controlChange(93, 0) }
            }
        }
    }
}
