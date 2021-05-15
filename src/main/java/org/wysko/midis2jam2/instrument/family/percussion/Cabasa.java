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
 * The cabasa.
 */
public class Cabasa extends NonDrumSetPercussion {
	
	/**
	 * Contains the cabasa.
	 */
	private final Node cabasaNode = new Node();
	
	private final Spatial cabasaModel;
	
	/**
	 * Instantiates a new cabasa.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Cabasa(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		cabasaModel = context.loadModel("Cabasa.obj", "Cabasa.bmp");
		cabasaModel.setLocalTranslation(0, 0, -3);
		cabasaNode.attachChild(cabasaModel);
		
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(45)));
		instrumentNode.setLocalTranslation(-10, 48, -50);
		instrumentNode.attachChild(cabasaNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		var stickStatus = handleStick(context, cabasaNode, time, delta, hits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
		cabasaModel.setLocalRotation(new Quaternion().fromAngles(0, stickStatus.getRotationAngle(), 0));
	}
}
