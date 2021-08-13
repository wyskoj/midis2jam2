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
 * A MIDI event that adjusts a controller.
 */
public class MidiControlEvent extends MidiEvent {
	
	/**
	 * The number of the controller.
	 */
	public final int controlNum;
	
	/**
	 * The value.
	 */
	public final int value;
	
	/**
	 * The channel.
	 */
	public final int channel;
	
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
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MidiControlEvent that = (MidiControlEvent) o;
		return controlNum == that.controlNum && value == that.value && channel == that.channel;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), controlNum, value, channel);
	}
}
