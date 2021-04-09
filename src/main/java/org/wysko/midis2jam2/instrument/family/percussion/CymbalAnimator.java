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

package org.wysko.midis2jam2.instrument.family.percussion;

import com.jme3.math.FastMath;

/**
 * Animates the wobble on cymbals.
 */
public class CymbalAnimator {
	
	/**
	 * The amplitude, or maximum angle of wobble.
	 */
	private final double amplitude;
	
	/**
	 * How fast the cymbal wobbles after being struck.
	 */
	private final double wobbleSpeed;
	
	/**
	 * The dampening, or how fast the cymbal returns to an idle state.
	 */
	private final double dampening;
	
	/**
	 * The current time.
	 */
	private double animTime = -1;
	
	/**
	 * Constructs a CymbalAnimator.
	 *
	 * @param amplitude   the amplitude
	 * @param wobbleSpeed the wobble speed
	 * @param dampening   the dampening
	 */
	public CymbalAnimator(double amplitude, double wobbleSpeed, double dampening) {
		this.amplitude = amplitude;
		this.wobbleSpeed = wobbleSpeed;
		this.dampening = dampening;
	}
	
	/**
	 * <a href="https://www.desmos.com/calculator/vvbwlit9he">link</a>
	 *
	 * @return the amount to rotate the cymbal, due to wobble
	 */
	public float rotationAmount() {
		if (animTime >= 0) {
			if (animTime < 4.5)
				return (float) (amplitude * (Math.cos(animTime * wobbleSpeed * FastMath.PI) / (3 + Math.pow(animTime,
						3) * wobbleSpeed * dampening * FastMath.PI)));
			else
				return 0;
		}
		return 0;
	}
	
	/**
	 * Call this method to indicate that the cymbal has just been struck.
	 */
	public void strike() {
		animTime = 0;
	}
	
	/**
	 * Updates the internal clock for proper animation.
	 *
	 * @param delta the amount of time since the last frame
	 */
	public void tick(float delta) {
		if (animTime != -1)
			this.animTime += delta;
	}
}
