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

package org.wysko.midis2jam2.instrument.clone;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.world.Axis;

/**
 * Instruments, such as the trumpet, french horn, tuba, etc., animate by transforming models of keys. The specific
 * transformation is handled by the implementing class, but the determining of which keys to press is handled here in
 * {@link #tick(double, float)}. It calls {@link #animateKeys(Integer[])} which is implemented in the subclass.
 */
public abstract class AnimatedKeyCloneByIntegers extends StretchyClone {
	
	/**
	 * The keys of the instrument.
	 */
	@NotNull
	protected final Spatial[] keys;
	
	/**
	 * Instantiates a new Animated key clone by integers.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 * @param stretchFactor  the stretch factor
	 * @param numberOfKeys   the number of keys
	 * @param stretchAxis    the stretch axis
	 * @param rotationAxis   the rotation axis
	 */
	protected AnimatedKeyCloneByIntegers(@NotNull MonophonicInstrument parent,
	                                     float rotationFactor,
	                                     float stretchFactor,
	                                     int numberOfKeys,
	                                     Axis stretchAxis,
	                                     Axis rotationAxis) {
		
		super(parent, rotationFactor, stretchFactor, stretchAxis, rotationAxis);
		this.keys = new Spatial[numberOfKeys];
	}
	
	/**
	 * Animates a key.
	 *
	 * @param pressed true if this key is pressed, false otherwise
	 */
	protected abstract void animateKeys(@NotNull Integer[] pressed);
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		if (currentNotePeriod != null) {
			assert parent.manager != null;
			var ints = (Integer[]) parent.manager.fingering(currentNotePeriod.getMidiNote());
			if (ints != null) {
				animateKeys(ints);
			}
		}
	}
}
