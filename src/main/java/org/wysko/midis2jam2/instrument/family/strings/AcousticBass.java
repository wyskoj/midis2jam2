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
 * The upright bass.
 */
public class AcousticBass extends StringFamilyInstrument {
	
	/**
	 * Instantiates a new Acoustic bass.
	 *
	 * @param context the context
	 * @param events  the events
	 * @param style   the style
	 */
	public AcousticBass(Midis2jam2 context, List<MidiChannelSpecificEvent> events, PlayingStyle style) {
		super(context,
				events,
				style == PlayingStyle.ARCO,
				20,
				new Vector3f(0.75f, 0.75f, 0.75f),
				new int[]{28, 33, 38, 43},
				28,
				91,
				context.loadModel("DoubleBass.obj", "DoubleBassSkin.bmp", Midis2jam2.MatType.UNSHADED, 0)
		);
		
		instrumentNode.setLocalScale(2.5f);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(rad(-15), rad(45), 0));
		
		highestLevel.setLocalTranslation(-50, 46, -95);
		highestLevel.attachChild(instrumentNode);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(new Vector3f(-25f * indexForMoving(delta), 0, 0));
	}
	
	/**
	 * The acoustic bass can be played two ways in MIDI, arco (Contrabass) and pizzicato (Acoustic Bass)
	 */
	public enum PlayingStyle {
		
		/**
		 * Arco playing style.
		 */
		ARCO,
		
		/**
		 * Pizzicato playing style.
		 */
		PIZZICATO
	}
}
