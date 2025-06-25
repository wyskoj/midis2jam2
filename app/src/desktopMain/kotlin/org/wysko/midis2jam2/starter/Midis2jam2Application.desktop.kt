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

import com.jme3.app.SimpleApplication
import com.jme3.system.lwjgl.LwjglContext
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.midis2jam2.DesktopMidis2jam2
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
    actual fun execute() {
        applyConfigurations(configurations)
        start()
    }

    actual override fun simpleInitApp() {
        setupState(configurations, platform = Platform.Desktop)
        CoroutineScope(Dispatchers.IO).launch {
            val data = file.readBytes()
            val sequence = StandardMidiFileReader().readByteArray(data).toTimeBasedSequence()
            DesktopMidis2jam2(
                sequencer = sequencer,
                midiFile = sequence,
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