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

package org.wysko.midis2jam2.instrument.family.reed.sax;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

/**
 * Shared code for Saxophones.
 */
public abstract class Saxophone extends MonophonicInstrument {
	
	/**
	 * Constructs a saxophone.
	 *
	 * @param context    context to midis2jam2
	 * @param eventList  the list of events for this instrument
	 * @param cloneClass the class of the clone
	 */
	protected Saxophone(@NotNull Midis2jam2 context,
	                    @NotNull List<MidiChannelSpecificEvent> eventList,
	                    @NotNull Class<? extends Clone> cloneClass,
	                    @NotNull PressedKeysFingeringManager fingeringManager) throws ReflectiveOperationException {
		
		super(context, eventList, cloneClass, fingeringManager);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 40 * indexForMoving(delta), 0);
	}
}
