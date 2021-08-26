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

package org.wysko.midis2jam2.instrument;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.NotePeriod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;

/**
 * A sustained instrument is any instrument that also depends on knowing the {@link MidiNoteOffEvent} for proper
 * animation. Examples include: saxophone, piano, guitar, telephone ring.
 */
public abstract class SustainedInstrument extends Instrument {
	
	/**
	 * This list shall not be updated and shall be used for visibility calculations.
	 */
	@Unmodifiable
	@NotNull
	protected final List<NotePeriod> unmodifiableNotePeriods;
	
	/**
	 * The list of current note periods. Will always be updating as the MIDI file progresses.
	 */
	@NotNull
	protected final List<NotePeriod> currentNotePeriods = new ArrayList<>();
	
	/**
	 * The list of note periods. This class expects that this variable will be truncated as the MIDI file progresses.
	 *
	 * @see NotePeriod
	 */
	@NotNull
	protected List<NotePeriod> notePeriods;
	
	/**
	 * Instantiates a new sustained instrument.
	 *
	 * @param context the context to the main class
	 */
	@SuppressWarnings("java:S1699")
	protected SustainedInstrument(@NotNull Midis2jam2 context,
	                              @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context);
		this.notePeriods = calculateNotePeriods(scrapeMidiNoteEvents(eventList));
		this.unmodifiableNotePeriods = List.copyOf(notePeriods);
	}
	
	/**
	 * Filters a list of MIDI channel specific events and returns only the {@link MidiNoteEvent}s.
	 *
	 * @param events the event list
	 * @return only the MidiNoteEvents
	 * @see MidiNoteEvent
	 */
	@NotNull
	@Contract(pure = true)
	public static List<MidiNoteEvent> scrapeMidiNoteEvents(@NotNull Collection<MidiChannelSpecificEvent> events) {
		return events.stream()
				.filter(MidiNoteEvent.class::isInstance)
				.map(MidiNoteEvent.class::cast)
				.collect(Collectors.toList());
	}
	
	/**
	 * Calculate the current visibility.
	 *
	 * @param time                    the time
	 * @param unmodifiableNotePeriods the unmodifiable note periods
	 * @return true if this instrument should be visible, false otherwise
	 */
	public static boolean calcVisibility(double time, @NotNull Iterable<NotePeriod> unmodifiableNotePeriods) {
		var show = false;
		for (NotePeriod notePeriod : unmodifiableNotePeriods) {
			// Within 1 second of a note on,
			// within 4 seconds of a note off,
			// or during a note, be visible
			if (notePeriod.isPlayingAt(time)
					|| Math.abs(time - notePeriod.getStartTime()) < START_BUFFER
					|| (Math.abs(time - notePeriod.getEndTime()) < END_BUFFER && time > notePeriod.getEndTime())) {
				show = true;
				break;
			}
		}
		return show;
	}
	
	/**
	 * Determines which note periods should have starting animations at the specified time. Removes the returned
	 * elements from {@link #notePeriods}. The method also removes elapsed note periods. All results are stored in
	 * {@link #currentNotePeriods}.
	 *
	 * @param time the current time
	 * @see #currentNotePeriods
	 */
	protected void calculateCurrentNotePeriods(double time) {
		while (!notePeriods.isEmpty() && notePeriods.get(0).getStartTime() <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
		currentNotePeriods.removeIf(notePeriod -> notePeriod.getEndTime() <= time);
	}
	
	@Override
	public void tick(double time, float delta) {
		calculateCurrentNotePeriods(time);
		setIdleVisibilityByPeriods(time);
		moveForMultiChannel(delta);
	}
	
	/**
	 * Determines whether this instrument should be visible at the time, and sets the visibility accordingly.
	 * <p>
	 * The instrument should be visible if:
	 * <ul>
	 *     <li>There is at least 1 second between now and the start of any note period,</li>
	 *     <li>There is at least 4 seconds between now and the end of any note period, or</li>
	 *     <li>Any note period is currently playing</li>
	 * </ul>
	 *  @param time the current time
	 */
	protected void setIdleVisibilityByPeriods(double time) {
		boolean b = calcVisibility(time, unmodifiableNotePeriods);
		setVisible(b);
		if (b) {
			instrumentNode.setCullHint(Dynamic);
		} else {
			instrumentNode.setCullHint(Always);
		}
	}
	
}
