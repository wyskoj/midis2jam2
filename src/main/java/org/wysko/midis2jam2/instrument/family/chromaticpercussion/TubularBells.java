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

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.util.MatType;
import org.wysko.midis2jam2.world.Axis;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The tubular bells.
 */
public class TubularBells extends DecayedInstrument {
	
	/**
	 * Each of the twelve bells.
	 */
	@NotNull
	private final Bell[] bells = new Bell[12];
	
	/**
	 * Contains the list of strikes for each of the 12 bells.
	 */
	@NotNull
	@SuppressWarnings("unchecked")
	private final List<MidiNoteOnEvent>[] bellStrikes = new ArrayList[12];
	
	public TubularBells(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> events) {
		super(context, events);
		
		for (var i = 0; i < 12; i++) {
			bells[i] = new Bell(i);
			instrumentNode.attachChild(bells[i].highestLevel);
		}
		
		// Hide the bright ones
		for (var i = 0; i < 12; i++) {
			bells[i].bellNode.getChild(0).setCullHint(Spatial.CullHint.Always);
			bellStrikes[i] = new ArrayList<>();
		}
		
		for (MidiNoteOnEvent event : hits) {
			int midiNote = event.note;
			int bellNumber = (midiNote + 3) % 12;
			bellStrikes[bellNumber].add(event);
		}
		
		instrumentNode.setLocalTranslation(-65, 100, -130);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(25), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		for (int i = 0, barsLength = 12; i < barsLength; i++) { // For each bar on the instrument
			bells[i].tick(delta);
			var stickStatus = Stick.handleStick(context, bells[i].malletNode, time, delta,
					bellStrikes[i], Stick.STRIKE_SPEED, Stick.MAX_ANGLE, Axis.X);
			if (stickStatus.justStruck() && stickStatus.getStrike() != null) {
				bells[i].recoilBell(stickStatus.getStrike().velocity);
			}
		}
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(-10F * indexForMoving(delta), 0, -10F * indexForMoving(delta));
	}
	
	/**
	 * A single bell.
	 */
	private class Bell {
		
		/**
		 * The base amplitude of the strike.
		 */
		private static final double BASE_AMPLITUDE = 0.5;
		
		/**
		 * The speed the bell will wobble at.
		 */
		private static final int WOBBLE_SPEED = 3;
		
		/**
		 * How quickly the bell will return to rest.
		 */
		private static final double DAMPENING = 0.3;
		
		/**
		 * The highest level node.
		 */
		@NotNull
		private final Node highestLevel = new Node();
		
		/**
		 * Contains the tubular bell.
		 */
		@NotNull
		private final Node bellNode = new Node();
		
		/**
		 * Contains the mallet.
		 */
		@NotNull
		private final Node malletNode;
		
		/**
		 * The current amplitude of the recoil.
		 */
		private double amplitude = 0.5;
		
		/**
		 * The current time of animation, or -1 if the animation has never started yet.
		 */
		private double animTime = -1;
		
		/**
		 * True if this bell is recoiling, false if not.
		 */
		private boolean bellIsRecoiling;
		
		public Bell(int i) {
			bellNode.attachChild(context.loadModel("TubularBell.obj", "ShinySilver.bmp",
					MatType.REFLECTIVE, 0.9F));
			
			bellNode.attachChild(context.loadModel("TubularBellDark.obj", "ShinySilver.bmp",
					MatType.REFLECTIVE, 0.5F));
			
			highestLevel.attachChild(bellNode);
			bellNode.setLocalTranslation((i - 5) * 4f, 0, 0);
			bellNode.setLocalScale((float) (-0.04545 * i) + 1);
			
			malletNode = new Node();
			Spatial child = context.loadModel("TubularBellMallet.obj", "Wood.bmp");
			child.setLocalTranslation(0, 5, 0);
			malletNode.attachChild(child);
			malletNode.setLocalTranslation((i - 5) * 4f, -25, 4);
			highestLevel.attachChild(malletNode);
			malletNode.setCullHint(Spatial.CullHint.Always);
		}
		
		/**
		 * Updates animation.
		 *
		 * @param delta the amount of time since the last frame update
		 */
		public void tick(float delta) {
			animTime += delta;
			if (bellIsRecoiling) {
				bellNode.getChild(0).setCullHint(Spatial.CullHint.Dynamic); // Show bright
				bellNode.getChild(1).setCullHint(Spatial.CullHint.Always); // Hide dark
				bellNode.setLocalRotation(new Quaternion().fromAngles(rotationAmount(), 0, 0));
			} else {
				bellNode.getChild(0).setCullHint(Spatial.CullHint.Always); // Hide bright
				bellNode.getChild(1).setCullHint(Spatial.CullHint.Dynamic); // Show dark
			}
		}
		
		/**
		 * Calculates the rotation during the recoil.
		 *
		 * @return the rotation amount
		 */
		float rotationAmount() {
			if (animTime >= 0) {
				if (animTime < 2)
					return (float) (amplitude * (Math.sin(animTime * WOBBLE_SPEED * FastMath.PI) / (3 + Math.pow(animTime, 3) * WOBBLE_SPEED * DAMPENING * FastMath.PI)));
				else {
					bellIsRecoiling = false;
					return 0;
				}
			}
			return 0;
		}
		
		/**
		 * Recoils the bell.
		 *
		 * @param velocity the velocity of the MIDI note
		 */
		public void recoilBell(int velocity) {
			amplitude = PercussionInstrument.velocityRecoilDampening(velocity) * BASE_AMPLITUDE;
			animTime = 0;
			bellIsRecoiling = true;
		}
	}
}
