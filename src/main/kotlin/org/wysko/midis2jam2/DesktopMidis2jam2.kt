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
import org.wysko.gervill.JwRealTimeSequencer
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.starter.configuration.*
import org.wysko.midis2jam2.util.ErrorHandling.errorDisp
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.KeyMap
import org.wysko.midis2jam2.world.background.BackgroundController
import org.wysko.midis2jam2.world.background.BackgroundImageFormatException
import org.wysko.midis2jam2.world.camera.CameraAngle.Companion.preventCameraFromLeaving
import javax.sound.midi.MidiDevice
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer
import javax.sound.midi.SysexMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Implementation of [Midis2jam2] for desktop. This is so that we can access `javax` classes.
 *
 * @property sequencer The sequencer responsible for playing the MIDI file.
 * @property midiFile The MIDI file that this instance is playing.
 * @property onClose A function to call when the application is closed.
 * @property synthesizer The synthesizer responsible for playing the MIDI file, if any.
 * @param configs A collection of the application's configurations.
 */
open class DesktopMidis2jam2(
    override val fileName: String,
    val sequencer: Sequencer,
    val midiFile: TimeBasedSequence,
    val onClose: () -> Unit,
    val synthesizer: Synthesizer?,
    val midiDevice: MidiDevice,
    configs: Collection<Configuration>,
) : Midis2jam2(midiFile, fileName, configs) {

    private var isSequencerStarted: Boolean = false
    private var skippedFrames = 0

    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        logger().debug("Initializing application...")

        KeyMap.registerMappings(app, this)
        try {
            BackgroundController.configureBackground(
                context = this@DesktopMidis2jam2,
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

        // Send the reset message to MIDI device
        with(configs.find<MidiDeviceConfiguration>()) {
            if (isSendResetMessage) {
                midiDevice.receiver.send(
                    SysexMessage(
                        resetMessageSpecification.resetMessage,
                        resetMessageSpecification.resetMessage.size
                    ), -1L
                )
                logger().debug("Sent ${resetMessageSpecification.initialism} reset message to MIDI device")
            }
        }
    }

    override fun cleanup() {
        logger().debug("Cleaning up...")
        sequencer.run {
            stop()
            close()
        }
        onClose()
        logger().debug("Cleanup complete")
    }

    override fun update(tpf: Float) {
        super.update(tpf)

        val delta = tpf.toDouble().seconds

        startSequencerIfNeeded()

        fadeFilter.value = when {
            time in (-2.0).seconds..(-1.5).seconds -> {
                0.0f
            }

            time in (-1.5).seconds..(-1.0).seconds -> {
                Utils.mapRangeClamped(time.toDouble(DurationUnit.SECONDS), -1.5, -1.0, 0.0, 1.0).toFloat()
            }

            (midiFile.duration + 3.seconds - time) < 0.5.seconds -> {
                Utils.mapRangeClamped(
                    time.toDouble(DurationUnit.SECONDS),
                    ((midiFile.duration + 3.seconds) - 0.5.seconds).toDouble(DurationUnit.SECONDS),
                    (midiFile.duration + 3.seconds).toDouble(DurationUnit.SECONDS),
                    1.0,
                    0.0
                ).toFloat()
            }

            else -> 1.0f
        }

        instruments.forEach { it.tick(time, delta) }

        if (time >= sequence.duration) {
            if (!isSongFinished) endTime = time
            isSongFinished = true
        }

        handleSongCompletion()

        // This is a hack to prevent the first few frames from updating the timeSinceStart variable.
        if (skippedFrames++ < 3) {
            return
        }

        tickControllers(delta)

        if (sequencer.isOpen && !paused) {
            time += delta
        }
    }

    override fun exit() {
        logger().debug("Exiting...")
        if (sequencer.isOpen) {
            sequencer.stop()
        }
        app.run {
            stateManager.detach(this@DesktopMidis2jam2)
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
        if (!isSequencerStarted) return

        if (paused) {
            sequencer.stop()
        } else {
            sequencer.start()
        }
    }

    /**
     * Called to handle the completion of the song.
     */
    protected open fun handleSongCompletion() {
        if (isSongFinished && time >= endTime + 3.seconds) {
            if (configs.getType(HomeConfiguration::class).isLooping) {
                // Loop the song
                isSequencerStarted = false
                sequencer.stop()
                seek((-2).seconds)
                slideCamController.onLoop()
                (sequencer as JwRealTimeSequencer).resetDevice()
            } else {
                exit()
            }
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
