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
 * Indicates a change in the value of a MIDI controller.
 *
 * @property time The time this event occurs in MIDI ticks.
 * @property channel The channel on which the controller change should occur.
 * @property controller The controller number that is changing.
 * @property value The new value of the controller.
 */
data class MidiControlChangeEvent(
    override val time: Long,
    override val channel: Int,
    val controller: Int,
    val value: Int
) : MidiChannelEvent(time, channel)
