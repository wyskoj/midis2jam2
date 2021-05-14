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

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class SquareClick extends NonDrumSetPercussion {
	
	private final Node scNode = new Node();
	
	private final Node stickNode = new Node();
	
	/**
	 * Instantiates a new non drum set percussion.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected SquareClick(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		stickNode.attachChild(context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp"));
		var child = context.loadModel("SquareShaker.obj", "Wood.bmp");
		child.setLocalTranslation(0, -2, -2);
		scNode.attachChild(child);
		
		instrumentNode.attachChild(stickNode);
		instrumentNode.attachChild(scNode);
		
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-90), rad(-90), rad(-135)));
		instrumentNode.setLocalTranslation(-42, 44, -79);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var stickStatus = Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		stickNode.setCullHint(Spatial.CullHint.Dynamic);
		scNode.setLocalRotation(new Quaternion().fromAngles(-stickStatus.getRotationAngle(), 0, 0));
		
	}
}
