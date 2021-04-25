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

package org.wysko.midis2jam2.instrument.family.organ;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.NotePeriod;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Numbers 1-4.
 */
public class Harmonica extends SustainedInstrument {
	
	/**
	 * Each note on the harmonica has a separate puffer.
	 */
	final SteamPuffer[] puffers = new SteamPuffer[12];
	
	/**
	 * Contains each puffer.
	 */
	final Node[] pufferNodes = new Node[12];
	
	/**
	 * For each note, true if it is playing, false otherwise.
	 */
	final boolean[] eachNotePlaying = new boolean[12];
	
	public Harmonica(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		instrumentNode.attachChild(context.loadModel("Harmonica.obj", "Harmonica.bmp"));
		
		for (var i = 0; i < 12; i++) {
			pufferNodes[i] = new Node();
			puffers[i] = new SteamPuffer(context, SteamPuffer.SteamPuffType.HARMONICA, 0.75);
			puffers[i].steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-90), 0));
			puffers[i].steamPuffNode.setLocalTranslation(0, 0, 7.2f);
			pufferNodes[i].attachChild(puffers[i].steamPuffNode);
			instrumentNode.attachChild(pufferNodes[i]);
			pufferNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(5 * (i - 5.5)), 0));
		}
		
		// Position harmonica
		instrumentNode.setLocalTranslation(74, 32, -38);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-90), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		Arrays.fill(eachNotePlaying, false);
		
		for (NotePeriod currentNotePeriod : currentNotePeriods) {
			int i = currentNotePeriod.midiNote % 12;
			eachNotePlaying[i] = true;
		}
		
		IntStream.range(0, puffers.length).forEach(i -> puffers[i].tick(delta, eachNotePlaying[i]));
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10f * indexForMoving(), 0);
	}
}
