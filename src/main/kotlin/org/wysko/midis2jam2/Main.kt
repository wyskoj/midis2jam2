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
import org.wysko.midis2jam2.gui.UpdateChecker.checkForUpdates
import org.wysko.midis2jam2.gui.launcherState
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.starter.loadSequencerJob
import java.io.File
import java.util.*

/**
 * The configuration directory.
 */
const val CONFIGURATION_DIRECTORY: String = ".midis2jam2"


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

    /* Check for command line arguments */
    SplashScreen.writeMessage("Loading...")
    FlatDarkLaf.setup()
    application {
        var setFreeze: ((Boolean) -> Unit)? = null
        Window(
            onCloseRequest = ::exitApplication, title = "midis2jam2 launcher", state = rememberWindowState(
                placement = WindowPlacement.Maximized, position = WindowPosition(Alignment.Center)
            ), icon = BitmapPainter(useResource("ico/icon32.png", ::loadImageBitmap))
        ) {
            setFreeze = Launcher()
        }
        if (args.isNotEmpty()) {
            Execution.start(
                properties = Properties().apply {
                    setProperty("midi_file", args.first())
                    setProperty("midi_device", launcherState.getProperty("midi_device"))
                },
                onStart = {
                    setFreeze?.invoke(true)
                },
                onReady = {},
                onFinish = {
                    println("unfreezing!!")
                    setFreeze?.invoke(false)
                }
            )
        }
        checkForUpdates() // I'm checking for updates, whether you like it or not.
    }
}