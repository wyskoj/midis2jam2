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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A DecayedInstrument is any instrument that only depends on {@link MidiNoteOnEvent}s to function. The note off
 * event is discarded.
 */
public abstract class DecayedInstrument extends Instrument {
	
	/**
	 * List of events this instrument should play.
	 */
	@NotNull
	protected final List<MidiNoteOnEvent> hits;
	
	/**
	 * The list of unmodifiable hits.
	 */
	@NotNull
	@Unmodifiable
	protected final List<MidiNoteOnEvent> finalHits;
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	protected DecayedInstrument(@NotNull Midis2jam2 context,
	                            @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context);
		this.hits = eventList.stream()
				.filter(e -> e instanceof MidiNoteOnEvent)
				.map(e -> ((MidiNoteOnEvent) e))
				.collect(Collectors.toList());
		finalHits = Collections.unmodifiableList(new ArrayList<>(hits));
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByStrikes(finalHits, time, instrumentNode);
		moveForMultiChannel();
	}
}
