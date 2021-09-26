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

package org.wysko.midis2jam2.instrument.family.pipe;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

/**
 * Any instrument that animates using hands.
 */
public abstract class HandedInstrument extends MonophonicInstrument {
	
	/**
	 * Instantiates a new Handed instrument.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 * @param clazz     the class of the clone
	 * @param manager   the fingering manager
	 * @throws ReflectiveOperationException if clone constructor errors
	 */
	protected HandedInstrument(@NotNull Midis2jam2 context,
	                           @NotNull List<MidiChannelSpecificEvent> eventList,
	                           @NotNull Class<? extends Clone> clazz,
	                           @NotNull HandPositionFingeringManager manager) throws ReflectiveOperationException {
		super(context, eventList, clazz, manager);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 10f * indexForMoving(delta), 0);
	}
}
