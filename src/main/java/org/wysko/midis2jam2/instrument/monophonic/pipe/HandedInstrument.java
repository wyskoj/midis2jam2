package org.wysko.midis2jam2.instrument.monophonic.pipe;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiFile;

import java.util.HashMap;

public abstract class HandedInstrument extends MonophonicInstrument {
	public HashMap<Integer, HandedClone.Hands> KEY_MAPPING;
	public HandedInstrument(Midis2jam2 context, MidiFile file) {
		super(context);
	}
}
