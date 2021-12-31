/*
 * Copyright (C) 2021 Jacob Wysko
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
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.gui.SwingWrapper
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.util.PassedSettings
import java.awt.Dimension
import java.awt.Toolkit
import javax.imageio.ImageIO
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer

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
    ) {
        onStart() // Disable launcher

        /* Get MIDI file */
        val sequence = MidiSystem.getSequence(passedSettings.midiFile)

        /* Get MIDI device */
        val midiDevice = MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo().first {
            it.name == passedSettings
                .midiDevice
        })

        /* Get sequencer */
        val sequencer = if (passedSettings.midiDevice == "Gervill") {
            /* Get internal synth */
            val synthesizer = MidiSystem.getSynthesizer()
            synthesizer.open()

            /* Get SoundFont */
            passedSettings.soundFont?.let { sf2 ->
                MidiSystem.getSequencer(false).also {
                    it.transmitter.receiver = synthesizer.receiver
                    synthesizer.loadAllInstruments(MidiSystem.getSoundbank(sf2))
                }
            } ?: MidiSystem.getSequencer(true)
        } else {
            midiDevice.open()
            MidiSystem.getSequencer(false).also {
                it.transmitter.receiver = midiDevice.receiver
            }
        }.also {
            it.open()
            it.sequence = sequence
        }

        onReady()
        if (!passedSettings.legacyDisplayEngine) {
            Thread({
                StandardExecution(
                    passedSettings,
                    onFinish,
                    sequencer
                ).execute()
            }, "midis2jam2 starter").start()
        } else {
            Thread({
                LegacyExecution(
                    passedSettings,
                    onFinish,
                    sequencer
                ).execute()
            }, "midis2jam2 starter").start()
        }
    }
}

private val defaultSettings = AppSettings(true).apply {
    frameRate = 120
    frequency = 60
    isVSync = true
    isResizable = true
    samples = 4
    isGammaCorrection = true
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
            SwingWrapper(canvas, "midis2jam2").run {
                isVisible = true
            }
            startCanvas()
        }
    }

    override fun stop() {
        stop(false)
        onFinish()
    }

    override fun simpleInitApp() {
        DesktopMidis2jam2(
            sequencer = sequencer,
            MidiFile.readMidiFile(passedSettings.midiFile),
            settings = passedSettings,
            onFinish
        ).also {
            stateManager.attach(it)
            rootNode.attachChild(it.rootNode)
        }
    }
}

private open class LegacyExecution(
    val passedSettings: PassedSettings,
    val onFinish: () -> Unit,
    val sequencer: Sequencer
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
        DesktopMidis2jam2(
            sequencer = sequencer,
            MidiFile.readMidiFile(passedSettings.midiFile),
            settings = passedSettings,
            onFinish
        ).also {
            stateManager.attach(it)
            rootNode.attachChild(it.rootNode)
        }
    }
}

/** Determines the width of the screen. */
private fun screenWidth() = Toolkit.getDefaultToolkit().screenSize.width

/** Determines the height of the screen. */
private fun screenHeight() = Toolkit.getDefaultToolkit().screenSize.height