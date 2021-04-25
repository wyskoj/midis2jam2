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

import static org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager.Hands;

/**
 * Instruments that have hands. Includes piccolo, flute, recorder, ocarina.
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
	 * Instantiates a new clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 */
	protected HandedClone(HandedInstrument parent, float rotationFactor) {
		super(parent, rotationFactor, Axis.X);
		modelNode.attachChild(leftHandNode);
		modelNode.attachChild(rightHandNode);
	}
	
	/**
	 * Loads the hands appropriate to this instrument.
	 */
	protected abstract void loadHands();
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		if (isPlaying()) {
			/* Set the hands */
			assert currentNotePeriod != null;
			final int midiNote = currentNotePeriod.midiNote;
			assert parent.manager != null;
			var hands = (Hands) parent.manager.fingering(midiNote);
			if (hands != null) {
				// Set the left hands
				if (leftHands != null) {
					/* May be null because ocarina does not implement left hands */
					for (var i = 0; i < leftHands.length; i++) {
						if (i == hands.left) {
							leftHands[i].setCullHint(Spatial.CullHint.Dynamic);
						} else {
							leftHands[i].setCullHint(Spatial.CullHint.Always);
						}
					}
				}
				// Set the right hands
				for (var i = 0; i < rightHands.length; i++) {
					if (i == hands.right) {
						rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
					} else {
						rightHands[i].setCullHint(Spatial.CullHint.Always);
					}
				}
			}
		}
	}
}
