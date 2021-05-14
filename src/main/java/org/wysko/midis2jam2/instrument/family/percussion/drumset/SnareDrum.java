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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.STRIKE_SPEED;

/**
 * The Snare drum.
 */
public class SnareDrum extends PercussionInstrument {
	
	/**
	 * The list of hits for regular notes.
	 */
	List<MidiNoteOnEvent> regularHits;
	
	/**
	 * The list of hits for side sticks.
	 */
	List<MidiNoteOnEvent> sideHits;
	
	/**
	 * The snare drum.
	 */
	Spatial drum;
	
	/**
	 * Contains the {@link #regularStick}.
	 */
	Node regularStickNode = new Node();
	
	/**
	 * Contains the side stick.
	 */
	Node sideStickNode = new Node();
	
	/**
	 * The stick used for regular hits.
	 */
	private final Spatial regularStick;
	
	/**
	 * Instantiates a new Snare drum.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public SnareDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		this.regularHits = hits.stream().filter(e -> e.note == 40 || e.note == 38).collect(Collectors.toList());
		this.sideHits = hits.stream().filter(e -> e.note == 37).collect(Collectors.toList());
		drum = context.loadModel("DrumSet_SnareDrum.obj", "DrumShell_Snare.bmp");
		regularStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		Spatial sideStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		regularStickNode.attachChild(regularStick);
		sideStickNode.attachChild(sideStick);
		recoilNode.attachChild(drum);
		recoilNode.attachChild(regularStickNode);
		recoilNode.attachChild(sideStickNode);
		highLevelNode.attachChild(recoilNode);
		highLevelNode.move(-10.9f, 16, -72.5f);
		highLevelNode.rotate(rad(10), 0, rad(-10));
		regularStickNode.rotate(0, rad(80), 0);
		regularStickNode.move(10, 0, 3);
		sideStick.setLocalTranslation(0, 0, -2);
		sideStick.setLocalRotation(new Quaternion().fromAngles(0, rad(-20), 0));
		sideStickNode.setLocalTranslation(-1, 0.4f, 6);
	}
	
	@Override
	public void tick(double time, float delta) {
		var regularStickStatus = Stick.handleStick(context, regularStick, time, delta, regularHits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
		var sideStickStatus = Stick.handleStick(context, sideStickNode, time, delta, sideHits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
		
		var regVel = 0;
		var sideVel = 0;
		if (regularStickStatus.justStruck()) {
			assert regularStickStatus.getStrike() != null;
			regVel = regularStickStatus.getStrike().velocity;
		}
		if (sideStickStatus.justStruck()) {
			assert sideStickStatus.getStrike() != null;
			sideVel = (int) (sideStickStatus.getStrike().velocity * 0.5);
		}
		int velocity = Math.max(regVel, sideVel);
		PercussionInstrument.recoilDrum(recoilNode, velocity != 0, velocity, delta);
	}
}
