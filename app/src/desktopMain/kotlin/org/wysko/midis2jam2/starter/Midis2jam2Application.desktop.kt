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

@file:Suppress("TooGenericExceptionCaught")

package org.wysko.midis2jam2.starter

import Platform
import ch.qos.logback.core.util.EnvUtil.isWindows
import com.jme3.app.SimpleApplication
import com.jme3.system.lwjgl.LwjglContext
import org.koin.mp.KoinPlatformTools
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.DesktopPerformanceManager
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.domain.Jme3ExceptionHandler
import org.wysko.midis2jam2.manager.MidiDeviceManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.DesktopCameraManager
import org.wysko.midis2jam2.domain.settings.AppSettings.GraphicsSettings.WindowMode
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.record.FixedStepTimer
import org.wysko.midis2jam2.record.OfflineSynthesizer
import org.wysko.midis2jam2.record.RecordOptions
import org.wysko.midis2jam2.record.RecordingListener
import org.wysko.midis2jam2.record.RecordingManager
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import org.wysko.midis2jam2.world.AssetLoader
import java.lang.invoke.MethodHandles
import javax.sound.midi.Synthesizer

internal actual class Midis2jam2Application(
    private val sequence: TimeBasedSequence,
    private val fileName: String,
    private val config: PerformanceConfig,
    private val onFinish: () -> Unit,
    private val sequencer: JwSequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
    private val recording: Recording? = null,
) : SimpleApplication() {
    /**
     * Records the performance instead of playing it. The sound comes from [synthesizer], rendered offline, so the
     * sequencer passed alongside should be silent and [synthesizer] should also be the MIDI device.
     */
    class Recording(
        val options: RecordOptions,
        val synthesizer: OfflineSynthesizer,
        val listener: RecordingListener,
    )

    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()

    actual fun execute() {
        try {
            applyConfigurations(config)
            recording?.let { configureForRecording(it.options) }
            start()
        } catch (e: Exception) {
            e.printStackTrace()
            errorLogService.addError(
                message = "There was an error applying configurations.",
                stackTrace = e.stackTraceToString()
            )
            onFinish()
        }
    }

    actual override fun simpleInitApp() {
        installGlfwJoystickCallbackWorkaround()
        if (config.settings.graphicsSettings.windowMode ==
            WindowMode.BorderlessFullscreen && isWindows()
        ) {
            applyBorderlessWindow(context)
        }
        Jme3ExceptionHandler.setup {
            stop()
            sequencer.stop()
            sequencer.close()
        }
        setupState(config, platform = Platform.Desktop)
        stateManager.attach(AssetLoader())
        val performanceAppState = DesktopPerformanceManager(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            fileName = fileName,
            config = config,
        )
        stateManager.attach(performanceAppState)
        rootNode.attachChild(performanceAppState.root)
        val recordingManager = recording?.let {
            RecordingManager(it.options, sequence, it.synthesizer, it.listener)
        }
        addManagers(
            config,
            sequence,
            sequencer,
            onPlaybackComplete = recordingManager?.let { manager -> { manager.complete(); stop() } },
            isRecording = recording != null,
        )
        stateManager.attach(MidiDeviceManager(config, midiDevice))
        // After the MIDI device manager, whose reset must reach the synthesizer before the song does.
        recordingManager?.let { stateManager.attach(it) }
    }

    /** Draws at the video's size, as fast as frames can be encoded rather than at the screen's pace. */
    private fun configureForRecording(options: RecordOptions) {
        settings.apply {
            width = options.width
            height = options.height
            isFullscreen = false
            isVSync = false
            frameRate = -1
            title = "midis2jam2 — Recording"
        }
        setTimer(FixedStepTimer(options.fps))
    }

    actual override fun stop() {
        onFinish()
        super.stop()
    }

    actual override fun destroy() {
        rootNode.detachAllChildren()
        assetManager.clearCache()
        inputManager.clearMappings()
        (context as LwjglContext).systemListener = null
        super.destroy()
    }
}

internal actual fun getCameraManager(): CameraManager = DesktopCameraManager()