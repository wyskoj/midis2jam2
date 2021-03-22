package org.wysko.midis2jam2.midi;

/**
 * MIDI files are made up of events.
 */
public abstract class MidiEvent {
	
	/**
	 * The time at which this MIDI event occurs, expressed in MIDI ticks.
	 */
	public final long time;
	
	/**
	 * Instantiates a new MIDI event.
	 *
	 * @param time the time
	 */
	public MidiEvent(long time) {
		this.time = time;
	}
	
}
