package org.wysko.midis2jam2.particle;

public interface ParticleGenerator {
	void tick(double time, float delta, boolean active);
	
	interface Particle {
		boolean tick(float delta);
	}
}
