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
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.*;

/**
 * The shaker.
 */
public class Shaker extends NonDrumSetPercussion {
	
	/**
	 * Contains the shaker.
	 */
	private final Node shakerNode = new Node();
	
	/**
	 * Instantiates a new shaker.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Shaker(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		Spatial shaker = context.loadModel("Shaker.obj", "DarkWood.bmp");
		shaker.setLocalTranslation(0, 0, -3);
		shakerNode.attachChild(shaker);
		
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-25)));
		instrumentNode.setLocalTranslation(13, 45, -42);
		instrumentNode.attachChild(shakerNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		handleStick(context, shakerNode, time, delta, hits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
	}
}
