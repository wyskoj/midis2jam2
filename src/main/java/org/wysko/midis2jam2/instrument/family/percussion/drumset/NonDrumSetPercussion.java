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

package org.wysko.midis2jam2.instrument.family.percussion.drumset;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

/**
 * Any percussion instrument that is not attached to the drum set and should therefore only appear when playing.
 */
public class NonDrumSetPercussion extends PercussionInstrument {
	
	/**
	 * The unmodifiable list of hits.
	 */
	protected final @Unmodifiable List<MidiNoteOnEvent> finalHits;
	
	/**
	 * Instantiates a new non drum set percussion.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected NonDrumSetPercussion(Midis2jam2 context,
	                               List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		this.finalHits = List.copyOf(hits);
	}
	
	@Override
	public void tick(double time, float delta) {
		instrumentNode.setCullHint(calculateVisibility(time) ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
	}
	
	/**
	 * Calculates whether this instrument should be visible.
	 *
	 * @param time the current time
	 * @return true if this instrument should be visible, false otherwise
	 */
	public boolean calculateVisibility(double time) {
		for (MidiNoteOnEvent hit : finalHits) {
			double leftMarginTime = context.getFile().midiTickInSeconds(hit.getTime() - (context.getFile().getDivision()));
			double rightMarginTime = context.getFile().midiTickInSeconds(hit.getTime() + (context.getFile().getDivision() / 2));
			if (time >= leftMarginTime && time <= rightMarginTime) {
				return true;
			}
		}
		return false;
	}
}
