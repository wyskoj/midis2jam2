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

package org.wysko.midis2jam2.instrument.family.percussive;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The Steel drums.
 */
public class SteelDrums extends OneDrumOctave {
	
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public SteelDrums(@NotNull Midis2jam2 context,
	                  @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		Spatial drum = context.loadModel("SteelDrum.obj", "ShinySilver.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
		
		var adjustments = new Node[12];
		
		for (var i = 0; i < 12; i++) {
			adjustments[i] = new Node();
			malletNodes[i] = new Node();
			
			adjustments[i].attachChild(malletNodes[i]);
			
			Spatial mallet = context.loadModel("SteelDrumMallet.obj", "StickSkin.bmp");
			
			malletNodes[i].attachChild(mallet);
			
			instrumentNode.attachChild(adjustments[i]);
		}
		
		adjustments[0].setLocalTranslation(4.31f, 4.95f, 16.29f);
		adjustments[0].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(15), 0));
		
		adjustments[1].setLocalTranslation(7.47f, 4.95f, 14.66f);
		adjustments[1].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(10), 0));
		
		adjustments[2].setLocalTranslation(8.57f, 4.95f, 11.25f);
		adjustments[2].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(5), 0));
		
		adjustments[3].setLocalTranslation(7.06f, 4.95f, 6.90f);
		adjustments[3].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(-5), 0));
		
		adjustments[4].setLocalTranslation(3.57f, 4.95f, 3.08f);
		adjustments[4].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(-10), 0));
		
		adjustments[5].setLocalTranslation(-0.32f, 4.95f, 0.89f);
		adjustments[5].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(-15), 0));
		
		// Left side //
		
		adjustments[6].setLocalTranslation(0.32f, 4.95f, 0.89f);
		adjustments[6].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(15), 0));
		
		adjustments[7].setLocalTranslation(-3.56f, 4.95f, 3f);
		adjustments[7].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(10), 0));
		
		adjustments[8].setLocalTranslation(-6.84f, 4.95f, 7f);
		adjustments[8].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(5), 0));
		
		adjustments[9].setLocalTranslation(-8.5f, 4.95f, 11.02f);
		adjustments[9].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(-5), 0));
		
		adjustments[10].setLocalTranslation(-7.15f, 4.95f, 14.32f);
		adjustments[10].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(-10), 0));
		
		adjustments[11].setLocalTranslation(-4.33f, 4.95f, 16.2f);
		adjustments[11].setLocalRotation(new Quaternion().fromAngles(rad(-30), rad(-15), 0));
		
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(29), 0, 0));
		
		drum.setLocalTranslation(0, 2, 0);
		animNode.attachChild(drum);
		instrumentNode.attachChild(animNode);
		instrumentNode.setLocalTranslation(0, 44.55f, -98.189f);
		highestLevel.attachChild(instrumentNode);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(-37f - 15 * indexForMoving(delta)), 0));
	}
}