package org.wysko.midis2jam2.instrument.percussion;

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
