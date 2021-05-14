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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.guitar.FrettedInstrument;
import org.wysko.midis2jam2.instrument.family.guitar.StandardFrettingEngine;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;


/**
 * The type String family instrument, including the violin, viola, cello, and double bass.
 *
 * @see Violin
 * @see Viola
 * @see Cello
 * @see AcousticBass
 */
public abstract class StringFamilyInstrument extends FrettedInstrument {
	
	/**
	 * The bow of this string instrument.
	 */
	protected final Spatial bow;
	
	/**
	 * The Bow node.
	 */
	protected final Node bowNode = new Node();
	
	/**
	 * True if the bow is going left, false if the bow is going right.
	 */
	boolean bowGoesLeft = false;
	
	/**
	 * Instantiates a new String family instrument.
	 *
	 * @param context             the context
	 * @param events              the events
	 * @param showBow             true if the bow should be seen, false otherwise
	 * @param bowRotation         the bow rotation
	 * @param bowScale            the bow scale
	 * @param openStringMidiNotes the midi notes of each string in open position
	 * @param rangeLow            the lowest note that can be played
	 * @param rangeHigh           the highest note that can be played
	 * @param body                the body of the instrument
	 */
	protected StringFamilyInstrument(Midis2jam2 context,
	                                 List<MidiChannelSpecificEvent> events,
	                                 boolean showBow,
	                                 double bowRotation,
	                                 Vector3f bowScale,
	                                 int[] openStringMidiNotes,
	                                 int rangeLow,
	                                 int rangeHigh,
	                                 Spatial body) {
		super(context,
				new StandardFrettingEngine(
						4, 48, openStringMidiNotes, rangeLow, rangeHigh),
				events,
				new FrettedInstrumentPositioning.FrettedInstrumentPositioningWithZ(8.84f,
						-6.17f,
						new Vector3f[] {
								new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1)
						},
						new float[] {-0.369f, -0.122f, 0.126f, 0.364f},
						new float[] {-0.8f, -0.3f, 0.3f, 0.8f},
						fret -> 1 - ((float) (((0.0003041886 * Math.pow(fret, 2)) + (-0.0312677 * fret)) + 1)),
						new float[] {-0.6f, -0.6f, -0.6f, -0.6f},
						new float[] {
								0.47f,
								0.58f,
								0.58f,
								0.47f,
						}),
				4,
				body
		);
		
		body.setLocalTranslation(0, 0, -1.2f);
		instrumentNode.attachChild(body);
		bow = context.loadModel("ViolinBow.obj", "ViolinSkin.bmp");
		bowNode.attachChild(bow);
		instrumentNode.attachChild(bowNode);
		bowNode.setLocalScale(bowScale);
		bowNode.setLocalTranslation(0, -4, 1f);
		bowNode.setLocalRotation(new Quaternion().fromAngles(rad(180), rad(180), rad(bowRotation)));
		
		if (!showBow) {
			bowNode.setCullHint(Spatial.CullHint.Always);
		}
		
		for (var i = 0; i < 4; i++) {
			Spatial string;
			string = context.loadModel("ViolinString.obj", "ViolinSkin.bmp");
			upperStrings[i] = string;
			instrumentNode.attachChild(upperStrings[i]);
		}
		
		positionUpperStrings();
		loadLowerStrings();
		loadNoteFingers();
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		animateBow(delta);
	}
	
	@Override
	protected boolean handleStrings(double time, float delta) {
		boolean b = super.handleStrings(time, delta);
		if (b) {
			/* Reverse bow direction */
			bowGoesLeft = !bowGoesLeft;
		}
		return b;
	}
	
	/**
	 * Position upper strings.
	 */
	protected void positionUpperStrings() {
		final float forward = -0.6f;
		upperStrings[0].setLocalTranslation(positioning.topX[0], positioning.topY, forward);
		upperStrings[0].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(-1.63)));
		
		upperStrings[1].setLocalTranslation(positioning.topX[1], positioning.topY, forward);
		upperStrings[1].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(-0.685)));
		
		upperStrings[2].setLocalTranslation(positioning.topX[2], positioning.topY, forward);
		upperStrings[2].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(0.667)));
		
		upperStrings[3].setLocalTranslation(positioning.topX[3], positioning.topY, forward);
		upperStrings[3].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(1.69)));
	}
	
	/**
	 * Load lower strings.
	 */
	protected void loadLowerStrings() {
		// Lower strings
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j] = context.loadModel("ViolinStringPlayed" + j + ".obj", "DoubleBassSkin.bmp",
						Midis2jam2.MatType.UNSHADED, 0.9f);
				instrumentNode.attachChild(lowerStrings[i][j]);
			}
		}
		
		
		// Position lower strings
		for (var i = 0; i < 5; i++) {
			lowerStrings[0][i].setLocalTranslation(positioning.bottomX[0], positioning.bottomY, 0.47f);
			lowerStrings[0][i].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(-1.61)));
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[1][i].setLocalTranslation(positioning.bottomX[1], positioning.bottomY, 0.58f);
			lowerStrings[1][i].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(-0.663)));
		}
		for (var i = 0; i < 5; i++) {
			lowerStrings[2][i].setLocalTranslation(positioning.bottomX[2], positioning.bottomY, 0.58f);
			lowerStrings[2][i].setLocalRotation(new Quaternion().fromAngles(rad(-4.6), 0, rad(0.647)));
		}
		
		for (var i = 0; i < 5; i++) {
			lowerStrings[3][i].setLocalTranslation(positioning.bottomX[3], positioning.bottomY, 0.47f);
			lowerStrings[3][i].setLocalRotation(new Quaternion().fromAngles(rad(-4), 0, rad(1.65)));
		}
		
		// Hide all wobbly strings
		for (var i = 0; i < 4; i++) {
			for (var j = 0; j < 5; j++) {
				lowerStrings[i][j].setCullHint(Spatial.CullHint.Always);
			}
		}
	}
	
	/**
	 * Loads the note fingers.
	 */
	protected void loadNoteFingers() {
		// Initialize note fingers
		for (var i = 0; i < 4; i++) {
			noteFingers[i] = context.loadModel("BassNoteFinger.obj", "BassSkin.bmp");
			instrumentNode.attachChild(noteFingers[i]);
			noteFingers[i].setCullHint(Spatial.CullHint.Always);
			noteFingers[i].setLocalScale(0.75f);
		}
	}
	
	/**
	 * Animates the movement of the bow.
	 *
	 * @param delta time since the last frame
	 */
	protected void animateBow(float delta) {
		if (!currentNotePeriods.isEmpty()) {
			bowNode.setLocalTranslation(0, -4, 0.5f);
			if (bowGoesLeft) {
				bow.move(-3 * delta, 0, 0);
			} else {
				bow.move(3 * delta, 0, 0);
			}
			if (bow.getLocalTranslation().x > 7) {
				bow.setLocalTranslation(7, 0, 0);
				bowGoesLeft = true;
			}
			if (bow.getLocalTranslation().x < -7) {
				bow.setLocalTranslation(-7, 0, 0);
				bowGoesLeft = false;
			}
		} else {
			Vector3f pos = bowNode.getLocalTranslation();
			if (pos.z < 1) {
				bowNode.setLocalTranslation(pos.setZ(pos.z + 1 * delta));
			}
		}
	}
	
}
