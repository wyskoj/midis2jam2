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

package org.wysko.midis2jam2.instrument.algorithmic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.NotePeriod;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides utility functions that check a list of {@link NotePeriod}s or {@link MidiEvent}s and collects any events
 * that are now to be performed.
 */
public final class NoteQueue {
	
	private NoteQueue() {
		// Hidden constructor
	}
	
	/**
	 * Given a list of {@link MidiEvent MidiEvents}, removes the events that are needing animation. This is any event
	 * that has a time equal to or less than the current time.
	 * <p>
	 * This method assumes that the events parameter is sorted by event time.
	 *
	 * @param events the events to pull from
	 * @param time   the current time, in seconds
	 * @return a list of events that occur now or before
	 */
	@NotNull
	public static <E extends MidiEvent> List<E> collect(@NotNull Iterable<E> events,
	                                                    @NotNull Midis2jam2 context,
	                                                    double time) {
		List<E> queue = new ArrayList<>();
		for (var iterator = events.iterator(); iterator.hasNext(); ) {
			var next = iterator.next();
			/* If the event should occur now, or before now, remove it from the list and add it to the queue */
			if (context.getFile().eventInSeconds(next) <= time) {
				queue.add(next);
				iterator.remove();
			} else {
				/* Because we know the list is sorted (or assuming it is), we can get the fuck out if we see an event
				 * that is not supposed to occur yet. */
				break;
			}
			
		}
		return queue;
	}
	
	/**
	 * Given a list of {@link MidiEvent MidiEvents}, removes the events that are needing animation. This is any event
	 * that has a time equal to or less than the current time.
	 * <p>
	 * This method assumes that the events parameter is sorted by event time.
	 *
	 * @param events the events to pull from
	 * @param time   the current time, in seconds
	 * @return the last hit to play
	 */
	@Nullable
	public static <E extends MidiEvent> E collectOne(@NotNull Iterable<E> events,
	                                                 @NotNull Midis2jam2 context,
	                                                 double time) {
		List<E> queue = new ArrayList<>();
		for (var iterator = events.iterator(); iterator.hasNext(); ) {
			var next = iterator.next();
			/* If the event should occur now, or before now, remove it from the list and add it to the queue */
			if (context.getFile().eventInSeconds(next) <= time) {
				queue.add(next);
				iterator.remove();
			} else {
				/* Because we know the list is sorted (or assuming it is), we can get the fuck out if we see an event
				 * that is not supposed to occur yet. */
				break;
			}
			
		}
		if (queue.isEmpty()) {
			return null;
		}
		return queue.get(queue.size() - 1);
	}
	
	/**
	 * Given a list of {@link MidiEvent MidiEvents}, removes the events that are needing animation. This is any event
	 * that has a time equal to or less than the current time.
	 * <p>
	 * For events that are {@link MidiNoteOffEvent MidiNoteOffEvents}, they will be removed 1/30th of a second early so
	 * that repeated notes on some instruments can be differentiated.
	 * <p>
	 * This method assumes that the events parameter is sorted by event time.
	 *
	 * @param events the events to pull from
	 * @param time   the current time, in seconds
	 * @return a list of events that occur now or before
	 */
	@NotNull
	public static <E extends MidiEvent> List<E> collectWithOffGap(@NotNull Iterable<E> events,
	                                                              @NotNull Midis2jam2 context,
	                                                              double time) {
		List<E> queue = new ArrayList<>();
		final var midi = context.getFile();
		
		for (var iterator = events.iterator(); iterator.hasNext(); ) {
			var next = iterator.next();
			/* If the event should occur now, or before now, remove it from the list and add it to the queue */
			if (midi.eventInSeconds(next) <= time
					|| (next instanceof MidiNoteOffEvent && midi.eventInSeconds(next) <= time - 0.033F)) {
				queue.add(next);
				iterator.remove();
			} else {
				/* Because we know the list is sorted (or assuming it is), we can get the fuck out if we see an event
				 * that is not supposed to occur yet. */
				break;
			}
			
		}
		return queue;
	}
}
