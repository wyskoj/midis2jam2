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

package org.wysko.midis2jam2.instrument.family.percussion;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The agogo.
 */
public class Agogo extends NonDrumSetPercussion {
	
	private final Spatial leftStick;
	
	private final Spatial rightStick;
	
	private final Node recoilNode = new Node();
	
	private final List<MidiNoteOnEvent> highHits;
	
	private final List<MidiNoteOnEvent> lowHits;
	
	/**
	 * Instantiates a new agogo.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Agogo(Midis2jam2 context, List<MidiNoteOnEvent> hits) {
		super(context, hits);
		highHits = hits.stream().filter(a -> a.note == 67).collect(Collectors.toList());
		lowHits = hits.stream().filter(a -> a.note == 68).collect(Collectors.toList());
		leftStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		rightStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		
		recoilNode.attachChild(leftStick);
		recoilNode.attachChild(rightStick);
		recoilNode.attachChild(context.loadModel("Agogo.obj", "HornSkinGrey.bmp")); // sic
		
		instrumentNode.attachChild(recoilNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
	}
}
