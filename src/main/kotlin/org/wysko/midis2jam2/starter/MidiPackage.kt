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

import org.wysko.gervill.RealTimeSequencerProvider
import org.wysko.midis2jam2.gui.viewmodel.GERVILL
import org.wysko.midis2jam2.starter.configuration.Configuration
import org.wysko.midis2jam2.starter.configuration.HomeConfiguration
import org.wysko.midis2jam2.starter.configuration.find
import java.io.File
import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.Synthesizer

internal class MidiPackage private constructor(
    val sequencer: Sequencer,
    val synthesizer: Synthesizer?,
    val midiDevice: MidiDevice
) {
    companion object {
        fun build(midiFile: File?, configurations: Collection<Configuration>): MidiPackage {
            val homeConfiguration = configurations.find<HomeConfiguration>()

            val deviceName = homeConfiguration.selectedMidiDevice
            val midiDevice = MidiSystem.getMidiDevice(MidiSystem.getMidiDeviceInfo().first { it.name == deviceName })
                .also { it.open() }

            val sequencer = with(RealTimeSequencerProvider()) {
                (getDevice(deviceInfo.first()) as Sequencer).apply {
                    open()
                }
            }

            val synthesizer = when (deviceName) {
                GERVILL -> MidiSystem.getSynthesizer().apply {
                    open()
                    homeConfiguration.selectedSoundbank?.let {
                        val soundbank = MidiSystem.getSoundbank(File(it))
                        loadAllInstruments(soundbank)
                    }
                }

                else -> null
            }

            sequencer.transmitter.receiver = when (synthesizer) {
                null -> midiDevice.receiver
                else -> synthesizer.receiver
            }

            midiFile?.let {
                sequencer.sequence = MidiSystem.getSequence(it)
            }

            return MidiPackage(sequencer, synthesizer, midiDevice)
        }
    }
}
