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

package org.wysko.midis2jam2.starter

import Platform
import com.jme3.app.SimpleApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.midis2jam2.AndroidPerformanceManager
import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.domain.Jme3ExceptionHandler
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.manager.AndroidInputManager
import org.wysko.midis2jam2.manager.LoadingProgressManager
import org.wysko.midis2jam2.manager.MidiDeviceManager
import org.wysko.midis2jam2.manager.camera.AndroidCameraManager
import org.wysko.midis2jam2.manager.camera.CameraManager
import org.wysko.midis2jam2.manager.camera.CameraStateListener
import org.wysko.midis2jam2.midi.system.JwSequencerImpl
import org.wysko.midis2jam2.util.logger
import org.wysko.midis2jam2.util.state
import org.wysko.midis2jam2.world.AssetLoader

internal actual class Midis2jam2Application(
    private val onFinish: () -> Unit = {},
) : SimpleApplication(), KoinComponent {

    actual fun execute() = Unit

    override fun start() {
        super.start()
        logger().debug("Midis2jam2Application: start() called")
    }

    actual override fun simpleInitApp() {
        logger().debug("Midis2jam2Application: simpleInitApp() called")

        val errorLogService: ErrorLogService by inject()
        val applicationService: ApplicationService by inject()
        val midiService: MidiService by inject()
        val midiFile = applicationService.midiFile.value!!
        val configurations = applicationService.configurations.value

        CoroutineScope(Dispatchers.Default).launch {
            val sequence = StandardMidiFileReader().readByteArray(midiFile.readBytes()).toTimeBasedSequence()
            val midiDevice = midiService.getMidiDevices().first()
            val sequencer = JwSequencerImpl().apply {
                open(midiDevice)
                this.sequence = sequence
            }

            Jme3ExceptionHandler.setup {
                stop()
                sequencer.stop()
                sequencer.close()
            }

            enqueue {
                setupState(configurations, platform = Platform.Desktop)
                val loadingProgressManager = LoadingProgressManager()
                stateManager.attach(loadingProgressManager)
                stateManager.attach(AssetLoader {
                    loadingProgressManager.onLoadingAsset(it)
                })
                val performanceAppState = AndroidPerformanceManager(
                    sequencer = sequencer,
                    midiFile = sequence,
                    onClose = { stop() },
                    fileName = midiFile.name,
                    configs = configurations,
                )
                stateManager.attach(performanceAppState)
                rootNode.attachChild(performanceAppState.root)
                addManagers(configurations, sequence, sequencer)
                stateManager.attach(AndroidInputManager())
                stateManager.attach(MidiDeviceManager(configurations, midiDevice))
            }
        }
    }

    actual override fun stop() {
        super.stop()
        onFinish()
    }

    actual override fun destroy() {
        rootNode.detachAllChildren()
        assetManager.clearCache()
        renderer.invalidateState()
        inputManager.clearMappings()
        super.destroy()
    }

    fun callAction(action: Midis2jam2Action) {
        state<AndroidInputManager>()?.callAction(action)
    }

    fun enqueue(block: () -> Unit) {
        super.enqueue(block)
    }

    fun registerProgressListener(listener: ProgressListener) {
        fun getProgressManager(): LoadingProgressManager? =
            stateManager.getState(LoadingProgressManager::class.java)
        CoroutineScope(Dispatchers.Default).launch {
            while (getProgressManager() == null) yield()
            (getProgressManager() ?: return@launch).registerProgressListener(listener)
        }
    }

    fun registerCameraStateListener(listener: CameraStateListener) {
        CoroutineScope(Dispatchers.Default).launch {
            while (state<CameraManager>() == null) yield()
            (state<CameraManager>() ?: return@launch).registerCameraStateListener(listener)
        }
    }
}

internal actual fun getCameraManager(): CameraManager = AndroidCameraManager()