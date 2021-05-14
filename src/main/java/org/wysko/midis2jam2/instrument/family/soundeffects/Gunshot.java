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

package org.wysko.midis2jam2.instrument.family.soundeffects;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Gunshot.
 */
public class Gunshot extends DecayedInstrument {
	
	/**
	 * The Pistols.
	 */
	final Pistol[] pistols = new Pistol[12];
	
	/**
	 * The Gun nodes.
	 */
	final Node[] gunNodes = new Node[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public Gunshot(@NotNull Midis2jam2 context,
	               @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		for (var i = 0; i < 12; i++) {
			gunNodes[i] = new Node();
			pistols[i] = new Pistol();
			Node highestLevel = pistols[i].highestLevel;
			gunNodes[i].attachChild(highestLevel);
			instrumentNode.attachChild(gunNodes[i]);
			highestLevel.setLocalTranslation(0, 45, -140);
			gunNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(i * 1.5), 0));
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiNoteOnEvent> ons = new ArrayList<>();
		while (!hits.isEmpty() && context.getFile().eventInSeconds(hits.get(0)) <= time) {
			ons.add(hits.remove(0));
		}
		for (MidiNoteOnEvent on : ons) {
			int i = (on.note + 3) % 12;
			pistols[i].fire();
		}
		for (Pistol pistol : pistols) {
			pistol.tick(time, delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-70f + 20 * indexForMoving(delta)), 0));
	}
	
	/**
	 * A single pistol.
	 */
	public class Pistol extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		/**
		 * The blast effect.
		 */
		private final Spatial blast;
		
		/**
		 * Has this pistol fired once?
		 */
		private boolean firedOnce = false;
		
		/**
		 * The scale of the blast.
		 */
		private float scale = 0;
		
		/**
		 * Instantiates a new Pistol.
		 */
		public Pistol() {
			Spatial pistol = context.loadModel("Pistol.obj", "PistolSkin.png");
			blast = context.shadow("Assets/PistolBlast.fbx", "Assets/Explosion.png");
			blast.setLocalScale(0);
			blast.setLocalTranslation(0, 3, 5.5f);
			var modelNode = new Node();
			modelNode.attachChild(pistol);
			highestLevel.attachChild(blast);
			animNode.attachChild(modelNode);
		}
		
		@Override
		public void tick(double time, float delta) {
			Vector3f localTranslation = animNode.getLocalTranslation();
			Quaternion localRotation = animNode.getLocalRotation();
			blast.setLocalScale(Math.max(0, blast.getLocalScale().x - delta * 10));
			
			if (firedOnce) {
				blast.setLocalScale(scale);
				scale += delta * 30;
				((Geometry) ((Node) ((Node) blast).getChild(0)).getChild(0)).getMaterial().setFloat(
						"AlphaDiscardThreshold", scale / 7f);
			}
			
			if (localTranslation.z < 0.001) {
				animNode.move(0, 0, delta * 30);
				Vector3f newTranslation = animNode.getLocalTranslation();
				animNode.setLocalTranslation(newTranslation.setZ(Math.min(newTranslation.getZ(), 0)));
			} else {
				animNode.setLocalTranslation(0, 0, 0);
			}
			if (Math.toDegrees(localRotation.toAngles(null)[0]) < 0.001) {
				animNode.rotate(rad(3), 0, 0);
				float v = animNode.getLocalRotation().toAngles(null)[0];
				animNode.setLocalRotation(new Quaternion().fromAngles(Math.min(v, 0), 0, 0));
			}
		}
		
		/**
		 * Fire the pistol.
		 */
		public void fire() {
			animNode.setLocalTranslation(0, 0, -5);
			animNode.setLocalRotation(new Quaternion().fromAngles(rad(-30), 0, 0));
			firedOnce = true;
			scale = 0;
		}
	}
}
