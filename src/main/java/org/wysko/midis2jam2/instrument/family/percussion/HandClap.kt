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

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.instrument.family.percussive.Stick.StickStatus;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static com.jme3.math.FastMath.PI;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The hand clap consists of two hands that come together and recoil, just like a regular hand clap. To animate this, I
 * used {@link Stick#handleStick} to animate one hand, then copy and mirror the rotation and culling to the other hand.
 */
public class HandClap extends NonDrumSetPercussion {
	
	/**
	 * Contains the left hand.
	 */
	@NotNull
	private final Node leftHandNode = new Node();
	
	/**
	 * Contains the right hand.
	 */
	@NotNull
	private final Node rightHandNode = new Node();
	
	/**
	 * Instantiates hand claps.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public HandClap(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		this.hits = hits;
		
		/* Load the left hand */
		Spatial leftHand = context.loadModel("hand_left.obj", "hands.bmp");
		leftHand.setLocalTranslation(0, 0, -1.5F);
		leftHandNode.attachChild(leftHand);
		
		/* Load the right hand */
		Spatial rightHand = context.loadModel("hand_right.obj", "hands.bmp");
		rightHand.setLocalTranslation(0, 0, -1.5F);
		rightHand.setLocalRotation(new Quaternion().fromAngles(0, rad(10), PI));
		rightHandNode.attachChild(rightHand);
		
		/* Positioning */
		instrumentNode.setLocalTranslation(0, 42.3F, -48.4F);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(90), rad(-70), 0));
		
		/* Attach hands to instrument node */
		instrumentNode.attachChild(leftHandNode);
		instrumentNode.attachChild(rightHandNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Animate the left hand like you normally would for a stick */
		StickStatus status = Stick.handleStick(
				context, leftHandNode, time, delta, hits, Stick.STRIKE_SPEED * 0.8, 40, Axis.X
		);
		
		/* Override handleStick making the leftHandNode cull */
		leftHandNode.setCullHint(Dynamic);
		
		/* Copy the rotation and mirror it to the right hand */
		rightHandNode.setLocalRotation(new Quaternion().fromAngles(-status.getRotationAngle(), 0, 0));
	}
}
