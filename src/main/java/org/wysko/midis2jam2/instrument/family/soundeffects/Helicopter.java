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

import com.auburn.fastnoiselite.FastNoiseLite;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.NotePeriod;
import org.wysko.midis2jam2.world.ShadowController;

import java.util.List;
import java.util.Random;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The helicopter has several animation components.
 * <p>
 * The first is the rotor that spins on each frame. The rotor has 12 different layers of lights that can be turned on
 * and off, representing the 12 notes in an octave.
 * <p>
 * The second is the random wobble when it is flying. Uses {@link FastNoiseLite} to get some perlin-like noise for
 * smooth wobbling.
 * <p>
 * Finally, the helicopter moves down when not playing and moves up when playing. This motion is eased with a sinusoidal
 * function.
 */
public class Helicopter extends SustainedInstrument {
	
	/**
	 * The simplex noise generator.
	 */
	@NotNull
	private final FastNoiseLite noise = new FastNoiseLite((int) System.currentTimeMillis());
	
	/**
	 * "Seed" for X-value generation.
	 */
	private final float rotXRand;
	
	/**
	 * "Seed" for Y-value generation.
	 */
	private final float rotYRand;
	
	/**
	 * "Seed" for Z-value generation.
	 */
	private final float rotZRand;
	
	/**
	 * Contains all geometry for animation.
	 */
	@NotNull
	private final Node animNode = new Node();
	
	/**
	 * The rotor which spins.
	 */
	@NotNull
	private final Node rotor = new Node();
	
	@NotNull
	private final Spatial[] lights = new Spatial[12];
	
	/**
	 * The amount of height and random movement.
	 */
	private float force;
	
	/**
	 * Instantiates a new Helicopter.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Helicopter(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		/* Load helicopter */
		Spatial copter = context.loadModel("HelicopterBody.fbx", "Helicopter.png");
		rotor.attachChild(ShadowController.shadow(context, "Assets/HelicopterRotorPlane.fbx", "Assets/HelicopterRotor" +
				".png"));
		
		/* Load lights */
		for (var i = 1; i <= 12; i++) {
			var light = ShadowController.shadow(context, "Assets/HelicopterRotorPlane.fbx",
					"Assets/HelicopterLights%d.png".formatted(i));
			this.lights[i - 1] = light;
			rotor.attachChild(light);
			light.setCullHint(Spatial.CullHint.Always);
		}
		
		rotor.setLocalTranslation(40, 36, 0);
		animNode.attachChild(copter);
		animNode.attachChild(rotor);
		
		/* Load rotor cap */
		var cap = context.loadModel("HelicopterRotorCap.fbx", "Helicopter.png");
		cap.setLocalTranslation(0, 0, 0.5F);
		animNode.attachChild(cap);
		
		/* Initialize RNG */
		noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
		var random = new Random();
		rotXRand = random.nextFloat();
		rotYRand = random.nextFloat();
		rotZRand = random.nextFloat();
		
		instrumentNode.attachChild(animNode);
	}
	
	/**
	 * easeInOutSine, ripped from <a href="https://easings.net/#easeInOutSine">easings.net</a>.
	 *
	 * @param x in
	 * @return out
	 */
	private static float easeInOutSine(float x) {
		return (float) (-(cos(PI * x) - 1) / 2);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Turn all lights off */
		for (Spatial light : lights) {
			light.setCullHint(Spatial.CullHint.Always);
		}
		
		/* Turn on active lights */
		for (NotePeriod notePeriod : currentNotePeriods) {
			lights[11 - ((notePeriod.midiNote + 3) % 12)].setCullHint(Spatial.CullHint.Dynamic);
		}
		
		/* If playing a note, increase the force, but cap it at 1. */
		if (!currentNotePeriods.isEmpty()) {
			force += delta;
			force = Math.min(force, 1);
		} else {
			/* Otherwise, decrease force but cup at 0. */
			force -= delta;
			force = Math.max(force, 0);
		}
		
		/* Vroom */
		rotor.rotate(new Quaternion().fromAngles(0, rad(3141 * delta), 0));
		
		/* Slight wobble */
		animNode.setLocalRotation(
				new Quaternion().fromAngles(
						force * 0.5F * rad(noise.GetNoise((float) (time * 100 + 4000 * rotXRand), 0)),
						force * 0.5F * rad(noise.GetNoise((float) (time * 100 + 4000 * rotYRand), 0)),
						force * 0.5F * rad(noise.GetNoise((float) (time * 100 + 4000 * rotZRand), 0))
				)
		);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(5), rad(120), rad(11)));
		animNode.setLocalTranslation(0, force * noise.GetNoise((float) (time * 100), 0), 0);
		highestLevel.setLocalTranslation(0, -120 + 120 * easeInOutSine(force), 0);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(20 + 50 * indexForMoving(delta), 40, -300);
	}
}
