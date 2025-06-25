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

package org.wysko.midis2jam2.midi.system

import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import kotlin.time.Duration

class JwSequencerImpl : JwSequencer {
    override var sequence: TimeBasedSequence? = null
        set(value) {
            check(isOpen) { "Sequencer is not open." }
            if (isRunning) {
                stop()
            }

            if (value != null) {
                pump = DataPump(value)
                events = value.smf.tracks.flatMap { it.events }.sortedBy { it.tick }
            } else {
                pump = null
                events = null
            }

            field = value
        }

    private var _isRunning = false
    override val isRunning: Boolean
        get() = _isRunning

    private var pump: DataPump? = null

    private var job: Thread? = null
    private var device: MidiDevice? = null
    private var events: List<Event>? = null

    private var _isOpen = false
    override val isOpen: Boolean
        get() = _isOpen

    override fun open(device: MidiDevice) {
        check(!isOpen) { "Sequencer is already open" }
        this.device = device.also { it.open() }
        _isOpen = true
    }

    override fun close() {
        check(isOpen) { "Sequencer is not open" }
        if (isRunning) stop()
        this.device!!.close()
        this.device = null
        _isOpen = false
    }

    override fun start() {
        check(isOpen) { "Sequencer is not open" }
        if (isRunning) return

        _isRunning = true
        pump!!.checkpoint(null)
        job = object : Thread() {
            init {
                priority = MAX_PRIORITY
            }

            override fun run() {
                super.run()
                while (isRunning) {
                    pump!!.pump()
                    sleep(1)
                }
            }
        }.also {
            it.start()
        }
    }

    override fun stop() {
        check(isOpen) { "Sequencer is open" }
        if (!isRunning) return

        _isRunning = false
        job!!.join()
        pump!!.sendAllNotesOff()
    }

    override fun setPosition(position: Duration) {
        check(isOpen) { "Sequencer is not open" }
        check(sequence != null) { "Sequence is not set" }
        pump!!.setPosition(position.inWholeMilliseconds)
    }

    override fun resetDevice() {
        // TODO
    }

    override fun sendData(data: ByteArray) {
        device?.sendSysex(data)
    }

    inner class DataPump(private val sequence: TimeBasedSequence) {
        private var globalCheckpoint = 0L
        private var localCheckpoint = 0L
        private var localCurrentTime = 0L
        private var eventIndex = 0

        internal fun pump() {
            val currentTime = System.currentTimeMillis()

            localCurrentTime = localCheckpoint + (currentTime - globalCheckpoint)

            events?.let {
                while (eventIndex in it.indices && tickToTime(it[eventIndex].tick) < localCurrentTime) {
                    dispatch(it[eventIndex++])
                }
            }

            if (localCurrentTime > sequence.duration.inWholeMilliseconds) {
                stop()
            }
        }

        internal fun setPosition(time: Long) {
            val wasRunning = isRunning
            if (wasRunning) stop()
            chaseEvents(time)
            checkpoint(time)
            if (wasRunning) start()
        }

        internal fun checkpoint(time: Long?) {
            globalCheckpoint = System.currentTimeMillis()
            time?.let { localCurrentTime = it }
            localCheckpoint = localCurrentTime
        }

        internal fun sendAllNotesOff() {
            for (channel in 0..15) {
                for (note in 0..127) {
                    device?.sendNoteOffMessage(channel, note)
                }
            }
        }

        private fun chaseEvents(time: Long) {
            val programs = MutableList(16) { -1 }
            val pitchBends = MutableList(16) { 0x2000 }
            val channelPressures = MutableList(16) { 0 }
            val polyphonicKeyPressures = List(16) { MutableList(128) { 0 } }
            val controlChanges = List(16) { MutableList(128) { -1 } }

            events?.let {
                for ((i, event) in it.withIndex()) {
                    if (tickToTime(event.tick) > time) {
                        eventIndex = i
                        break
                    }

                    if (event is MidiEvent) {
                        when (event) {
                            is ProgramEvent -> programs[event.channel.toInt()] = event.program.toInt()
                            is PitchWheelChangeEvent -> pitchBends[event.channel.toInt()] = event.value.toInt()
                            is ChannelPressureEvent -> channelPressures[event.channel.toInt()] = event.pressure.toInt()
                            is PolyphonicKeyPressureEvent -> {
                                polyphonicKeyPressures[event.channel.toInt()][event.note.toInt()] =
                                    event.pressure.toInt()
                            }

                            is ControlChangeEvent -> controlChanges[event.channel.toInt()][event.controller.toInt()] =
                                event.value.toInt()

                            else -> Unit
                        }
                    }
                }
            }

            for (channel in 0..<16) {
                for (controller in 0..<127) {
                    if (controlChanges[channel][controller] >= 0) {
                        device?.sendControlChangeMessage(channel, controller, controlChanges[channel][controller])
                    }
                }
                for (noteNumber in 0..<128) {
                    if (polyphonicKeyPressures[channel][noteNumber] >= 0) {
                        device?.sendPolyphonicPressureMessage(
                            channel,
                            noteNumber,
                            polyphonicKeyPressures[channel][noteNumber]
                        )
                    }
                }

                if (programs[channel] >= 0) {
                    device?.sendProgramChangeMessage(channel, programs[channel])
                }

                device?.sendPitchBendMessage(channel, pitchBends[channel])
                device?.sendChannelPressureMessage(channel, channelPressures[channel])
            }
        }

        private fun tickToTime(tick: Int): Long = sequence.getTimeAtTick(tick).inWholeMilliseconds

        private fun dispatch(event: Event) {
            if (event is MidiEvent) {
                when (event) {
                    is NoteEvent.NoteOff -> device?.sendNoteOffMessage(event.channel.toInt(), event.note.toInt())

                    is NoteEvent.NoteOn -> device?.sendNoteOnMessage(
                        event.channel.toInt(),
                        event.note.toInt(),
                        event.velocity.toInt()
                    )

                    is ChannelPressureEvent -> device?.sendChannelPressureMessage(
                        event.channel.toInt(),
                        event.pressure.toInt()
                    )

                    is ControlChangeEvent -> device?.sendControlChangeMessage(
                        event.channel.toInt(),
                        event.controller.toInt(),
                        event.value.toInt()
                    )

                    is PitchWheelChangeEvent -> device?.sendPitchBendMessage(
                        event.channel.toInt(),
                        event.value.toInt()
                    )

                    is PolyphonicKeyPressureEvent -> device?.sendPolyphonicPressureMessage(
                        event.channel.toInt(),
                        event.note.toInt(),
                        event.pressure.toInt()
                    )

                    is ProgramEvent -> device?.sendProgramChangeMessage(
                        event.channel.toInt(),
                        event.program.toInt()
                    )
                }
            }
        }
    }
}
