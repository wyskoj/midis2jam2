package org.wysko.midis2jam2.midi;

public class MidiPitchBendEvent extends MidiChannelSpecificEvent {
	final int value;
	
	public MidiPitchBendEvent(long time, int channel, int value) {
		super(time, channel);
		this.value = value;
	}
}
