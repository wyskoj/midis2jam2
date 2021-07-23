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

package org.wysko.midis2jam2.instrument.family.ensemble;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.brass.BouncyTwelfth;
import org.wysko.midis2jam2.instrument.family.brass.WrappedOctaveSustained;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The choir.
 */
public class StageChoir extends WrappedOctaveSustained {
	
	/**
	 * The base position.
	 */
	private static final Vector3f BASE_POSITION = new Vector3f(0, 29.5F, -152.65F);
	
	/**
	 * Instantiates a new choir.
	 *
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public StageChoir(@NotNull Midis2jam2 context,
	                  @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList, true);
		twelfths = new ChoirPeep[12];
		var peepNodes = new Node[12];
		for (var i = 0; i < 12; i++) {
			peepNodes[i] = new Node();
			twelfths[i] = new ChoirPeep();
			peepNodes[i].attachChild(twelfths[i].highestLevel);
			twelfths[i].highestLevel.setLocalTranslation(BASE_POSITION);
			peepNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(11.27 + i * -5.636), 0));
			instrumentNode.attachChild(peepNodes[i]);
		}
		
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		for (TwelfthOfOctave twelfth : twelfths) {
			ChoirPeep peep = (ChoirPeep) twelfth;
			peep.highestLevel.setLocalTranslation(new Vector3f(BASE_POSITION).add(
					new Vector3f(0, 10, -15).mult(indexForMoving(delta))
			));
		}
	}
	
	/**
	 * A single choir peep.
	 */
	public class ChoirPeep extends BouncyTwelfth {
		
		public ChoirPeep() {
			animNode.attachChild(context.loadModel("StageChoir.obj", "ChoirPeep.bmp"));
		}
	}
}
