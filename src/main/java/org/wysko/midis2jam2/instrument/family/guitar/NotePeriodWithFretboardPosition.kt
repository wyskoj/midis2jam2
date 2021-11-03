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
package org.wysko.midis2jam2.instrument.family.guitar

import org.wysko.midis2jam2.midi.MidiNoteOffEvent
import org.wysko.midis2jam2.midi.MidiNoteOnEvent
import org.wysko.midis2jam2.midi.NotePeriod

/** Note periods that also need to keep track of where they have been assigned to on the fretboard. */
class NotePeriodWithFretboardPosition private constructor(
    midiNote: Int, startTime: Double, endTime: Double,
    noteOn: MidiNoteOnEvent, noteOff: MidiNoteOffEvent,
) : NotePeriod(midiNote, startTime, endTime, noteOn, noteOff) {

    /** The fretboard position this note period was assigned to. */
    var position: FretboardPosition? = null

    companion object {
        /**
         * Returns a NotePeriodWithFretboardPosition from a [NotePeriod].
         *
         * @param notePeriod the note period
         * @return a note period with fretboard position
         */
        @JvmStatic
        fun fromNotePeriod(notePeriod: NotePeriod): NotePeriodWithFretboardPosition {
            return NotePeriodWithFretboardPosition(
                notePeriod.midiNote, notePeriod.startTime, notePeriod.endTime,
                notePeriod.noteOn, notePeriod.noteOff
            )
        }
    }

    /** Instantiates a new NotePeriodWithFretboardPosition with default position (-1, -1). */
    init {
        position = FretboardPosition(-1, -1)
    }
}