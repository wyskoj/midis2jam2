/*
 * Copyright (C) 2026 Jacob Wysko
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

package org.wysko.midis2jam2.testing

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.midis2jam2.midi.system.JwSequencer
import org.wysko.midis2jam2.midi.system.MidiDevice
import kotlin.time.Duration

/**
 * A sequencer that makes no sound and keeps no clock of its own.
 *
 * Playback is what the shipped sequencer does on a background thread against the system
 * clock; none of that belongs in a test. Recording the calls it receives is enough to assert
 * that the app asked for the right thing.
 */
class NoOpSequencer : JwSequencer {

    override var sequence: TimeBasedSequence? = null

    override var isRunning: Boolean = false
        private set

    override var isOpen: Boolean = false
        private set

    /** Every position the app has seeked to, in order. */
    val seeks: MutableList<Duration> = mutableListOf()

    /** Every raw MIDI message the app has sent, in order. */
    val sentData: MutableList<ByteArray> = mutableListOf()

    var openedDevice: MidiDevice? = null
        private set

    var resetCount: Int = 0
        private set

    var startCount: Int = 0
        private set

    var stopCount: Int = 0
        private set

    override fun open(device: MidiDevice) {
        openedDevice = device
        isOpen = true
    }

    override fun close() {
        isOpen = false
        isRunning = false
    }

    override fun start() {
        isRunning = true
        startCount++
    }

    override fun stop() {
        isRunning = false
        stopCount++
    }

    override fun setPosition(position: Duration, start: Boolean, onFinish: () -> Unit) {
        seeks += position
        isRunning = start
        onFinish()
    }

    override fun resetDevice() {
        resetCount++
    }

    override fun sendData(data: ByteArray) {
        sentData += data
    }
}

/** A MIDI device that swallows everything sent to it. */
class NoOpMidiDevice(override val name: String = "Test Device") : MidiDevice {

    /** Every message the app has sent, as a readable description, in order. */
    val messages: MutableList<String> = mutableListOf()

    var isOpen: Boolean = false
        private set

    override fun open() {
        isOpen = true
    }

    override fun close() {
        isOpen = false
    }

    override fun sendNoteOnMessage(channel: Int, note: Int, velocity: Int) {
        messages += "noteOn ch=$channel note=$note velocity=$velocity"
    }

    override fun sendNoteOffMessage(channel: Int, note: Int) {
        messages += "noteOff ch=$channel note=$note"
    }

    override fun sendControlChangeMessage(channel: Int, controller: Int, value: Int) {
        messages += "controlChange ch=$channel controller=$controller value=$value"
    }

    override fun sendProgramChangeMessage(channel: Int, program: Int) {
        messages += "programChange ch=$channel program=$program"
    }

    override fun sendPitchBendMessage(channel: Int, pitch: Int) {
        messages += "pitchBend ch=$channel pitch=$pitch"
    }

    override fun sendChannelPressureMessage(channel: Int, pressure: Int) {
        messages += "channelPressure ch=$channel pressure=$pressure"
    }

    override fun sendPolyphonicPressureMessage(channel: Int, note: Int, pressure: Int) {
        messages += "polyphonicPressure ch=$channel note=$note pressure=$pressure"
    }

    override fun sendData(data: ByteArray) {
        messages += "data ${data.joinToString(" ") { byte -> byte.toUByte().toString(16) }}"
    }
}
