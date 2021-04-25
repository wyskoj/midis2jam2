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

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Adam Neely would be proud.
 *
 * @see FrettedInstrument
 */
public class BassGuitar extends FrettedInstrument {
	
	
	private final static Vector3f BASE_POSITION = new Vector3f(51.5863f, 54.5902f, -16.5817f);
	
	/**
	 * Instantiates a new Bass guitar.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public BassGuitar(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context,
				new StandardFrettingEngine(4, 22, new int[]{28, 33, 38, 43}, 28, 65),
				events,
				new FrettedInstrumentPositioning(19.5F, -26.57f, new Vector3f[]{
						new Vector3f(1, 1, 1),
						new Vector3f(1, 1, 1),
						new Vector3f(1, 1, 1),
						new Vector3f(1, 1, 1)
					
				},
						new float[]{-0.85f, -0.31f, 0.20f, 0.70f},
						new float[]{-1.86f, -0.85f, 0.34f, 1.37f},
						FretHeightByTable.fromXml(BassGuitar.class)),
				4,
				context.loadModel("Bass.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f)
		);
		
		
		for (int i = 0; i < 4; i++) {
			Spatial string = context.loadModel("BassString.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		final float forward = 0.125f;
		upperStrings[0].setLocalTranslation(positioning.topX[0], positioning.topY, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.24)));
		
		upperStrings[1].setLocalTranslation(positioning.topX[1], positioning.topY, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.673)));
		
		upperStrings[2].setLocalTranslation(positioning.topX[2], positioning.topY, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.17)));
		
		upperStrings[3].setLocalTranslation(positioning.topX[3], positioning.topY, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.824)));
		
		// Lower strings
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 5; j++) {
				lowerStrings[i][j] = context.loadModel("BassStringBottom" + j + ".obj", "BassSkin.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (int i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(positioning.bottomX[0], positioning.bottomY, forward);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.24)));
		}
		for (int i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(positioning.bottomX[1], positioning.bottomY, forward);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.673)));
		}
		for (int i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(positioning.bottomX[2], positioning.bottomY, forward);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.17)));
		}
		
		for (int i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(positioning.bottomX[3], positioning.bottomY, forward);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.824)));
		}
		
		// Hide all wobbly strings
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		// Initialize note fingers
		for (int i = 0; i < 4; i++) {
			noteFingers[i] = context.loadModel("BassNoteFinger.obj", "BassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		// Position guitar
		instrumentNode.setLocalTranslation(BASE_POSITION);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-3.21), rad(-43.5), rad(-29.1)));
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(new Vector3f(7, -2.43f, 0).mult(indexForMoving()));
	}
}
