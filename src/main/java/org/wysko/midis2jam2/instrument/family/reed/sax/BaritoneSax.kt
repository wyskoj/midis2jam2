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

package org.wysko.midis2jam2.instrument.family.reed.sax;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.PressedKeysFingeringManager;
import org.wysko.midis2jam2.instrument.clone.Clone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The baritone sax.
 */
public class BaritoneSax extends Saxophone {
	
	/**
	 * The baritone sax fingering manager.
	 */
	public static final PressedKeysFingeringManager FINGERING_MANAGER =
			PressedKeysFingeringManager.from(BaritoneSax.class);
	
	/**
	 * The amount to stretch the baritone sax.
	 */
	private static final float STRETCH_FACTOR = 0.65F;
	
	/**
	 * Constructs a baritone sax.
	 *
	 * @param context context to midis2jam2
	 * @param events  all events that pertain to this instance of a baritone sax
	 */
	public BaritoneSax(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> events)
			throws ReflectiveOperationException {
		super(context, events, BaritoneSaxClone.class, FINGERING_MANAGER);
		groupOfPolyphony.move(10, 48.5F, -42);
		groupOfPolyphony.scale(1.5F);
	}
	
	/**
	 * Implements {@link Clone}, as baritone sax clones.
	 */
	public class BaritoneSaxClone extends SaxophoneClone {
		
		/**
		 * Instantiates a new Baritone sax clone.
		 */
		public BaritoneSaxClone() {
			super(BaritoneSax.this, STRETCH_FACTOR);
			
			var shinyHornSkin = context.reflectiveMaterial("Assets/HornSkin.bmp");
			var black = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			black.setColor("Color", ColorRGBA.Black);
			
			var shinyHornSkin2 = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			
			this.body = context.getAssetManager().loadModel("Assets/BaritoneSaxBody.fbx");
			this.bell.attachChild(context.getAssetManager().loadModel("Assets/BaritoneSaxHorn.obj"));
			
			var bodyNode = ((Node) body);
			
			bodyNode.getChild(0).setMaterial(shinyHornSkin);
			bodyNode.getChild(1).setMaterial(shinyHornSkin2);
			bodyNode.getChild(2).setMaterial(black);
			bell.setMaterial(shinyHornSkin);
			
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			
			/* The bell has to be moved down to attach to the body */
			bell.move(0, -10, 0);
			
			animNode.setLocalTranslation(0, 0, 20);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(10), rad(30), 0));
		}
	}
}
