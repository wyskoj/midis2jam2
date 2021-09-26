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
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.instrument.family.percussive.Stick.handleStick;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The maracas animate with {@link Stick#handleStick} applied to one maraca, then the rotation is copied to the other
 * maraca.
 */
public class Maracas extends NonDrumSetPercussion {
	
	/**
	 * The left maraca.
	 */
	@NotNull
	private final Spatial leftMaraca;
	
	/**
	 * The right maraca.
	 */
	@NotNull
	private final Spatial rightMaraca;
	
	/**
	 * Instantiates maracas.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Maracas(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Load maracas */
		leftMaraca = context.loadModel("Maraca.obj", "Maraca.bmp");
		rightMaraca = context.loadModel("Maraca.obj", "Maraca.bmp");
		
		/* Create nodes for maracas */
		var leftMaracaNode = new Node();
		leftMaracaNode.attachChild(leftMaraca);
		
		var rightMaracaNode = new Node();
		rightMaracaNode.attachChild(rightMaraca);
		
		/* Tilt maracas */
		leftMaracaNode.setLocalRotation(new Quaternion().fromAngles(0, 0, 0.2F));
		rightMaracaNode.setLocalRotation(new Quaternion().fromAngles(0, 0, -0.2F));
		
		/* Positioning */
		rightMaracaNode.setLocalTranslation(5, -1, 0);
		instrumentNode.setLocalTranslation(-13, 65, -41);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-Stick.MAX_ANGLE / 2), 0, 0));
		
		/* Attach maracas */
		instrumentNode.attachChild(leftMaracaNode);
		instrumentNode.attachChild(rightMaracaNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Animate left maraca */
		var status = handleStick(
				context, leftMaraca, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X
		);
		
		/* Override handleStick culling the left maraca */
		leftMaraca.setCullHint(Spatial.CullHint.Dynamic);
		
		/* Copy rotation to right maraca */
		rightMaraca.setLocalRotation(new Quaternion().fromAngles(status.getRotationAngle(), 0, 0));
		
	}
}
