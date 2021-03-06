package org.wysko.midis2jam2.instrument.percussive;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.midi.MidiTempoEvent;

import java.util.List;

public abstract class Drum extends Instrument {
	static final float DRUM_RECOIL_COMEBACK = 22;
	protected final List<MidiNoteOnEvent> hits;
	
	protected Drum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		this.context = context;
		this.hits = hits;
	}
}
