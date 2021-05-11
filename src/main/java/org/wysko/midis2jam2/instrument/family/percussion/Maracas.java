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

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.handleStick;

public class Maracas extends NonDrumSetPercussion {
	
	private final Node leftMaracaNode = new Node();
	
	private final Node rightMaracaNode = new Node();
	
	private final Spatial leftMaraca;
	
	private final Spatial rightMaraca;
	
	/**
	 * Instantiates maracas.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Maracas(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		leftMaraca = context.loadModel("Maraca.obj", "Maraca.bmp");
		rightMaraca = leftMaraca.clone();
		
		leftMaracaNode.attachChild(leftMaraca);
		rightMaracaNode.attachChild(rightMaraca);
		
		/* Tilt maracas */
		leftMaracaNode.setLocalRotation(new Quaternion().fromAngles(0, 0, 0.2f));
		rightMaracaNode.setLocalRotation(new Quaternion().fromAngles(0, 0, -0.2f));
		
		rightMaracaNode.setLocalTranslation(5, -1, 0);
		
		instrumentNode.setLocalTranslation(-13, 65, -41);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-Stick.MAX_ANGLE / 2), 0, 0));
		instrumentNode.attachChild(leftMaracaNode);
		instrumentNode.attachChild(rightMaracaNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var status = handleStick(context, leftMaraca, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		leftMaraca.setCullHint(Spatial.CullHint.Dynamic);
		rightMaraca.setLocalRotation(new Quaternion().fromAngles(status.getRotationAngle(), 0, 0));
		
	}
}
