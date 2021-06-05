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

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.midi.NotePeriod;
import org.wysko.midis2jam2.world.Axis;

/**
 * Standard implementation of {@link BellStretcher}.
 */
public class StandardBellStretcher implements BellStretcher {
	
	/**
	 * The maximum stretch amount applied to the bell.
	 */
	private final float stretchiness;
	
	/**
	 * The axis on which to stretch the bell.
	 */
	private final Axis stretchAxis;
	
	/**
	 * The bell to stretch.
	 */
	private final Spatial bell;
	
	/**
	 * Instantiates a new standard bell stretcher.
	 *
	 * @param stretchiness the maximum stretch amount applied to the bell
	 * @param stretchAxis  the axis on which to stretch the bell
	 * @param bell         the bell to stretch
	 */
	public StandardBellStretcher(float stretchiness, Axis stretchAxis, Spatial bell) {
		this.stretchiness = stretchiness;
		this.stretchAxis = stretchAxis;
		this.bell = bell;
	}
	
	@Override
	public void tick(double stretchAmount) {
		scaleBell((float) ((stretchiness - 1) * stretchAmount + 1));
	}
	
	/**
	 * Sets the scale of the bell, appropriately and automatically scaling on the correct axis.
	 *
	 * @param scale the scale of the bell
	 */
	private void scaleBell(float scale) {
		bell.setLocalScale(
				stretchAxis == Axis.X ? scale : 1,
				stretchAxis == Axis.Y ? scale : 1,
				stretchAxis == Axis.Z ? scale : 1
		);
	}
	
	public void tick(NotePeriod currentNotePeriod, double time) {
		if (currentNotePeriod != null) {
			float scale = (float) ((stretchiness * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1);
			scaleBell(scale);
		} else {
			scaleBell(1.0f);
		}
	}
}
