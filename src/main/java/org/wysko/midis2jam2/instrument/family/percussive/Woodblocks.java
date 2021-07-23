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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static com.jme3.math.FastMath.HALF_PI;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The Woodblocks consist of 12 blocks.
 */
public class Woodblocks extends OctavePercussion {
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public Woodblocks(@NotNull Midis2jam2 context,
	                  @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		for (var i = 0; i < 12; i++) {
			/* Load drum stick */
			Spatial stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
			stick.setLocalTranslation(0, 0, -5);
			
			/* Initialize node and attach stick */
			malletNodes[i] = new Node();
			malletNodes[i].setLocalTranslation(0, 0, 18);
			malletNodes[i].attachChild(stick);
			
			var oneBlock = new Node();
			oneBlock.attachChild(malletNodes[i]);
			
			var woodblock = new Woodblock(i);
			twelfths[i] = woodblock;
			
			oneBlock.attachChild(woodblock.highestLevel);
			percussionNodes[i].attachChild(oneBlock);
			oneBlock.setLocalTranslation(0, 0, 20);
			percussionNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(7.5 * i), 0));
			percussionNodes[i].setLocalTranslation(0, 0.3F * i, 0);
			
			instrumentNode.attachChild(percussionNodes[i]);
		}
		
		instrumentNode.setLocalTranslation(75, 0, -35);
		
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		
		for (TwelfthOfOctaveDecayed woodblock : twelfths) {
			woodblock.tick(delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 15 + 3.6F * indexForMoving(delta), 0);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, -HALF_PI + HALF_PI * indexForMoving(delta), 0));
	}
	
	/**
	 * A single Woodblock.
	 */
	public class Woodblock extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		/**
		 * Instantiates a new Woodblock.
		 *
		 * @param i the index of this woodblock
		 */
		public Woodblock(int i) {
			Spatial mesh = context.loadModel("WoodBlockSingle.obj", "SimpleWood.bmp");
			mesh.setLocalScale(1 - 0.036F * i);
			animNode.attachChild(mesh);
		}
		
		@Override
		public void tick(float delta) {
			Vector3f localTranslation = highestLevel.getLocalTranslation();
			if (localTranslation.y < -0.0001) {
				highestLevel.setLocalTranslation(
						0,
						Math.min(0, localTranslation.y + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)),
						0
				);
			} else {
				highestLevel.setLocalTranslation(0, 0, 0);
			}
		}
	}
}
