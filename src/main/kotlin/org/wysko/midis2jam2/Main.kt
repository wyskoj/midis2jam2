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

package org.wysko.midis2jam2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.*
import com.formdev.flatlaf.FlatDarkLaf
import com.install4j.api.launcher.SplashScreen
import org.wysko.midis2jam2.gui.Launcher
import org.wysko.midis2jam2.gui.LauncherController
import org.wysko.midis2jam2.gui.UpdateChecker.checkForUpdates
import org.wysko.midis2jam2.gui.launcherState
import org.wysko.midis2jam2.midi.search.MIDISearchFrame
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.starter.loadSequencerJob
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import java.util.*
import javax.swing.UIManager

/**
 * The configuration directory.
 */
const val CONFIGURATION_DIRECTORY: String = ".midis2jam2"

var launcherController: LauncherController? = null
/**
 * Where it all begins.
 */
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun main(args: Array<String>) {
    /* Get the default sequencer loaded in a coroutine now since it takes a while (about 1.5 seconds) to load. */
    loadSequencerJob.start()

    /* Ensure configuration folders are initialized */
    File(System.getProperty("user.home"), ".midis2jam2").also {
        it.mkdirs()
    }

    SplashScreen.writeMessage("Loading...")

    /* Initialize themes */
    try {
        UIManager.setLookAndFeel(FlatDarkLaf())
    } catch (e: Exception) {
        with(Main.logger()) {
            warn("Failed to initialize FlatLaf theme, reverting to default.")
            warn(Utils.exceptionToLines(e))
        }
    }

    application {
        Window(
            onCloseRequest = ::exitApplication, title = "midis2jam2 launcher", state = rememberWindowState(
                placement = WindowPlacement.Maximized, position = WindowPosition(Alignment.Center)
            ), icon = BitmapPainter(useResource("ico/icon32.png", ::loadImageBitmap))
        ) {
            launcherController = Launcher()
            this.window.contentPane.dropTarget = object : DropTarget() {
                @Synchronized
                override fun drop(dtde: DropTargetDropEvent) {
                    dtde.let {
                        it.acceptDrop(DnDConstants.ACTION_REFERENCE)
                        (it.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>).firstOrNull()
                            ?.let { file -> launcherController?.setSelectedFile?.invoke(file as File) }
                    }
                }
            }
        }
        if (args.isNotEmpty()) {
            Execution.start(
                properties = Properties().apply {
                    setProperty("midi_file", args.first())
                    setProperty("midi_device", launcherState.getProperty("midi_device"))
                },
                onStart = {
                    launcherController?.setFreeze?.invoke(true)
                    MIDISearchFrame.lock()
                },
                onReady = {},
                onFinish = {
                    launcherController?.setFreeze?.invoke(false)
                    MIDISearchFrame.unlock()
                }
            )
        }
        checkForUpdates() // I'm checking for updates, whether you like it or not.
    }
}

/**
 * The entrypoint class. Used for logging purposes.
 */
object Main