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

package org.wysko.midis2jam2.instrument.family.strings;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.instrument.algorithmic.NoteQueue;
import org.wysko.midis2jam2.instrument.algorithmic.VibratingStringAnimator;
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor.BLACK;
import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The harp is a diatonic instrument, so chromatic notes are rounded down to the nearest white key. For example, if a C#
 * is to be played, a C is instead played.
 */
public class Harp extends SustainedInstrument {
	
	/**
	 * The harp strings. The harp contains 47 strings.
	 */
	@NotNull
	private final HarpString[] strings = new HarpString[47];
	
	/**
	 * The notes to play.
	 */
	@NotNull
	private final List<MidiNoteEvent> notes;
	
	/**
	 * Instantiates a new harp.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public Harp(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		this.notes = eventList.stream()
				.filter(MidiNoteEvent.class::isInstance)
				.map(MidiNoteEvent.class::cast)
				.collect(Collectors.toList());
		
		/* Load model */
		instrumentNode.attachChild(context.loadModel("Harp.obj", "HarpSkin.bmp"));
		instrumentNode.setLocalTranslation(5, 3.6F, 17);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-35), 0));
		highestLevel.attachChild(instrumentNode);
		
		/* Create harp strings */
		for (var i = 0; i < 47; i++) {
			strings[i] = new HarpString(i);
			instrumentNode.attachChild(strings[i].stringNode);
		}
	}
	
	/**
	 * Given a note within an octave, represented as an integer (0 = C, 2 = D, 4 = E, 5 = F, etc.), returns the harp
	 * string number to animate.
	 *
	 * @param noteNumber the note number
	 * @return the harp string number
	 * @throws IllegalArgumentException if you specify a black key
	 */
	@Range(from = 0, to = 6)
	@Contract(pure = true)
	private static int getHarpString(int noteNumber) throws IllegalArgumentException {
		int harpString;
		harpString = switch (noteNumber) {
			case 0 -> 0;
			case 2 -> 1;
			case 4 -> 2;
			case 5 -> 3;
			case 7 -> 4;
			case 9 -> 5;
			case 11 -> 6;
			default -> throw new IllegalArgumentException("Unexpected value: " + noteNumber);
		};
		return harpString;
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiNoteEvent> eventsToPerform = NoteQueue.collectWithOffGap(notes, context, time);
		
		for (MidiNoteEvent event : eventsToPerform) {
			
			int midiNote = event.note;
			
			/* If the note falls on a black key (if it were played on a piano) we need to "round it down" to the
			 * nearest white key. */
			if (KeyedInstrument.midiValueToColor(midiNote) == BLACK) {
				midiNote--;
			}
			int harpString = -1;
			
			/* Only consider notes within the range of the instrument */
			if (midiNote >= 24 && midiNote <= 103) {
				harpString = getHarpString(midiNote % 12);
				harpString += ((midiNote - 24) / 12) * 7;
			}
			if (event instanceof MidiNoteOnEvent) {
				if (harpString != -1) {
					strings[harpString].beginPlaying();
				}
			} else {
				if (harpString != -1) {
					strings[harpString].endPlaying();
				}
			}
			
		}
		
		/* Update each harp string */
		for (HarpString string : strings) {
			string.tick(delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(14.7F * indexForMoving(delta), 0, 10.3F * indexForMoving(delta));
	}
	
	/**
	 * A single harp string.
	 */
	private class HarpString {
		
		/**
		 * The idle string.
		 */
		private final Spatial string;
		
		/**
		 * The Vibrating strings.
		 */
		private final Spatial[] vibratingStrings = new Spatial[5];
		
		/**
		 * The String node.
		 */
		private final Node stringNode = new Node();
		
		/**
		 * The string animator.
		 */
		private final VibratingStringAnimator stringAnimator;
		
		/**
		 * True if this string is vibrating, false otherwise.
		 */
		private boolean vibrating;
		
		/**
		 * Instantiates a new Harp string.
		 *
		 * @param i the string index
		 */
		public HarpString(int i) {
			
			/* Select correct texture from note */
			String t;
			String vt;
			if (i % 7 == 0) {
				t = "HarpStringRed.bmp";
				vt = "HarpStringRedPlaying.bmp";
			} else if (i % 7 == 3) {
				t = "HarpStringBlue.bmp";
				vt = "HarpStringBluePlaying.bmp";
			} else {
				t = "HarpStringWhite.bmp";
				vt = "HarpStringWhitePlaying.bmp";
			}
			string = context.loadModel("HarpString.obj", t);
			
			/* Load vibrating strings */
			for (var v = 0; v < 5; v++) {
				vibratingStrings[v] = context.loadModel("HarpStringPlaying" + v + ".obj", vt, Midis2jam2.MatType.UNSHADED, 0);
				vibratingStrings[v].setCullHint(Spatial.CullHint.Always);
				stringNode.attachChild(vibratingStrings[v]);
			}
			stringNode.attachChild(string);
			
			/* Funky math to polynomially scale each string */
			stringNode.setLocalTranslation(0, 2.1444F + 0.8777F * i, -2.27F + (0.75651F * -i));
			float scale = (float) ((2.44816E-4 * Math.pow(i, 2)) + (-0.02866 * i) + 0.97509);
			stringNode.setLocalScale(1, scale, 1);
			
			stringAnimator = new VibratingStringAnimator(vibratingStrings);
		}
		
		/**
		 * Update animation and notes.
		 *
		 * @param delta the amount of time since the last frame update
		 */
		public void tick(float delta) {
			if (vibrating) {
				string.setCullHint(Spatial.CullHint.Always);
				stringAnimator.tick(delta);
			} else {
				string.setCullHint(Spatial.CullHint.Dynamic);
				for (Spatial vibratingString : vibratingStrings) {
					vibratingString.setCullHint(Spatial.CullHint.Always);
				}
			}
		}
		
		/**
		 * Begin playing this string.
		 */
		public void beginPlaying() {
			vibrating = true;
		}
		
		/**
		 * End playing this string.
		 */
		public void endPlaying() {
			vibrating = false;
		}
	}
}
