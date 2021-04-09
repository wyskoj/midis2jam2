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

package org.wysko.midis2jam2.instrument.family.brass;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The stage horns are positioned back and to the left. There are 12 of them for each note in the octave. Stage horns
 * are bouncy.
 *
 * @see BouncyTwelfth
 */
public class StageHorns extends WrappedOctaveSustained {
	
	/**
	 * The base position of a horn.
	 */
	private static final Vector3f BASE_POSITION = new Vector3f(0, 29.5f, -152.65f);
	
	/**
	 * Contains each horn.
	 */
	final Node[] hornNodes = new Node[12];
	
	/**
	 * Instantiates new stage horns.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public StageHorns(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList, false);
		
		twelfths = new StageHornNote[12];
		for (int i = 0; i < 12; i++) {
			hornNodes[i] = new Node();
			twelfths[i] = new StageHornNote();
			hornNodes[i].attachChild(twelfths[i].highestLevel);
			twelfths[i].highestLevel.setLocalTranslation(BASE_POSITION);
			hornNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(16 + i * 1.5), 0));
			instrumentNode.attachChild(hornNodes[i]);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		for (TwelfthOfOctave twelfth : twelfths) {
			StageHornNote horn = (StageHornNote) twelfth;
			horn.highestLevel.setLocalTranslation(new Vector3f(BASE_POSITION).add(
					new Vector3f(0, 0, -5).mult(indexForMoving())
			));
		}
	}
	
	/**
	 * A single horn.
	 */
	public class StageHornNote extends BouncyTwelfth {
		
		/**
		 * Instantiates a new stage horn note.
		 */
		public StageHornNote() {
			super();
			// Load horn
			animNode.attachChild(context.loadModel("StageHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
		}
	}
}
