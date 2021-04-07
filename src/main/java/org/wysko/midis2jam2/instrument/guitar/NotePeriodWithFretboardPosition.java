package org.wysko.midis2jam2.instrument.guitar;

import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

/**
 * Note periods that also need to keep track of where they have been assigned to on the fretboard.
 */
public class NotePeriodWithFretboardPosition extends NotePeriod {
	
	/**
	 * The fretboard position this note period was assigned to.
	 */
	public FretboardPosition position;
	
	
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
		position = new FretboardPosition(-1, -1);
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
}
