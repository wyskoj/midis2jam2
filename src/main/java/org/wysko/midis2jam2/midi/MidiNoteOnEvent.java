package org.wysko.midis2jam2.midi;

public class MidiNoteOnEvent extends MidiNoteEvent {
	public final int velocity;
	
	public MidiNoteOnEvent(long time, int channel, int note, int velocity) {
		super(time, channel, note);
		this.velocity = velocity;
	}
	
	@Override
	public String toString() {
		return "MidiNoteOnEvent{" +
				"time=" + time +
				", channel=" + channel +
				", note=" + note +
				", velocity=" + velocity +
				'}';
	}
}
