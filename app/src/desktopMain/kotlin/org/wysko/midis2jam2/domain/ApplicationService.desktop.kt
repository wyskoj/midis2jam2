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
import org.wysko.midis2jam2.renderer.RendererCommand
import org.wysko.midis2jam2.renderer.RendererMessage
import org.wysko.midis2jam2.starter.MidiPackage
import org.wysko.midis2jam2.starter.Midis2jam2Application
import org.wysko.midis2jam2.starter.Midis2jam2QueueApplication
import org.wysko.midis2jam2.starter.applyConfigurations
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.ConfigurationService
import org.wysko.midis2jam2.util.isMacOs
import org.wysko.midis2jam2.util.logger
import java.io.File
import java.io.InputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val PROCESS_EXIT_GRACE_SECONDS = 5L
private const val RENDERER_DEBUG_ENV = "MIDIS2JAM2_RENDERER_DEBUG"

actual class ApplicationService : KoinComponent {
    private val errorLogService: ErrorLogService by inject()
    private val playbackHistoryStore: PlaybackHistoryStore by inject()
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
                manageRendererProcess(process)
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
                    recordPlaybackHistory(midiFile.file)
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
                manageRendererProcess(process, midiFiles.map { it.file })
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
                        onTrackStart = { trackIndex ->
                            executionState.queue.getOrNull(trackIndex)?.file?.let(::recordPlaybackHistory)
                        },
                        onPlaylistFinish = {
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

    private fun manageRendererProcess(process: Process, queueFiles: List<File> = emptyList()) {
        val reportedResult = AtomicBoolean(false)

        consumeLines("renderer-stdout", process.inputStream) { line ->
            when (val message = runCatching { Json.decodeFromString<RendererMessage>(line) }.getOrNull()) {
                null -> println(line)
                else -> handleRendererMessage(message, queueFiles, reportedResult)
            }
        }

        consumeLines("renderer-stderr", process.errorStream) { line -> System.err.println(line) }

        process.onExit().thenAccept { exited ->
            if (!reportedResult.get()) {
                errorLogService.addError(
                    "The rendering process stopped unexpectedly",
                    "The renderer exited with code ${exited.exitValue()}"
                )
            }
            _isApplicationRunning.value = false
        }

        Runtime.getRuntime().addShutdownHook(Thread { stopRenderer(process) })
    }

    private fun handleRendererMessage(
        message: RendererMessage,
        queueFiles: List<File>,
        reportedResult: AtomicBoolean,
    ) {
        when (message.type) {
            "QueueTrackStart" -> {
                val trackIndex = message.trackIndex ?: return
                queueFiles.getOrNull(trackIndex)?.let(::recordPlaybackHistory)
            }

            "Error" -> {
                errorLogService.addError(
                    message.message ?: "The renderer reported an error",
                    message.stackTrace ?: "No stacktrace"
                )
                reportedResult.set(true)
                _isApplicationRunning.value = false
            }

            "Finish" -> {
                reportedResult.set(true)
                _isApplicationRunning.value = false
            }
        }
    }

    private fun stopRenderer(process: Process) {
        runCatching {
            process.outputStream.bufferedWriter().use { writer ->
                writer.write(Json.encodeToString(RendererCommand.stop()))
                writer.newLine()
                writer.flush()
            }
        }
        if (!process.waitFor(PROCESS_EXIT_GRACE_SECONDS, TimeUnit.SECONDS)) {
            process.destroy()
        }
    }

    private fun consumeLines(threadName: String, stream: InputStream, onLine: (String) -> Unit) {
        Thread {
            runCatching { stream.bufferedReader().forEachLine(onLine) }
        }.apply {
            name = threadName
            isDaemon = true
        }.start()
    }

    private fun encodeBundle(rendererBundle: RendererBundle): String =
        Base64.getEncoder().encodeToString(Json.encodeToString(rendererBundle).encodeToByteArray())

    private fun getConfigurations(): List<Configuration> {
        val configurationService: ConfigurationService by inject()
        return configurationService.getConfigurations()
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

        val cmd = mutableListOf(javaExec)
        rendererDebugAgent()?.let { agent -> // must precede -cp when set
            cmd += agent
            logger().warn("Renderer debug agent enabled: $agent")
        }
        cmd += listOf("-cp", classpath, mainClass)
        cmd.addAll(extraArgs)

        return ProcessBuilder(cmd).start()
    }

    private fun rendererDebugAgent(): String? {
        val spec = System.getenv(RENDERER_DEBUG_ENV)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val suspend = if (spec.endsWith(":nosuspend")) "n" else "y"
        val port = spec.removeSuffix(":nosuspend")
        return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=$suspend,address=127.0.0.1:$port"
    }

    private fun recordPlaybackHistory(file: File) {
        playbackHistoryStore.addPlayback(
            filePath = file.absolutePath,
            title = file.name,
        )
    }
}
