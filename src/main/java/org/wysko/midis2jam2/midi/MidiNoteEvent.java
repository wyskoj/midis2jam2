package org.wysko.midis2jam2.midi;

public class MidiNoteEvent extends MidiChannelSpecificEvent {
	public final int note;
	
	protected MidiNoteEvent(long time, int channel, int note) {
		super(time, channel);
		this.note = note;
	}
}
