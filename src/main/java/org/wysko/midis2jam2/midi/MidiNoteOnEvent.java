package org.wysko.midis2jam2.midi;

/**
 * Signals a note should begin playing.
 */
public class MidiNoteOnEvent extends MidiNoteEvent {
	
	/**
	 * The velocity of the note.
	 */
	public final int velocity;
	
	/**
	 * Instantiates a new MIDI note on event.
	 *
	 * @param time     the time
	 * @param channel  the channel
	 * @param note     the note
	 * @param velocity the velocity
	 */
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
