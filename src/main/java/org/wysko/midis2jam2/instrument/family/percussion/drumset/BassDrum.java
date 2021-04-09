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

package org.wysko.midis2jam2.instrument.family.percussion.drumset;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.MAX_ANGLE;

/**
 * The bass drum, or kick drum.
 */
public class BassDrum extends PercussionInstrument {
	
	/**
	 * The maximum angle the pedal will fall back to when at rest.
	 */
	private final static int PEDAL_MAX_ANGLE = 20;
	
	/**
	 * The Bass drum.
	 */
	final Spatial bassDrum;
	
	/**
	 * The Bass drum beater arm.
	 */
	final Spatial bassDrumBeaterArm;
	
	/**
	 * The Bass drum beater holder.
	 */
	final Spatial bassDrumBeaterHolder;
	
	/**
	 * The Bass drum pedal.
	 */
	final Spatial bassDrumPedal;
	
	/**
	 * The High level node.
	 */
	final Node highLevelNode = new Node();
	
	/**
	 * The Drum node.
	 */
	final Node drumNode = new Node();
	
	/**
	 * The Beater node.
	 */
	final Node beaterNode = new Node();
	
	public BassDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		bassDrum = context.loadModel("DrumSet_BassDrum.obj", "DrumShell.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		bassDrumBeaterArm = context.loadModel("DrumSet_BassDrumBeaterArm.fbx", "MetalTexture.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		
		// Apply materials
		final Node arm = (Node) bassDrumBeaterArm;
		
		final Texture shinySilverTexture = context.getAssetManager().loadTexture("Assets/ShinySilver.bmp");
		final Material shinySilverMaterial = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		shinySilverMaterial.setTexture("ColorMap", shinySilverTexture);
		arm.getChild(0).setMaterial(shinySilverMaterial);
		
		final Texture darkMetalTexture = context.getAssetManager().loadTexture("Assets/MetalTextureDark.bmp");
		final Material darkMetalMaterial = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		darkMetalMaterial.setTexture("ColorMap", darkMetalTexture);
		arm.getChild(1).setMaterial(darkMetalMaterial);
		
		bassDrumBeaterHolder = context.loadModel("DrumSet_BassDrumBeaterHolder.fbx", "MetalTexture.bmp",
				Midis2jam2.MatType.UNSHADED, 0.9f);
		final Node holder = (Node) bassDrumBeaterHolder;
		
		holder.getChild(0).setMaterial(darkMetalMaterial);
		
		
		bassDrumPedal = context.loadModel("DrumSet_BassDrumPedal.obj", "MetalTexture.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		
		drumNode.attachChild(bassDrum);
		beaterNode.attachChild(this.bassDrumBeaterArm);
		beaterNode.attachChild(bassDrumBeaterHolder);
		beaterNode.attachChild(bassDrumPedal);
		highLevelNode.attachChild(drumNode);
		highLevelNode.attachChild(beaterNode);
		
		this.bassDrumBeaterArm.setLocalTranslation(0, 5.5f, 1.35f);
		this.bassDrumBeaterArm.setLocalRotation(new Quaternion().fromAngles(rad(MAX_ANGLE), 0, 0));
		bassDrumPedal.setLocalRotation(new Quaternion().fromAngles(rad(PEDAL_MAX_ANGLE), 0, 0));
		bassDrumPedal.setLocalTranslation(0, 0.5f, 7.5f);
		beaterNode.setLocalTranslation(0, 0, 1.5f);
		
		highLevelNode.setLocalTranslation(0, 0, -80);
	}
	
	@Override
	public void tick(double time, float delta) {
		MidiNoteOnEvent nextHit = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time)
			nextHit = hits.remove(0);
		
		if (nextHit != null) {
			// We need to strike
			bassDrumBeaterArm.setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
			bassDrumPedal.setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
			drumNode.setLocalTranslation(0, 0, (float) (-3 * velocityRecoilDampening(nextHit.velocity)));
		} else {
			
			// Drum recoil
			Vector3f localTranslation = drumNode.getLocalTranslation();
			if (localTranslation.z < -0.0001) {
				drumNode.setLocalTranslation(0, 0, Math.min(0,
						localTranslation.z + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)));
			} else {
				drumNode.setLocalTranslation(0, 0, 0);
			}
			
			// Beater comeback
			float[] beaterAngles = bassDrumBeaterArm.getLocalRotation().toAngles(new float[3]);
			float beaterAngle = beaterAngles[0] + 8f * delta;
			beaterAngle = Math.min(rad((float) MAX_ANGLE), beaterAngle);
			bassDrumBeaterArm.setLocalRotation(new Quaternion().fromAngles(
					beaterAngle, 0, 0
			));
			
			// Pedal comeback
			float[] pedalAngles = bassDrumPedal.getLocalRotation().toAngles(new float[3]);
			float pedalAngle = (float) (pedalAngles[0] + 8f * delta * (PEDAL_MAX_ANGLE / MAX_ANGLE));
			pedalAngle = Math.min(rad((float) PEDAL_MAX_ANGLE), pedalAngle);
			bassDrumPedal.setLocalRotation(new Quaternion().fromAngles(
					pedalAngle, 0, 0
			));
		}
	}
}
