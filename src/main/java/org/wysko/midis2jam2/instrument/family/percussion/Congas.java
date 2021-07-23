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
import org.wysko.midis2jam2.midi.Midi;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.midi.Midi.*;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * Although there are three MIDI congas, there are two physical congas on stage. The left conga plays {@link
 * Midi#OPEN_HIGH_CONGA} and {@link Midi#MUTE_HIGH_CONGA}, where the right conga plays {@link Midi#LOW_CONGA}.
 * <p>
 * The left conga has two left hands. The second left hand is slightly offset and near the top of the head of the conga
 * to represent a muted note. The hands are animated with {@link Stick#handleStick}.
 * <p>
 * Because both the high and muted notes are played on the same conga, instances where both notes play at the same time
 * use the maximum velocity of the two for recoiling animation.
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
		
		/* Separate notes */
		lowCongaHits = hits.stream().filter(h -> h.note == LOW_CONGA).collect(Collectors.toList());
		highCongaHits = hits.stream().filter(h -> h.note == OPEN_HIGH_CONGA).collect(Collectors.toList());
		mutedCongaHits = hits.stream().filter(h -> h.note == MUTE_HIGH_CONGA).collect(Collectors.toList());
		
		/* Load left conga */
		Spatial leftConga = context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp");
		leftConga.setLocalScale(0.92F);
		leftCongaAnimNode.attachChild(leftConga);
		
		/* Load right conga */
		rightCongaAnimNode.attachChild(context.loadModel("DrumSet_Conga.obj", "DrumShell_Conga.bmp"));
		
		/* Create nodes for congas and attach them */
		var leftCongaNode = new Node();
		leftCongaNode.attachChild(leftCongaAnimNode);
		var rightCongaNode = new Node();
		rightCongaNode.attachChild(rightCongaAnimNode);
		
		/* Attach to instrument */
		instrumentNode.attachChild(leftCongaNode);
		instrumentNode.attachChild(rightCongaNode);
		
		/* Positioning */
		highestLevel.setLocalTranslation(-32.5F, 35.7F, -44.1F);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(4.8), rad(59.7), rad(-3.79F)));
		
		leftCongaNode.setLocalTranslation(0.87F, -1.15F, 2.36F);
		leftCongaNode.setLocalRotation(new Quaternion().fromAngles(rad(4.2F), rad(18.7F), rad(5.66F)));
		
		rightCongaNode.setLocalTranslation(15.42F, 0.11F, -1.35F);
		rightCongaNode.setLocalRotation(new Quaternion().fromAngles(rad(3.78F), rad(18), rad(5.18)));
		
		/* Load and position muted hand */
		Spatial mutedHand = context.loadModel("hand_left.obj", "hands.bmp");
		mutedHand.setLocalTranslation(0, 0, -1);
		mutedHandNode.attachChild(mutedHand);
		mutedHandNode.setLocalTranslation(-2.5F, 0, 1);
		
		/* Load and position left hand */
		Spatial leftHand = context.loadModel("hand_left.obj", "hands.bmp");
		leftHand.setLocalTranslation(0, 0, -1);
		leftHandNode.attachChild(leftHand);
		leftHandNode.setLocalTranslation(1.5F, 0, 6);
		
		/* Load and position right hand */
		Spatial rightHand = context.loadModel("hand_right.obj", "hands.bmp");
		rightHand.setLocalTranslation(0, 0, -1);
		rightHandNode.attachChild(rightHand);
		rightHandNode.setLocalTranslation(0, 0, 6);
		
		/* Attach hands */
		leftCongaAnimNode.attachChild(mutedHandNode);
		leftCongaAnimNode.attachChild(leftHandNode);
		rightCongaAnimNode.attachChild(rightHandNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Animate each hand */
		Stick.StickStatus statusLow = Stick.handleStick(context, rightHandNode, time, delta, lowCongaHits,
				Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		Stick.StickStatus statusHigh = Stick.handleStick(context, leftHandNode, time, delta, highCongaHits,
				Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		Stick.StickStatus statusMuted = Stick.handleStick(context, mutedHandNode, time, delta, mutedCongaHits,
				Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		/* Recoil right conga */
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			recoilDrum(rightCongaAnimNode, true, strike.velocity, delta);
		} else {
			recoilDrum(rightCongaAnimNode, false, 0, delta);
		}
		
		/* Recoil left conga */
		if (statusHigh.justStruck() || statusMuted.justStruck()) {
			
			/* If a muted and a high note play at the same time, we check the velocities of both hits and use the
			 * maximum velocity of the two for recoiling, since the animation is velocity sensitive */
			MidiNoteOnEvent highStrike = statusHigh.getStrike();
			MidiNoteOnEvent mutedStrike = statusMuted.getStrike();
			var maxVelocity = 0;
			
			if (highStrike != null && highStrike.velocity > maxVelocity) {
				maxVelocity = highStrike.velocity;
			}
			
			if (mutedStrike != null && mutedStrike.velocity > maxVelocity) {
				maxVelocity = mutedStrike.velocity;
			}
			
			recoilDrum(leftCongaAnimNode, true, maxVelocity, delta);
		} else {
			recoilDrum(leftCongaAnimNode, false, 0, delta);
		}
	}
}
