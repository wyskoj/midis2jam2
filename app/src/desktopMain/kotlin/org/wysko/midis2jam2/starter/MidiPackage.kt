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

import org.koin.mp.KoinPlatformTools
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.domain.GervillMidiDevice
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.JwSequencerImpl
import org.wysko.midis2jam2.midi.system.MidiDevice
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.Configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import java.io.File
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

private const val GERVILL = "Gervill"

internal class MidiPackage private constructor(
    val sequence: TimeBasedSequence?,
    val sequencer: JwSequencer,
    val synthesizer: Synthesizer?,
    val midiDevice: MidiDevice,
) {
    companion object {
        fun build(midiFile: File?, configurations: Collection<Configuration>): MidiPackage {
            val homeConfiguration = configurations.find<HomeConfiguration>()
            val midiService = KoinPlatformTools.defaultContext().get().get<MidiService>()

            val deviceName = homeConfiguration.selectedMidiDevice
            val midiDevice = midiService.getMidiDevices().find { it.name == deviceName }

            check(midiDevice != null) {
                "MIDI device $deviceName not found"
            }

            val sequencer = JwSequencerImpl().apply {
                open(midiDevice)
            }

            val synthesizer = when (deviceName) {
                GERVILL -> GervillMidiDevice.instance.synthesizer.apply {
                    open()
                    homeConfiguration.selectedSoundbank?.let {
                        val soundbank = MidiSystem.getSoundbank(File(it))
                        loadAllInstruments(soundbank)
                    }
                }

                else -> null
            }

            val reader = StandardMidiFileReader()
            midiFile?.let {
                val sequence = reader.readFile(it).toTimeBasedSequence()
                sequencer.sequence = sequence
            }

            return MidiPackage(sequencer.sequence, sequencer, synthesizer, midiDevice)
        }
    }
}
