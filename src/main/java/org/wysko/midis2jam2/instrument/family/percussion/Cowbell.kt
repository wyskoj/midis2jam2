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

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.util.MatType;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The cowbell. Simply animates with {@link Stick#handleStick} and {@link PercussionInstrument#recoilDrum}.
 */
public class Cowbell extends NonDrumSetPercussion {
	
	/**
	 * Contains the stick.
	 */
	private final Node stickNode = new Node();
	
	/**
	 * Instantiates a new cowbell.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	public Cowbell(Midis2jam2 context,
	               List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		/* Load cowbell */
		recoilNode.attachChild(context.loadModel("CowBell.obj", "MetalTexture.bmp", MatType.REFLECTIVE, 0.9F));
		
		/* Load and position stick */
		Spatial stick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		stick.setLocalTranslation(0, 0, -2);
		stickNode.attachChild(stick);
		stickNode.setLocalTranslation(0, 0, 14);
		
		/* Positioning */
		recoilNode.attachChild(stickNode);
		highestLevel.setLocalTranslation(-9.7F, 40, -99);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(24), rad(26.7), rad(-3.81)));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		/* Animate stick */
		var stickStatus = Stick.handleStick(context, stickNode, time, delta, hits, Stick.STRIKE_SPEED,
				Stick.MAX_ANGLE, Axis.X);
		
		/* Animate cowbell */
		PercussionInstrument.recoilDrum(recoilNode, stickStatus.justStruck(),
				stickStatus.justStruck() ? requireNonNull(stickStatus.getStrike()).getVelocity() : 0, delta);
	}
}
