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

import static org.wysko.midis2jam2.instrument.family.percussive.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.STRIKE_SPEED;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * Anything that is hit with a stick.
 */
public abstract class SingleStickInstrument extends PercussionInstrument {
	
	/**
	 * The Stick.
	 */
	protected final Spatial stick;
	
	/**
	 * The Stick node.
	 */
	protected final Node stickNode = new Node();
	
	protected SingleStickInstrument(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		stickNode.attachChild(stick);
		highLevelNode.attachChild(stickNode);
		stick.setLocalRotation(new Quaternion().fromAngles(rad(MAX_ANGLE), 0, 0));
	}
	
	/**
	 * Handles the animation of the stick.
	 *
	 * @param time  the current time
	 * @param delta the amount of time since the last frame update
	 * @param hits  the running list of hits
	 */
	Stick.StickStatus handleStick(double time, float delta, List<MidiNoteOnEvent> hits) {
		return Stick.handleStick(context, stick, time, delta, hits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
	}
}
