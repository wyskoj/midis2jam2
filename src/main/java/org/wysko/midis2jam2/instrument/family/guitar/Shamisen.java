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

package org.wysko.midis2jam2.instrument.family.guitar;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The shamisen.
 */
public class Shamisen extends FrettedInstrument {
	
	/**
	 * Instantiates a new Shamisen.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Shamisen(@NotNull Midis2jam2 context,
	                @NotNull List<MidiChannelSpecificEvent> events) {
		super(context,
				new StandardFrettingEngine(3, 15, new int[]{50, 57, 62}, 50, 77),
				events,
				new FrettedInstrumentPositioning(38.814f,
						-6.1f,
						new Vector3f[]{Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ, Vector3f.UNIT_XYZ},
						new float[]{-0.5f, 0, 0.5f},
						new float[]{-0.5f, 0, 0.5f},
						fret -> fret * 0.048f // 0 --> 0; 15 --> 0.72
				),
				3,
				context.loadModel("Shamisen.fbx", "ShamisenSkin.png"));
		
		// Load strings
		for (var i = 0; i < 3; i++) {
			upperStrings[i] = context.loadModel("ShamisenString.fbx", "ShamisenSkin.png");
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		var forward = -0.23126f;
		for (var i = 0; i < 3; i++) {
			upperStrings[i].setLocalTranslation(positioning.upperX[i], positioning.upperY, forward);
		}
		
		for (var i = 0; i < 3; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j] = context.loadModel("ShamisenStringBottom%d.fbx".formatted(j), "ShamisenSkin.png");
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		for (var i = 0; i < 3; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j].setLocalTranslation(positioning.lowerX[i], positioning.lowerY, forward);
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		for (var i = 0; i < 3; i++) {
			noteFingers[i] = context.loadModel("GuitarNoteFinger.obj", "ShamisenSkin.png");
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		instrumentNode.setLocalTranslation(56, 43, -23);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-5), rad(-46), rad(-33)));
		
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(new Vector3f(5, -4, 0).mult(indexForMoving(delta)));
	}
}
