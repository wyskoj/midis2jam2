package org.wysko.midis2jam2.midi;

public class MidiProgramEvent extends MidiChannelSpecificEvent {
	public final int programNum;
	
	public MidiProgramEvent(long time, int channel, int programNum) {
		super(time, channel);
		this.programNum = programNum;
	}
}
