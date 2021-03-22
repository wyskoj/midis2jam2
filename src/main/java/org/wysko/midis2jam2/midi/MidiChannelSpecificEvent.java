package org.wysko.midis2jam2.midi;

import java.util.Comparator;

/**
 * Any MIDI event that is specific to a certain channel.
 */
public class MidiChannelSpecificEvent extends MidiEvent {
	
	/**
	 * Compares events by time.
	 */
	public static final Comparator<MidiChannelSpecificEvent> COMPARE_BY_TIME = new CompareByTime();
	
	/**
	 * The channel this MIDI event applies to.
	 */
	public final int channel;
	
	/**
	 * Instantiates a new MIDI channel specific event.
	 *
	 * @param time    the time
	 * @param channel the channel
	 */
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
