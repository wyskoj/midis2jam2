package org.wysko.midis2jam2.midi;

public class MidiProgramEvent extends MidiEvent {
	final int programNum;
	final int channel;
	
	public MidiProgramEvent(long time, int channel, int programNum) {
		super(time);
		this.channel = channel;
		this.programNum = programNum;
	}
}
