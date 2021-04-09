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

import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The recorder.
 */
public class Recorder extends HandedInstrument {
	
	private static final HandPositionFingeringManager FINGERING_MANAGER = HandPositionFingeringManager.from(Recorder.class);
	
	/**
	 * Constructs a recorder.
	 *
	 * @param context context to midis2jam2
	 * @param events  the events to play
	 */
	public Recorder(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		
		super(
				context,
				events,
				RecorderClone.class,
				FINGERING_MANAGER
		);
		
		// positioning
		groupOfPolyphony.setLocalTranslation(-7, 35, -30);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10 * indexForMoving(), 0);
	}
	
	/**
	 * The type Flute clone.
	 */
	public class RecorderClone extends PuffingClone {
		
		/**
		 * Instantiates a new Flute clone.
		 */
		public RecorderClone() {
			super(Recorder.this, 0, SteamPuffer.SteamPuffType.POP, 1f);
			
			Spatial horn = Recorder.this.context.loadModel(
					"Recorder.obj",
					"Recorder.bmp"
			);
			
			loadHands();
			
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[] {0, 0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0, -12.3f, 0);
			
			modelNode.attachChild(horn);
			animNode.setLocalTranslation(0, 0, 23);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(45.5 - 90), 0, 0));
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(15 + indexForMoving() * 15), 0));
		}
		
		@Override
		protected void loadHands() {
			leftHands = new Spatial[13];
			for (int i = 0; i < 13; i++) {
				leftHands[i] = parent.context.loadModel(String.format("RecorderHandLeft%d.obj", i), "hands.bmp");
				leftHandNode.attachChild(leftHands[i]);
				if (i != 0) {
					leftHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
			rightHands = new Spatial[11];
			for (int i = 0; i < 11; i++) {
				rightHands[i] = parent.context.loadModel("RecorderHandRight" + i + ".obj", "hands.bmp");
				rightHandNode.attachChild(rightHands[i]);
				if (i != 0) {
					rightHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
		}
	}
}
