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
import com.install4j.api.launcher.SplashScreen
import kotlinx.coroutines.runBlocking
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.wysko.midis2jam2.gui.Launcher
import org.wysko.midis2jam2.gui.UpdateChecker.checkForUpdates
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.starter.loadSequencerJob
import org.wysko.midis2jam2.util.PassedSettings
import org.wysko.midis2jam2.util.Utils.resourceToString
import java.io.File
import javax.sound.midi.MidiSystem

/**
 * Where it all begins.
 */
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun main(args: Array<String>) {
    /* Get the default sequencer loaded in a coroutine now since it takes a while (about 1.5 seconds) to load. */
    loadSequencerJob.start()

    /* Check for command line arguments */
    if (args.isNotEmpty()) {
        val options = Options().apply {
            addOption("a", "autocam", false, "Enables auto-cam when starting.")
            addOption("d", "device", true, "The device to use for MIDI playback.")
            addOption("e", "legacy-engine", false, "Use the legacy window engine.")
            addOption("f", "settings.fullscreen", false, "Starts the application in fullscreen mode.")
            addOption("h", "help", false, "Prints a help message and exits.")
            addOption("l", "list-devices", false, "Lists the available MIDI devices and exits.")
            addOption(
                "s",
                "configuration.soundfont",
                true,
                "Specifies the SoundFont to use for MIDI playback by a path to a SoundFont file."
            )
            addOption("v", "version", false, "Prints the version of this program and exits.")
            addOption("y", "latency", true, "Adjusts the audio for A/V sync.")
        }
        val cmd = DefaultParser().parse(options, args)

        /* CLI arguments that just print text and exit */
        when {
            cmd.hasOption("help") -> {
                println(resourceToString("/man.txt"))
                return
            }
            cmd.hasOption("version") -> {
                println(resourceToString("/version.txt"))
                return
            }
            cmd.hasOption("list-devices") -> {
                MidiSystem.getMidiDeviceInfo().filter { it.name != "Real Time Sequencer" }.forEach {
                    println(it.name)
                }
                return
            }
        }

        /*** CLI ARGUMENTS ***/

        /* MIDI file */
        if (cmd.args.isEmpty()) {
            System.err.println("No MIDI file specified.")
            return
        }
        val midiFile = cmd.args[0]

        /* MIDI device */
        val midiDevice = if (cmd.hasOption("device")) cmd.getOptionValue("device") else "Gervill"

        /* SoundFont */
        val soundFont =
            if (cmd.hasOption("configuration.soundfont")) File(cmd.getOptionValue("configuration.soundfont")) else null

        /* Legacy window engine */
        val legacyWindowEngine = cmd.hasOption("legacy-engine")

        /* Fullscreen */
        val fullscreen = cmd.hasOption("settings.fullscreen")

        /* Auto-cam */
        val autoCam = cmd.hasOption("autocam")

        /* Latency */
        val latency = if (cmd.hasOption("latency")) cmd.getOptionValue("latency").toInt() else 0

        runBlocking {
            Execution.start(
                PassedSettings(
                    latency,
                    autoCam,
                    fullscreen,
                    legacyWindowEngine,
                    File(midiFile),
                    midiDevice,
                    soundFont
                ),
                onStart = {},
                onReady = {},
                onFinish = { Runtime.getRuntime().halt(0) }
            ).join()
        }
    } else {
        SplashScreen.writeMessage("Loading...")
        application {
            Window(
                onCloseRequest = ::exitApplication, title = "midis2jam2 launcher", state = rememberWindowState(
                    placement = WindowPlacement.Maximized, position = WindowPosition(Alignment.Center)
                ), icon = BitmapPainter(useResource("ico/icon32.png", ::loadImageBitmap))
            ) {
                Launcher()
            }
            checkForUpdates() // I'm checking for updates, whether you like it or not.
        }
    }
}