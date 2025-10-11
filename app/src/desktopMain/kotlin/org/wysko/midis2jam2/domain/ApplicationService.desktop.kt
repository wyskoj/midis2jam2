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

package org.wysko.midis2jam2.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.renderer.RendererBundle
import org.wysko.midis2jam2.renderer.RendererMessage
import org.wysko.midis2jam2.renderer.SERVER_PORT
import org.wysko.midis2jam2.starter.MidiPackage
import org.wysko.midis2jam2.starter.Midis2jam2Application
import org.wysko.midis2jam2.starter.Midis2jam2QueueApplication
import org.wysko.midis2jam2.starter.applyConfigurations
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.ConfigurationService
import java.io.File
import java.net.Socket
import java.util.Base64

actual class ApplicationService : KoinComponent {
    private val errorLogService: ErrorLogService by inject()
    private val _isApplicationRunning = MutableStateFlow(false)
    actual val isApplicationRunning: StateFlow<Boolean>
        get() = _isApplicationRunning

    actual fun startApplication(executionState: ExecutionState) {
        _isApplicationRunning.value = true
        val configurations = getConfigurations()
        val midiFile = executionState.midiFile

        when {
            isMacOs() -> {
                val bundle = encodeBundle(
                    RendererBundle(midiFiles = listOf(midiFile.file.absolutePath), configurations)
                )
                val process = launchRendererProcess(extraArgs = listOf(bundle))
                listenOnSocket(process)
            }

            else -> {
                val midiPackage = runCatching {
                    MidiPackage.build(
                        midiFile.file,
                        configurations
                    )
                }.onFailure { t ->
                    errorLogService.addError("There was an error initializing the MIDI device.", t.stackTraceToString())
                    _isApplicationRunning.value = false
                    return
                }
                with(midiPackage.getOrNull() ?: return) {
                    Midis2jam2Application(
                        sequence!!,
                        fileName = midiFile.file.name,
                        configurations,
                        onFinish = {
                            _isApplicationRunning.value = false
                        },
                        sequencer,
                        synthesizer,
                        midiDevice
                    ).execute()
                }
            }
        }
    }

    actual fun startQueueApplication(executionState: QueueExecutionState) {
        _isApplicationRunning.value = true
        val midiFiles = executionState.queue
        val configurations = getConfigurations()

        when {
            isMacOs() -> {
                val bundle = encodeBundle(
                    RendererBundle(midiFiles = midiFiles.map { it.file.absolutePath }, configurations)
                )
                val process = launchRendererProcess(extraArgs = listOf(bundle))
                listenOnSocket(process)
            }

            else -> {
                val midiPackage = runCatching { MidiPackage.build(null, configurations) }.onFailure { t ->
                    errorLogService.addError("There was an error initializing the MIDI device.", t.stackTraceToString())
                    _isApplicationRunning.value = false
                    return
                }

                val reader = StandardMidiFileReader()
                val sequences = executionState.queue.map { reader.readFile(it.file).toTimeBasedSequence() }

                with(midiPackage.getOrNull() ?: return) {
                    Midis2jam2QueueApplication(
                        sequences = sequences,
                        fileNames = executionState.queue.map { it.file.name },
                        configurations,
                        {
                            _isApplicationRunning.value = false
                        },
                        sequencer,
                        synthesizer,
                        midiDevice
                    ).run {
                        applyConfigurations(configurations)
                        start()
                    }
                }
            }
        }
    }

    private fun listenOnSocket(process: Process) {
        Thread {
            val socket = waitForPort("127.0.0.1", SERVER_PORT)
            socket.inputStream.bufferedReader().forEachLine {
                val message = Json.decodeFromString<RendererMessage>(it)
                when (message.type) {
                    "Error" -> {
                        errorLogService.addError(message.message!!, message.stackTrace!!)
                        _isApplicationRunning.value = false
                    }

                    "Finish" -> {
                        _isApplicationRunning.value = false
                    }
                }
            }
        }.start()
        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })
    }

    private fun encodeBundle(rendererBundle: RendererBundle): String =
        Base64.getEncoder().encodeToString(Json.encodeToString(rendererBundle).encodeToByteArray())

    private fun getConfigurations(): List<Configuration> {
        val configurationService: ConfigurationService by inject()
        return configurationService.getConfigurations()
    }

    private fun waitForPort(host: String, port: Int, timeoutMs: Long = 10000): Socket {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                return Socket(host, port)
            } catch (_: Exception) {
                Thread.sleep(50)
            }
        }
        error("Timed out waiting for $host:$port")
    }

    private fun detectJavaExecutable(): String {
        val javaHome = System.getProperty("java.home") // works in both cases
        val bin = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "java.exe" else "java"
        return "$javaHome/bin/$bin"
    }

    private fun detectRendererClasspath(): String {
        val cp = System.getProperty("java.class.path")
        return when {
            cp.contains("build") -> {
                // Likely running in IntelliJ or Gradle
                cp
            }
            else -> {
                // Likely running from install4j or packaged distribution
                val appHome = System.getProperty("install4j.appDir")
                    ?: File(".").absolutePath // fallback

                File(appHome).listFiles { f -> f.extension == "jar" }
                    ?.joinToString(File.pathSeparator) { it.absolutePath }
                    ?: error("Could not detect classpath")
            }
        }
    }

    private fun launchRendererProcess(
        mainClass: String = "org.wysko.midis2jam2.renderer.RendererMainKt",
        extraArgs: List<String> = emptyList(),
    ): Process {
        val javaExec = detectJavaExecutable()
        val classpath = detectRendererClasspath()

        val cmd = mutableListOf(javaExec, /*"-XstartOnFirstThread",*/ "-cp", classpath, mainClass)
        cmd.addAll(extraArgs)

        return ProcessBuilder(cmd).inheritIO().redirectErrorStream(true).start()
    }

    private fun isMacOs(): Boolean {
//        return System.getProperty("os.name").lowercase().contains("mac")
        return true
    }
}
