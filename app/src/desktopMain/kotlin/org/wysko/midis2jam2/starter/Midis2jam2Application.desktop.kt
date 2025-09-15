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
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatformTools
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.domain.ErrorLogService
import org.wysko.midis2jam2.domain.Jme3ExceptionHandler
import org.wysko.midis2jam2.starter.configuration.Configuration
import javax.sound.midi.MidiDevice
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer

internal actual class Midis2jam2Application(
    private val file: PlatformFile,
    private val configurations: Collection<Configuration>,
    private val onFinish: () -> Unit,
    private val sequencer: Sequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    private val errorLogService = KoinPlatformTools.defaultContext().get().get<ErrorLogService>()

    actual fun execute() {
        try {
            applyConfigurations(configurations)
            start()
        } catch (e: Exception) {
            // Log the error but continue to start the application
            errorLogService.addError("There was an error applying configurations.", e)
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
        CoroutineScope(Dispatchers.IO).launch {
            val data = runCatching { file.readBytes() }.onFailure {
                errorLogService.addError("There was an error reading the MIDI file.", it)
                onFinish()
                return@launch
            }.getOrThrow()

            val sequence =
                runCatching { StandardMidiFileReader().readByteArray(data).toTimeBasedSequence() }.onFailure {
                    errorLogService.addError("There was an error parsing the MIDI file.", it)
                    onFinish()
                    return@launch
                }

            DesktopMidis2jam2(
                sequencer = sequencer,
                midiFile = sequence.getOrThrow(),
                onClose = { stop() },
                configs = configurations,
                fileName = file.name,
                synthesizer = synthesizer,
                midiDevice = midiDevice,
            ).also {
                enqueue {
                    stateManager.attach(it)
                    rootNode.attachChild(it.root)
                }
            }
        }
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
