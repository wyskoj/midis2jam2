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

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.world.Axis;

/**
 * Instruments that stretch when they play.
 */
public abstract class StretchyClone extends Clone {
	
	/**
	 * The stretch factor.
	 */
	protected final float stretchFactor;
	
	/**
	 * The axis on which to scale the bell on.
	 */
	protected final Axis scaleAxis;
	
	/**
	 * The bell of the instrument. This must be a node to account for variations of the bell (e.g., Muted Trumpet).
	 */
	protected final Node bell = new Node();
	
	/**
	 * The body of the instrument.
	 */
	protected Spatial body;
	
	/**
	 * Instantiates a new Stretchy clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 * @param stretchFactor  the stretch factor
	 * @param scaleAxis      the scale axis
	 * @param rotationAxis   the rotation axis
	 */
	protected StretchyClone(MonophonicInstrument parent, float rotationFactor, float stretchFactor,
	                        Axis scaleAxis, Axis rotationAxis) {
		super(parent, rotationFactor, rotationAxis);
		this.stretchFactor = stretchFactor;
		this.scaleAxis = scaleAxis;
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Stretch the bell of the instrument */
		
		if (currentNotePeriod != null) {
			float scale = (float) ((stretchFactor * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1);
			
			bell.setLocalScale(
					scaleAxis == Axis.X ? scale : 1,
					scaleAxis == Axis.Y ? scale : 1,
					scaleAxis == Axis.Z ? scale : 1
			);
			
		} else {
			bell.setLocalScale(1, 1, 1);
		}
	}
}
