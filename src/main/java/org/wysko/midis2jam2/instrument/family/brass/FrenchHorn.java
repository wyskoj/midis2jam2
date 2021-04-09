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

import com.jme3.material.Material;
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
 * The French Horn. It animates like most other {@link MonophonicInstrument}s.
 * <p>
 * The French Horn has four keys, the first one being the trigger key. It animates on a different axis than the rest.
 */
public class FrenchHorn extends MonophonicInstrument {
	
	/**
	 * The French Horn fingering manager.
	 */
	public static final PressedKeysFingeringManager FINGERING_MANAGER = PressedKeysFingeringManager.from(FrenchHorn.class);
	
	/**
	 * Instantiates a new French horn.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public FrenchHorn(Midis2jam2 context,
	                  List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(context, eventList, FrenchHornClone.class, FINGERING_MANAGER);
		
		/* Position French Horn */
		groupOfPolyphony.setLocalTranslation(-83.1f, 41.6f, -63.7f);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 15 * indexForMoving(), 0);
	}
	
	/**
	 * A single instance of a French Horn.
	 */
	public class FrenchHornClone extends AnimatedKeyCloneByIntegers {
		
		/**
		 * Instantiates a new French Horn clone.
		 */
		public FrenchHornClone() {
			super(FrenchHorn.this, 0.1f, 0.9f, 4, Axis.Y, Axis.X);
			
			/* Load models */
			body = context.loadModel("FrenchHornBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.attachChild(context.loadModel("FrenchHornHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
			
			/* Attach models */
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			
			/* Set grey metal material */
			Material material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			((Node) body).getChild(1).setMaterial(material);
			
			/* Move bell to body of horn */
			bell.setLocalTranslation(0, -4.63f, -1.87f);
			bell.setLocalRotation(new Quaternion().fromAngles(rad(112 - 90), 0, 0));
			
			/* Load keys */
			for (int i = 0; i < 4; i++) {
				String id = i == 0 ? "Trigger" : "Key" + (i);
				keys[i] = context.loadModel("FrenchHorn" + id + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			
			keys[0].setLocalTranslation(0, 0, 1);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(110 - 90), rad(90), 0));
			animNode.setLocalTranslation(0, 0, 20);
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(47 * indexForMoving()), 0));
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			/* French horn keys rotate when pressed */
			for (int i = 0; i < 4; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					if (i == 0) // Trigger key, so rotate on different axis
						keys[i].setLocalRotation(new Quaternion().fromAngles(rad(-25), 0, 0));
					else
						keys[i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-30)));
				} else {
					keys[i].setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
				}
			}
		}
	}
}
