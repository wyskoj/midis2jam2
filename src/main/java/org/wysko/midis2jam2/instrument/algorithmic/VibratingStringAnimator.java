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
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;

/**
 * Animates vibrating strings, as seen on the guitar, violin, etc.
 */
public class VibratingStringAnimator {
	
	/**
	 * The number of frames that are used for animation.
	 */
	private static final int FRAME_COUNT = 5;
	
	/**
	 * Each frame of the animation.
	 */
	@Unmodifiable
	private final List<Spatial> stringFrames;
	
	/**
	 * The current frame to show.
	 */
	private double frame;
	
	public VibratingStringAnimator(Spatial... frames) {
		stringFrames = List.of(frames);
	}
	
	/**
	 * Update animation.
	 *
	 * @param delta the amount of time since the last frame update
	 */
	public void tick(float delta) {
		
		final double inc = delta * 60;
		this.frame += inc;
		
		for (var i = 0; i < FRAME_COUNT; i++) {
			frame = frame % FRAME_COUNT;
			if (i == (int) Math.floor(frame)) {
				stringFrames.get(i).setCullHint(Dynamic);
			} else {
				stringFrames.get(i).setCullHint(Always);
			}
		}
	}
}
