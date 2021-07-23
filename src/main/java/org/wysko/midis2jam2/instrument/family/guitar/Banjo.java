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
 * @see FrettedInstrument
 */
public class Banjo extends FrettedInstrument {
	
	
	private static final Vector3f BASE_POSITION = new Vector3f(54.6F, 48.7F, 2);
	
	/**
	 * Instantiates a new Bass guitar.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Banjo(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context,
				new StandardFrettingEngine(4, 17, new int[]{48, 55, 62, 69}),
				events,
				new FrettedInstrumentPositioning(
						13.93F,
						-19.54F,
						new Vector3f[]{
								new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1)
						},
						new float[]{-0.53F, -0.13F, 0.28F, 0.68F},
						new float[]{-1.14F, -0.40F, 0.47F, 1.21F},
						FretHeightByTable.fromXml(Banjo.class)),
				4,
				context.loadModel("Banjo.fbx", "BanjoSkin.png")
		);
		
		
		for (var i = 0; i < 4; i++) {
			Spatial string = context.loadModel("BanjoString.fbx", "BassSkin.bmp");
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		// Position each string
		var forward = 0;
		upperStrings[0].setLocalTranslation(positioning.upperX[0], positioning.upperY, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.03)));
		
		upperStrings[1].setLocalTranslation(positioning.upperX[1], positioning.upperY, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.47)));
		
		upperStrings[2].setLocalTranslation(positioning.upperX[2], positioning.upperY, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.33)));
		
		upperStrings[3].setLocalTranslation(positioning.upperX[3], positioning.upperY, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.92)));
		
		// Lower strings
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j] = context.loadModel("BanjoStringBottom%d.fbx".formatted(j), "BassSkin.bmp");
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		// Position lower strings
		for (var i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(positioning.lowerX[0], positioning.lowerY, forward);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-1.04)));
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(positioning.lowerX[1], positioning.lowerY, forward);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-0.46)));
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(positioning.lowerX[2], positioning.lowerY, forward);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.33)));
		}
		
		for (var i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(positioning.lowerX[3], positioning.lowerY, forward);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(0.91)));
		}
		
		// Hide all wobbly strings
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		// Initialize note fingers
		for (var i = 0; i < 4; i++) {
			noteFingers[i] = context.loadModel("BassNoteFinger.obj", "BanjoSkin.png");
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
		}
		
		// Position guitar
		instrumentNode.setLocalTranslation(BASE_POSITION);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(0.8), rad(-43.3), rad(-40.5)));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(new Vector3f(7, -2.43F, 0).mult(indexForMoving(delta)));
	}
}
