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

package org.wysko.midis2jam2.midi.system

import org.wysko.kmidi.midi.TimeBasedSequence
import kotlin.time.Duration

/**
 * A sequencer that plays nothing.
 *
 * For performances whose sound is made some other way, such as a recording, whose audio is rendered offline.
 */
class SilentSequencer : JwSequencer {
    override var sequence: TimeBasedSequence? = null

    override var isRunning: Boolean = false
        private set

    override var isOpen: Boolean = false
        private set

    override fun open(device: MidiDevice) {
        isOpen = true
    }

    override fun close() {
        isOpen = false
        isRunning = false
    }

    override fun start() {
        isRunning = true
    }

    override fun stop() {
        isRunning = false
    }

    override fun setPosition(position: Duration, start: Boolean, onFinish: () -> Unit) {
        isRunning = start
        onFinish()
    }

    override fun resetDevice() = Unit

    override fun sendData(data: ByteArray) = Unit
}
