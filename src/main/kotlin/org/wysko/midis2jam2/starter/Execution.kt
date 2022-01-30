/*
 * Copyright (C) 2022 Jacob Wysko
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
import com.jme3.system.JmeCanvasContext
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.gui.ExceptionPanel
import org.wysko.midis2jam2.gui.SwingWrapper
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.util.PassedSettings
import java.awt.Dimension
import java.awt.Toolkit
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.sound.midi.InvalidMidiDataException
import javax.sound.midi.MidiSystem
import javax.sound.midi.MidiUnavailableException
import javax.sound.midi.Sequencer
import javax.swing.JFrame
import javax.swing.JOptionPane

/** The sequencer that is connected to the synthesizer. */
lateinit var connectedSequencer: Sequencer

/** Loads [connectedSequencer]. */
var loadSequencerJob: Job = CoroutineScope(Default).launch {
    try {
        connectedSequencer = MidiSystem.getSequencer(true)
    } catch (e: MidiUnavailableException) {
        err(e,
            "The MIDI sequencer is not available due to resource restrictions, or no sequencer is installed in the system.",
            "Error loading MIDI sequencer")
    }
}


/** Starts midis2jam2 with given settings. */
object Execution {

    /**
     * Begins midis2jam2 with given settings.
     *
     * @param passedSettings settings to use
     * @param onStart function to call when midis2jam2 is started
     * @param onFinish function to call when midis2jam2 is finished
     */
    fun start(
        passedSettings: PassedSettings,
        onStart: () -> Unit,
        onReady: () -> Unit,
        onFinish: () -> Unit,
    ): Job {
        System.gc()
        return CoroutineScope(Default).launch {

            onStart() // Disable launcher

            /* Get MIDI file */

            val sequence = try {
                withContext(Dispatchers.IO) {
                    MidiSystem.getSequence(passedSettings.midiFile)
                }
            } catch (e: InvalidMidiDataException) {
                err(e, "The MIDI file has bad data.", "Error reading MIDI file", onFinish)
                return@launch
            } catch (e: IOException) {
                err(e, "The MIDI file could not be loaded.", "Error reading MIDI file", onFinish)
                return@launch
            }


            /* Get MIDI device */
            val midiDevice = try {
                MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo().first { it.name == passedSettings.midiDevice })
            } catch (e: MidiUnavailableException) {
                err(e,
                    "The \"${passedSettings.midiDevice}\" MIDI device is unavailable. Are any other applications using it?",
                    "Error opening MIDI device",
                    onFinish)
                return@launch
            } catch (e: IllegalArgumentException) {
                err(e,
                    "The \"${passedSettings.midiDevice}\" MIDI device doesn't currently exist. Did you unplug it?",
                    "Error opening MIDI device",
                    onFinish)
                return@launch
            }

            /* Get sequencer */
            val sequencer = if (passedSettings.midiDevice == "Gervill") {

                /* Get internal synth */
                val synthesizer = try {
                    MidiSystem.getSynthesizer()
                } catch (e: MidiUnavailableException) {
                    err(e,
                        "The internal synthesizer is not available due to resource restrictions, or no synthesizer is installed in the system.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                }

                /* Open synthesizer */
                try {
                    synthesizer.open()
                } catch (e: MidiUnavailableException) {
                    err(e,
                        "The MIDI device cannot be opened due to resource restrictions.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                } catch (e: SecurityException) {
                    err(e,
                        "The MIDI device cannot be opened due to security restrictions.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                }

                /* Get SoundFont */
                passedSettings.soundFont?.let { sf2 ->
                    getUnconnectedSequencer().also {
                        try {
                            it.transmitter.receiver = synthesizer.receiver
                            synthesizer.loadAllInstruments(MidiSystem.getSoundbank(sf2))
                        } catch (e: InvalidMidiDataException) {
                            err(e, "The SoundFont file is bad.", "Error loading SoundFont", onFinish)
                            return@launch
                        } catch (e: IOException) {
                            err(e, "Could not load the SoundFont.", "Error loading SoundFont", onFinish)
                            return@launch
                        } catch (e: IllegalArgumentException) {
                            err(e, "midis2jam2 does not support this soundbank.", "Error loading SoundFont", onFinish)
                            return@launch
                        }
                    }
                } ?: let {
                    loadSequencerJob.join()
                    connectedSequencer
                }

            } else {
                try {
                    midiDevice.open()
                } catch (e: MidiUnavailableException) {
                    err(e,
                        "The MIDI device cannot be opened due to resource restrictions.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                } catch (e: SecurityException) {
                    err(e,
                        "The MIDI device cannot be opened due to security restrictions.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                }
                MidiSystem.getSequencer(false).also {
                    it.transmitter.receiver = midiDevice.receiver
                }
            }.also {
                try {
                    it.open()
                } catch (e: MidiUnavailableException) {

                    err(e,
                        "The MIDI device cannot be opened due to resource restrictions.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                } catch (e: SecurityException) {
                    err(e,
                        "The MIDI device cannot be opened due to security restrictions.",
                        "Error opening MIDI synthesizer",
                        onFinish)
                    return@launch
                }
                it.sequence = sequence
            }

            /* Hush JME */
            Logger.getLogger("com.jme3").level = Level.FINEST

            onReady()

            if (!passedSettings.legacyDisplayEngine) {
                Thread({
                    StandardExecution(passedSettings, onFinish, sequencer).execute()
                }, "midis2jam2 starter").start()
            } else {
                Thread({
                    LegacyExecution(passedSettings, onFinish, sequencer).execute()
                }, "midis2jam2 starter").start()
            }
        }
    }

    private fun getUnconnectedSequencer() = MidiSystem.getSequencer(false)
}

/** Handles an error. */
fun err(exception: Exception, message: String, title: String, onFinish: () -> Unit = {}) {
    JOptionPane.showMessageDialog(null, ExceptionPanel(message, exception), title, JOptionPane.ERROR_MESSAGE)
    onFinish.invoke()
}

/* EXECUTORS */

private val defaultSettings = AppSettings(true).apply {
    frameRate = 120
    frequency = 60
    isVSync = true
    isResizable = true
    samples = 4
    isGammaCorrection = false
    icons = arrayOf("/ico/icon16.png", "/ico/icon32.png", "/ico/icon128.png", "/ico/icon256.png").map {
        ImageIO.read(this::class.java.getResource(it))
    }.toTypedArray()
    title = "midis2jam2"
}

private open class StandardExecution(
    val passedSettings: PassedSettings,
    val onFinish: () -> Unit,
    val sequencer: Sequencer,
) : SimpleApplication() {

    private lateinit var frame: JFrame

    fun execute() {
        /* Set JME3 settings */
        setDisplayStatView(false)
        setDisplayFps(false)
        isPauseOnLostFocus = false
        isShowSettings = false

        if (passedSettings.fullscreen) {
            defaultSettings.isFullscreen = true
            defaultSettings.setResolution(screenWidth(), screenHeight())
            setSettings(defaultSettings)
            super.start()
        } else {
            setSettings(defaultSettings)
            createCanvas()
            val context = getContext() as JmeCanvasContext
            context.setSystemListener(this)
            val dimensions = Dimension(((screenWidth() * 0.95).toInt()), (screenHeight() * 0.85).toInt())
            val canvas = context.canvas
            canvas.preferredSize = dimensions
            frame = SwingWrapper.wrap(canvas, "midis2jam2") {
                stop()
            }
            startCanvas()
        }
    }

    override fun stop() {
        stop(false)
        onFinish()
        if (::frame.isInitialized) {
            frame.isVisible = false
        }
    }

    override fun simpleInitApp() {
        DesktopMidis2jam2(sequencer = sequencer,
            MidiFile(passedSettings.midiFile),
            settings = passedSettings,
            onFinish).also {
            stateManager.attach(it)
            rootNode.attachChild(it.rootNode)
        }
    }
}

private open class LegacyExecution(
    val passedSettings: PassedSettings,
    val onFinish: () -> Unit,
    val sequencer: Sequencer,
) : SimpleApplication() {

    fun execute() {
        if (passedSettings.fullscreen) {
            defaultSettings.isFullscreen = true
            defaultSettings.setResolution(screenWidth(), screenHeight())
        } else {
            defaultSettings.isFullscreen = false
            defaultSettings.setResolution((screenWidth() * 0.95).toInt(), (screenHeight() * 0.85).toInt())
        }
        defaultSettings.isResizable = true
        setSettings(defaultSettings)
        setDisplayStatView(false)
        setDisplayFps(false)
        isPauseOnLostFocus = false
        isShowSettings = false
        start()
    }

    override fun stop() {
        stop(false)
        onFinish()
    }

    override fun simpleInitApp() {
        DesktopMidis2jam2(sequencer = sequencer,
            MidiFile(passedSettings.midiFile),
            settings = passedSettings,
            onFinish).also {
            stateManager.attach(it)
            rootNode.attachChild(it.rootNode)
        }
    }
}

/** Determines the width of the screen. */
fun screenWidth(): Int = Toolkit.getDefaultToolkit().screenSize.width

/** Determines the height of the screen. */
fun screenHeight(): Int = Toolkit.getDefaultToolkit().screenSize.height