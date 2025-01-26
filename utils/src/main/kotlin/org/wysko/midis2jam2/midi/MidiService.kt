package org.wysko.midis2jam2.midi

import javax.sound.midi.MidiDevice
import javax.sound.midi.MidiSystem

/**
 * Service for MIDI operations.
 */
class MidiService {
    fun getMidiDeviceInfos(): List<MidiDevice.Info> = MidiSystem.getMidiDeviceInfo().toList()
}