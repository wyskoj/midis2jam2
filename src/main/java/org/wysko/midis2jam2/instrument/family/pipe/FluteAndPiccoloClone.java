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

package org.wysko.midis2jam2.instrument.family.pipe;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.pipe.Flute.FluteClone;
import org.wysko.midis2jam2.instrument.family.pipe.Piccolo.PiccoloClone;
import org.wysko.midis2jam2.particle.SteamPuffer;

/**
 * Contains shared code between the {@link FluteClone} and {@link PiccoloClone}
 */
public class FluteAndPiccoloClone extends PuffingClone {
	
	/**
	 * Instantiates a new flute/piccolo clone.
	 *
	 * @param parent    the parent
	 * @param puffType  the puff type
	 * @param puffScale the puff scale
	 */
	public FluteAndPiccoloClone(HandedInstrument parent,
	                            SteamPuffer.SteamPuffType puffType,
	                            float puffScale) {
		
		super(parent, 0, puffType, puffScale);
		
	}
	
	/**
	 * Loads the left and right hands for flute and piccolo.
	 */
	@Override
	protected void loadHands() {
		leftHands = new Spatial[13];
		for (int i = 0; i < 13; i++) {
			leftHands[i] = parent.context.loadModel(String.format("Flute_LeftHand%02d.obj", i), "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			leftHandNode.attachChild(leftHands[i]);
			if (i != 0) {
				leftHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		rightHands = new Spatial[12];
		for (int i = 0; i < 12; i++) {
			rightHands[i] = parent.context.loadModel(String.format("Flute_RightHand%02d.obj", i), "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			rightHandNode.attachChild(rightHands[i]);
			if (i != 0) {
				rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		
	}
	
	@Override
	protected void moveForPolyphony() {
		offsetNode.setLocalTranslation(5 * indexForMoving(), 0, 5 * -indexForMoving());
	}
}
