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

package org.wysko.midis2jam2.instrument.family.piano;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Any key on a keyed instrument.
 *
 * @see KeyedInstrument
 */
public class Key {
	
	/**
	 * The uppermost node of this key.
	 */
	protected final Node keyNode = new Node();
	
	/**
	 * Contains geometry for the "up" key.
	 */
	protected final Node upNode = new Node();
	
	/**
	 * Contains geometry for the "down" key.
	 */
	protected final Node downNode = new Node();
	
	/**
	 * Is this key being pressed?
	 */
	protected boolean beingPressed;
	
	/**
	 * Instantiates a new Key.
	 *
	 */
	protected Key() {
	}
	
	public boolean isBeingPressed() {
		return beingPressed;
	}
	
	public void setBeingPressed(boolean beingPressed) {
		this.beingPressed = beingPressed;
	}
	
	/**
	 * Animates the motion of the key.
	 *
	 * @param delta the amount of time since the last frame update
	 */
	public void tick(float delta) {
		if (beingPressed) {
			keyNode.setLocalRotation(new Quaternion().fromAngles(0.1F, 0, 0));
			downNode.setCullHint(Spatial.CullHint.Dynamic);
			upNode.setCullHint(Spatial.CullHint.Always);
		} else {
			var angles = new float[3];
			keyNode.getLocalRotation().toAngles(angles);
			if (angles[0] > 0.0001) {
				keyNode.setLocalRotation(new Quaternion(new float[]
						{
								Math.max(angles[0] - (0.02F * delta * 50), 0), 0, 0
						}
				));
			} else {
				keyNode.setLocalRotation(new Quaternion(new float[]{0, 0, 0}));
				
				downNode.setCullHint(Spatial.CullHint.Always);
				upNode.setCullHint(Spatial.CullHint.Dynamic);
			}
		}
	}
	
	public Node getKeyNode() {
		return keyNode;
	}
}
