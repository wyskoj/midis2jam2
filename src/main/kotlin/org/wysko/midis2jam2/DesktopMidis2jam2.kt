/*
 * Copyright (C) 2024 Jacob Wysko
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
import kotlinx.coroutines.Job
import org.wysko.gervill.JwRealTimeSequencer
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.getType
import org.wysko.midis2jam2.util.ErrorHandling.errorDisp
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.world.BackgroundController
import org.wysko.midis2jam2.world.ImageFormatException
import org.wysko.midis2jam2.world.KeyMap
import org.wysko.midis2jam2.world.camera.CameraAngle.Companion.preventCameraFromLeaving
import javax.sound.midi.Sequencer

/**
 * Contains all the code relevant to operating the 3D scene.
 *
 * @param sequencer The MIDI sequencer.
 * @param midiFile The MIDI file to play.
 * @param configs The settings to use.
 * @param onClose Callback when midis2jam2 closes.
 */
class DesktopMidis2jam2(
    val sequencer: Sequencer,
    val midiFile: MidiFile,
    val onClose: () -> Unit,
    configs: Collection<Configuration>,
) : Midis2jam2(midiFile, configs) {
    /** Whether the sequencer has started. */
    private var isSequencerStarted: Boolean = false

    /** The number of currently skipped frames at the start of the application. */
    private var skippedTicks = 0

    /** Whether the fade-in has been initiated. */
    private var initiatedFadeIn = false

    /** Job for the tempo change coroutine. */
    private lateinit var tempoChangeCoroutine: Job

    /** Initializes the application. */
    override fun initialize(
        stateManager: AppStateManager,
        app: Application,
    ) {
        super.initialize(stateManager, app)
        logger().debug("Initializing application...")

        KeyMap.registerMappings(app, this)
        try {
            BackgroundController.configureBackground(
                configs.getType(BackgroundConfiguration::class),
                this@DesktopMidis2jam2,
                rootNode,
            )
        } catch (e: ImageFormatException) {
            exit()
            logger().errorDisp(e.message ?: "There was an error loading the images for the background.", e)
        } catch (e: Exception) {
            exit()
            logger().errorDisp(
                when (e.message) {
                    "Image width and height must be the same" -> "The background image(s) must be square."
                    else -> e.message ?: "There was an error loading the images for the background."
                },
                e,
            )
        }

        logger().debug("Application initialized")
    }

    /**
     * Cleans up the application.
     */
    override fun cleanup() {
        logger().debug("Cleaning up...")
        sequencer.run {
            stop()
            close()
        }
        onClose()
        logger().debug("Cleanup complete")
    }

    /**
     * Performs a tick.
     */
    override fun update(tpf: Float) {
        super.update(tpf)

        // Start sequencer if not started
        if (!isSequencerStarted && time > 0.0) {
            with(sequencer) {
                start()
            }
            isSequencerStarted = true
        }

        // Initiate fade-in
        if (!initiatedFadeIn && time > -1.5) {
            fadeFilter.fadeIn()
            initiatedFadeIn = true
        }

        // Tick instruments
        instruments.forEach {
            it.tick(time, tpf)
        }

        // If at the end of the file
        if (time >= file.length) {
            if (!isSongFinished) {
                endTime = time
            }
            isSongFinished = true
        }

        // If after the end, by three seconds
        if (isSongFinished && time >= endTime + 3.0) {
            if (configs.getType(HomeConfiguration::class).isLooping) {
                loop()
            } else {
                exit()
            }
        }

        // This is a hack to prevent the first few frames from updating the timeSinceStart variable.
        if (skippedTicks++ < 3) {
            return
        }

        shadowController?.tick()
        standController.tick()
        lyricController.tick(time)
        hudController.tick(time, fadeFilter.value)
        flyByCamera.tick(tpf)
        autocamController.tick(time, tpf)
        slideCamController.tick(tpf, time)
        preventCameraFromLeaving(app.camera)
        drumSetVisibilityManager.tick(time)

        if (sequencer.isOpen && !paused) {
            // Increment time if sequencer is ready / playing
            time += tpf.toDouble()
        }
    }

    private fun loop() {
        isSequencerStarted = false
        sequencer.stop()
        seek(-2.0)
        slideCamController.onLoop()
        (sequencer as JwRealTimeSequencer).resetDevice()
    }

    override fun seek(time: Double) {
        logger().debug("Seeking to time: $time")
        this.time = time
        sequencer.microsecondPosition = (time * 1E6).coerceAtLeast(0.0).toLong()
        eventCollectors.forEach { it.seek(time) }
        notePeriodCollectors.forEach { it.seek(time) }
        notePeriodGroupCollectors.forEach { it.seek(time) }
    }

    /**
     * Stops the app state.
     */
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
        if (::tempoChangeCoroutine.isInitialized) {
            tempoChangeCoroutine.cancel()
        }
        logger().debug("Exit complete")
    }

    override fun togglePause() {
        super.togglePause()
        if (paused) {
            sequencer.stop()
        } else {
            sequencer.start()
        }
    }
}
