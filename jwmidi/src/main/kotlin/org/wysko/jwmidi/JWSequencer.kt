package org.wysko.jwmidi

import kotlinx.coroutines.*
import org.wysko.kmidi.midi.TimeBasedSequence
import org.wysko.kmidi.midi.event.*
import javax.sound.midi.MidiDevice
import javax.sound.midi.ShortMessage
import kotlin.experimental.and
import kotlin.time.Duration

class JWSequencer {
    var sequence: TimeBasedSequence? = null
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

    var isRunning = false
        private set
    private var pump: DataPump? = null

    private var job: Job? = null
    private var device: MidiDevice? = null
    private var events: List<Event>? = null

    private var isOpen = false

    fun open(device: MidiDevice) {
        check(!isOpen) { "Sequencer is already open" }
        this.device = device.also { it.open() }
        isOpen = true
    }

    fun close() {
        check(isOpen) { "Sequencer is not open" }
        if (isRunning) stop()
        this.device!!.close()
        this.device = null
        isOpen = false
    }

    fun start() {
        check(isOpen) { "Sequencer is not open" }
        if (isRunning) return

        isRunning = true
        pump!!.checkpoint(null)
        job = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                pump!!.pump()
                delay(1)
            }
        }
    }

    fun stop() {
        check(isOpen) { "Sequencer is open" }
        if (!isRunning) return

        isRunning = false
        job!!.cancel()
        pump!!.sendAllNotesOff()
    }

    fun setPosition(time: Duration) {
        check(isOpen) { "Sequencer is not open" }
        check(sequence != null) { "Sequence is not set" }
        pump!!.setPosition(time.inWholeMilliseconds)
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
            with(device!!.receiver) {
                for (channel in 0..15) {
                    for (note in 0..127) {
                        send(ShortMessage(ShortMessage.NOTE_OFF, channel, note, 0), -1)
                    }
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
                            is PolyphonicKeyPressureEvent -> polyphonicKeyPressures[event.channel.toInt()][event.note.toInt()] =
                                event.pressure.toInt()

                            is ControlChangeEvent -> controlChanges[event.channel.toInt()][event.controller.toInt()] =
                                event.value.toInt()

                            else -> Unit
                        }
                    }
                }
            }

            with(device!!.receiver) {
                for (channel in 0..<16) {
                    for (controller in 0..<127) {
                        if (controlChanges[channel][controller] >= 0) {
                            send(
                                ShortMessage(
                                    ShortMessage.CONTROL_CHANGE,
                                    channel, controller, controlChanges[channel][controller]
                                ), -1
                            )
                        }
                    }
                    for (noteNumber in 0..<128) {
                        if (polyphonicKeyPressures[channel][noteNumber] >= 0) {
                            send(
                                ShortMessage(
                                    ShortMessage.POLY_PRESSURE,
                                    channel, noteNumber, polyphonicKeyPressures[channel][noteNumber]
                                ), -1
                            )
                        }
                    }

                    if (programs[channel] >= 0) {
                        send(
                            ShortMessage(
                                ShortMessage.PROGRAM_CHANGE,
                                channel, programs[channel], 0
                            ), -1
                        )
                    }

                    send(
                        ShortMessage(
                            ShortMessage.PITCH_BEND,
                            channel, pitchBends[channel] and 0x7F, pitchBends[channel] shr 7
                        ), -1
                    )

                    send(
                        ShortMessage(
                            ShortMessage.CHANNEL_PRESSURE,
                            channel, channelPressures[channel], 0
                        ), -1
                    )
                }
            }
        }

        private fun tickToTime(tick: Int): Long = sequence.getTimeAtTick(tick).inWholeMilliseconds

        private fun dispatch(event: Event) {
            with(device!!.receiver) {
                if (event is MidiEvent) {
                    when (event) {
                        is NoteEvent.NoteOff -> send(
                            ShortMessage(
                                ShortMessage.NOTE_OFF,
                                event.channel.toInt(), event.note.toInt(), event.velocity.toInt()
                            ), -1
                        )

                        is NoteEvent.NoteOn -> send(
                            ShortMessage(
                                ShortMessage.NOTE_ON,
                                event.channel.toInt(), event.note.toInt(), event.velocity.toInt()
                            ), -1
                        )

                        is ChannelPressureEvent -> send(
                            ShortMessage(
                                ShortMessage.CHANNEL_PRESSURE,
                                event.channel.toInt(), event.pressure.toInt(), 0
                            ), -1
                        )

                        is ControlChangeEvent -> send(
                            ShortMessage(
                                ShortMessage.CONTROL_CHANGE,
                                event.channel.toInt(), event.controller.toInt(), event.value.toInt()
                            ), -1
                        )

                        is PitchWheelChangeEvent -> send(
                            ShortMessage(
                                ShortMessage.PITCH_BEND,
                                event.channel.toInt(), (event.value and 0x7F).toInt(), (event.value.toInt() shr 7)
                            ), -1
                        )

                        is PolyphonicKeyPressureEvent -> send(
                            ShortMessage(
                                ShortMessage.POLY_PRESSURE,
                                event.channel.toInt(), event.note.toInt(), event.pressure.toInt()
                            ), -1
                        )

                        is ProgramEvent -> send(
                            ShortMessage(
                                ShortMessage.PROGRAM_CHANGE,
                                event.channel.toInt(), event.program.toInt(), 0
                            ), -1
                        )
                    }
                }
            }
        }
    }
}