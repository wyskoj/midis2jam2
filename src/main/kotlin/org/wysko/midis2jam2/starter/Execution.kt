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

package org.wysko.midis2jam2.starter

import com.jme3.app.SimpleApplication
import com.jme3.system.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.launch
import org.wysko.gervill.RealTimeSequencerProvider
import org.wysko.kmidi.midi.StandardMidiFileReader
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.readFile
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.gui.viewmodel.GERVILL
import org.wysko.midis2jam2.record.FixedTimer
import org.wysko.midis2jam2.record.RecordOptions
import org.wysko.midis2jam2.record.captureVideo
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.GraphicsConfiguration
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.Resolution
import org.wysko.midis2jam2.starter.configuration.SettingsConfiguration
import org.wysko.midis2jam2.util.ErrorHandling.errorDisp
import org.wysko.midis2jam2.util.logger
import java.awt.GraphicsEnvironment
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.sound.midi.InvalidMidiDataException
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.Sequence
import javax.sound.midi.Sequencer

/**
 * This class represents the execution of a MIDI file with given configurations.
 */
object Execution {
    /**
     * Starts the midis2jam2 JME application.
     *
     * @param midiFile The MIDI file to play.
     * @param configurations The configurations for the application.
     * @param onStart Callback function called when the application starts.
     * @param onReady Callback function called when the application is ready to begin.
     * @param onFinish Callback function called when the application is finished.
     */
    fun start(
        midiFile: File,
        configurations: Collection<Configuration>,
        onStart: () -> Unit,
        onReady: () -> Unit,
        onFinish: () -> Unit,
        recordOptions: RecordOptions? = null,
    ) {
        CoroutineScope(Default).launch {
            onStart()
            val homeConfiguration = configurations.first { it is HomeConfiguration } as HomeConfiguration

            val sequence = try {
                MidiSystem.getSequence(midiFile)
            } catch (e: InvalidMidiDataException) {
                this@Execution.logger().errorDisp("The MIDI file is invalid.", e)
                onFinish()
                return@launch
            } catch (e: IOException) {
                this@Execution.logger().errorDisp("There was an error reading the MIDI file.", e)
                onFinish()
                return@launch
            }

            val midiDevice =
                try {
                    if (recordOptions == null) { // Not recording
                        MidiSystem.getMidiDevice(
                            MidiSystem.getMidiDeviceInfo().first { it.name == homeConfiguration.selectedMidiDevice },
                        )
                    } else {
                        null
                    }
                } catch (e: MidiUnavailableException) {
                    this@Execution.logger().errorDisp("The MIDI device is unavailable due to resource restrictions.", e)
                    onFinish()
                    return@launch
                } catch (e: IllegalArgumentException) {
                    this@Execution.logger().errorDisp("The MIDI device is not found.", e)
                    onFinish()
                    return@launch
                } catch (e: NoSuchElementException) {
                    this@Execution.logger().errorDisp("The MIDI device is not found.", e)
                    onFinish()
                    return@launch
                }

            val sequencer = try {
                getAndLoadSequencer(homeConfiguration, midiDevice, sequence)
            } catch (e: Exception) {
                logger().errorDisp("There was an error.", e)
                onFinish()
                return@launch
            }

            onReady()
            Midis2jam2Application(midiFile, configurations, onFinish, sequencer, recordOptions).execute()
        }
    }

    private fun getAndLoadSequencer(
        homeConfiguration: HomeConfiguration,
        midiDevice: MidiDevice?,
        sequence: Sequence?,
    ): Sequencer {
        val provider = RealTimeSequencerProvider()
        return when (homeConfiguration.selectedMidiDevice) {
            GERVILL -> {
                val synthesizer =
                    MidiSystem.getSynthesizer().apply {
                        open()
                        homeConfiguration.selectedSoundbank?.let { loadAllInstruments(MidiSystem.getSoundbank(File(it))) }
                    }

                provider
                    .getDevice(provider.deviceInfo[0])
                    .apply { transmitter.receiver = synthesizer.receiver } as Sequencer
            }

            else -> {
                provider.getDevice(provider.deviceInfo[0]).apply {
                    midiDevice?.let {
                        it.open()
                        transmitter.receiver = it.receiver
                    }
                } as Sequencer
            }
        }.also {
            it.open()
            it.sequence = sequence
        }
    }
}

private class Midis2jam2Application(
    val file: File,
    val configurations: Collection<Configuration>,
    val onFinish: () -> Unit,
    val sequencer: Sequencer,
    val recordOptions: RecordOptions?,
) : SimpleApplication() {
    fun execute() {
        val jmeSettings = AppSettings(false).apply { copyFrom(DEFAULT_JME_SETTINGS) }
        val settingsConfiguration = configurations.first { it is SettingsConfiguration } as SettingsConfiguration
        val graphicsConfiguration = configurations.first { it is GraphicsConfiguration } as GraphicsConfiguration

        if (settingsConfiguration.isFullscreen) {
            jmeSettings.isFullscreen = true
            with(screenResolution()) {
                jmeSettings.width = width
                jmeSettings.height = height
            }
        } else {
            jmeSettings.isFullscreen = false
            when (graphicsConfiguration.windowResolution) {
                is Resolution.DefaultResolution ->
                    with(preferredResolution()) {
                        jmeSettings.width = width
                        jmeSettings.height = height
                    }

                is Resolution.CustomResolution ->
                    with(graphicsConfiguration.windowResolution) {
                        jmeSettings.width = width
                        jmeSettings.height = height
                    }
            }
        }

        setSettings(jmeSettings)
        setDisplayStatView(false)
        setDisplayFps(false)
        isPauseOnLostFocus = false
        isShowSettings = false

        start()
    }

    override fun simpleInitApp() {
        val sequence = StandardMidiFileReader.readFile(file).toTimeBasedSequence()
        DesktopMidis2jam2(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            configs = configurations,
            fileName = file.name
        ).also {
            stateManager.attach(it)
            rootNode.attachChild(it.root)
        }

        recordOptions?.let {
            setTimer(FixedTimer(it.videoFps.toLong()))
            captureVideo(
                app = this,
                video = it.outputVideoFile,
                frameRate = it.videoFps,
                resolution = settings.width to settings.height,
                quality = it.videoQuality
            )
        }
    }

    override fun stop() {
        onFinish()
        super.stop()
    }
}

private val DEFAULT_JME_SETTINGS =
    AppSettings(true).apply {
        frameRate = -1
        frequency =
            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayModes.first().refreshRate
        isVSync = true
        isResizable = false
        isGammaCorrection = false
        icons =
            arrayOf("/ico/icon16.png", "/ico/icon32.png", "/ico/icon128.png", "/ico/icon256.png").map {
                ImageIO.read(this::class.java.getResource(it))
            }.toTypedArray()
        title = "midis2jam2"
        audioRenderer = null
        centerWindow = true
    }

private fun screenResolution(): Resolution.CustomResolution =
    with(GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode) {
        Resolution.CustomResolution(width, height)
    }

private fun preferredResolution(): Resolution.CustomResolution =
    with(screenResolution()) {
        Resolution.CustomResolution((width * 0.95).toInt(), (height * 0.85).toInt())
    }
