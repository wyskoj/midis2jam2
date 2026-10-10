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
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import org.wysko.midis2jam2.domain.GervillMidiDevice
import org.wysko.midis2jam2.domain.MidiService
import org.wysko.midis2jam2.midi.portCount
import org.wysko.midis2jam2.midi.toPerformanceSequence
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.JwSequencerImpl
import org.wysko.midis2jam2.midi.system.MidiPorts
import org.wysko.midis2jam2.starter.configuration.PerformanceConfig
import java.io.File
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

private const val GERVILL = "Gervill"

internal class MidiPackage private constructor(
    val sequence: TimeBasedSequence?,
    val sequencer: JwSequencer,
    val synthesizer: Synthesizer?,
    val midiPorts: MidiPorts,
) {
    companion object {
        /**
         * Opens the MIDI device [config] names, and reads [midiFile] into a sequencer playing on it, if there is one.
         *
         * Gervill gets one synthesizer for each MIDI port the file uses (or [portCount], without a file), so that each
         * port's channels are heard apart. Any other device plays every port.
         */
        fun build(midiFile: File?, config: PerformanceConfig, portCount: Int = 1): MidiPackage {
            val midiService = KoinPlatformTools.defaultContext().get().get<MidiService>()

            val deviceName = config.midiDevice
            val midiDevice = midiService.getMidiDevices().find { it.name == deviceName }

            check(midiDevice != null) {
                "MIDI device $deviceName not found"
            }

            val sequence = midiFile?.let { StandardMidiFileReader().readFile(it).toPerformanceSequence(config.settings) }

            val midiPorts = when (deviceName) {
                GERVILL -> gervillPorts(sequence?.smf?.portCount ?: portCount)
                else -> MidiPorts.of(midiDevice)
            }

            val sequencer = JwSequencerImpl().apply {
                open(midiPorts)
            }

            val synthesizer = when (deviceName) {
                GERVILL -> {
                    val soundbank = config.soundbank?.let { MidiSystem.getSoundbank(File(it)) }
                    midiPorts.distinctDevices.filterIsInstance<GervillMidiDevice>().forEach { gervill ->
                        gervill.synthesizer.apply {
                            open()
                            soundbank?.let { loadAllInstruments(it) }
                        }
                    }
                    GervillMidiDevice.instance.synthesizer
                }

                else -> null
            }

            sequence?.let { sequencer.sequence = it }

            return MidiPackage(sequencer.sequence, sequencer, synthesizer, midiPorts)
        }

        /** A Gervill for each of [portCount] MIDI ports: the app's own for the first, and a new one for each other. */
        fun gervillPorts(portCount: Int): MidiPorts =
            MidiPorts(listOf(GervillMidiDevice.instance) + List(portCount - 1) { GervillMidiDevice.forExtraPort() })
    }
}
