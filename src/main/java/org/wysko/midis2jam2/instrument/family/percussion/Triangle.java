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

import static org.wysko.midis2jam2.instrument.family.percussion.Triangle.TriangleType.MUTED;
import static org.wysko.midis2jam2.instrument.family.percussion.Triangle.TriangleType.OPEN;
import static org.wysko.midis2jam2.util.MatType.REFLECTIVE;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The triangle.
 */
public class Triangle extends NonDrumSetPercussion {
	
	/**
	 * The Triangle node.
	 */
	private final Node triangleNode = new Node();
	
	/**
	 * The Beater node.
	 */
	private final Node beaterNode = new Node();
	
	/**
	 * Instantiates a new triangle.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Triangle(Midis2jam2 context, List<MidiNoteOnEvent> hits, TriangleType type) {
		super(context, hits);
		
		/* Load triangle */
		var triangle = context.loadModel(type.modelFile, "ShinySilver.bmp", REFLECTIVE, 0.9F);
		triangleNode.attachChild(triangle);
		
		/* Fix material if a muted triangle */
		if (type == MUTED) {
			var hands = context.unshadedMaterial("hands.bmp");
			((Node) triangle).getChild(1).setMaterial(hands);
		}
		
		/* Load beater */
		beaterNode.attachChild(context.loadModel("Triangle_Stick.obj", "ShinySilver.bmp", REFLECTIVE, 0.9F));
		beaterNode.setLocalTranslation(0, 2, 4);
		
		/* Attach nodes and position */
		instrumentNode.attachChild(triangleNode);
		instrumentNode.attachChild(beaterNode);
		
		triangleNode.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(45)));
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
	
	/**
	 * The type of triangle.
	 */
	public enum TriangleType {
		
		/**
		 * Open triangle type.
		 */
		OPEN("Triangle.obj"),
		
		/**
		 * Muted triangle type.
		 */
		MUTED("MutedTriangle.fbx");
		
		/**
		 * The file name of the model.
		 */
		private final String modelFile;
		
		/**
		 * Instantiates a new triangle type.
		 *
		 * @param modelFile the file name of the model
		 */
		TriangleType(String modelFile) {
			this.modelFile = modelFile;
		}
	}
}
