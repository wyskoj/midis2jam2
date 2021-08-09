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

import org.jetbrains.annotations.NotNull;

import java.util.List;
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
	
	/**
	 * Given a list of program events, removes duplicate events. There are two types of duplicate events:
	 * <ul>
	 *     <li>Events that occur at the same time</li>
	 *     <li>Adjacent events that have the same program value</li>
	 * </ul>
	 * <p>
	 * For events at the same time, the last of two events is kept (in the order of the list). So, if a list contained
	 * <pre>
	 *     [time = 0, num = 43], [time = 0, num = 24], [time = 0, num = 69]
	 * </pre>
	 * it would afterwards contain
	 * <pre>
	 *      [time = 0, num = 69]
	 * </pre>
	 * <p>
	 * For events that have the same program value, the first of two events is kept (in the order of the list). So,
	 * if a list contained
	 * <pre>
	 *      [time = 0, num = 50], [time = 128, num = 50], [time = 3000, num = 50]
	 * </pre>
	 * it would afterwards contain
	 * <pre>
	 *     [time = 0, num = 50]
	 * </pre>
	 *
	 * @param programEvents the list of program events
	 */
	public static void removeDuplicateProgramEvents(@NotNull List<MidiProgramEvent> programEvents) {
		/* Remove program events at same time (keep the last one) */
		for (int i = programEvents.size() - 2; i >= 0; i--) {
			while (i < programEvents.size() - 1 && programEvents.get(i).time == programEvents.get(i + 1).time) {
				programEvents.remove(i);
			}
		}
		
		/* Remove program events with same value (keep the first one) */
		for (int i = programEvents.size() - 2; i >= 0; i--) {
			while (i != programEvents.size() - 1 && programEvents.get(i).programNum == programEvents.get(i + 1).programNum) {
				programEvents.remove(i + 1);
			}
		}
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
