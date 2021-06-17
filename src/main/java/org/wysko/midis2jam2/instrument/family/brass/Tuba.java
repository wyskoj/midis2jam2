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

package org.wysko.midis2jam2.instrument.family.brass;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager;
import org.wysko.midis2jam2.instrument.clone.AnimatedKeyCloneByIntegers;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.Arrays;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The tuba. Has four keys and animates just like other {@link MonophonicInstrument}s.
 */
public class Tuba extends MonophonicInstrument {
	
	/**
	 * The tuba fingering manager.
	 */
	public static final PressedKeysFingeringManager FINGERING_MANAGER = PressedKeysFingeringManager.from(Tuba.class);
	
	/**
	 * Instantiates a new Tuba.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Tuba(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(context, eventList, TubaClone.class, FINGERING_MANAGER);
		
		/* Tuba positioning */
		groupOfPolyphony.setLocalTranslation(-110, 25, -30);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 40f * indexForMoving(delta), 0);
	}
	
	/**
	 * A single tuba.
	 */
	public class TubaClone extends AnimatedKeyCloneByIntegers {
		
		/**
		 * Instantiates a new tuba clone.
		 */
		public TubaClone() {
			super(Tuba.this, -0.05f, 0.8f, 4, Axis.Y, Axis.Z);
			
			body = context.loadModel("TubaBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.attachChild(context.loadModel("TubaHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			((Node) body).getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"));
			
			/* Load tuba keys */
			for (var i = 0; i < 4; i++) {
				keys[i] = context.loadModel("TubaKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			
			idleNode.setLocalRotation(new Quaternion().fromAngles(rad(-10), rad(90), 0));
			highestLevel.setLocalTranslation(10, 0, 0);
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(50f * indexForMoving()), 0));
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			/* Tuba keys move down when pressed */
			for (var i = 0; i < 4; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					keys[i].setLocalTranslation(0, -0.5f, 0);
				} else {
					keys[i].setLocalTranslation(0, 0, 0);
				}
			}
		}
	}
}
