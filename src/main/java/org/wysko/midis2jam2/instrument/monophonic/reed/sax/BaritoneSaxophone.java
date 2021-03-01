package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiFile;

import java.util.List;

public class BaritoneSaxophone extends Saxophone {
	
	
	/**
	 * Constructs a saxophone.
	 *
	 * @param context context to midis2jam2
	 * @param file    context to the midi file
	 * @param events  the MIDI events related to this instrument
	 */
	public BaritoneSaxophone(Midis2jam2 context, MidiFile file, List<MidiChannelSpecificEvent> events) {
		super(context, file, events);
		
		
	}
	
	@Override
	public void tick(double time, float delta) {
	
	}
	
	public class BaritoneSaxophoneClone extends MonophonicClone {
		@Override
		public void tick(double time, float delta) {
		
		}
	}
}
