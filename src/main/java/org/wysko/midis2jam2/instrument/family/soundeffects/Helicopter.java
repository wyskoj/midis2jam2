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

package org.wysko.midis2jam2.instrument.family.soundeffects;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.util.FastNoiseLite;
import org.wysko.midis2jam2.world.Axis;

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
		highestLevel.setLocalTranslation(50f * indexForMoving(), 0, 0);
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
		final FastNoiseLite noise = new FastNoiseLite((int) System.currentTimeMillis());
		
		final float rotXRand;
		
		final float rotYRand;
		
		final float rotZRand;
		
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
			
			
			var random = new Random();
			rotXRand = random.nextFloat();
			rotYRand = random.nextFloat();
			rotZRand = random.nextFloat();
		}
		
		@Override
		public void tick(double time, float delta) {
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
			offsetNode.setLocalTranslation(0, 0, -100f * indexForMoving());
		}
	}
}
