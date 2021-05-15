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

package org.wysko.midis2jam2.instrument.family.percussion;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.midi.Midi.*;

/**
 * The Congas.
 */
public class Congas extends NonDrumSetPercussion {
	
	/**
	 * The Right hand node.
	 */
	private final Node rightHandNode = new Node();
	
	/**
	 * The Left hand node.
	 */
	private final Node leftHandNode = new Node();
	
	/**
	 * The Left conga anim node.
	 */
	private final Node leftCongaAnimNode = new Node();
	
	/**
	 * The Right conga anim node.
	 */
	private final Node rightCongaAnimNode = new Node();
	
	/**
	 * The Muted hand node.
	 */
	private final Node mutedHandNode = new Node();
	
	/**
	 * The Low conga hits.
	 */
	private final List<MidiNoteOnEvent> lowCongaHits;
	
	/**
	 * The High conga hits.
	 */
	private final List<MidiNoteOnEvent> highCongaHits;
	
	/**
	 * The Muted conga hits.
	 */
	private final List<MidiNoteOnEvent> mutedCongaHits;
	
	/**
	 * Instantiates new congas.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Congas(Midis2jam2 context,
	              List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		lowCongaHits = hits.stream().filter(h -> h.note == LOW_CONGA).collect(Collectors.toList());
		highCongaHits = hits.stream().filter(h -> h.note == OPEN_HIGH_CONGA).collect(Collectors.toList());
		mutedCongaHits = hits.stream().filter(h -> h.note == MUTE_HIGH_CONGA).collect(Collectors.toList());
		
		Spatial leftConga = context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp");
		leftConga.setLocalScale(0.92f);
		leftCongaAnimNode.attachChild(leftConga);
		rightCongaAnimNode.attachChild(context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp"));
		
		var leftCongaNode = new Node();
		leftCongaNode.attachChild(leftCongaAnimNode);
		var rightCongaNode = new Node();
		rightCongaNode.attachChild(rightCongaAnimNode);
		
		instrumentNode.attachChild(leftCongaNode);
		instrumentNode.attachChild(rightCongaNode);
		
		highestLevel.setLocalTranslation(-32.5f, 35.7f, -44.1f);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(4.8), rad(59.7), rad(-3.79f)));
		
		leftCongaNode.setLocalTranslation(0.87f, -1.15f, 2.36f);
		leftCongaNode.setLocalRotation(new Quaternion().fromAngles(rad(4.2f), rad(18.7f), rad(5.66f)));
		
		rightCongaNode.setLocalTranslation(15.42f, 0.11f, -1.35f);
		rightCongaNode.setLocalRotation(new Quaternion().fromAngles(rad(3.78f), rad(18), rad(5.18)));
		
		Spatial mutedHand = context.loadModel("hand_left.obj", "hands.bmp");
		mutedHand.setLocalTranslation(0, 0, -1);
		mutedHandNode.attachChild(mutedHand);
		mutedHandNode.setLocalTranslation(-2.5f, 0, 1);
		
		Spatial leftHand = context.loadModel("hand_left.obj", "hands.bmp");
		leftHand.setLocalTranslation(0, 0, -1);
		leftHandNode.attachChild(leftHand);
		leftHandNode.setLocalTranslation(1.5f, 0, 6);
		
		leftCongaAnimNode.attachChild(mutedHandNode);
		leftCongaAnimNode.attachChild(leftHandNode);
		
		Spatial rightHand = context.loadModel("hand_right.obj", "hands.bmp");
		rightHand.setLocalTranslation(0, 0, -1);
		rightHandNode.attachChild(rightHand);
		rightHandNode.setLocalTranslation(0, 0, 6);
		
		rightCongaAnimNode.attachChild(rightHandNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus statusLow = Stick.handleStick(context, rightHandNode, time, delta, lowCongaHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		Stick.StickStatus statusHigh = Stick.handleStick(context, leftHandNode, time, delta, highCongaHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		Stick.StickStatus statusMuted = Stick.handleStick(context, mutedHandNode, time, delta, mutedCongaHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			recoilDrum(rightCongaAnimNode, true, strike.velocity, delta);
		} else {
			recoilDrum(rightCongaAnimNode, false, 0, delta);
		}
		
		if (statusHigh.justStruck() || statusMuted.justStruck()) {
			MidiNoteOnEvent highStrike = statusHigh.getStrike();
			MidiNoteOnEvent mutedStrike = statusMuted.getStrike();
			var maxVelocity = 0;
			
			if (highStrike != null && highStrike.velocity > maxVelocity) maxVelocity = highStrike.velocity;
			if (mutedStrike != null && mutedStrike.velocity > maxVelocity) maxVelocity = mutedStrike.velocity;
			
			recoilDrum(leftCongaAnimNode, true, maxVelocity, delta);
		} else {
			recoilDrum(leftCongaAnimNode, false, 0, delta);
		}
	}
}
