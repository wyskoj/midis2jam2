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
	public FrettingEngine.FretboardPosition position;
	
	private NotePeriodWithFretboardPosition(int midiNote, double startTime, double endTime,
	                                        MidiNoteOnEvent noteOn, MidiNoteOffEvent noteOff) {
		super(midiNote, startTime, endTime, noteOn, noteOff);
		position = new FrettingEngine.FretboardPosition(0, 0);
	}
	
	public static NotePeriodWithFretboardPosition fromNotePeriod(NotePeriod notePeriod) {
		return new NotePeriodWithFretboardPosition(notePeriod.midiNote, notePeriod.startTime, notePeriod.endTime,
				notePeriod.noteOn, notePeriod.noteOff);
	}
}
