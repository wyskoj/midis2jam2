package org.wysko.midis2jam2.instrument.guitar;

import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

class NotePeriodWithFretboardPosition extends NotePeriod {
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
