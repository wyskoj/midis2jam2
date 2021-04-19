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
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		MidiProgramEvent that = (MidiProgramEvent) o;
		return programNum == that.programNum;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), programNum);
	}
}
