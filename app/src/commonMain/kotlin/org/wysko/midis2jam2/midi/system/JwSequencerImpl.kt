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
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.midis2jam2.midi.CHANNELS_PER_PORT
import org.wysko.midis2jam2.midi.PortedEvent
import org.wysko.midis2jam2.midi.channelKey
import org.wysko.midis2jam2.midi.channelOf
import org.wysko.midis2jam2.midi.portCount
import org.wysko.midis2jam2.midi.portOf
import org.wysko.midis2jam2.midi.portedEventsInPlayOrder
import org.wysko.midis2jam2.util.logger
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

class JwSequencerImpl : JwSequencer {
    private val threadPool = Executors.newCachedThreadPool()

    override var sequence: TimeBasedSequence? = null
        set(value) {
            check(isOpen) { "Sequencer is not open." }
            if (isRunning) {
                stop()
            }

            if (value != null) {
                pump = DataPump(value)
                events = value.smf.portedEventsInPlayOrder()
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
    private var ports: MidiPorts? = null
    private var events: List<PortedEvent>? = null

    private var _isOpen = false
    override val isOpen: Boolean
        get() = _isOpen

    override fun open(ports: MidiPorts) {
        if (isOpen) return
        this.ports = ports.also { it.open() }
        _isOpen = true
    }

    override fun close() {
        if (!isOpen) return
        if (isRunning) stop()
        threadPool.execute {
            this@JwSequencerImpl.ports?.close()
            this.ports = null
        }
        threadPool.shutdown()
        _isOpen = false
    }

    override fun start() {
        check(isOpen) { "Sequencer is not open" }
        if (isRunning) return

        _isRunning = true
        pump!!.checkpoint(null)
        job = object : Thread("JwSequencerPumpingStation") {
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
        if (!isOpen || !isRunning) return

        _isRunning = false
        job?.takeIf { it !== Thread.currentThread() }?.join()
        pump!!.sendAllNotesOff()
    }

    override fun setPosition(position: Duration, start: Boolean, onFinish: () -> Unit) {
        threadPool.execute {
            check(isOpen) { "Sequencer is not open" }
            check(sequence != null) { "Sequence is not set" }
            pump!!.setPosition(position.inWholeMilliseconds, start)
            onFinish()
        }
    }

    override fun resetDevice() {
        // TODO
    }

    override fun sendData(data: ByteArray) {
        ports?.sendData(data)
    }

    val position: Duration
        get() = (pump?.localCurrentTime?.div(1000.0))?.seconds ?: Duration.ZERO

    inner class DataPump(private val sequence: TimeBasedSequence) {
        private var globalCheckpoint = 0L
        private var localCheckpoint = 0L
        internal var localCurrentTime = 0L
        private var eventIndex = 0

        internal fun pump() {
            val currentTime = System.currentTimeMillis()

            localCurrentTime = localCheckpoint + (currentTime - globalCheckpoint)

            events?.let {
                while (eventIndex in it.indices && tickToTime(it[eventIndex].event.tick) < localCurrentTime) {
                    try {
                        dispatch(it[eventIndex++])
                    } catch (e: Exception) {
                        logger().error("MIDI event dispatch failed.", e)
                    }
                }
            }

            if (localCurrentTime > sequence.duration.inWholeMilliseconds) {
                stop()
            }
        }

        internal fun setPosition(time: Long, start: Boolean) {
            val wasRunning = isRunning || start
            if (wasRunning) stop()

            when (time) {
                0L -> eventIndex = 0
                else -> chaseEvents(time)
            }
            checkpoint(time)
            if (wasRunning) start()
        }

        internal fun checkpoint(time: Long?) {
            globalCheckpoint = System.currentTimeMillis()
            time?.let { localCurrentTime = it }
            localCheckpoint = localCurrentTime
        }

        internal fun sendAllNotesOff() {
            ports?.distinctDevices?.forEach { device ->
                for (channel in 0..<CHANNELS_PER_PORT) {
                    for (note in 0..127) {
                        device.sendNoteOffMessage(channel, note)
                    }
                }
            }
        }

        private fun chaseEvents(time: Long) {
            // Indexed by channel key, so that each port's channels are chased separately. Ports that share a device
            // share its channels, so the last change on either is the one in effect.
            val portSlots = minOf(sequence.smf.portCount, ports?.size ?: 1)
            val channelCount = portSlots * CHANNELS_PER_PORT
            val programs = MutableList(channelCount) { -1 }
            val pitchBends = MutableList(channelCount) { 0x2000 }
            val channelPressures = MutableList(channelCount) { 0 }
            val polyphonicKeyPressures = List(channelCount) { MutableList(128) { 0 } }
            val controlChanges = List(channelCount) { MutableList(128) { -1 } }

            events?.let {
                for ((i, ported) in it.withIndex()) {
                    val event = ported.event
                    if (tickToTime(event.tick) > time) {
                        eventIndex = i
                        break
                    }

                    if (event is MidiEvent) {
                        val key = channelKey(ported.port % portSlots, event.channel.toInt())
                        when (event) {
                            is ProgramEvent -> programs[key] = event.program.toInt()
                            is PitchWheelChangeEvent -> pitchBends[key] = event.value.toInt()
                            is ChannelPressureEvent -> channelPressures[key] = event.pressure.toInt()
                            is PolyphonicKeyPressureEvent -> {
                                polyphonicKeyPressures[key][event.note.toInt()] = event.pressure.toInt()
                            }

                            is ControlChangeEvent -> controlChanges[key][event.controller.toInt()] = event.value.toInt()

                            else -> Unit
                        }
                    }
                }
            }

            for (key in 0..<channelCount) {
                val device = ports?.get(portOf(key))
                val channel = channelOf(key)
                for (controller in 0..<127) {
                    if (controlChanges[key][controller] >= 0) {
                        device?.sendControlChangeMessage(channel, controller, controlChanges[key][controller])
                    }
                }
                for (noteNumber in 0..<128) {
                    if (polyphonicKeyPressures[key][noteNumber] >= 0) {
                        device?.sendPolyphonicPressureMessage(
                            channel,
                            noteNumber,
                            polyphonicKeyPressures[key][noteNumber]
                        )
                    }
                }

                if (programs[key] >= 0) {
                    device?.sendProgramChangeMessage(channel, programs[key])
                }

                device?.sendPitchBendMessage(channel, pitchBends[key])
                device?.sendChannelPressureMessage(channel, channelPressures[key])
            }
        }

        private fun tickToTime(tick: Int): Long = sequence.getTimeAtTick(tick).inWholeMilliseconds

        private fun dispatch(event: PortedEvent) {
            ports?.dispatch(event)
        }
    }
}
