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
