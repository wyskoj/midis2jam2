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

import com.jme3.system.AppSettings
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.gui.GuiLauncher
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.util.M2J2Settings
import java.awt.Toolkit
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.sound.midi.Sequencer

class LegacyLiaison(
    guiLauncher: GuiLauncher?,
    sequencer: Sequencer,
    midiFile: MidiFile,
    settings: M2J2Settings,
    fullscreen: Boolean
) : Liaison(guiLauncher, sequencer, midiFile, settings, fullscreen) {

    companion object {
        private val midis2Jam2Settings = AppSettings(true)

        init {
            // Set settings
            midis2Jam2Settings.frameRate = 120
            midis2Jam2Settings.title = "midis2jam2"
            midis2Jam2Settings.frequency = 60
            try {
                midis2Jam2Settings.icons = arrayOf(
                    ImageIO.read(Objects.requireNonNull(Liaison::class.java.getResource("/ico/icon16.png"))),
                    ImageIO.read(Objects.requireNonNull(Liaison::class.java.getResource("/ico/icon32.png"))),
                    ImageIO.read(Objects.requireNonNull(Liaison::class.java.getResource("/ico/icon128.png"))),
                    ImageIO.read(Objects.requireNonNull(Liaison::class.java.getResource("/ico/icon256.png")))
                )
            } catch (e: IOException) {
                Midis2jam2.getLOGGER().warning("Failed to set window icon.")
                e.printStackTrace()
            }
            midis2Jam2Settings.isVSync = true
            midis2Jam2Settings.isResizable = true
            midis2Jam2Settings.samples = 4
            midis2Jam2Settings.isGammaCorrection = true
        }
    }

    override fun start() {
        val dim = Toolkit.getDefaultToolkit().screenSize
        if (fullscreen) {
            midis2Jam2Settings.isFullscreen = true
            midis2Jam2Settings.setResolution(dim.width, dim.height)
        } else {
            midis2Jam2Settings.isFullscreen = false
            midis2Jam2Settings.setResolution((dim.width * 0.95).toInt(), (dim.height * 0.85).toInt())
        }
        midis2Jam2Settings.isResizable = true
        setSettings(midis2Jam2Settings)
        setDisplayStatView(false)
        setDisplayFps(false)
        isPauseOnLostFocus = false
        isShowSettings = false
        super.start()
        guiLauncher?.disableAll()
    }

    override fun simpleInitApp() {
        val midis2jam2 = DesktopMidis2jam2(sequencer, midiFile, m2j2settings)
        stateManager.attach(midis2jam2)
        rootNode.attachChild(midis2jam2.rootNode)
    }

    override fun stop() {
        stop(false)
        enableLauncher()
    }

    override fun enableLauncher() {
        guiLauncher?.enableAll()
    }
}