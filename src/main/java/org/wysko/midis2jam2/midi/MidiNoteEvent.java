package org.wysko.midis2jam2.midi;

public class MidiNoteEvent extends MidiEvent {
	final int channel;
	public final int note;
	
	protected MidiNoteEvent(long time, int channel, int note) {
		super(time);
		this.channel = channel;
		this.note = note;
	}
}
