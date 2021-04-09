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

package org.wysko.midis2jam2.instrument.family.chromaticpercussion;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.MatType.REFLECTIVE;

public class MusicBox extends DecayedInstrument {
	
	final OneMusicBoxNote[] notes = new OneMusicBoxNote[12];
	
	final Spatial cylinder;
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public MusicBox(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		for (int i = 0; i < 12; i++) {
			notes[i] = new OneMusicBoxNote(i);
			instrumentNode.attachChild(notes[i].highestLevel);
		}
		
		instrumentNode.attachChild(context.loadModel("MusicBoxCase.obj", "Wood.bmp"));
		instrumentNode.attachChild(context.loadModel("MusicBoxTopBlade.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f));
		cylinder = context.loadModel("MusicBoxSpindle.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
		instrumentNode.attachChild(cylinder);
		instrumentNode.setLocalTranslation(0, 10, 0);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
	}
	
	@Override
	protected void moveForMultiChannel() {
	
	}
	
	public class OneMusicBoxNote extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		public OneMusicBoxNote(int i) {
			Spatial key = context.loadModel("MusicBoxKey.obj", "ShinySilver.bmp", REFLECTIVE, 0.9f);
			highestLevel.attachChild(key);
			key.setLocalTranslation(0, 7, 0);
		}
		
		@Override
		public void tick(double time, float delta) {
		
		}
	}
}
