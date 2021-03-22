package org.wysko.midis2jam2.midi;

public class MidiControlEvent extends MidiEvent {
	final int controlNum;
	
	final int value;
	
	final int channel;
	
	public MidiControlEvent(long time, int channel, int controlNum, int value) {
		super(time);
		this.channel = channel;
		this.controlNum = controlNum;
		this.value = value;
	}
}
