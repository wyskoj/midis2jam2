package org.wysko.midis2jam2.midi;

/**
 * Indicates the channel's pitch is to change.
 */
public class MidiPitchBendEvent extends MidiChannelSpecificEvent {
	
	/**
	 * The pitch bend amount.
	 */
	final int value;
	
	/**
	 * Instantiates a new MIDI pitch bend event.
	 *
	 * @param time    the time
	 * @param channel the channel
	 * @param value   the value
	 */
	public MidiPitchBendEvent(long time, int channel, int value) {
		super(time, channel);
		this.value = value;
	}
}
