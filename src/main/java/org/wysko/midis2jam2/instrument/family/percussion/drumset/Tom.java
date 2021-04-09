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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.MAX_ANGLE;

/**
 * The toms.
 */
public class Tom extends StickDrum {
	
	/**
	 * Instantiates a new Tom.
	 *
	 * @param context the context
	 * @param hits    the hits
	 * @param pitch   the tom pitch
	 */
	public Tom(Midis2jam2 context, List<MidiNoteOnEvent> hits, TomPitch pitch) {
		super(context, hits);
		drum = context.loadModel("DrumSet_Tom.obj", "DrumShell.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		drum.setLocalScale(pitch.scale);
		recoilNode.attachChild(drum);
		recoilNode.attachChild(stickNode);
		highLevelNode.attachChild(recoilNode);
		highLevelNode.setLocalTranslation(pitch.location);
		highLevelNode.setLocalRotation(pitch.rotation);
		
		if (pitch == TomPitch.HIGH_FLOOR || pitch == TomPitch.LOW_FLOOR) {
			stickNode.setLocalRotation(new Quaternion().fromAngles(0, rad(80), 0));
			stickNode.setLocalTranslation(10, 0, 0);
		} else {
			stickNode.setLocalTranslation(0, 0, 10);
		}
		
		stick.setLocalRotation(new Quaternion().fromAngles(rad(MAX_ANGLE), 0, 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		drumRecoil(time, delta);
		handleStick(time, delta, hits);
	}
	
	/**
	 * The pitch of the tom.
	 */
	public enum TomPitch {
		
		/**
		 * The Low floor tom.
		 */
		LOW_FLOOR(
				new Vector3f(1.5f, 1.5f, 1.5f),
				new Vector3f(20, 20, -60),
				new Quaternion().fromAngles(rad(-2), rad(180), rad(-10))),
		
		/**
		 * The High floor tom.
		 */
		HIGH_FLOOR(new Vector3f(1.4f, 1.4f, 1.4f),
				new Vector3f(17, 21, -75),
				new Quaternion().fromAngles(rad(-5), rad(180), rad(-15))),
		
		/**
		 * The Low tom.
		 */
		LOW(new Vector3f(1.2f, 1.2f, 1.2f),
				new Vector3f(10f, 27 + 2, -82),
				new Quaternion().fromAngles(rad(60), rad(-30), 0)),
		
		/**
		 * The Low mid tom.
		 */
		LOW_MID(new Vector3f(1f, 1f, 1f),
				new Vector3f(0, 30 + 2, -85),
				new Quaternion().fromAngles(rad(60), 0, 0)),
		
		/**
		 * The High mid tom.
		 */
		HIGH_MID(new Vector3f(0.8f, 0.8f, 0.8f),
				new Vector3f(-9, 29 + 2, -82),
				new Quaternion().fromAngles(rad(60), rad(20), 0)),
		
		/**
		 * The High tom.
		 */
		HIGH(new Vector3f(0.6f, 0.6f, 0.6f),
				new Vector3f(-15, 27 + 2, -78),
				new Quaternion().fromAngles(rad(50), rad(40), 0));
		
		/**
		 * The Scale.
		 */
		final Vector3f scale;
		
		/**
		 * The Location.
		 */
		final Vector3f location;
		
		/**
		 * The Rotation.
		 */
		final Quaternion rotation;
		
		/**
		 * Instantiates a new Tom pitch.
		 *
		 * @param scale    the scale
		 * @param location the location
		 * @param rotation the rotation
		 */
		TomPitch(Vector3f scale, Vector3f location, Quaternion rotation) {
			this.scale = scale;
			this.location = location;
			this.rotation = rotation;
		}
	}
}
