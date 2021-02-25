package org.wysko.midis2jam2.midi;

public class MidiTempoEvent extends MidiEvent {
	/**
	 * Expressed in microseconds per pulse.
	 */
	final int number;
	
	public MidiTempoEvent(long time, int number) {
		super(time);
		this.number = number;
	}
}
