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
import kotlin.time.Duration

interface JwSequencer {
    var sequence: TimeBasedSequence?
    val isRunning: Boolean
    val isOpen: Boolean

    fun open(device: MidiDevice)
    fun close()
    fun start()
    fun stop()
    fun setPosition(position: Duration)
    fun resetDevice()
    fun sendData(data: ByteArray)
}