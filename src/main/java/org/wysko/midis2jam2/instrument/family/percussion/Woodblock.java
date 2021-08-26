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

import static org.wysko.midis2jam2.midi.Midi.HIGH_WOODBLOCK;
import static org.wysko.midis2jam2.midi.Midi.LOW_WOODBLOCK;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The woodblock. High and low.
 */
public class Woodblock extends NonDrumSetPercussion {
	
	/**
	 * The Right hand node.
	 */
	private final Node rightStickNode = new Node();
	
	/**
	 * The Left hand node.
	 */
	private final Node leftStickNode = new Node();
	
	/**
	 * The Left woodblock anim node.
	 */
	private final Node leftWoodblockAnimNode = new Node();
	
	/**
	 * The Right woodblock anim node.
	 */
	private final Node rightWoodblockAnimNode = new Node();
	
	/**
	 * The Low woodblock hits.
	 */
	private final List<MidiNoteOnEvent> leftHits;
	
	/**
	 * The High woodblock hits.
	 */
	private final List<MidiNoteOnEvent> rightHits;
	
	/**
	 * Instantiates new woodblocks.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Woodblock(Midis2jam2 context,
	                 List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		leftHits = hits.stream().filter(h -> h.getNote() == LOW_WOODBLOCK).collect(Collectors.toList());
		rightHits = hits.stream().filter(h -> h.getNote() == HIGH_WOODBLOCK).collect(Collectors.toList());
		
		leftWoodblockAnimNode.attachChild(context.loadModel("WoodBlockHigh.obj", "SimpleWood.bmp"));
		rightWoodblockAnimNode.attachChild(context.loadModel("WoodBlockLow.obj", "SimpleWood.bmp"));
		
		var leftWoodblockNode = new Node();
		leftWoodblockNode.attachChild(leftWoodblockAnimNode);
		
		var rightWoodblockNode = new Node();
		rightWoodblockNode.attachChild(rightWoodblockAnimNode);
		
		instrumentNode.attachChild(leftWoodblockNode);
		instrumentNode.attachChild(rightWoodblockNode);
		
		highestLevel.setLocalTranslation(0, 40, -90);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(10), 0, 0));
		
		leftWoodblockNode.setLocalTranslation(-5, -0.3f, 0);
		
		Spatial leftStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		leftStick.setLocalTranslation(0, 0, -1);
		this.leftStickNode.attachChild(leftStick);
		this.leftStickNode.setLocalTranslation(0, 0, 13.5f);
		
		leftWoodblockAnimNode.attachChild(this.leftStickNode);
		
		Spatial rightStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		rightStick.setLocalTranslation(0, 0, -1);
		rightStickNode.attachChild(rightStick);
		rightStickNode.setLocalTranslation(0, 0, 13.5f);
		
		rightWoodblockNode.setLocalRotation(new Quaternion().fromAngles(0, rad(3), 0));
		leftWoodblockNode.setLocalRotation(new Quaternion().fromAngles(0, rad(5), 0));
		
		rightWoodblockAnimNode.attachChild(rightStickNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus statusLow = Stick.handleStick(context, rightStickNode, time, delta, leftHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		Stick.StickStatus statusHigh = Stick.handleStick(context, leftStickNode, time, delta, rightHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			recoilDrum(rightWoodblockAnimNode, true, strike.getVelocity(), delta);
		} else {
			recoilDrum(rightWoodblockAnimNode, false, 0, delta);
		}
		
		if (statusHigh.justStruck()) {
			MidiNoteOnEvent strike = statusHigh.getStrike();
			assert strike != null;
			recoilDrum(leftWoodblockAnimNode, true, strike.getVelocity(), delta);
		} else {
			recoilDrum(leftWoodblockAnimNode, false, 0, delta);
		}
	}
}
