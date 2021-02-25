package org.wysko.midis2jam2.midi;

public class MidiNoteOffEvent extends MidiNoteEvent {
	
	public MidiNoteOffEvent(long time, int channel, int note) {
		super(time, channel, note);
	}
}
