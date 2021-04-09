/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.midi;

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
	
	public boolean animationStarted = false;
	
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
	
	@Override
	public String toString() {
		return "NotePeriod{" +
				"midiNote=" + midiNote +
				", startTime=" + startTime +
				", endTime=" + endTime +
				'}';
	}
}
