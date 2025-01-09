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
import org.wysko.midis2jam2.DesktopPlaylistMidis2jam2
import org.wysko.midis2jam2.gui.viewmodel.GERVILL
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
import javax.sound.midi.Synthesizer

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

            val midiDevice = try {
                MidiSystem.getMidiDevice(
                    MidiSystem.getMidiDeviceInfo().first { it.name == homeConfiguration.selectedMidiDevice },
                )
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

            val (sequencer, synthesizer) = try {
                getAndLoadSequencer(homeConfiguration, midiDevice, sequence)
            } catch (e: Exception) {
                logger().errorDisp("There was an error.", e)
                onFinish()
                return@launch
            }

            onReady()
            Midis2jam2Application(midiFile, configurations, onFinish, sequencer, synthesizer).execute()
        }
    }

    private fun getAndLoadSequencer(
        homeConfiguration: HomeConfiguration,
        midiDevice: MidiDevice,
        sequence: Sequence?,
    ): Pair<Sequencer, Synthesizer?> {
        val provider = RealTimeSequencerProvider()
        return when (homeConfiguration.selectedMidiDevice) {
            GERVILL -> {
                val synthesizer = MidiSystem.getSynthesizer().apply {
                    open()
                    homeConfiguration.selectedSoundbank?.let { loadAllInstruments(MidiSystem.getSoundbank(File(it))) }
                }

                provider.getDevice(provider.deviceInfo[0])
                    .apply { transmitter.receiver = synthesizer.receiver } as Sequencer to synthesizer
            }

            else -> {
                midiDevice.open()
                provider.getDevice(provider.deviceInfo[0])
                    .apply { transmitter.receiver = midiDevice.receiver } as Sequencer to null
            }
        }.also {
            it.first.open()
            sequence?.let { sequence ->
                it.first.sequence = sequence
            }
        }
    }

    fun playPlaylist(
        midiFiles: List<File>,
        configurations: Collection<Configuration>,
        onStart: () -> Unit,
        onFinish: () -> Unit,
        isShuffle: Boolean,
    ) {
        val deviceInfo = MidiSystem.getMidiDeviceInfo().first { it.name == GERVILL }
        val device = MidiSystem.getMidiDevice(deviceInfo)
        device.open()
        val provider = RealTimeSequencerProvider()
        val sequencer = provider.getDevice(provider.deviceInfo.first()) as Sequencer
        sequencer.open()
        val synthesizer = MidiSystem.getSynthesizer()
        synthesizer.open()
        sequencer.transmitter.receiver = synthesizer.receiver
        Midis2jam2PlaylistApplication(
            if (isShuffle) midiFiles.shuffled() else midiFiles,
            configurations,
            onFinish,
            sequencer,
            synthesizer,
        ).run {
            applyConfigurations(configurations)
            onStart()
            start()
        }
    }
}

private class Midis2jam2PlaylistApplication(
    val files: List<File>,
    val configurations: Collection<Configuration>,
    val onPlaylistFinish: () -> Unit,
    val sequencer: Sequencer,
    val synthesizer: Synthesizer?,
) : SimpleApplication() {
    private var currentSongIndex = 0
    private lateinit var midis2jam2s: List<DesktopPlaylistMidis2jam2>
    private var currentState: DesktopPlaylistMidis2jam2? = null

    override fun simpleInitApp() {
        sequencer.open()
        buildStates()
        loadSong(0)
    }

    private fun buildStates() {
        midis2jam2s = files.map { file ->
            DesktopPlaylistMidis2jam2(
                sequencer = sequencer,
                midiFile = loadKMidiSequence(file).toTimeBasedSequence(),
                onClose = {
                    stop()
                },
                onFinish = {
                    logger().debug("Advance")
                    advance()
                },
                configs = configurations,
                fileName = file.name,
                synthesizer = synthesizer,
            )
        }
    }

    private fun loadKMidiSequence(file: File) = StandardMidiFileReader().readFile(file)
    private fun loadJavaXSequence(file: File) = MidiSystem.getSequence(file)

    private fun advance() = when {
        currentSongIndex != midis2jam2s.indices.last -> loadSong(currentSongIndex + 1)

        else -> {
            onPlaylistFinish()
            stop()
        }
    }

    private fun loadSong(index: Int) {
        rootNode.detachAllChildren()
        guiNode.detachAllChildren()
        midis2jam2s.forEach {
            stateManager.detach(it)
        }

        sequencer.stop()
        sequencer.sequence = loadJavaXSequence(files[index])

        midis2jam2s[index].let {
            stateManager.attach(it)
            rootNode.attachChild(it.root)
            currentState = it
        }

        currentSongIndex = index
    }
}

private fun SimpleApplication.applyConfigurations(configurations: Collection<Configuration>) {
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
}

private class Midis2jam2Application(
    val file: File,
    val configurations: Collection<Configuration>,
    val onFinish: () -> Unit,
    val sequencer: Sequencer,
    val synthesizer: Synthesizer?,
) : SimpleApplication() {
    fun execute() {
        applyConfigurations(configurations)
        start()
    }

    override fun simpleInitApp() {
        val sequence = StandardMidiFileReader().readFile(file).toTimeBasedSequence()
        DesktopMidis2jam2(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            configs = configurations,
            fileName = file.name,
            synthesizer = synthesizer,
        ).also {
            stateManager.attach(it)
            rootNode.attachChild(it.root)
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
        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayModes.firstOrNull()?.let {
            frequency = it.refreshRate
        }
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
