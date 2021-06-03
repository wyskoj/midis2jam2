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

package org.wysko.midis2jam2.instrument.family.reed;

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager;
import org.wysko.midis2jam2.instrument.clone.HandedClone;
import org.wysko.midis2jam2.instrument.family.pipe.HandedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Clarinet extends HandedInstrument {
	
	public static final HandPositionFingeringManager FINGERING_MANAGER = HandPositionFingeringManager.from(Clarinet.class);
	
	/**
	 * Constructs a monophonic instrument.
	 *
	 * @param context   context to midis2jam2
	 * @param eventList the event list
	 */
	public Clarinet(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(context, eventList, ClarinetClone.class, FINGERING_MANAGER);
		instrumentNode.setLocalTranslation(-25, 50, 0);
		instrumentNode.setLocalScale(0.9f); // haha yes
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 20 * indexForMoving(delta), 0);
	}
	
	public class ClarinetClone extends HandedClone {
		
		private final Spatial horn;
		private final Axis scaleAxis = Axis.Y;
		
		/**
		 * Instantiates a new clone.
		 */
		public ClarinetClone() {
			super(Clarinet.this, 0.075f);
			
			Spatial body = context.loadModel("ClarinetBody.obj", "ClarinetSkin.png");
			modelNode.attachChild(body);
			
			horn = context.loadModel("ClarinetHorn.obj", "ClarinetSkin.png");
			modelNode.attachChild(horn);
			
			horn.setLocalTranslation(0, -20.7125f, 0);
			
			loadHands();
			animNode.setLocalTranslation(0, 0, 10);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(-25), rad(45), 0));
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(25f * indexForMoving()), 0));
		}
		
		@Override
		protected void loadHands() {
			leftHands = new Spatial[20];
			for (var i = 0; i < 20; i++) {
				leftHands[i] = parent.context.loadModel(String.format("ClarinetLeftHand%d.obj", i), "hands.bmp");
				leftHandNode.attachChild(leftHands[i]);
				if (i != 0) {
					leftHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
			rightHands = new Spatial[13];
			for (var i = 0; i < 13; i++) {
				rightHands[i] = parent.context.loadModel("ClarinetRightHand%d.obj".formatted(i), "hands.bmp");
				rightHandNode.attachChild(rightHands[i]);
				if (i != 0) {
					rightHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
		}
		
		@Override
		public void tick(double time, float delta) {
			super.tick(time, delta);
			/* Stretch bell */
			if (currentNotePeriod != null) {
				
				var hands = (HandPositionFingeringManager.Hands) parent.manager.fingering(currentNotePeriod.midiNote);
				if (hands != null) {
					context.getDebugText().setText(hands.toString());
					float scale = (float) ((0.7 * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1);
					
					horn.setLocalScale(
							scaleAxis == Axis.X ? scale : 1,
							scaleAxis == Axis.Y ? scale : 1,
							scaleAxis == Axis.Z ? scale : 1
					);
				} else {
					animNode.setLocalRotation(new Quaternion()); // override rotation
				}
				
				
			} else {
				horn.setLocalScale(1, 1, 1);
			}
		}
	}
}
