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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator;
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The pizzicato strings.
 */
public class PizzicatoStrings extends DecayedInstrument {
	
	/**
	 * Each string.
	 */
	final PizzicatoString[] strings = new PizzicatoString[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public PizzicatoStrings(@NotNull Midis2jam2 context,
	                        @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		for (var i = 0; i < 12; i++) {
			strings[i] = new PizzicatoString();
			instrumentNode.attachChild(strings[i].highestLevel);
			strings[i].highestLevel.setLocalTranslation(i * 2f, i * 0.5f, 0);
			strings[i].highestLevel.setLocalScale(1, 0.5f - 0.019f * i, 1);
		}
		
		instrumentNode.setLocalTranslation(0, 6.7f, -138f);
	}
	
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiNoteOnEvent> eventsToDoOn = new ArrayList<>();
		while (!hits.isEmpty() && context.getFile().eventInSeconds(hits.get(0)) <= time) {
			eventsToDoOn.add(hits.remove(0));
		}
		for (MidiNoteOnEvent midiNoteOnEvent : eventsToDoOn) {
			int stringIndex = (midiNoteOnEvent.note + 3) % 12;
			strings[stringIndex].play();
		}
		
		Arrays.stream(strings).forEach(string -> string.tick(time, delta));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(45f + 12 * indexForMoving(delta)), 0));
	}
	
	/**
	 * A single string.
	 */
	public class PizzicatoString extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		/**
		 * Contains the anim strings.
		 */
		final Node animStringNode = new Node();
		
		/**
		 * The resting string.
		 */
		final Spatial restingString;
		
		/**
		 * Each frame of animation.
		 */
		final Spatial[] animStrings = new Spatial[5];
		
		/**
		 * Is this string currently playing?
		 */
		boolean playing = false;
		
		/**
		 * Animates the anim strings.
		 */
		final VibratingStringAnimator stringAnimator;
		
		/**
		 * The amount of progress playing the current note.
		 */
		private double progress = 0;
		
		public PizzicatoString() {
			animNode.attachChild(context.loadModel("PizzicatoStringHolder.obj", "Wood.bmp"));
			restingString = context.loadModel("StageString.obj", "StageString.bmp");
			for (var k = 0; k < 5; k++) {
				animStrings[k] = context.loadModel("StageStringBottom" + k + ".obj", "StageStringPlaying.bmp",
						Midis2jam2.MatType.UNSHADED, 0);
				animStrings[k].setCullHint(Spatial.CullHint.Always);
				animStringNode.attachChild(animStrings[k]);
			}
			stringAnimator = new VibratingStringAnimator(animStrings);
			animNode.attachChild(animStringNode);
			animNode.attachChild(restingString);
		}
		
		@Override
		public void tick(double time, float delta) {
			stringAnimator.tick(delta);
			
			if (progress >= 1) playing = false;
			
			if (playing) {
				animNode.setLocalTranslation(0, 0, 2);
				animStringNode.setCullHint(Spatial.CullHint.Dynamic);
				restingString.setCullHint(Spatial.CullHint.Always);
			} else {
				animNode.setLocalTranslation(0, 0, 0);
				animStringNode.setCullHint(Spatial.CullHint.Always);
				restingString.setCullHint(Spatial.CullHint.Dynamic);
			}
			
			progress += delta * 7;
		}
		
		/**
		 * Begin playing this string.
		 */
		public void play() {
			playing = true;
			progress = 0;
		}
	}
}
