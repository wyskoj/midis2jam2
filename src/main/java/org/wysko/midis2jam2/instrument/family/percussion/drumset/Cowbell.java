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

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The Cowbell.
 */
public class Cowbell extends NonDrumSetPercussion {
	
	/**
	 * The stick node.
	 */
	private final Node stickNode = new Node();
	
	/**
	 * Instantiates a new Cowbell.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Cowbell(Midis2jam2 context,
	               List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		recoilNode.attachChild(context.loadModel("CowBell.obj", "MetalTexture.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
		Spatial stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		stick.setLocalTranslation(0, 0, -2);
		stickNode.attachChild(stick);
		stickNode.setLocalTranslation(0, 0, 14);
		
		recoilNode.attachChild(stickNode);
		highestLevel.setLocalTranslation(-9.7f, 40, -99);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(114 - 90), rad(26.7), rad(-3.81)));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		Stick.StickStatus stickStatus = Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE);
		//noinspection ConstantConditions
		PercussionInstrument.recoilDrum(recoilNode, stickStatus.justStruck(), stickStatus.justStruck() ? stickStatus.getStrike().velocity : 0, delta);
	}
}
