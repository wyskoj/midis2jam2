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
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * Most cymbals are represented with this class, excluding the {@link HiHat}.
 */
public class Cymbal extends SingleStickInstrument {
	
	/**
	 * The Cymbal node.
	 */
	final Node cymbalNode = new Node();
	private final CymbalType type;
	
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
	public Cymbal(Midis2jam2 context, List<MidiNoteOnEvent> hits, CymbalType type) {
		super(context, hits);
		this.type = type;
		/* Load cymbal model */
		final Spatial cymbal;
		
		if (this.type == CymbalType.CHINA)
			cymbal = context.loadModel("DrumSet_ChinaCymbal.obj", "CymbalSkinSphereMap.bmp", Midis2jam2.MatType.REFLECTIVE, 0.7F);
		else
			cymbal = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp", Midis2jam2.MatType.REFLECTIVE, 0.7F);
		
		cymbalNode.attachChild(cymbal);
		cymbalNode.setLocalScale(type.size);
		
		highLevelNode.setLocalTranslation(type.location);
		highLevelNode.setLocalRotation(type.rotation);
		highLevelNode.attachChild(cymbalNode);
		
		stick.setLocalTranslation(0, 0, 0);
		stick.setLocalTranslation(0, 0, -2.6F);
		stick.setLocalRotation(new Quaternion().fromAngles(rad(-20), 0, 0));
		stickNode.setLocalTranslation(0, 2, 18);
		
		this.animator = new CymbalAnimator(type.amplitude, type.wobbleSpeed, type.dampening);
	}
	
	
	@Override
	public void tick(double time, float delta) {
		var stickStatus = handleStick(time, delta, hits);
		handleCymbalStrikes(time, delta, stickStatus.justStruck());
	}
	
	/**
	 * Handle cymbal strikes.
	 *
	 * @param time  the time
	 * @param delta the amount of time since the last frame update
	 */
	void handleCymbalStrikes(double time, float delta, boolean struck) {
		if (struck) {
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
		CRASH_1(new Vector3f(-18, 48, -90), new Quaternion().fromAngles(rad(20), rad(45), 0), 2.0F, 2.5, 4.5),
		
		/**
		 * The Crash 2 cymbal.
		 */
		CRASH_2(new Vector3f(13, 48, -90), new Quaternion().fromAngles(rad(20), rad(-45), 0), 1.5F, 2.5, 5),
		
		/**
		 * The Splash cymbal.
		 */
		SPLASH(new Vector3f(-2, 48, -90), new Quaternion().fromAngles(rad(20), 0, 0), 1.0F, 2, 5),
		
		/**
		 * The Ride 1 cymbal.
		 */
		RIDE_1(new Vector3f(22, 43, -77.8F), new Quaternion().fromAngles(rad(17), rad(291), rad(-9.45)), 2, 0.5, 3),
		
		/**
		 * The Ride 2 cymbal.
		 */
		RIDE_2(new Vector3f(-23, 40, -78.8F), new Quaternion().fromAngles(rad(20), rad(37.9), rad(-3.49)), 2, 0.5, 3),
		
		/**
		 * The China cymbal.
		 */
		CHINA(new Vector3f(32.7F, 34.4F, -68.4F), new Quaternion().fromAngles(rad(18), rad(-89.2), rad(-10)), 2.0F, 2, 5);
		
		/**
		 * The size of ths cymbal (the scale).
		 */
		private final float size;
		
		/**
		 * The Location.
		 */
		private final Vector3f location;
		
		/**
		 * The Rotation.
		 */
		private final Quaternion rotation;
		
		/**
		 * The amplitude of the cymbal when struck.
		 */
		private final double amplitude;
		
		/**
		 * The wobble speed.
		 */
		private final double wobbleSpeed;
		
		/**
		 * The dampening.
		 */
		private final double dampening;
		
		CymbalType(Vector3f location, Quaternion rotation, float size, double amplitude, double wobbleSpeed) {
			this.location = location;
			this.rotation = rotation;
			this.size = size;
			this.amplitude = amplitude;
			this.wobbleSpeed = wobbleSpeed;
			this.dampening = 1.5;
		}
		
		public float getSize() {
			return size;
		}
		
		public Vector3f getLocation() {
			return location;
		}
		
		public Quaternion getRotation() {
			return rotation;
		}
		
		public double getAmplitude() {
			return amplitude;
		}
		
		public double getWobbleSpeed() {
			return wobbleSpeed;
		}
		
		public double getDampening() {
			return dampening;
		}
	}
}
