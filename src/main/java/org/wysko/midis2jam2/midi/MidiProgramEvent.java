package org.wysko.midis2jam2.midi;

/**
 * Indicates which instrument should be playing at a certain time.
 */
public class MidiProgramEvent extends MidiChannelSpecificEvent {
	
	/**
	 * The number of the instrument to switch to.
	 */
	public final int programNum;
	
	/**
	 * Instantiates a new MIDI program event.
	 *
	 * @param time       the time
	 * @param channel    the channel
	 * @param programNum the program num
	 */
	public MidiProgramEvent(long time, int channel, int programNum) {
		super(time, channel);
		this.programNum = programNum;
	}
}
