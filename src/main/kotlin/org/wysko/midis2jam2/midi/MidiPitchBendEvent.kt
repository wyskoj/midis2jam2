/*
 * Copyright (C) 2024 Jacob Wysko
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
package org.wysko.midis2jam2.midi

/**
 * Indicates a change in the pitch-bend value of a MIDI channel.
 *
 * @property time The time this event occurs in MIDI ticks.
 * @property channel The channel on which the pitch-bend change should occur.
 * @property value The new value of the pitch-bend.
 */
data class MidiPitchBendEvent(
    override val time: Long,
    override val channel: Int,
    val value: Int
) : MidiChannelEvent(time, channel)
