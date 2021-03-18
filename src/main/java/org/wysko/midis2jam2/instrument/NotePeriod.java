package org.wysko.midis2jam2.instrument;

import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

/**
 * A note period is pair of a {@link MidiNoteOnEvent} and a {@link MidiNoteOffEvent}. A note period is the equivalent
 * of the blocks you would see in a MIDI piano roll editor.
 */
public class NotePeriod {
	
	/**
	 * The MIDI pitch of this note period.
	 */
	public final int midiNote;
	/**
	 * The start time, expressed in seconds.
	 */
	public final double startTime;
	/**
	 * The end time, expressed in seconds.
	 */
	public final double endTime;
	/**
	 * The {@link MidiNoteOnEvent}.
	 */
	public final MidiNoteOnEvent noteOn;
	/**
	 * The {@link MidiNoteOffEvent}.
	 */
	public final MidiNoteOffEvent noteOff;
	
	/**
	 * Instantiates a new Note period.
	 *
	 * @param midiNote  the midi note
	 * @param startTime the start time
	 * @param endTime   the end time
	 * @param noteOn    the note on
	 * @param noteOff   the note off
	 */
	public NotePeriod(int midiNote, double startTime, double endTime,
	                  MidiNoteOnEvent noteOn, MidiNoteOffEvent noteOff) {
		this.midiNote = midiNote;
		this.startTime = startTime;
		this.endTime = endTime;
		this.noteOn = noteOn;
		this.noteOff = noteOff;
	}
	
	/**
	 * Returns the MIDI tick this note period starts.
	 *
	 * @return the MIDI tick this note period starts
	 */
	public long startTick() {
		return noteOn.time;
	}
	
	/**
	 * Returns the MIDI tick this note period ends.
	 *
	 * @return the MIDI tick this note period ends
	 */
	public long endTick() {
		return noteOff.time;
	}
	
	/**
	 * Returns the length of this note period, expressed in seconds.
	 *
	 * @return the length of this note period, expressed in seconds
	 */
	public double duration() {
		return endTime - startTime;
	}
	
	/**
	 * Determines whether this note period would be playing at a given time, in seconds.
	 *
	 * @param time the time to check at, in seconds
	 * @return true if this note period is playing, false otherwise
	 */
	public boolean isPlayingAt(double time) {
		return time <= endTime && time >= startTime;
	}
}
