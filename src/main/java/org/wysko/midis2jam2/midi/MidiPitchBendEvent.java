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
 * Indicates the channel's pitch is to change.
 */
public class MidiPitchBendEvent extends MidiChannelSpecificEvent {
	
	/**
	 * The pitch bend amount.
	 */
	public final int value;
	
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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MidiPitchBendEvent that = (MidiPitchBendEvent) o;
		return value == that.value;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), value);
	}
}
