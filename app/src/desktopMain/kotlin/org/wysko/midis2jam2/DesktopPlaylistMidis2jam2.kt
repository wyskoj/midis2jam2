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

package org.wysko.midis2jam2

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.starter.configuration.Configuration
import javax.sound.midi.MidiDevice
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer
import kotlin.time.Duration.Companion.seconds

class DesktopPlaylistMidis2jam2(
    override val fileName: String,
    sequencer: Sequencer,
    midiFile: TimeBasedSequence,
    private val onFinish: () -> Unit,
    onClose: () -> Unit,
    synthesizer: Synthesizer?,
    configs: Collection<Configuration>,
    midiDevice: MidiDevice,
) : DesktopMidis2jam2(fileName, sequencer, midiFile, onClose, synthesizer, midiDevice, configs) {

    override fun cleanup() {
        app.inputManager.removeListener(this)
    }

    override fun handleSongCompletion() {
        if (isSongFinished && time >= endTime + 3.seconds) {
            onFinish()
        }
    }
}