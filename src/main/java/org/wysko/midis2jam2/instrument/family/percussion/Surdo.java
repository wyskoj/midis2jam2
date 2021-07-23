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
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.Midi;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;


/**
 * The surdo.
 */
public class Surdo extends NonDrumSetPercussion {
	
	/**
	 * The stick node.
	 */
	private final Node stickNode = new Node();
	
	private final Spatial hand;
	
	/**
	 * Instantiates a new Cowbell.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Surdo(Midis2jam2 context,
	             List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		var drum = context.loadModel("DrumSet_Surdo.fbx", "DrumShell_Surdo.png");
		recoilNode.attachChild(drum);
		drum.setLocalScale(1.7f);
		
		Spatial stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		stick.setLocalTranslation(0, 0, -2);
		stickNode.attachChild(stick);
		stickNode.setLocalTranslation(0, 0, 14);
		
		recoilNode.attachChild(stickNode);
		highestLevel.setLocalTranslation(25f, 25, -41);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(14.2), rad(-90), rad(0)));
		
		hand = context.loadModel("hand_left.obj", "hands.bmp");
		recoilNode.attachChild(hand);
	}
	
	/**
	 * Moves the hand to a position.
	 *
	 * @param position the hand position
	 */
	private void moveHand(HandPosition position) {
		if (position == HandPosition.DOWN) {
			hand.setLocalTranslation(0, 0, 0);
			hand.setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
		} else {
			hand.setLocalTranslation(0, 2, 0);
			hand.setLocalRotation(new Quaternion().fromAngles(rad(30), 0, 0));
		}
		
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		var stickStatus = Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		//noinspection ConstantConditions
		PercussionInstrument.recoilDrum(recoilNode, stickStatus.justStruck(), stickStatus.justStruck() ? stickStatus.getStrike().velocity : 0, delta);
		
		if (stickStatus.justStruck()) {
			var strike = stickStatus.getStrike();
			assert strike != null;
			moveHand(strike.note == Midi.OPEN_SURDO ? HandPosition.UP : HandPosition.DOWN);
		}
	}
	
	/**
	 * Defines if the hand is on the drum or raised.
	 */
	enum HandPosition {
		/**
		 * Up hand position.
		 */
		UP,
		
		/**
		 * Down hand position.
		 */
		DOWN
	}
}
