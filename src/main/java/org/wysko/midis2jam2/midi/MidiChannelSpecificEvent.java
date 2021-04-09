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

import java.util.Comparator;

/**
 * Any MIDI event that is specific to a certain channel.
 */
public class MidiChannelSpecificEvent extends MidiEvent {
	
	/**
	 * Compares events by time.
	 */
	public static final Comparator<MidiChannelSpecificEvent> COMPARE_BY_TIME = new CompareByTime();
	
	/**
	 * The channel this MIDI event applies to.
	 */
	public final int channel;
	
	/**
	 * Instantiates a new MIDI channel specific event.
	 *
	 * @param time    the time
	 * @param channel the channel
	 */
	public MidiChannelSpecificEvent(long time, int channel) {
		super(time);
		this.channel = channel;
	}
	
	private static class CompareByTime implements Comparator<MidiChannelSpecificEvent> {
		
		@Override
		public int compare(MidiChannelSpecificEvent o1, MidiChannelSpecificEvent o2) {
			return Long.compare(o1.time, o2.time);
		}
	}
}
