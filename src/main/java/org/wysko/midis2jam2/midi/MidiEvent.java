package org.wysko.midis2jam2.midi;

import java.util.Comparator;

public abstract class MidiEvent{
	public final long time;
	
	public MidiEvent(long time) {
		this.time = time;
	}
	
	public static class MidiEventComparator implements Comparator<MidiEvent> {
		
		@Override
		public int compare(MidiEvent e1, MidiEvent e2) {
			return (int) (e1.time-e2.time);
		}
	}
}
