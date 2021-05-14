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
 * The trumpet has three keys, and are simply animated. It handles much like other {@link MonophonicInstrument}s.
 */
public class Trumpet extends MonophonicInstrument {
	
	/**
	 * The trumpet fingering manager.
	 */
	public static final PressedKeysFingeringManager FINGERING_MANAGER = PressedKeysFingeringManager.from(Trumpet.class);
	
	public Trumpet(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList,
	               TrumpetType type) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				type == TrumpetType.NORMAL ? TrumpetClone.class : MutedTrumpetClone.class,
				FINGERING_MANAGER
		);
		
		groupOfPolyphony.setLocalTranslation(-36.5f, 60, 10);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-2), rad(90), 0));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 10f * indexForMoving(delta), 0);
	}
	
	/**
	 * The type of trumpet.
	 */
	public enum TrumpetType {
		/**
		 * The normal, open trumpet.
		 */
		NORMAL,
		
		/**
		 * The muted trumpet.
		 */
		MUTED
	}
	
	/**
	 * A single instance of a trumpet.
	 */
	public class TrumpetClone extends AnimatedKeyCloneByIntegers {
		
		/**
		 * Instantiates a new trumpet clone.
		 */
		public TrumpetClone() {
			super(Trumpet.this, 0.15f, 0.9f, 3, Axis.Z, Axis.X);
			
			body = context.loadModel("TrumpetBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			((Node) body).getChild(1).setMaterial(context.reflectiveMaterial("Assets/HornSkinGrey.bmp"));
			
			this.bell.attachChild(context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
			this.bell.setLocalTranslation(0, 0, 5.58f);
			
			for (var i = 0; i < 3; i++) {
				keys[i] = context.loadModel("TrumpetKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			
			idleNode.setLocalRotation(new Quaternion().fromAngles(rad(-10), 0, 0));
			
			animNode.setLocalTranslation(0, 0, 15);
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			for (var i = 0; i < 3; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					keys[i].setLocalTranslation(0, -0.5f, 0);
				} else {
					keys[i].setLocalTranslation(0, 0, 0);
				}
			}
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-10f * indexForMoving()), 0));
			offsetNode.setLocalTranslation(0, indexForMoving() * -1f, 0);
		}
	}
	
	/**
	 * Exact same as {@link TrumpetClone} but just adds the mute to the bell.
	 */
	public class MutedTrumpetClone extends TrumpetClone {
		
		public MutedTrumpetClone() {
			super();
			this.bell.attachChild(context.loadModel("TrumpetMute.obj", "RubberFoot.bmp"));
		}
	}
}
