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

package org.wysko.midis2jam2

import com.install4j.api.launcher.SplashScreen
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.ExecutionState
import org.wysko.midis2jam2.record.RecordCliArgs
import org.wysko.midis2jam2.record.RecordTabPersistor
import org.wysko.midis2jam2.record.RecordingState
import java.io.File
import kotlin.system.exitProcess

/** The exit code for a command line that can't be run. */
private const val EXIT_USAGE = 2

/** How often, in percent, recording progress is printed. */
private const val PROGRESS_STEP = 5

object CmdStart : KoinComponent {
    fun start(args: Array<String>) {
        if (args.isEmpty()) return

        val applicationService: ApplicationService by inject()

        when (val record = RecordCliArgs.parse(args)) {
            null -> startApplicationWithFile(applicationService, PlatformFile(File(args.first())))
            is RecordCliArgs.Invalid -> {
                System.err.println(record.message)
                System.err.println(RecordCliArgs.USAGE)
                exitProcess(EXIT_USAGE)
            }

            is RecordCliArgs.Record -> exitProcess(record(applicationService, record))
        }
    }

    /**
     * Records as [command] says, printing progress, and returns the exit code: zero only if the video was saved.
     */
    private fun record(applicationService: ApplicationService, command: RecordCliArgs.Record): Int {
        val options = command.toOptions(RecordTabPersistor.forUser().load())
        println("Recording ${command.midiFile} to ${options.outputPath} (${options.width}x${options.height}, ${options.fps} fps)")
        applicationService.startRecording(command.midiFile, options)
        try {
            SplashScreen.hide()
        } catch (_: Exception) {}

        var lastPercent = -1
        val result = runBlocking {
            applicationService.recordingState.first { state ->
                if (state is RecordingState.Rendering && state.progress != null) {
                    val percent = (state.progress * 100).toInt()
                    if (percent / PROGRESS_STEP > lastPercent / PROGRESS_STEP) {
                        println("$percent%")
                        lastPercent = percent
                    }
                }
                state is RecordingState.Finished || state is RecordingState.Cancelled || state is RecordingState.Failed
            }
        }
        runBlocking { applicationService.isApplicationRunning.first { !it } }

        return when (result) {
            is RecordingState.Finished -> {
                println("Saved ${result.file}")
                0
            }

            is RecordingState.Failed -> {
                System.err.println("The recording failed: ${result.message}")
                1
            }

            else -> {
                System.err.println("The recording was cancelled.")
                1
            }
        }
    }

    /**
     * Starts the application with a specific MIDI file.
     * This method can be called from both command line args and startup listener events.
     */
    fun startApplicationWithFile(
        applicationService: ApplicationService,
        midiFile: PlatformFile
    ) {
        applicationService.startApplication(ExecutionState(midiFile))
        try {
            SplashScreen.hide()
        } catch (_: Exception) {}
        runBlocking {
            applicationService.isApplicationRunning.first { !it }
        }
    }
}
