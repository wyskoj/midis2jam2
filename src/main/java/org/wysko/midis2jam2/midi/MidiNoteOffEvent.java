package org.wysko.midis2jam2.midi;

/**
 * Signals that a note should stop playing.
 */
public class MidiNoteOffEvent extends MidiNoteEvent {
	
	/**
	 * Instantiates a new MIDI note off event.
	 *
	 * @param time    the time
	 * @param channel the channel
	 * @param note    the note
	 */
	public MidiNoteOffEvent(long time, int channel, int note) {
		super(time, channel, note);
	}
	
	@Override
	public String toString() {
		return "MidiNoteOffEvent{" +
				"time=" + time +
				", channel=" + channel +
				", note=" + note +
				'}';
	}
}
