package org.wysko.midis2jam2.midi;

/**
 * A {@link MidiNoteOnEvent} or a {@link MidiNoteOffEvent}.
 */
public class MidiNoteEvent extends MidiChannelSpecificEvent {
	
	/**
	 * The MIDI note.
	 */
	public final int note;
	
	/**
	 * Instantiates a new MIDI note event.
	 *
	 * @param time    the time
	 * @param channel the channel
	 * @param note    the note
	 */
	protected MidiNoteEvent(long time, int channel, int note) {
		super(time, channel);
		this.note = note;
	}
}
