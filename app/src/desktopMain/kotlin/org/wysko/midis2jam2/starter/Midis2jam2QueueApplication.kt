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
import org.koin.mp.KoinPlatformTools
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.DesktopPlaylistPerformanceManager
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.manager.BaseManager
import org.wysko.midis2jam2.manager.MidiDeviceManager
import org.wysko.midis2jam2.manager.instantiateManagers
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.world.AssetLoader
import javax.sound.midi.Synthesizer

internal class Midis2jam2QueueApplication(
    private val sequences: List<TimeBasedSequence>,
    private val fileNames: List<String>,
    private val configurations: Collection<Configuration>,
    private val onTrackStart: (Int) -> Unit,
    private val onPlaylistFinish: () -> Unit,
    private val sequencer: JwSequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    private data class TrackRuntimeBundle(
        val performanceManager: DesktopPlaylistPerformanceManager,
        val managers: List<BaseManager>,
        val assetLoader: AssetLoader,
        val midiDeviceManager: MidiDeviceManager,
    )

    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()
    private var currentSongIndex = -1
    private var currentRuntimeBundle: TrackRuntimeBundle? = null
    private var isAdvancing = false
    private var isStopping = false
    private var isShutDown = false
    private var hasFinished = false

    override fun simpleInitApp() {
        setupState(configurations, platform = Platform.Desktop)
        playFrom(0)
    }

    private fun playFrom(startIndex: Int) {
        var index = startIndex
        while (index in sequences.indices) {
            if (loadSong(index)) {
                return
            }
            index++
        }
        stop()
    }

    private fun loadSong(index: Int): Boolean = runCatching {
        teardownCurrentRuntime()

        sequencer.stop()
        sequencer.sequence = sequences[index]

        val performanceManager = DesktopPlaylistPerformanceManager(
            fileName = fileNames[index],
            sequencer = sequencer,
            midiFile = sequences[index],
            onClose = {},
            configs = configurations
        )
        val assetLoader = AssetLoader()
        val managers = instantiateManagers(
            configurations = configurations,
            sequence = sequences[index],
            sequencer = sequencer,
            isQueueApplication = true,
            onPlaybackComplete = { onTrackCompleted() },
        )
        val midiDeviceManager = MidiDeviceManager(configurations, midiDevice)

        stateManager.attach(assetLoader)
        stateManager.attach(performanceManager)
        stateManager.attachAll(*managers.toTypedArray())
        stateManager.attach(midiDeviceManager)
        rootNode.attachChild(performanceManager.root)

        currentRuntimeBundle = TrackRuntimeBundle(
            performanceManager = performanceManager,
            managers = managers,
            assetLoader = assetLoader,
            midiDeviceManager = midiDeviceManager,
        )
        currentSongIndex = index
        onTrackStart(index)
        true
    }.getOrElse { error ->
        errorLogService.addError(
            message = "There was an error loading queued track \"${fileNames.getOrNull(index) ?: index}\". Skipping track.",
            stackTrace = error.stackTraceToString(),
        )
        false
    }

    private fun onTrackCompleted() {
        if (isStopping || isAdvancing) return
        isAdvancing = true
        try {
            playFrom(currentSongIndex + 1)
        } finally {
            isAdvancing = false
        }
    }

    private fun teardownCurrentRuntime() {
        currentRuntimeBundle?.let { bundle ->
            stateManager.detach(bundle.midiDeviceManager)
            bundle.managers.reversed().forEach(stateManager::detach)
            stateManager.detach(bundle.performanceManager)
            stateManager.detach(bundle.assetLoader)
            rootNode.detachChild(bundle.performanceManager.root)
        }
        currentRuntimeBundle = null
    }

    override fun stop() {
        if (isStopping) return
        isStopping = true
        shutdownRuntime()
        super.stop()
    }

    override fun destroy() {
        shutdownRuntime()
        super.destroy()
    }

    private fun shutdownRuntime() {
        if (isShutDown) return
        isShutDown = true
        teardownCurrentRuntime()
        sequencer.stop()
        sequencer.close()
        if (!hasFinished) {
            hasFinished = true
            onPlaylistFinish()
        }
    }
}
