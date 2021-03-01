package org.wysko.midis2jam2.midi;

import java.util.Comparator;

public abstract class MidiEvent {
	public long time;
	public MidiEvent(long time) {
		this.time = time;
	}
}
