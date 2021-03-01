package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiFile;

public abstract class Saxophone extends MonophonicInstrument {
	public static final int KEY_COUNT = 20;
	
	/**
	 * Constructs a saxophone.
	 *
	 * @param context context to midis2jam2
	 * @param file    context to the midi file
	 */
	public Saxophone(Midis2jam2 context, MidiFile file) {
		super(context, file);
		
	}
}
