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

package org.wysko.midis2jam2.instrument.family.strings;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Cello.
 */
public class Cello extends StringFamilyInstrument {
	
	/**
	 * Instantiates a new Cello.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Cello(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context,
				events,
				true,
				20,
				new Vector3f(0.75f, 0.75f, 0.75f),
				new int[]{36, 43, 50, 57},
				36,
				93,
				context.loadModel("Cello.obj", "CelloSkin.bmp")
		);
		
		highestLevel.setLocalTranslation(-69, 39.5f, -49.6f);
		highestLevel.attachChild(instrumentNode);
		
		instrumentNode.setLocalScale(1.86f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-15), rad(45), 0));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(-20f * indexForMoving(delta), 0, 0);
	}
}
