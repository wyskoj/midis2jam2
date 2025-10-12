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
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.CountDownLatch

const val SERVER_PORT = 31320

fun main(args: Array<String>) {
    startKoin {
        modules(applicationModule, midiSystemModule, systemModule, uiModule)
    }
    val arguments = Base64.getDecoder().decode(args.first()).toString(Charsets.UTF_8)
    val config = Json.decodeFromString<RendererBundle>(arguments)
    val serverWriter = startTcpListener()
    val midiFiles = config.midiFiles.map { File(it) }

    when (midiFiles.size) {
        0 -> exit(serverWriter)

        1 -> {
            val midiFile = midiFiles.first()
            val midiPackage = runCatching { MidiPackage.build(midiFile, config.configurations) }.onFailure { t ->
                onFailGetMidiPackage(t, serverWriter)
                return
            }
            val latch = CountDownLatch(1)
            with(midiPackage.getOrNull() ?: return) {
                Midis2jam2Application(
                    sequence!!,
                    midiFile.name,
                    config.configurations,
                    {
                        latch.countDown()
                        serverWriter.write(Json.encodeToString(RendererMessage.finish()))
                        serverWriter.flush()
                        serverWriter.close()
                    },
                    sequencer,
                    synthesizer,
                    midiDevice
                ).execute()
            }
            latch.await()
        }

        else -> {
            val reader = StandardMidiFileReader()
            val sequences = midiFiles.map { reader.readFile(it).toTimeBasedSequence() }

            val midiPackage = runCatching { MidiPackage.build(null, config.configurations) }.onFailure { t ->
                onFailGetMidiPackage(t, serverWriter)
                return
            }

            with(midiPackage.getOrNull() ?: return) {
                Midis2jam2QueueApplication(
                    sequences = sequences,
                    fileNames = midiFiles.map { it.name },
                    config.configurations,
                    {
                        serverWriter.write(Json.encodeToString(RendererMessage.finish()))
                        serverWriter.flush()
                        serverWriter.close()
                    },
                    sequencer,
                    synthesizer,
                    midiDevice
                ).run {
                    applyConfigurations(config.configurations)
                    start()
                }
            }
        }
    }
}

private fun onFailGetMidiPackage(t: Throwable, serverWriter: BufferedWriter) {
    t.printStackTrace()
    serverWriter.write(
        Json.encodeToString(
            RendererMessage.error(
                "There was an error initializing the MIDI device.",
                t.stackTraceToString()
            )
        )
    )
    serverWriter.flush()
    serverWriter.close()
}

private fun exit(serverWriter: BufferedWriter) {
    serverWriter.write(
        Json.encodeToString(
            RendererMessage.error(
                "No MIDI files passed to renderer server.",
                IllegalArgumentException("No MIDI files passed to renderer server.").stackTraceToString()
            )
        )
    )
    serverWriter.flush()
    serverWriter.close()
}

private fun startTcpListener(): BufferedWriter {
    val server = ServerSocket(SERVER_PORT)
    val client = server.accept()
    val writer = client.getOutputStream().bufferedWriter()
    return writer
}
