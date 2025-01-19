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
import org.wysko.kmidi.midi.StandardMidiFileReader
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.readFile
import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.starter.configuration.Configuration
import java.io.File
import javax.sound.midi.MidiDevice
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer

internal class Midis2jam2Application(
    private val file: File,
    private val configurations: Collection<Configuration>,
    private val onFinish: () -> Unit,
    private val sequencer: Sequencer,
    private val synthesizer: Synthesizer?,
    private val midiDevice: MidiDevice,
) : SimpleApplication() {
    fun execute() {
        applyConfigurations(configurations)
        start()
    }

    override fun simpleInitApp() {
        val sequence = StandardMidiFileReader().readFile(file).toTimeBasedSequence()
        DesktopMidis2jam2(
            sequencer = sequencer,
            midiFile = sequence,
            onClose = { stop() },
            configs = configurations,
            fileName = file.name,
            synthesizer = synthesizer,
            midiDevice = midiDevice,
        ).also {
            stateManager.attach(it)
            rootNode.attachChild(it.root)
        }
    }

    override fun stop() {
        onFinish()
        super.stop()
    }
}