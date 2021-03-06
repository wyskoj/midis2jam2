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
import static org.wysko.midis2jam2.midi.Midi.HIGH_BONGO;
import static org.wysko.midis2jam2.midi.Midi.LOW_BONGO;

/**
 * The bongos.
 */
public class Bongos extends NonDrumSetPercussion {
	
	/**
	 * The low bongo.
	 */
	final Spatial lowBongo;
	
	/**
	 * The high bongo.
	 */
	final Spatial highBongo;
	
	private final List<MidiNoteOnEvent> lowBongoHits;
	
	private final List<MidiNoteOnEvent> highBongoHits;
	
	/**
	 * The Right hand node.
	 */
	private final Node highStickNode = new Node();
	
	/**
	 * The Left hand node.
	 */
	private final Node lowStickNode = new Node();
	
	/**
	 * The Left bongo anim node.
	 */
	private final Node lowBongoAnimNode = new Node();
	
	/**
	 * The Right bongo anim node.
	 */
	private final Node highBongoAnimNode = new Node();
	
	/**
	 * Instantiates bongos.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Bongos(Midis2jam2 context,
	              List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		lowBongoHits = hits.stream().filter(h -> h.note == LOW_BONGO).collect(Collectors.toList());
		highBongoHits = hits.stream().filter(h -> h.note == HIGH_BONGO).collect(Collectors.toList());
		
		lowBongo = context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp");
		highBongo = context.loadModel("DrumSet_Bongo.obj", "DrumShell_Bongo.bmp");
		highBongo.setLocalScale(0.9f);
		
		lowBongoAnimNode.attachChild(lowBongo);
		highBongoAnimNode.attachChild(highBongo);
		
		var lowBongoNode = new Node();
		lowBongoNode.attachChild(lowBongoAnimNode);
		var highBongoNode = new Node();
		highBongoNode.attachChild(highBongoAnimNode);
		
		instrumentNode.attachChild(lowBongoNode);
		instrumentNode.attachChild(highBongoNode);
		
		lowStickNode.attachChild(context.loadModel("hand_right.obj", "hands.bmp"));
		highStickNode.attachChild(context.loadModel("hand_left.obj", "hands.bmp"));
		
		lowBongoAnimNode.attachChild(lowStickNode);
		highBongoAnimNode.attachChild(highStickNode);
		
		lowBongoNode.setLocalTranslation(-35.88f, 40.4f, -62.6f);
		lowBongoNode.setLocalRotation(new Quaternion().fromAngles(rad(32.7), rad(61.2), rad(-3.6)));
		
		highBongoNode.setLocalTranslation(-38.3f, 40.2f, -54.5f);
		highBongoNode.setLocalRotation(new Quaternion().fromAngles(rad(32.9), rad(68.1), rad(-0.86)));
		
		lowStickNode.setLocalTranslation(0, 0, 5);
		highStickNode.setLocalTranslation(0, 0, 5);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Stick.StickStatus statusLow = Stick.handleStick(context, lowStickNode, time, delta, lowBongoHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		Stick.StickStatus statusHigh = Stick.handleStick(context, highStickNode, time, delta, highBongoHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		if (statusLow.justStruck()) {
			MidiNoteOnEvent strike = statusLow.getStrike();
			assert strike != null;
			recoilDrum(lowBongoAnimNode, true, strike.velocity, delta);
		} else {
			recoilDrum(lowBongoAnimNode, false, 0, delta);
		}
		
		if (statusHigh.justStruck()) {
			MidiNoteOnEvent strike = statusHigh.getStrike();
			assert strike != null;
			recoilDrum(highBongoAnimNode, true, strike.velocity, delta);
		} else {
			recoilDrum(highBongoAnimNode, false, 0, delta);
		}
	}
}
