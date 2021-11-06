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
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.gui.Displays
import org.wysko.midis2jam2.gui.GuiLauncher
import org.wysko.midis2jam2.midi.JavaXSequencer
import org.wysko.midis2jam2.midi.MidiFile
import org.wysko.midis2jam2.util.M2J2Settings
import org.wysko.midis2jam2.util.Utils.exceptionToLines
import java.awt.Canvas
import java.awt.Dimension
import java.awt.Toolkit
import javax.sound.midi.Sequencer
import javax.swing.SwingUtilities

open class Liaison(
    protected val guiLauncher: GuiLauncher?,
    protected val sequencer: Sequencer,
    protected val midiFile: MidiFile,
    protected val m2j2settings: M2J2Settings,
    protected val fullscreen: Boolean
) : SimpleApplication() {

    val sequencerHandler = JavaXSequencer(sequencer)

    companion object {
        private val midis2Jam2Settings = AppSettings(true)

        init {
            // Set settings
            midis2Jam2Settings.frameRate = 120
            midis2Jam2Settings.frequency = 60
            midis2Jam2Settings.isVSync = true
            midis2Jam2Settings.isResizable = true
            midis2Jam2Settings.samples = 4
            midis2Jam2Settings.isGammaCorrection = false
        }
    }

    private var display: Displays? = null
    private var canvas: Canvas? = null
    private var midis2jam2: DesktopMidis2jam2? = null
    fun start(displayType: Class<out Displays?>) {
        setDisplayStatView(false)
        setDisplayFps(false)
        isPauseOnLostFocus = false
        isShowSettings = false
        val screen = Toolkit.getDefaultToolkit().screenSize
        if (fullscreen) {
            midis2Jam2Settings.isFullscreen = true
            midis2Jam2Settings.setResolution(screen.width, screen.height)
            setSettings(midis2Jam2Settings)
            super.start()
        } else {
            val thisLiaison = this
            SwingUtilities.invokeLater {
                setSettings(midis2Jam2Settings)
                setDisplayStatView(false)
                setDisplayFps(false)
                isPauseOnLostFocus = false
                isShowSettings = false
                createCanvas()
                val ctx = getContext() as JmeCanvasContext
                ctx.setSystemListener(thisLiaison)
                val dim = Dimension((screen.width * 0.95).toInt(), (screen.height * 0.85).toInt())
                canvas = ctx.canvas
                canvas?.preferredSize = dim
                try {
                    val constructor = displayType.getConstructor(
                        Liaison::class.java,
                        Canvas::class.java, Midis2jam2::class.java
                    )
                    display = constructor.newInstance(this, ctx.canvas, midis2jam2)
                    display?.display()
                } catch (e: ReflectiveOperationException) {
                    Midis2jam2.getLOGGER().severe(exceptionToLines(e))
                }
                startCanvas()
            }
        }
        guiLauncher?.disableAll()
    }

    override fun simpleInitApp() {
        midis2jam2 = DesktopMidis2jam2(sequencer, midiFile, m2j2settings)
        midis2jam2?.setWindow(display)
        stateManager.attach(midis2jam2)
        rootNode.attachChild(midis2jam2!!.rootNode)
    }

    override fun stop() {
        super.stop()
        if (!fullscreen) {
            display?.isVisible = false
            canvas?.isEnabled = false
        }
        enableLauncher()

        /* If the GuiLauncher is null, this likely means that the program was started
         * from the command line. In this case, we should exit the program. */
        if (guiLauncher == null) {
            Runtime.getRuntime().halt(0) // This is pretty harsh, but it works.
        }
    }

    /** Unlock the [guiLauncher] so the user can use it. */
    open fun enableLauncher() {
        guiLauncher?.enableAll()
    }
}