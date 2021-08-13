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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.instrument.family.percussive.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The bass drum, or kick drum.
 */
public class BassDrum extends PercussionInstrument {
	
	/**
	 * The maximum angle the pedal will fall back to when at rest.
	 */
	private static final int PEDAL_MAX_ANGLE = 20;
	
	/**
	 * The Bass drum beater arm.
	 */
	@NotNull
	private final Spatial bassDrumBeaterArm;
	
	/**
	 * The Bass drum pedal.
	 */
	@NotNull
	private final Spatial bassDrumPedal;
	
	/**
	 * The Drum node.
	 */
	@NotNull
	private final Node drumNode = new Node();
	
	public BassDrum(@NotNull Midis2jam2 context, @NotNull List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Load bass drum */
		Spatial drumModel = context.loadModel("DrumSet_BassDrum.obj", "DrumShell.bmp");
		
		/* Load beater arm */
		bassDrumBeaterArm = context.loadModel("DrumSet_BassDrumBeaterArm.fbx", "MetalTexture.bmp");
		
		/* Load beater holder */
		Spatial bassDrumBeaterHolder = context.loadModel("DrumSet_BassDrumBeaterHolder.fbx", "MetalTexture.bmp");
		final Node holder = (Node) bassDrumBeaterHolder;
		
		/* Apply materials */
		final Node arm = (Node) bassDrumBeaterArm;
		
		final var shinySilverMaterial = context.reflectiveMaterial("Assets/ShinySilver.bmp");
		arm.getChild(0).setMaterial(shinySilverMaterial);
		
		final var darkMetalMaterial = context.unshadedMaterial("Assets/MetalTextureDark.bmp");
		arm.getChild(1).setMaterial(darkMetalMaterial);
		
		holder.getChild(0).setMaterial(darkMetalMaterial);
		
		/* Load pedal */
		bassDrumPedal = context.loadModel("DrumSet_BassDrumPedal.obj", "MetalTexture.bmp");
		
		drumNode.attachChild(drumModel);
		
		var beaterNode = new Node();
		beaterNode.attachChild(this.bassDrumBeaterArm);
		beaterNode.attachChild(bassDrumBeaterHolder);
		beaterNode.attachChild(bassDrumPedal);
		highLevelNode.attachChild(drumNode);
		highLevelNode.attachChild(beaterNode);
		
		this.bassDrumBeaterArm.setLocalTranslation(0, 5.5F, 1.35F);
		this.bassDrumBeaterArm.setLocalRotation(new Quaternion().fromAngles(rad(MAX_ANGLE), 0, 0));
		bassDrumPedal.setLocalRotation(new Quaternion().fromAngles(rad(PEDAL_MAX_ANGLE), 0, 0));
		bassDrumPedal.setLocalTranslation(0, 0.5F, 7.5F);
		beaterNode.setLocalTranslation(0, 0, 1.5F);
		
		highLevelNode.setLocalTranslation(0, 0, -80);
	}
	
	@Override
	public void tick(double time, float delta) {
		MidiNoteOnEvent nextHit = NoteQueue.collectOne(hits, context, time);
		
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
			float beaterAngle = beaterAngles[0] + 8F * delta;
			beaterAngle = Math.min(rad((float) MAX_ANGLE), beaterAngle);
			bassDrumBeaterArm.setLocalRotation(new Quaternion().fromAngles(
					beaterAngle, 0, 0
			));
			
			// Pedal comeback
			float[] pedalAngles = bassDrumPedal.getLocalRotation().toAngles(new float[3]);
			float pedalAngle = (float) (pedalAngles[0] + 8F * delta * (PEDAL_MAX_ANGLE / MAX_ANGLE));
			pedalAngle = Math.min(rad(PEDAL_MAX_ANGLE), pedalAngle);
			bassDrumPedal.setLocalRotation(new Quaternion().fromAngles(
					pedalAngle, 0, 0
			));
		}
	}
}
