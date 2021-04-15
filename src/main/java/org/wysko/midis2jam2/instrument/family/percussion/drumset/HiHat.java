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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The hi-hat.
 */
public class HiHat extends SingleStickInstrument {
	
	private final static int WOBBLE_SPEED = 10;
	
	private final static double DAMPENING = 2;
	
	private final static double AMPLITUDE = 0.25;
	
	/**
	 * The list of NoteOn events that the stick needs to worry about (closed and open).
	 */
	private final List<MidiNoteOnEvent> hitsToStrike;
	
	/**
	 * The Top cymbal.
	 */
	private final Node topCymbal = new Node();
	
	/**
	 * The Whole hat.
	 */
	private final Node wholeHat = new Node();
	
	/**
	 * The cymbal animator.
	 */
	private final CymbalAnimator animator;
	
	/**
	 * The current animation time.
	 */
	private double animTime;
	
	/**
	 * The current status of the hi-hat.
	 *
	 * @see HiHatStatus
	 */
	private HiHatStatus status = HiHatStatus.CLOSED;
	
	public HiHat(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		hitsToStrike = hits.stream().filter(h -> h.note == 42 || h.note == 46).collect(Collectors.toList());
		Spatial topCymbalModel = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		Spatial bottomCymbalModel = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		bottomCymbalModel.setLocalRotation(new Quaternion().fromAngles(rad(180), 0, 0));
		
		topCymbal.setLocalTranslation(0, 1.2f, 0);
		topCymbal.attachChild(topCymbalModel);
		Node bottomCymbal = new Node();
		bottomCymbal.attachChild(bottomCymbalModel);
		wholeHat.attachChild(topCymbal);
		wholeHat.attachChild(bottomCymbal);
		wholeHat.setLocalScale(1.3f);
		
		wholeHat.setLocalTranslation(0, 0, -14);
		highLevelNode.attachChild(wholeHat);
		highLevelNode.setLocalTranslation(-6, 22, -72);
		highLevelNode.setLocalRotation(new Quaternion().fromAngles(0, rad(90), 0));
		highLevelNode.detachChild(stickNode);
		wholeHat.attachChild(stickNode);
		stickNode.setLocalTranslation(0, 1, 13);
		this.animator = new CymbalAnimator(AMPLITUDE, WOBBLE_SPEED, DAMPENING);
	}
	
	@Override
	public void tick(double time, float delta) {
		animator.tick(delta);
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		
		if (recoil != null) {
			animator.strike();
			wholeHat.setLocalTranslation(0, (float) (-0.7 * velocityRecoilDampening(recoil.velocity)), -14);
			if (recoil.note == 46) {
				status = HiHatStatus.OPEN;
				topCymbal.setLocalTranslation(0, 2, 0);
			} else {
				status = HiHatStatus.CLOSED;
				topCymbal.setLocalTranslation(0, 1.2f, 0);
			}
			animTime = 0;
		}
		topCymbal.setLocalRotation(new Quaternion().fromAngles(status == HiHatStatus.CLOSED ? 0 : animator.rotationAmount(), 0, 0));
		if (animTime != -1) animTime += delta;
		handleStick(time, delta, hitsToStrike);
		
		wholeHat.move(0, 0.025f, 0);
		if (wholeHat.getLocalTranslation().y > 0) {
			Vector3f localTranslation = new Vector3f(wholeHat.getLocalTranslation());
			localTranslation.y = Math.min(localTranslation.y, 0);
			wholeHat.setLocalTranslation(localTranslation);
		}
	}
	
	/**
	 * The status of the hi-hat.
	 */
	private enum HiHatStatus {
		/**
		 * The hat is closed, meaning the top cymbal and bottom cymbal are together.
		 */
		CLOSED,
		
		/**
		 * The hat is open, meaning the top cymbal is raised.
		 */
		OPEN
	}
}
