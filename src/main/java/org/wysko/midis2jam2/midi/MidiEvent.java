package org.wysko.midis2jam2.midi;

public abstract class MidiEvent {
	public final long time;
	
	public MidiEvent(long time) {
		this.time = time;
	}
}
