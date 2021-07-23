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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.MatType.REFLECTIVE;
import static org.wysko.midis2jam2.instrument.family.percussion.Triangle.TriangleType.MUTED;
import static org.wysko.midis2jam2.instrument.family.percussion.Triangle.TriangleType.OPEN;
import static org.wysko.midis2jam2.util.Utils.rad;

public class Triangle extends NonDrumSetPercussion {
	
	private final Node triangleNode = new Node();
	
	private final Node beaterNode = new Node();
	
	/**
	 * Instantiates a new triangle.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Triangle(Midis2jam2 context, List<MidiNoteOnEvent> hits, TriangleType type) {
		super(context, hits);
		
		var triangle = context.loadModel(type.modelFile, "ShinySilver.bmp", REFLECTIVE, 0.9f);
		triangleNode.attachChild(triangle);
		
		if (type == MUTED) {
			var hands = context.unshadedMaterial("hands.bmp");
			((Node) triangle).getChild(1).setMaterial(hands);
		}
		
		
		beaterNode.attachChild(context.loadModel("Triangle_Stick.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f));
		
		beaterNode.setLocalTranslation(0, 2, 4);
		
		triangleNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(45)));
		
		instrumentNode.attachChild(triangleNode);
		instrumentNode.attachChild(beaterNode);
		
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-45)));
		
		if (type == OPEN) {
			instrumentNode.setLocalTranslation(-5, 53, -57);
		} else {
			instrumentNode.setLocalTranslation(5, 53, -57);
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		var stickStatus = Stick.handleStick(context, beaterNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		PercussionInstrument.recoilDrum(triangleNode, stickStatus.justStruck(), stickStatus.getStrike() == null ? 0 : stickStatus.getStrike().velocity, delta);
	}
	
	public enum TriangleType {
		OPEN("Triangle.obj"), MUTED("MutedTriangle.fbx");
		
		private final String modelFile;
		
		TriangleType(String modelFile) {
			this.modelFile = modelFile;
		}
	}
}
