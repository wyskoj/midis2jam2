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

package org.wysko.midis2jam2.instrument.family.percussion.drumset;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Snare drum.
 */
public class SnareDrum extends StickDrum {
	
	/**
	 * Instantiates a new Snare drum.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public SnareDrum(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		drum = context.loadModel("DrumSet_SnareDrum.obj", "DrumShell_Snare.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		recoilNode.attachChild(drum);
		recoilNode.attachChild(stickNode);
		highLevelNode.attachChild(recoilNode);
		highLevelNode.move(-10.9f, 16, -72.5f);
		highLevelNode.rotate(rad(10), 0, rad(-10));
		stickNode.rotate(0, rad(80), 0);
		stickNode.move(10, 0, 3);
	}
	
	@Override
	public void tick(double time, float delta) {
		drumRecoil(time, delta);
		handleStick(time, delta, hits);
	}
}
