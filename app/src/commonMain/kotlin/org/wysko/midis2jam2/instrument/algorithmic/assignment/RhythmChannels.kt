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

package org.wysko.midis2jam2.instrument.algorithmic.assignment

import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent

/** The channel that the General MIDI specification reserves for percussion (channel 10, zero-based). */
const val PRIMARY_RHYTHM_CHANNEL: Int = 9

/** The kit program that the SFX kit is treated as, regardless of how it was selected. */
const val SFX_KIT_PROGRAM: Int = 56

/**
 * Each channel is either in a melodic or rhythmic state.
 */
sealed class ChannelState {
    /** Melodic state. */
    data object Melody : ChannelState()

    /** Rhythmic state. */
    data object Rhythm : ChannelState()

    companion object {
        val DEFAULT_STATE: Array<ChannelState> = Array(16) { if (it == PRIMARY_RHYTHM_CHANNEL) Rhythm else Melody }
    }
}

/**
 * Which channels are rhythm channels, and when.
 *
 * General MIDI only has channel 10, but GS, XG and GM2 files can turn any channel into a rhythm channel (and back)
 * with system exclusive messages or bank select. The synthesizer honours these, so the visuals need to as well.
 */
class ChannelStateTimeline private constructor(private val transitions: Array<List<Transition>>) {

    /**
     * A change of state on one channel.
     *
     * @property kit Overrides the kit program for this stretch, when the kit was not chosen by program number alone.
     */
    private data class Transition(val tick: Int, val state: ChannelState, val kit: Int? = null)

    /** The state of [channel] (zero-based) at [tick]. */
    fun stateAt(channel: Int, tick: Int): ChannelState =
        transitionAt(channel, tick)?.state ?: ChannelState.DEFAULT_STATE[channel]

    /** The kit a rhythm note on [channel] at [tick] plays, given the channel's current [program]. */
    fun kitAt(channel: Int, tick: Int, program: Int): Int = transitionAt(channel, tick)?.kit ?: program

    private fun transitionAt(channel: Int, tick: Int): Transition? = transitions[channel].lastOrNull { it.tick <= tick }

    /** How bank select is interpreted, which depends on the last system reset in the file. */
    private enum class Mode { Default, XG, GM2 }

    companion object {
        private const val CC_BANK_SELECT_MSB = 0
        private const val XG_DRUM_BANK = 127
        private const val XG_SFX_KIT_BANK = 126
        private const val GM2_RHYTHM_BANK = 120
        private const val GM2_MELODY_BANK = 121

        /**
         * Reads the rhythm channel assignments out of [events], which may come from any number of tracks.
         */
        @Suppress("CyclomaticComplexMethod")
        fun from(events: List<Event>): ChannelStateTimeline {
            val transitions = Array(16) { mutableListOf<Transition>() }
            val current = ChannelState.DEFAULT_STATE.copyOf()
            val currentKit = arrayOfNulls<Int>(16)
            val bankMsb = IntArray(16)
            var mode = Mode.Default

            fun set(channel: Int, tick: Int, state: ChannelState, kit: Int? = null) {
                if (current[channel] == state && currentKit[channel] == kit) return
                current[channel] = state
                currentKit[channel] = kit
                transitions[channel] += Transition(tick, state, kit)
            }

            fun reset(tick: Int, newMode: Mode) {
                mode = newMode
                bankMsb.fill(0)
                repeat(16) { set(it, tick, ChannelState.DEFAULT_STATE[it]) }
            }

            events.sortedBy { it.tick }.forEach { event ->
                when (event) {
                    is SysexEvent -> when (val message = SysexMessage.parse(event.data)) {
                        is SysexMessage.Reset -> reset(event.tick, message.mode)
                        is SysexMessage.PartState -> set(message.channel, event.tick, message.state)
                        null -> Unit
                    }

                    is ControlChangeEvent -> if (event.controller.toInt() == CC_BANK_SELECT_MSB) {
                        bankMsb[event.channel.toInt()] = event.value.toInt()
                    }

                    is ProgramEvent -> {
                        val channel = event.channel.toInt()
                        when (mode) {
                            Mode.XG -> when (bankMsb[channel]) {
                                XG_DRUM_BANK -> set(channel, event.tick, ChannelState.Rhythm)
                                XG_SFX_KIT_BANK -> set(channel, event.tick, ChannelState.Rhythm, SFX_KIT_PROGRAM)
                                // Channel 10 is left alone: plenty of files send bank 0 to every channel as a matter
                                // of habit, and we'd rather keep the drums than lose them.
                                else -> if (channel != PRIMARY_RHYTHM_CHANNEL) {
                                    set(channel, event.tick, ChannelState.Melody)
                                } else {
                                    set(channel, event.tick, current[channel])
                                }
                            }

                            Mode.GM2 -> when (bankMsb[channel]) {
                                GM2_RHYTHM_BANK -> set(channel, event.tick, ChannelState.Rhythm)
                                GM2_MELODY_BANK -> set(channel, event.tick, ChannelState.Melody)
                            }

                            // In GS, bank select only picks a variation (and bank 127 is a *melodic* sound set).
                            Mode.Default -> Unit
                        }
                    }

                    else -> Unit
                }
            }

            return ChannelStateTimeline(Array(16) { transitions[it].toList() })
        }
    }

    /** The system exclusive messages that affect which channels are rhythm channels. */
    private sealed class SysexMessage {
        /** A GM, GM2, GS or XG system reset. */
        data class Reset(val mode: Mode) : SysexMessage()

        /** One channel set to melody or rhythm. */
        data class PartState(val channel: Int, val state: ChannelState) : SysexMessage()

        companion object {
            private const val UNIVERSAL_NON_REALTIME = 0x7E
            private const val ROLAND = 0x41
            private const val YAMAHA = 0x43

            fun parse(raw: ByteArray): SysexMessage? {
                // The data may or may not carry the leading F0 and trailing F7, depending on where it came from.
                val data = raw.map { it.toInt() and 0xFF }
                    .let { if (it.firstOrNull() == 0xF0) it.drop(1) else it }
                    .let { if (it.lastOrNull() == 0xF7) it.dropLast(1) else it }

                return when (data.firstOrNull()) {
                    UNIVERSAL_NON_REALTIME -> parseUniversal(data)
                    ROLAND -> parseRoland(data)
                    YAMAHA -> parseYamaha(data)
                    else -> null
                }
            }

            /** `7E dev 09 0x`: General MIDI system on/off. */
            private fun parseUniversal(data: List<Int>): SysexMessage? {
                if (data.size < 4 || data[2] != 0x09) return null
                return when (data[3]) {
                    0x01, 0x02 -> Reset(Mode.Default) // GM1 system on, GM system off
                    0x03 -> Reset(Mode.GM2)
                    else -> null
                }
            }

            /** `41 dev 42 12 addr addr addr value... checksum`: Roland GS data set. The checksum isn't verified. */
            private fun parseRoland(data: List<Int>): SysexMessage? {
                if (data.size < 8 || data[2] != 0x42 || data[3] != 0x12) return null
                val address = Triple(data[4], data[5], data[6])
                val value = data[7]
                return when {
                    address == Triple(0x40, 0x00, 0x7F) && value == 0x00 -> Reset(Mode.Default) // GS reset

                    // "Use for rhythm part": 40 1p 15, where p numbers the parts 10, 1-9, 11-16.
                    address.first == 0x40 && (address.second and 0xF0) == 0x10 && address.third == 0x15 -> {
                        val part = address.second and 0x0F
                        val channel = when (part) {
                            0 -> PRIMARY_RHYTHM_CHANNEL
                            in 1..9 -> part - 1
                            else -> part
                        }
                        PartState(channel, if (value == 0) ChannelState.Melody else ChannelState.Rhythm)
                    }

                    else -> null
                }
            }

            /** `43 1n 4C addr addr addr value`: Yamaha XG parameter change. */
            private fun parseYamaha(data: List<Int>): SysexMessage? {
                if (data.size < 7 || (data[1] and 0xF0) != 0x10 || data[2] != 0x4C) return null
                return when {
                    data[3] == 0x00 && data[4] == 0x00 && data[5] == 0x7E && data[6] == 0x00 -> Reset(Mode.XG)

                    // Part mode: 08 pp 07 mm, where mm is 0 for normal and anything else for a drum setup.
                    data[3] == 0x08 && data[4] in 0..15 && data[5] == 0x07 -> PartState(
                        data[4],
                        if (data[6] == 0) ChannelState.Melody else ChannelState.Rhythm,
                    )

                    else -> null
                }
            }
        }
    }
}
