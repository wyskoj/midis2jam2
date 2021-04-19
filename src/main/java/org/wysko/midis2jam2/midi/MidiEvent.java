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

import java.util.Objects;

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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MidiEvent midiEvent = (MidiEvent) o;
		return time == midiEvent.time;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(time);
	}
}
