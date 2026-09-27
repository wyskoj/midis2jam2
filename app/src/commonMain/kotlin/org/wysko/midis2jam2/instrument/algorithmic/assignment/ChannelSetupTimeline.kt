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

/** The specification a file (or the synthesizer playing it) follows, which decides how bank select is read. */
enum class MidiMode {
    /** General MIDI: bank select is ignored. */
    GM,

    /** Roland GS: bank select MSB picks a variation of the program. */
    GS,

    /** Yamaha XG: bank select MSB and LSB pick a variation; MSB 126 and 127 pick drum kits. */
    XG,

    /** General MIDI 2: bank 121 holds the melodic voices, with LSB picking a variation; bank 120 the drum kits. */
    GM2,
}

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
 * Everything that decides what a channel plays at a moment in the file.
 *
 * @property mode The specification in effect.
 * @property state Whether the channel is a melodic or rhythm channel.
 * @property msb The bank select MSB the current program was chosen with.
 * @property lsb The bank select LSB the current program was chosen with.
 * @property program The zero-based program, or `null` before the channel's first program change.
 */
data class ChannelSetup(
    val mode: MidiMode,
    val state: ChannelState,
    val msb: Int,
    val lsb: Int,
    val program: Int?,
)

/**
 * What each channel is set up to play, and when that changes.
 *
 * Bank select only takes effect at the next program change, as every specification has it. On top of that, GS, XG
 * and GM2 files can turn any channel into a rhythm channel (and back) with system exclusive messages or bank select.
 * The synthesizer honours all of this, so the visuals need to as well.
 */
class ChannelSetupTimeline private constructor(
    private val initial: Array<ChannelSetup>,
    private val transitions: Array<List<Transition>>,
) {

    private class Transition(val tick: Int, val setup: ChannelSetup)

    /** The setup of [channel] (zero-based) at [tick], counting changes made at that very tick. */
    fun setupAt(channel: Int, tick: Int): ChannelSetup {
        val list = transitions[channel]
        // Find the first transition after [tick]; the one before it is in effect.
        var low = 0
        var high = list.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (list[middle].tick <= tick) low = middle + 1 else high = middle
        }
        return if (low == 0) initial[channel] else list[low - 1].setup
    }

    /** The state of [channel] (zero-based) at [tick]. */
    fun stateAt(channel: Int, tick: Int): ChannelState = setupAt(channel, tick).state

    companion object {
        private const val CC_BANK_SELECT_MSB = 0
        private const val CC_BANK_SELECT_LSB = 32
        private const val XG_DRUM_BANK = 127
        private const val XG_SFX_KIT_BANK = 126
        private const val GM2_RHYTHM_BANK = 120
        private const val GM2_MELODY_BANK = 121

        /** The bank select MSB a channel starts with in [mode]. */
        private fun defaultMsb(mode: MidiMode, channel: Int): Int = when (mode) {
            MidiMode.GM2 -> if (channel == PRIMARY_RHYTHM_CHANNEL) GM2_RHYTHM_BANK else GM2_MELODY_BANK
            MidiMode.XG -> if (channel == PRIMARY_RHYTHM_CHANNEL) XG_DRUM_BANK else 0
            MidiMode.GS, MidiMode.GM -> 0
        }

        /**
         * Reads the channel setups out of [events], which may come from any number of tracks.
         *
         * @param initialMode The specification in effect before the file sends a reset of its own: the one the
         * synthesizer was reset to, if any.
         */
        @Suppress("CyclomaticComplexMethod", "LongMethod")
        fun from(events: List<Event>, initialMode: MidiMode = MidiMode.GM): ChannelSetupTimeline {
            val initial = Array(16) {
                ChannelSetup(initialMode, ChannelState.DEFAULT_STATE[it], defaultMsb(initialMode, it), 0, null)
            }
            val transitions = Array(16) { mutableListOf<Transition>() }
            val current = initial.copyOf()
            val msbRegister = IntArray(16) { initial[it].msb }
            val lsbRegister = IntArray(16)

            fun set(channel: Int, tick: Int, setup: ChannelSetup) {
                if (current[channel] == setup) return
                current[channel] = setup
                transitions[channel] += Transition(tick, setup)
            }

            // A reset puts every channel back to its defaults. The program is left alone: a reset and a program
            // change at the same tick, in different tracks, can arrive in either order.
            fun reset(tick: Int, mode: MidiMode) = repeat(16) {
                msbRegister[it] = defaultMsb(mode, it)
                lsbRegister[it] = 0
                set(
                    it,
                    tick,
                    current[it].copy(
                        mode = mode,
                        state = ChannelState.DEFAULT_STATE[it],
                        msb = msbRegister[it],
                        lsb = 0,
                    ),
                )
            }

            events.sortedBy { it.tick }.forEach { event ->
                when (event) {
                    is SysexEvent -> when (val message = SysexMessage.parse(event.data)) {
                        is SysexMessage.Reset -> reset(event.tick, message.mode)
                        is SysexMessage.PartState -> with(message) {
                            set(channel, event.tick, current[channel].copy(state = state))
                        }

                        null -> Unit
                    }

                    is ControlChangeEvent -> when (event.controller.toInt()) {
                        CC_BANK_SELECT_MSB -> msbRegister[event.channel.toInt()] = event.value.toInt()
                        CC_BANK_SELECT_LSB -> lsbRegister[event.channel.toInt()] = event.value.toInt()
                    }

                    is ProgramEvent -> {
                        val channel = event.channel.toInt()
                        val setup = current[channel]
                        val msb = msbRegister[channel]
                        val state = when (setup.mode) {
                            MidiMode.XG -> when (msb) {
                                XG_DRUM_BANK, XG_SFX_KIT_BANK -> ChannelState.Rhythm
                                // Channel 10 is left alone: plenty of files send bank 0 to every channel as a matter
                                // of habit, and we'd rather keep the drums than lose them.
                                else -> if (channel == PRIMARY_RHYTHM_CHANNEL) setup.state else ChannelState.Melody
                            }

                            MidiMode.GM2 -> when (msb) {
                                GM2_RHYTHM_BANK -> ChannelState.Rhythm
                                GM2_MELODY_BANK -> ChannelState.Melody
                                else -> setup.state
                            }

                            // In GS, bank select only picks a variation (and bank 127 is a *melodic* sound set).
                            MidiMode.GS, MidiMode.GM -> setup.state
                        }
                        set(
                            channel,
                            event.tick,
                            setup.copy(state = state, msb = msb, lsb = lsbRegister[channel], program = event.program.toInt()),
                        )
                    }

                    else -> Unit
                }
            }

            return ChannelSetupTimeline(initial, Array(16) { transitions[it].toList() })
        }
    }

    /** The system exclusive messages that affect how channels are set up. */
    private sealed class SysexMessage {
        /** A GM, GM2, GS or XG system reset. */
        data class Reset(val mode: MidiMode) : SysexMessage()

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
                    0x01, 0x02 -> Reset(MidiMode.GM) // GM1 system on, GM system off
                    0x03 -> Reset(MidiMode.GM2)
                    else -> null
                }
            }

            /** `41 dev 42 12 addr addr addr value... checksum`: Roland GS data set. The checksum isn't verified. */
            private fun parseRoland(data: List<Int>): SysexMessage? {
                if (data.size < 8 || data[2] != 0x42 || data[3] != 0x12) return null
                val address = Triple(data[4], data[5], data[6])
                val value = data[7]
                return when {
                    address == Triple(0x40, 0x00, 0x7F) && value == 0x00 -> Reset(MidiMode.GS) // GS reset

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
                    data[3] == 0x00 && data[4] == 0x00 && data[5] == 0x7E && data[6] == 0x00 -> Reset(MidiMode.XG)

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
