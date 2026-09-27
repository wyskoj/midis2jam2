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

package org.wysko.midis2jam2.renderer

import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.di.applicationModule
import org.wysko.midis2jam2.di.midiSystemModule
import org.wysko.midis2jam2.di.systemModule
import org.wysko.midis2jam2.di.uiModule
import org.wysko.midis2jam2.starter.MidiPackage
import org.wysko.midis2jam2.starter.Midis2jam2Application
import org.wysko.midis2jam2.starter.Midis2jam2QueueApplication
import org.wysko.midis2jam2.starter.applyConfigurations
import java.io.BufferedWriter
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val protocol = System.out.bufferedWriter()
    installFatalErrorReporter(protocol)

    startKoin { modules(applicationModule, midiSystemModule, systemModule, uiModule) }
    val arguments = Base64.getDecoder().decode(args.first()).toString(Charsets.UTF_8)
    val config = Json.decodeFromString<RendererBundle>(arguments)
    val midiFiles = config.midiFiles.map { File(it) }

    when (midiFiles.size) {
        0 -> reportNoFiles(protocol)
        1 -> launchApplication(midiFiles, config, protocol)
        else -> launchQueueApplication(midiFiles, config, protocol)
    }
}

private fun launchApplication(
    midiFiles: List<File>,
    config: RendererBundle,
    protocol: BufferedWriter,
) {
    val midiFile = midiFiles.first()
    val midiPackage = runCatching { MidiPackage.build(midiFile, config.configurations) }.onFailure { t ->
        onFailGetMidiPackage(t, protocol)
        return
    }
    val latch = CountDownLatch(1)
    with(midiPackage.getOrNull() ?: return) {
        val application = Midis2jam2Application(
            sequence!!,
            midiFile.name,
            config.configurations,
            {
                latch.countDown()
                protocol.send(RendererMessage.finish())
            },
            sequencer,
            synthesizer,
            midiDevice
        )
        watchParentCommands { application.stop() }
        application.execute()
    }
    latch.await()
}

private fun launchQueueApplication(
    midiFiles: List<File>,
    config: RendererBundle,
    protocol: BufferedWriter,
) {
    val reader = StandardMidiFileReader()
    val sequences = midiFiles.map { reader.readFile(it).toTimeBasedSequence() }

    val midiPackage = runCatching { MidiPackage.build(null, config.configurations) }.onFailure { t ->
        onFailGetMidiPackage(t, protocol)
        return
    }

    with(midiPackage.getOrNull() ?: return) {
        val application = Midis2jam2QueueApplication(
            sequences = sequences,
            fileNames = midiFiles.map { it.name },
            config.configurations,
            onTrackStart = { trackIndex -> protocol.send(RendererMessage.queueTrackStart(trackIndex)) },
            { protocol.send(RendererMessage.finish()) },
            sequencer,
            synthesizer,
            midiDevice
        )
        watchParentCommands { application.stop() }
        application.run {
            applyConfigurations(config.configurations)
            start()
        }
    }
}

private fun BufferedWriter.send(message: RendererMessage) {
    runCatching {
        write(Json.encodeToString(message))
        newLine()
        flush()
    }
}

private fun watchParentCommands(onStop: () -> Unit) {
    Thread {
        val reader = System.`in`.bufferedReader()
        while (true) {
            val line = runCatching { reader.readLine() }.getOrNull() ?: break
            val command = runCatching { Json.decodeFromString<RendererCommand>(line) }.getOrNull()
            if (command?.type == RendererCommand.STOP) break
        }
        onStop()
    }.apply {
        name = "parent-command-watcher"
        isDaemon = true
    }.start()
}

private fun onFailGetMidiPackage(t: Throwable, protocol: BufferedWriter) {
    t.printStackTrace()
    protocol.send(
        RendererMessage.error("There was an error initializing the MIDI device.", t.stackTraceToString())
    )
}

private fun reportNoFiles(protocol: BufferedWriter) {
    val cause = IllegalArgumentException("No MIDI files passed to the renderer!")
    protocol.send(RendererMessage.error(cause.message!!, cause.stackTraceToString()))
}

private fun installFatalErrorReporter(protocol: BufferedWriter) {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        throwable.printStackTrace()
        protocol.send(
            RendererMessage.error(
                "The 3D engine stopped unexpectedly.",
                "Uncaught exception on thread \"${thread.name}\":\n${throwable.stackTraceToString()}"
            )
        )
        exitProcess(1)
    }
}
