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

package org.wysko.midis2jam2.instrument.clone;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.instrument.family.pipe.HandedInstrument;
import org.wysko.midis2jam2.world.Axis;

import static com.jme3.scene.Spatial.CullHint.Always;
import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands;

/**
 * Some instruments visualize notes by showing hands in different playing positions. To do this, a separate 3D model for
 * each hand in each "shape" is created, then they are seamlessly swapped out during playback to give the illusion that
 * the hands are moving.
 * <p>
 * Wouldn't it be easier to do some .bvh? Maybe, but I'm sticking with the implementation from MIDIJam and just creating
 * a different file for each position.
 */
public abstract class HandedClone extends Clone {
	
	/**
	 * The Left hand node.
	 */
	protected final Node leftHandNode = new Node();
	
	/**
	 * The Right hand node.
	 */
	protected final Node rightHandNode = new Node();
	
	/**
	 * The Left hands.
	 */
	protected Spatial[] leftHands;
	
	/**
	 * The Right hands.
	 */
	protected Spatial[] rightHands;
	
	/**
	 * Instantiates a new handed clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 */
	protected HandedClone(HandedInstrument parent, float rotationFactor) {
		super(parent, rotationFactor, Axis.X);
		modelNode.attachChild(leftHandNode);
		modelNode.attachChild(rightHandNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		if (isPlaying()) {
			/* Set the hands */
			assert currentNotePeriod != null;
			assert parent.manager != null;
			
			var hands = (Hands) parent.manager.fingering(currentNotePeriod.getMidiNote());
			if (hands != null) {
				if (leftHands != null) {
					/* May be null because ocarina does not implement left hands */
					setHand(leftHands, hands.left);
				}
				setHand(rightHands, hands.right);
			}
		}
	}
	
	/**
	 * Sets the visibility of hands so that only the desired hand is visible.
	 *
	 * @param hands        the array of hands
	 * @param handPosition the index of the hand that should be visible
	 */
	private static void setHand(Spatial[] hands, int handPosition) {
		for (var i = 0; i < hands.length; i++) {
			if (i == handPosition) {
				hands[i].setCullHint(Dynamic);
			} else {
				hands[i].setCullHint(Always);
			}
		}
	}
}
