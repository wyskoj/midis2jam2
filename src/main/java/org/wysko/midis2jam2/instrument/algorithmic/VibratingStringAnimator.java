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

/**
 * Animates vibrating strings, as seen on the guitar, violin, etc.
 */
public class VibratingStringAnimator {
	
	/**
	 * Each frame of the animation.
	 */
	final Spatial[] stringFrames;
	
	/**
	 * The current frame to show.
	 */
	double frame;
	
	public VibratingStringAnimator(Spatial... frames) {
		stringFrames = frames;
	}
	
	/**
	 * Update animation.
	 *
	 * @param delta the amount of time since the last frame update
	 */
	public void tick(float delta) {
		
		final double inc = delta / 0.016666668f;
		this.frame += inc;
		
		for (int i = 0; i < 5; i++) {
			frame = frame % 5;
			if (i == Math.floor(frame)) {
				stringFrames[i].setCullHint(Spatial.CullHint.Dynamic);
			} else {
				stringFrames[i].setCullHint(Spatial.CullHint.Always);
			}
		}
	}
}
