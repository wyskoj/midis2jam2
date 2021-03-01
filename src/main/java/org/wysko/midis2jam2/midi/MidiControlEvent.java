package org.wysko.midis2jam2.midi;

public class MidiControlEvent extends MidiChannelSpecificEvent {
	final int controlNum;
	final int value;
	
	public MidiControlEvent(long time, int channel, int controlNum, int value) {
		super(time, channel);
		this.controlNum = controlNum;
		this.value = value;
	}
}
