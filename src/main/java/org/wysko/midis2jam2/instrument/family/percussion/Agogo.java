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

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.handleStick;
import static org.wysko.midis2jam2.midi.Midi.HIGH_AGOGO;
import static org.wysko.midis2jam2.midi.Midi.LOW_AGOGO;

/**
 * The agogo.
 */
public class Agogo extends NonDrumSetPercussion {
	
	/**
	 * The left stick.
	 */
	@NotNull
	private final Spatial leftStick;
	
	/**
	 * The right stick.
	 */
	@NotNull
	private final Spatial rightStick;
	
	/**
	 * The hits for the high agogo.
	 */
	@NotNull
	private final List<MidiNoteOnEvent> highHits;
	
	/**
	 * The hits for the low agogo.
	 */
	@NotNull
	private final List<MidiNoteOnEvent> lowHits;
	
	/**
	 * Instantiates a new agogo.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected Agogo(@NotNull Midis2jam2 context, @NotNull List<MidiNoteOnEvent> hits) {
		super(context, hits);
		highHits = hits.stream().filter(a -> a.note == HIGH_AGOGO).collect(Collectors.toList());
		lowHits = hits.stream().filter(a -> a.note == LOW_AGOGO).collect(Collectors.toList());
		
		leftStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		rightStick = context.loadModel("DrumSet_Stick.obj", "StickSkin.bmp");
		
		recoilNode.attachChild(leftStick);
		recoilNode.attachChild(rightStick);
		
		leftStick.setLocalTranslation(3, 0, 13);
		rightStick.setLocalTranslation(10, 0, 11);
		
		recoilNode.attachChild(context.loadModel("Agogo.obj", "HornSkinGrey.bmp")); // sic
		
		instrumentNode.setLocalTranslation(-5, 50, -85);
		instrumentNode.attachChild(recoilNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		
		var leftStatus = handleStick(context, leftStick, time, delta, highHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		var rightStatus = handleStick(context, rightStick, time, delta, lowHits, Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
		
		var velocity = 0;
		if (leftStatus.getStrike() != null) {
			velocity = max(velocity, leftStatus.getStrike().velocity);
		}
		if (rightStatus.getStrike() != null) {
			velocity = max(velocity, rightStatus.getStrike().velocity);
		}
		
		recoilDrum(recoilNode, leftStatus.justStruck() || rightStatus.justStruck(), velocity, delta);
	}
}
