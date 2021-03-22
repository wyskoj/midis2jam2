package org.wysko.midis2jam2.midi;

/**
 * A MIDI event that adjusts a controller.
 */
public class MidiControlEvent extends MidiEvent {
	
	/**
	 * The number of the controller.
	 */
	final int controlNum;
	
	/**
	 * The value.
	 */
	final int value;
	
	/**
	 * The channel.
	 */
	final int channel;
	
	/**
	 * Instantiates a new MIDI control event.
	 *
	 * @param time       the time
	 * @param channel    the channel
	 * @param controlNum the control num
	 * @param value      the value
	 */
	public MidiControlEvent(long time, int channel, int controlNum, int value) {
		super(time);
		this.channel = channel;
		this.controlNum = controlNum;
		this.value = value;
	}
}
