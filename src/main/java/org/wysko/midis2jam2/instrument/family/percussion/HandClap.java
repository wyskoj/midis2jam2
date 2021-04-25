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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static com.jme3.math.FastMath.PI;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The hand clap.
 */
public class HandClap extends NonDrumSetPercussion {
	
	/**
	 * Contains the left hand.
	 */
	private final Node leftHandNode = new Node();
	
	/**
	 * Contains the right hand.
	 */
	private final Node rightHandNode = new Node();
	
	/**
	 * Instantiates hand claps.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public HandClap(Midis2jam2 context,
	                List<MidiNoteOnEvent> hits) {
		super(context, hits);
		this.hits = hits;
		
		Spatial rightHand = context.loadModel("hand_left.obj", "hands.bmp");
		
		rightHand.setLocalTranslation(0, 0, -1.5f);
		leftHandNode.attachChild(rightHand);
		
		Spatial hand = context.loadModel("hand_right.obj", "hands.bmp");
		hand.setLocalTranslation(0, 0, -1.5f);
		hand.setLocalRotation(new Quaternion().fromAngles(0, rad(10), PI));
		rightHandNode.attachChild(hand);
		
		instrumentNode.setLocalTranslation(0, 42.3f, -48.4f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(90), rad(-70), 0));
		
		instrumentNode.attachChild(leftHandNode);
		instrumentNode.attachChild(rightHandNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus status = Stick.handleStick(context, leftHandNode, time, delta, hits,
				Stick.STRIKE_SPEED * 0.8, 40);
		leftHandNode.setCullHint(Dynamic);
		rightHandNode.setLocalRotation(new Quaternion().fromAngles(-status.getRotationAngle(), 0, 0));
	}
}
