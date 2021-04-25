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

package org.wysko.midis2jam2.instrument.family.percussion.drumset;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Cymbals. Excludes the hi hat.
 */
public class Cymbal extends SingleStickInstrument {
	
	/**
	 * The Cymbal node.
	 */
	final Node cymbalNode = new Node();
	
	/**
	 * The Animator.
	 */
	protected CymbalAnimator animator;
	
	/**
	 * Instantiates a new Cymbal.
	 *
	 * @param context the context
	 * @param hits    the hits
	 * @param type    the type of cymbal
	 */
	public Cymbal(Midis2jam2 context,
	              List<MidiNoteOnEvent> hits, CymbalType type) {
		super(context, hits);
		
		final Spatial cymbal = context.loadModel(type == CymbalType.CHINA ? "DrumSet_ChinaCymbal.obj" :
						"DrumSet_Cymbal.obj",
				"CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		cymbalNode.attachChild(cymbal);
		cymbalNode.setLocalScale(type.size);
		highLevelNode.setLocalTranslation(type.location);
		highLevelNode.setLocalRotation(type.rotation);
		highLevelNode.attachChild(cymbalNode);
		stick.setLocalTranslation(0, 0, 0);
		stick.setLocalTranslation(0, 0, -2.6f);
		stick.setLocalRotation(new Quaternion().fromAngles(rad(-20), 0, 0));
		stickNode.setLocalTranslation(0, 2, 18);
		this.animator = new CymbalAnimator(type.amplitude, type.wobbleSpeed, type.dampening);
	}
	
	
	@Override
	public void tick(double time, float delta) {
		handleCymbalStrikes(time, delta);
		
		Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
	}
	
	/**
	 * Handle cymbal strikes.
	 *
	 * @param time  the time
	 * @param delta the amount of time since the last frame update
	 */
	void handleCymbalStrikes(double time, float delta) {
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.getFile().eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		if (recoil != null) {
			animator.strike();
		}
		cymbalNode.setLocalRotation(new Quaternion().fromAngles(animator.rotationAmount(), 0, 0));
		animator.tick(delta);
	}
	
	/**
	 * The type of cymbal.
	 */
	public enum CymbalType {
		
		/**
		 * The Crash 1 cymbal.
		 */
		CRASH_1(new Vector3f(-18, 48, -90), new Quaternion().fromAngles(rad(20), rad(45), 0), 2.0f, 2.5, 4.5, 1.5),
		
		/**
		 * The Crash 2 cymbal.
		 */
		CRASH_2(new Vector3f(13, 48, -90), new Quaternion().fromAngles(rad(20), rad(-45), 0), 1.5f, 2.5, 5, 1.5),
		
		/**
		 * The Splash cymbal.
		 */
		SPLASH(new Vector3f(-2, 48, -90), new Quaternion().fromAngles(rad(20), 0, 0), 1.0f, 2, 5, 1.5),
		
		/**
		 * The Ride 1 cymbal.
		 */
		RIDE_1(new Vector3f(22, 43, -77.8f), new Quaternion().fromAngles(rad(17), rad(291), rad(-9.45)), 2f, 0.5, 3, 1.5),
		
		/**
		 * The Ride 2 cymbal.
		 */
		RIDE_2(new Vector3f(-23, 40, -78.8f), new Quaternion().fromAngles(rad(20), rad(37.9), rad(-3.49)), 2f, 0.5, 3, 1.5),
		
		/**
		 * The China cymbal.
		 */
		CHINA(new Vector3f(32.7f, 34.4f, -68.4f), new Quaternion().fromAngles(rad(18), rad(-89.2), rad(-10)), 2.0f, 2, 5, 1.5);
		
		/**
		 * The size of ths cymbal (the scale).
		 */
		final float size;
		
		/**
		 * The Location.
		 */
		final Vector3f location;
		
		/**
		 * The Rotation.
		 */
		final Quaternion rotation;
		
		/**
		 * The amplitude of the cymbal when struck.
		 */
		final double amplitude;
		
		/**
		 * The wobble speed.
		 */
		final double wobbleSpeed;
		
		/**
		 * The dampening.
		 */
		final double dampening;
		
		CymbalType(Vector3f location, Quaternion rotation, float size, double amplitude, double wobbleSpeed,
		           double dampening) {
			this.location = location;
			this.rotation = rotation;
			this.size = size;
			this.amplitude = amplitude;
			this.wobbleSpeed = wobbleSpeed;
			this.dampening = dampening;
		}
	}
}
