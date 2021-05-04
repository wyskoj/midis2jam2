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

import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.FingeringManager;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * A monophonic instrument is any instrument that can only play one note at a time (e.g., saxophones, clarinets,
 * ocarinas, etc.). Because this limitation is lifted in MIDI files, midis2jam2 needs to visualize polyphony by spawning
 * "clones" of an instrument. These clones will only appear when necessary.
 * <p>
 * It happens to be that every monophonic instrument is also a {@link SustainedInstrument}.
 *
 * @see Clone
 */
public abstract class MonophonicInstrument extends SustainedInstrument {
	
	/**
	 * Node contains all clones.
	 */
	@NotNull
	public final Node groupOfPolyphony = new Node();
	
	/**
	 * The list of clones this monophonic instrument needs to effectively display all notes.
	 */
	@NotNull
	public List<Clone> clones;
	
	@Nullable
	public FingeringManager<?> manager;
	
	/**
	 * Constructs a monophonic instrument.
	 *
	 * @param context   context to midis2jam2
	 * @param eventList the event list
	 */
	protected MonophonicInstrument(@NotNull Midis2jam2 context,
	                               @NotNull List<MidiChannelSpecificEvent> eventList,
	                               @NotNull Class<? extends Clone> cloneClass,
	                               @Nullable FingeringManager<?> manager) throws ReflectiveOperationException {
		super(context, eventList);
		this.clones = calculateClones(this, cloneClass);
		
		for (Clone clone : clones) {
			groupOfPolyphony.attachChild(clone.offsetNode);
		}
		
		this.instrumentNode.attachChild(groupOfPolyphony);
		this.manager = manager;
	}
	
	/**
	 * Since MIDI channels that play monophonic instruments can play with polyphony, we need to calculate the number of
	 * "clones" needed to visualize this and determine which note events shall be assigned to which clones, using the
	 * least number of clones.
	 *
	 * @param instrument the monophonic instrument that is handling the clones
	 * @param cloneClass the class of the {@link Clone} to instantiate
	 * @throws ReflectiveOperationException usually is thrown if an error occurs in the clone constructor
	 */
	protected List<Clone> calculateClones(@NotNull MonophonicInstrument instrument,
	                                      @NotNull Class<? extends Clone> cloneClass) throws ReflectiveOperationException {
		List<Clone> calcClones = new ArrayList<>();
		Constructor<?> constructor = cloneClass.getDeclaredConstructor(instrument.getClass());
		calcClones.add((Clone) constructor.newInstance(instrument));
		for (var i = 0; i < notePeriods.size(); i++) {
			for (var j = 0; j < notePeriods.size(); j++) {
				if (j == i && i != notePeriods.size() - 1) continue;
				var comp1 = notePeriods.get(i);
				var comp2 = notePeriods.get(j);
				if (comp1.startTick() > comp2.endTick()) continue;
				if (comp1.endTick() < comp2.startTick()) {
					calcClones.get(0).notePeriods.add(comp1);
					break;
				}
				if (comp1.startTick() >= comp2.startTick() && comp1.startTick() <= comp2.endTick()) { // Overlapping note
					var added = false;
					for (Clone clone : calcClones) {
						if (!clone.isPlaying(comp1.startTick() + context.getFile().getDivision() / 4)) {
							clone.notePeriods.add(comp1);
							added = true;
							break;
						}
					}
					if (!added) {
						Clone e = (Clone) constructor.newInstance(instrument);
						e.notePeriods.add(comp1);
						calcClones.add(e);
					}
				} else {
					calcClones.get(0).notePeriods.add(comp1);
				}
				break;
			}
		}
		return calcClones;
	}
	
	/**
	 * Updates clones, performing the {@link Clone#tick(double, float)} method and calculating clone offsets.
	 *
	 * @param time  the current time, in seconds
	 * @param delta the amount of time since the last frame
	 */
	protected void updateClones(double time, float delta) {
		for (Clone clone : clones) {
			clone.tick(time, delta);
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		updateClones(time, delta);
	}
}
