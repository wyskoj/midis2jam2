/*
 * Copyright (C) 2023 Jacob Wysko
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
import kotlinx.coroutines.*
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import org.wysko.midis2jam2.starter.configuration.Configuration
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
    configs: Collection<Configuration>
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
    override fun initialize(stateManager: AppStateManager, app: Application) {
        super.initialize(stateManager, app)
        logger().debug("Initializing application...")

        KeyMap.registerMappings(app, this)
        launchSequencerWithTempoChanges()
        try {
            BackgroundController.configureBackground(
                configs.getType(BackgroundConfiguration::class),
                this@DesktopMidis2jam2,
                rootNode
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
                }, e
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

        if (!initiatedFadeIn && timeSinceStart > -1.5) {
            fade.fadeIn()
            initiatedFadeIn = true
        }

        instruments.forEach {
            /* Null if not implemented yet */
            it.tick(timeSinceStart, tpf)
        }

        /* If at the end of the file */
        if (timeSinceStart >= file.length) {
            if (!afterEnd) {
                stopTime = timeSinceStart
            }
            afterEnd = true
        }

        /* If after the end, by three seconds */
        if (afterEnd && timeSinceStart >= stopTime + 3.0) {
            exit()
        }

        /* This is a hack to prevent the first few frames from updating the timeSinceStart variable. */
        if (skippedTicks++ < 3) {
            return
        }

        shadowController?.tick()
        standController.tick()
        lyricController.tick(timeSinceStart)
        hudController.tick(timeSinceStart, fade.value)
        flyByCamera.tick(tpf)
        autocamController.tick(timeSinceStart, tpf)
        slideCamController.tick(tpf, timeSinceStart)
        preventCameraFromLeaving(app.camera)



        if (sequencer.isOpen && !paused) {
            /* Increment time if sequencer is ready / playing */
            timeSinceStart += tpf.toDouble()
        }
    }

    override fun seek(time: Double) {
        logger().debug("Seeking to time: $time")
        timeSinceStart = time
        sequencer.microsecondPosition = (time * 1E6).toLong()
        eventCollectors.forEach { it.seek(time) }
        notePeriodCollectors.forEach { it.seek(time) }
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
            stop(true)
        }
        onClose()
        if (::tempoChangeCoroutine.isInitialized) {
            tempoChangeCoroutine.cancel()
        }
        logger().debug("Exit complete")
    }

    override fun togglePause() {
        super.togglePause()
        if (paused) sequencer.stop() else sequencer.start()
    }

    private fun launchSequencerWithTempoChanges() {
        tempoChangeCoroutine = CoroutineScope(Dispatchers.Default).launch {
            while (sequencer.isOpen) {
                if (!isSequencerStarted && timeSinceStart >= 0) {
                    startSequencer()
                    logger().debug("Sequencer started")
                }
                delay(1)
            }
        }
    }

    private suspend fun startSequencer() {
        sequencer.tempoInBPM = file.tempos.first().bpm().toFloat()
        sequencer.start()
        isSequencerStarted = true
        startApplyingTempos()
    }

    private suspend fun startApplyingTempos() {
        val tempos = ArrayList(file.tempos)
        while (true) {
            while (tempos.isNotEmpty() && tempos.first().time < sequencer.tickPosition) {
                sequencer.tempoInBPM = tempos.removeAt(0).bpm().toFloat()
            }
            delay(1)
        }
    }
}