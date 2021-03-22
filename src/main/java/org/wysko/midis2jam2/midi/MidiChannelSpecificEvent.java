package org.wysko.midis2jam2.midi;

import java.util.Comparator;

public class MidiChannelSpecificEvent extends MidiEvent {
	public static final Comparator<MidiChannelSpecificEvent> COMPARE_BY_TIME = new CompareByTime();
	
	public final int channel;
	
	public MidiChannelSpecificEvent(long time, int channel) {
		super(time);
		this.channel = channel;
	}
	
	private static class CompareByTime implements Comparator<MidiChannelSpecificEvent> {
		@Override
		public int compare(MidiChannelSpecificEvent o1, MidiChannelSpecificEvent o2) {
			return Long.compare(o1.time, o2.time);
		}
	}
}
