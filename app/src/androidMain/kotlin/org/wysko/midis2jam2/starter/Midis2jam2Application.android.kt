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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.midis2jam2.AndroidMidis2jam2
import org.wysko.midis2jam2.CompatLibrary
import org.wysko.midis2jam2.Midis2jam2Action
import org.wysko.midis2jam2.domain.ApplicationService
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.midi.system.JwSequencerImpl
import org.wysko.midis2jam2.util.logger

internal actual class Midis2jam2Application(
    private val onFinish: () -> Unit = {},
) : SimpleApplication(), KoinComponent {
    actual fun execute() = Unit

    override fun start() {
        super.start()
        logger().debug("Midis2jam2Application: start() called")
    }

    private lateinit var androidMidis2jam2: AndroidMidis2jam2

    actual override fun simpleInitApp() {
        logger().debug("Midis2jam2Application: simpleInitApp() called")
        val applicationService: ApplicationService by inject()
        val midiService: MidiService by inject()
        val midiFile = applicationService.midiFile.value!!
        val configurations = applicationService.configurations.value

        applyConfigurations(configurations)
        setupState(configurations, addFpp = CompatLibrary.supportsFilterPostProcessor, platform = Platform.Android)

        CoroutineScope(Dispatchers.IO).launch {
            val data = midiFile.readBytes()
            val sequence = StandardMidiFileReader().readByteArray(data).toTimeBasedSequence()
            val sequencer = JwSequencerImpl()

            val midiDevice = midiService.getMidiDevices().first()
            sequencer.open(midiDevice)
            sequencer.sequence = sequence

            androidMidis2jam2 = AndroidMidis2jam2(
                fileName = midiFile.name,
                midiFile = sequence,
                onClose = { stop() },
                sequencer = sequencer,
                configs = configurations,
                midiDevice = midiDevice,
            )

            enqueue {
                rootNode.attachChild(androidMidis2jam2.root)
                stateManager.attach(androidMidis2jam2)
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
        if (::androidMidis2jam2.isInitialized) {
            enqueue {
                androidMidis2jam2.callAction(action)
            }
        }
    }

    fun enqueue(block: () -> Unit) {
        super.enqueue(block)
    }

    fun registerProgressListener(listener: ProgressListener) {
        CoroutineScope(Dispatchers.Default).launch {
            while (!(::androidMidis2jam2.isInitialized)) yield()
            androidMidis2jam2.registerProgressListener(listener)
        }
    }

    val isAutoCamActive: StateFlow<Boolean>
        get() = if (::androidMidis2jam2.isInitialized) {
            androidMidis2jam2.isAutoCamActive
        } else {
            MutableStateFlow(false)
        }

    val isSlideCamActive: StateFlow<Boolean>
        get() = if (::androidMidis2jam2.isInitialized) {
            androidMidis2jam2.isSlideCamActive
        } else {
            MutableStateFlow(false)
        }

    fun isPlaying(): Boolean = when {
        ::androidMidis2jam2.isInitialized -> androidMidis2jam2.isPlaying()
        else -> false
    }
}
