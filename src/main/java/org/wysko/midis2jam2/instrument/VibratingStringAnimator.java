package org.wysko.midis2jam2.instrument;

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
