/*
 * Copyright (C) 2021 Jacob Wysko
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

package org.wysko.midis2jam2.starter

import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.midi.MidiFile

/**
 * Implementations of this interface are responsible for starting and handling the sequencer when midis2jam2 is
 * running. The purpose of this interface is to move `javax.sound.midi` dependencies outside of [Midis2jam2] because
 * Android does not include this library.
 */
interface SequencerHandler {
    /** Returns true if the sequencer is open and ready for playback, false otherwise. */
    fun isOpen(): Boolean

    /** Begins playback. */
    fun start(midiFile: MidiFile)

    /** Stops playback. */
    fun stop()

    /** Returns the current position in the song. The unit does not matter, as long as it is the same as [duration]. */
    fun position(): Long

    /** Returns the length of the song. The unit does not matter, as long as it is the same as [position]. */
    fun duration(): Long

}