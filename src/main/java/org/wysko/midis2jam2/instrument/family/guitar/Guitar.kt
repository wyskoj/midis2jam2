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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The guitar. What more do you want?
 *
 * @see FrettedInstrument
 */
public class Guitar extends FrettedInstrument {
	
	/**
	 * The base position of the guitar.
	 */
	private static final Vector3f BASE_POSITION = new Vector3f(43.431F, 35.292F, 7.063F);
	
	/**
	 * After a while, guitars will begin to clip into the ground. We avoid this by defining after a certain index,
	 * guitars should only move on the XZ plane. This is the index when that alternative transformation applies.
	 */
	private static final int GUITAR_VECTOR_THRESHOLD = 3;
	
	public Guitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events, GuitarType type) {
		super(context,
				new StandardFrettingEngine(6, 22, new int[]{40, 45, 50, 55, 59, 64}),
				events,
				new FrettedInstrumentPositioning(16.6F,
						-18.1F,
						new Vector3f[]{
								new Vector3f(0.8F, 1, 0.8F),
								new Vector3f(0.75F, 1, 0.75F),
								new Vector3f(0.7F, 1, 0.7F),
								new Vector3f(0.77F, 1, 0.77F),
								new Vector3f(0.75F, 1, 0.75F),
								new Vector3f(0.7F, 1, 0.7F),
						},
						new float[]{-0.93F, -0.56F, -0.21F, 0.21F, 0.56F, 0.90F},
						new float[]{-1.55F, -0.92F, -0.35F, 0.25F, 0.82F, 1.45F},
						FretHeightByTable.fromXml(Guitar.class)),
				6,
				context.loadModel(type.modelFileName, type.textureFileName)
		);
		
		
		for (var i = 0; i < 6; i++) {
			Spatial string;
			if (i < 3) {
				string = context.loadModel("GuitarStringLow.obj", type.textureFileName);
			} else {
				string = context.loadModel("GuitarStringHigh.obj", type.textureFileName);
			}
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		var forward = 0.125F;
		upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
		upperStrings[0].setLocalScale(positioning.restingStrings[0]);
		
		upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
		upperStrings[1].setLocalScale(positioning.restingStrings[1]);
		
		upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
		upperStrings[2].setLocalScale(positioning.restingStrings[2]);
		
		upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
		upperStrings[3].setLocalScale(positioning.restingStrings[3]);
		
		upperStrings[4].setLocalTranslation(positioning.upperX[4], positioning.upperY, forward);
		upperStrings[4].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
		upperStrings[4].setLocalScale(positioning.restingStrings[4]);
		
		upperStrings[5].setLocalTranslation(positioning.upperX[5], positioning.upperY, forward);
		upperStrings[5].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
		upperStrings[5].setLocalScale(positioning.restingStrings[5]);
		
		// Lower strings
		for (var i = 0; i < 6; i++) {
			for (var j = 0; j < 5; j++) {
				if (i < 3) {
					lowerStrings[i][j] = context.loadModel("GuitarLowStringBottom" + j + ".obj", type.textureFileName);
				} else {
					lowerStrings[i][j] = context.loadModel("GuitarHighStringBottom" + j + ".obj", type.textureFileName);
				}
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (var i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, forward);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1)));
			lowerStrings[0][i].setLocalScale(positioning.restingStrings[0]);
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, forward);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.62)));
			lowerStrings[1][i].setLocalScale(positioning.restingStrings[0]);
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, forward);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.22)));
			lowerStrings[2][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		for (var i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, forward);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.08)));
			lowerStrings[3][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		for (var i = 0; i < 5; i++) {
			lowerStrings[4][i].setLocalTranslation(positioning.lowerX[4], positioning.lowerY, forward);
			lowerStrings[4][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.45)));
			lowerStrings[4][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		for (var i = 0; i < 5; i++) {
			lowerStrings[5][i].setLocalTranslation(positioning.lowerX[5], positioning.lowerY, forward);
			lowerStrings[5][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.9)));
			lowerStrings[5][i].setLocalScale(positioning.restingStrings[0]);
		}
		
		// Hide all wobbly strings
		for (var i = 0; i < 6; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		
		// Initialize note fingers
		for (var i = 0; i < 6; i++) {
			noteFingers[i] = context.loadModel("GuitarNoteFinger.obj", type.textureFileName);
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		// Position guitar
		instrumentNode.setLocalTranslation(BASE_POSITION);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(2.66), rad(-44.8), rad(-60.3)));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		float v = indexForMoving(delta);
		/* After a certain threshold, stop moving guitars down—only along the XZ plane. */
		if (v < GUITAR_VECTOR_THRESHOLD) {
			offsetNode.setLocalTranslation(new Vector3f(5, -4, 0).mult(indexForMoving(delta)));
		} else {
			Vector3f vector = new Vector3f(5, -4, 0).mult(indexForMoving(delta));
			vector.setY(-4F * GUITAR_VECTOR_THRESHOLD);
			offsetNode.setLocalTranslation(vector);
		}
	}
	
	/**
	 * The type of guitar.
	 */
	@SuppressWarnings("SameParameterValue")
	public enum GuitarType {
		
		/**
		 * Acoustic guitar type.
		 */
		ACOUSTIC("Guitar.obj", "GuitarSkin.bmp"),
		
		/**
		 * Electric guitar type.
		 */
		ELECTRIC("Guitar.obj", "GuitarSkin.bmp");
		
		/**
		 * The Model file name.
		 */
		private final String modelFileName;
		
		/**
		 * The Texture file name.
		 */
		private final String textureFileName;
		
		GuitarType(String modelFileName, String textureFileName) {
			this.modelFileName = modelFileName;
			this.textureFileName = textureFileName;
		}
	}
}
