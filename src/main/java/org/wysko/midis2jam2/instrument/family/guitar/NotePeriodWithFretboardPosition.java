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

package org.wysko.midis2jam2.instrument.family.guitar;

import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.midi.NotePeriod;

/**
 * Note periods that also need to keep track of where they have been assigned to on the fretboard.
 */
public final class NotePeriodWithFretboardPosition extends NotePeriod {
	
	/**
	 * The fretboard position this note period was assigned to.
	 */
	private FretboardPosition position;
	
	/**
	 * Instantiates a new NotePeriodWithFretboardPosition with default position (-1, -1).
	 *
	 * @param midiNote  the midi note
	 * @param startTime the start time
	 * @param endTime   the end time
	 * @param noteOn    the note on event
	 * @param noteOff   the note off event
	 */
	private NotePeriodWithFretboardPosition(int midiNote, double startTime, double endTime,
	                                        MidiNoteOnEvent noteOn, MidiNoteOffEvent noteOff) {
		super(midiNote, startTime, endTime, noteOn, noteOff);
		setPosition(new FretboardPosition(-1, -1));
	}
	
	/**
	 * Returns a NotePeriodWithFretboardPosition from a {@link NotePeriod}.
	 *
	 * @param notePeriod the note period
	 * @return a note period with fretboard position
	 */
	public static NotePeriodWithFretboardPosition fromNotePeriod(NotePeriod notePeriod) {
		return new NotePeriodWithFretboardPosition(notePeriod.midiNote, notePeriod.startTime, notePeriod.endTime,
				notePeriod.noteOn, notePeriod.noteOff);
	}
	
	public FretboardPosition getPosition() {
		return position;
	}
	
	public void setPosition(FretboardPosition position) {
		this.position = position;
	}
}
