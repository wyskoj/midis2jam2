package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiFile;

import java.util.List;

public abstract class Saxophone extends MonophonicInstrument implements Instrument {
	
	protected final static int KEY_COUNT = 20;
	
	/**
	 * Constructs a saxophone.
	 *
	 * @param context context to midis2jam2
	 * @param file    context to the midi file
	 * @param events  the MIDI events related to this instrument
	 */
	public Saxophone(Midis2jam2 context, MidiFile file,
	                 List<MidiChannelSpecificEvent> events) {
		super(context, file, events);
		calculateNotePeriods(noteEvents);
	}
}
