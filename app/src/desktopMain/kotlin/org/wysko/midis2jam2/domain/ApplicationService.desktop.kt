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
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.midi.toPerformanceSequence
import org.wysko.midis2jam2.record.GERVILL_EXPORTS_FLAG
import org.wysko.midis2jam2.record.ProgressEstimator
import org.wysko.midis2jam2.record.RecordOptions
import org.wysko.midis2jam2.record.Recorder
import org.wysko.midis2jam2.record.RecordingState
import org.wysko.midis2jam2.renderer.RendererBundle
import org.wysko.midis2jam2.renderer.RendererCommand
import org.wysko.midis2jam2.renderer.RendererMessage
import org.wysko.midis2jam2.starter.MidiPackage
import org.wysko.midis2jam2.starter.Midis2jam2Application
import org.wysko.midis2jam2.starter.Midis2jam2QueueApplication
import org.wysko.midis2jam2.starter.applyConfigurations
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import org.wysko.midis2jam2.starter.configuration.PerformanceConfigFactory
import org.wysko.midis2jam2.util.isMacOs
import org.wysko.midis2jam2.util.logger
import java.io.File
import java.io.InputStream
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

private const val PROCESS_EXIT_GRACE_SECONDS = 5L
private const val RENDERER_DEBUG_ENV = "MIDIS2JAM2_RENDERER_DEBUG"

actual class ApplicationService : KoinComponent, Recorder {
    private val errorLogService: ErrorLogService by inject()
    private val playbackHistoryStore: PlaybackHistoryStore by inject()
    private val _isApplicationRunning = MutableStateFlow(false)
    actual val isApplicationRunning: StateFlow<Boolean>
        get() = _isApplicationRunning

    override val isBusy: StateFlow<Boolean>
        get() = _isApplicationRunning

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val recordingState: StateFlow<RecordingState>
        get() = _recordingState

    @Volatile
    private var recordingProcess: Process? = null
    private val progressEstimator = ProgressEstimator()

    /**
     * Recording always runs in a renderer process of its own, which is started with the flag offline audio needs and
     * can be stopped cleanly with [cancelRecording].
     */
    override fun startRecording(midiFile: File, options: RecordOptions) {
        check(!_isApplicationRunning.value) { "A performance is already running." }
        _isApplicationRunning.value = true
        _recordingState.value = RecordingState.Rendering(progress = null)
        progressEstimator.reset()
        val bundle = encodeBundle(
            RendererBundle(
                midiFiles = listOf(midiFile.absolutePath),
                config = createConfig(isLooping = false),
                recordOptions = options,
            )
        )
        val process = launchRendererProcess(extraArgs = listOf(bundle))
        recordingProcess = process
        manageRendererProcess(process)
    }

    override fun cancelRecording() {
        val process = recordingProcess ?: return
        Thread({ stopRenderer(process) }, "renderer-stop").apply { isDaemon = true }.start()
    }

    actual fun startApplication(executionState: ExecutionState) {
        _isApplicationRunning.value = true
        val config = createConfig(isLooping = executionState.isLooping)
        val midiFile = executionState.midiFile

        when {
            isMacOs() -> {
                val bundle = encodeBundle(
                    RendererBundle(midiFiles = listOf(midiFile.file.absolutePath), config)
                )
                val process = launchRendererProcess(extraArgs = listOf(bundle))
                manageRendererProcess(process)
            }

            else -> {
                val midiPackage = runCatching {
                    MidiPackage.build(
                        midiFile.file,
                        config
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
                        config,
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
        val config = createConfig(isLooping = false)

        when {
            isMacOs() -> {
                val bundle = encodeBundle(
                    RendererBundle(midiFiles = midiFiles.map { it.file.absolutePath }, config)
                )
                val process = launchRendererProcess(extraArgs = listOf(bundle))
                manageRendererProcess(process, midiFiles.map { it.file })
            }

            else -> {
                val midiPackage = runCatching { MidiPackage.build(null, config) }.onFailure { t ->
                    errorLogService.addError("There was an error initializing the MIDI device.", t.stackTraceToString())
                    _isApplicationRunning.value = false
                    return
                }

                val reader = StandardMidiFileReader()
                val sequences = executionState.queue.map { reader.readFile(it.file).toPerformanceSequence(config.settings) }

                with(midiPackage.getOrNull() ?: return) {
                    Midis2jam2QueueApplication(
                        sequences = sequences,
                        fileNames = executionState.queue.map { it.file.name },
                        config,
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
                        applyConfigurations(config)
                        start()
                    }
                }
            }
        }
    }

    private fun manageRendererProcess(process: Process, queueFiles: List<File> = emptyList()) {
        val reportedResult = AtomicBoolean(false)

        val stdout = consumeLines("renderer-stdout", process.inputStream) { line ->
            when (val message = runCatching { Json.decodeFromString<RendererMessage>(line) }.getOrNull()) {
                null -> println(line)
                else -> handleRendererMessage(message, queueFiles, reportedResult)
            }
        }

        consumeLines("renderer-stderr", process.errorStream) { line -> System.err.println(line) }

        process.onExit().thenAccept { exited ->
            // The process can be seen to exit before its last messages have been read.
            stdout.join(PROCESS_EXIT_GRACE_SECONDS * 1000)
            if (!reportedResult.get()) {
                errorLogService.addError(
                    "The rendering process stopped unexpectedly",
                    "The renderer exited with code ${exited.exitValue()}"
                )
            }
            if (process === recordingProcess) {
                recordingProcess = null
                if (_recordingState.value is RecordingState.Rendering) {
                    _recordingState.value = RecordingState.Failed("The recording stopped unexpectedly.")
                }
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
                if (_recordingState.value is RecordingState.Rendering) {
                    _recordingState.value = RecordingState.Failed(message.message ?: "The recording failed.")
                }
                _isApplicationRunning.value = false
            }

            "Finish" -> {
                reportedResult.set(true)
                _isApplicationRunning.value = false
            }

            RendererMessage.RECORD_PROGRESS -> {
                val frames = message.framesCaptured ?: return
                val expected = message.expectedFrames?.takeIf { it > 0 } ?: return
                val progress = (frames.toFloat() / expected).coerceIn(0f, 1f)
                _recordingState.value = RecordingState.Rendering(progress, progressEstimator.remaining(progress))
            }

            RendererMessage.RECORD_FINISHED -> {
                _recordingState.value = RecordingState.Finished(File(message.path ?: return))
            }

            RendererMessage.RECORD_CANCELLED -> {
                _recordingState.value = RecordingState.Cancelled
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

    private fun consumeLines(threadName: String, stream: InputStream, onLine: (String) -> Unit): Thread =
        Thread {
            runCatching { stream.bufferedReader().forEachLine(onLine) }
        }.apply {
            name = threadName
            isDaemon = true
            start()
        }

    private fun encodeBundle(rendererBundle: RendererBundle): String =
        Base64.getEncoder().encodeToString(Json.encodeToString(rendererBundle).encodeToByteArray())

    private fun createConfig(isLooping: Boolean): PerformanceConfig {
        val factory: PerformanceConfigFactory by inject()
        return factory.create(isLooping)
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
        cmd += GERVILL_EXPORTS_FLAG // for recording's offline audio
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
