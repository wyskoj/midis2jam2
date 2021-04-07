package org.wysko.midis2jam2.instrument.soundeffects;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.FastNoiseLite;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Axis;
import org.wysko.midis2jam2.instrument.Clone;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.Random;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Helicopter.
 */
public class Helicopter extends MonophonicInstrument {
	
	/**
	 * Instantiates a new Helicopter.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Helicopter(@NotNull Midis2jam2 context,
	                  @NotNull List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(context, eventList, HelicopterClone.class, null);
		
		groupOfPolyphony.setLocalTranslation(20, 40, -300);
	}
	
	@Override
	protected void moveForMultiChannel() {
		highestLevel.setLocalTranslation(50 * indexForMoving(), 0, 0);
	}
	
	/**
	 * A single helicopter.
	 */
	public class HelicopterClone extends Clone {
		
		/**
		 * The rotor which spins.
		 */
		private final Spatial rotor;
		
		/**
		 * The simplex noise generator.
		 */
		FastNoiseLite noise = new FastNoiseLite((int) System.currentTimeMillis());
		
		float rotXRand, rotYRand, rotZRand;
		
		/**
		 * The amount of height and random movement.
		 */
		float force = 0;
		
		public HelicopterClone() {
			super(Helicopter.this, 0, Axis.X);
			Spatial copter = context.loadModel("HelicopterBody.fbx", "Helicopter.png");
			rotor = context.loadModel("HelicopterRotor.fbx", "Helicopter.png");
			rotor.setLocalTranslation(40, 36, 0);
			animNode.attachChild(copter);
			animNode.attachChild(rotor);
			
			noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
			
			
			Random random = new Random();
			rotXRand = random.nextFloat();
			rotYRand = random.nextFloat();
			rotZRand = random.nextFloat();
		}
		
		@Override
		protected void tick(double time, float delta) {
			super.tick(time, delta);
			if (isPlaying()) {
				force += delta;
				force = Math.min(force, 1);
				
			} else {
				force -= delta;
				force = Math.max(force, 0);
			}
			rotor.rotate(new Quaternion().fromAngles(0, rad(1E5 * delta), 0));
			animNode.setLocalRotation(
					new Quaternion().fromAngles(
							force * 0.5f * rad(noise.GetNoise((float) (time * 100 + 4000), 0)),
							force * 0.5f * rad(noise.GetNoise((float) (time * 100 + 8000), 0)),
							force * 0.5f * rad(noise.GetNoise((float) (time * 100 + 12000), 0))
					)
			);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(5), rad(120), rad(11)));
			animNode.setLocalTranslation(0, force * noise.GetNoise((float) (time * 100), 0), 0);
			highestLevel.setLocalTranslation(0, -120 + 120 * ease(force), 0);
		}
		
		/**
		 * Sinusoidal easing.
		 *
		 * @param x in
		 * @return out
		 */
		private float ease(float x) {
			return (float) (-(cos(PI * x) - 1) / 2);
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalTranslation(0, 0, -100 * indexForMoving());
		}
	}
}
