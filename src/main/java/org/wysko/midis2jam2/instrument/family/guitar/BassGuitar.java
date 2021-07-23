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
 * Adam Neely would be proud.
 *
 * @see FrettedInstrument
 */
public class BassGuitar extends FrettedInstrument {
	
	/**
	 * The bass skin texture file.
	 */
	public static final String BASS_SKIN_BMP = "BassSkin.bmp";
	
	/**
	 * The base position of the bass guitar.
	 */
	private static final Vector3f BASE_POSITION = new Vector3f(51.5863F, 54.5902F, -16.5817F);
	
	/**
	 * Instantiates a new Bass guitar.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public BassGuitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events, BassGuitarType type) {
		super(context,
				new StandardFrettingEngine(4, 22, new int[]{28, 33, 38, 43}),
				events,
				new FrettedInstrumentPositioning(19.5F, -26.57F, new Vector3f[]{
						new Vector3f(1, 1, 1),
						new Vector3f(1, 1, 1),
						new Vector3f(1, 1, 1),
						new Vector3f(1, 1, 1)
					
				},
						new float[]{-0.85F, -0.31F, 0.20F, 0.70F},
						new float[]{-1.86F, -0.85F, 0.34F, 1.37F},
						FretHeightByTable.fromXml(BassGuitar.class)),
				4,
				context.loadModel(type.modelFile, type.textureFile)
		);
		
		
		for (var i = 0; i < 4; i++) {
			Spatial string = context.loadModel("BassString.obj", BASS_SKIN_BMP);
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		/* Position each string */
		var forward = 0.125F;
		upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.24)));
		
		upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.673)));
		
		upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.17)));
		
		upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.824)));
		
		/* Lower strings */
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j] = context.loadModel("BassStringBottom" + j + ".obj", BASS_SKIN_BMP,
						Midis2jam2.MatType.UNSHADED, 0.9F);
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		/* Position lower strings */
		for (var i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, forward);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.24)));
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, forward);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.673)));
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, forward);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.17)));
		}
		
		for (var i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, forward);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.824)));
		}
		
		/* Hide all wobbly strings */
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		/* Initialize note fingers */
		for (var i = 0; i < 4; i++) {
			noteFingers[i] = context.loadModel("BassNoteFinger.obj", BASS_SKIN_BMP);
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		/* Position guitar */
		instrumentNode.setLocalTranslation(BASE_POSITION);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-3.21), rad(-43.5), rad(-29.1)));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(new Vector3f(7, -2.43F, 0).mult(indexForMoving(delta)));
	}
	
	public enum BassGuitarType {
		STANDARD("Bass.obj", BASS_SKIN_BMP),
		FRETLESS("BassFretless.fbx", "BassSkinFretless.png");
		
		public final String modelFile;
		
		public final String textureFile;
		
		BassGuitarType(String modelFile, String textureFile) {
			this.modelFile = modelFile;
			this.textureFile = textureFile;
		}
	}
}
