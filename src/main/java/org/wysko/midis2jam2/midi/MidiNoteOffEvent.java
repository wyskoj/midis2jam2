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
 * Signals that a note should stop playing.
 */
public class MidiNoteOffEvent extends MidiNoteEvent {
	
	/**
	 * Instantiates a new MIDI note off event.
	 *
	 * @param time    the time
	 * @param channel the channel
	 * @param note    the note
	 */
	public MidiNoteOffEvent(long time, int channel, int note) {
		super(time, channel, note);
	}
	
	@Override
	public String toString() {
		return "MidiNoteOffEvent{" +
				"time=" + time +
				", channel=" + channel +
				", note=" + note +
				'}';
	}
}
