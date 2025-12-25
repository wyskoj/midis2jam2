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
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.world.AssetLoader
import javax.sound.midi.Synthesizer

internal actual class Midis2jam2Application(
    private val sequence: TimeBasedSequence,
    private val fileName: String,
    private val configurations: Collection<Configuration>,
    private val onFinish: () -> Unit,
    private val sequencer: JwSequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()

    actual fun execute() {
        try {
            applyConfigurations(configurations)
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
        Jme3ExceptionHandler.setup {
            stop()
            sequencer.stop()
            sequencer.close()
        }
        setupState(configurations, platform = Platform.Desktop)
        stateManager.attach(AssetLoader())
        val performanceAppState = DesktopPerformanceManager(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            fileName = fileName,
            configs = configurations,
        )
        stateManager.attach(performanceAppState)
        rootNode.attachChild(performanceAppState.root)
        addManagers(configurations, sequence, sequencer)
        stateManager.attach(MidiDeviceManager(configurations, midiDevice))
    }

    actual override fun stop() {
        onFinish()
        super.stop()
    }

    actual override fun destroy() {
        rootNode.detachAllChildren()
        assetManager.clearCache()
        renderer.invalidateState()
        inputManager.clearMappings()
        (context as LwjglContext).systemListener = null
        super.destroy()
    }
}

internal actual fun getCameraManager(): CameraManager = DesktopCameraManager()