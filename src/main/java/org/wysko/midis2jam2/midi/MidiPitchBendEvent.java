package org.wysko.midis2jam2.midi;

public class MidiPitchBendEvent extends MidiEvent {
	final int channel;
	final int value;
	
	public MidiPitchBendEvent(long time, int channel, int value) {
		super(time);
		this.channel = channel;
		this.value = value;
	}
}
