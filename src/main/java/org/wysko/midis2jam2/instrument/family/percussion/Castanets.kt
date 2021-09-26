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

import static com.jme3.scene.Spatial.CullHint.Dynamic;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.*;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The castanets animate similarly to the {@link HandClap}.
 *
 * @see HandClap
 */
public class Castanets extends NonDrumSetPercussion {
	
	/**
	 * Contains the top castanet.
	 */
	private final Node topCastanetNode = new Node();
	
	/**
	 * Contains the bottom castanet.
	 */
	private final Node bottomCastanetNode = new Node();
	
	/**
	 * Instantiates castanets.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Castanets(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Load castanets */
		Spatial topCastanet = context.loadModel("Castanets.obj", "WoodBleach.bmp");
		Spatial bottomCastanet = context.loadModel("Castanets.obj", "WoodBleach.bmp");
		
		/* Attach to nodes */
		topCastanetNode.attachChild(topCastanet);
		bottomCastanetNode.attachChild(bottomCastanet);
		
		/* Move castanets away from pivot */
		topCastanet.setLocalTranslation(0, 0, -3);
		bottomCastanet.setLocalTranslation(0, 0, -3);
		
		/* Positioning */
		bottomCastanet.setLocalRotation(new Quaternion().fromAngles(0, 0, rad(180)));
		
		instrumentNode.setLocalTranslation(12, 45, -55);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-45), 0));
		instrumentNode.attachChild(topCastanetNode);
		instrumentNode.attachChild(bottomCastanetNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var stickStatus = handleStick(context, topCastanetNode, time, delta, hits, STRIKE_SPEED / 2, MAX_ANGLE / 2, Axis.X);
		topCastanetNode.setCullHint(Dynamic);
		bottomCastanetNode.setLocalRotation(new Quaternion().fromAngles(-stickStatus.getRotationAngle(), 0, 0));
	}
}
