package org.wysko.midis2jam2.midi;

/**
 * Defines how fast the MIDI file should play.
 */
public class MidiTempoEvent extends MidiEvent {
	
	/**
	 * The tempo value, expressed in microseconds per pulse.
	 */
	public final int number;
	
	/**
	 * Instantiates a new MIDI tempo event.
	 *
	 * @param time   the time
	 * @param number the tempo value, expressed in microseconds per pulse
	 */
	public MidiTempoEvent(long time, int number) {
		super(time);
		this.number = number;
	}
}
