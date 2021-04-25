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
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;
import static org.wysko.midis2jam2.instrument.family.piano.KeyedInstrument.KeyColor.BLACK;

/**
 * The Harp.
 */
public class Harp extends SustainedInstrument {
	
	/**
	 * The harp strings.
	 */
	final HarpString[] strings = new HarpString[47];
	
	/**
	 * The notes to play.
	 */
	private final List<MidiNoteEvent> notes;
	
	/**
	 * Instantiates a new Harp.
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
		instrumentNode.attachChild(context.loadModel("Harp.obj", "HarpSkin.bmp", Midis2jam2.MatType.UNSHADED, 0.9f));
		instrumentNode.setLocalTranslation(5, 3.6f, 17);
		instrumentNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-35), 0));
		highestLevel.attachChild(instrumentNode);
		
		for (var i = 0; i < 47; i++) {
			strings[i] = new HarpString(i);
			instrumentNode.attachChild(strings[i].stringNode);
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		
		if (!notes.isEmpty())
			while (
					!notes.isEmpty()
							&& ((notes.get(0) instanceof MidiNoteOnEvent && context.getFile().eventInSeconds(notes.get(0)) <= time)
							|| (notes.get(0) instanceof MidiNoteOffEvent && context.getFile().eventInSeconds(notes.get(0)) <= time - 0.01))
			) {
				eventsToPerform.add(notes.remove(0));
			}
		
		
		for (MidiEvent event : eventsToPerform) {
			if (!(event instanceof MidiNoteEvent)) continue;
			
			MidiNoteEvent note = (MidiNoteEvent) event;
			int midiNote = note.note;
			if (KeyedInstrument.midiValueToColor(note.note) == BLACK) {
				midiNote--; // round black notes down
			}
			int harpString = -1;
			if (midiNote >= 24 && midiNote <= 103) { // In range of harp
				int mod = midiNote % 12;
				harpString = switch (mod) {
					case 0 -> 0;
					case 2 -> 1;
					case 4 -> 2;
					case 5 -> 3;
					case 7 -> 4;
					case 9 -> 5;
					case 11 -> 6;
					default -> throw new IllegalStateException("Unexpected value: " + mod);
				};
				harpString += ((midiNote - 24) / 12) * 7;
			}
			if (event instanceof MidiNoteOnEvent) {
				if (harpString != -1) {
					strings[harpString].beginPlaying();
				}
			} else if (event instanceof MidiNoteOffEvent && harpString != -1) {
				strings[harpString].endPlaying();
			}
		}
		
		for (HarpString string : strings) {
			string.tick(delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(14.7f * indexForMoving(), 0, 10.3f * indexForMoving());
	}
	
	/**
	 * A single harp string.
	 */
	private class HarpString {
		
		/**
		 * The idle string.
		 */
		final Spatial string;
		
		/**
		 * The Vibrating strings.
		 */
		final Spatial[] vibratingStrings = new Spatial[5];
		
		/**
		 * The String node.
		 */
		final Node stringNode = new Node();
		
		/**
		 * True if this string is vibrating, false otherwise.
		 */
		boolean vibrating = false;
		
		/**
		 * The current frame of animation.
		 */
		private double frame = 0;
		
		/**
		 * Instantiates a new Harp string.
		 *
		 * @param i the string index
		 */
		public HarpString(int i) {
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
			string = context.loadModel("HarpString.obj", t, Midis2jam2.MatType.UNSHADED, 0.9f);
			
			for (var v = 0; v < 5; v++) {
				vibratingStrings[v] = context.loadModel("HarpStringPlaying" + v + ".obj", vt,
						Midis2jam2.MatType.UNSHADED, 0);
				vibratingStrings[v].setCullHint(Spatial.CullHint.Always);
				stringNode.attachChild(vibratingStrings[v]);
			}
			stringNode.attachChild(string);
			stringNode.setLocalTranslation(0, 2.1444f + 0.8777f * i, -2.27f + (0.75651f * -i));
			float scale = (float) ((2.44816E-4 * Math.pow(i, 2)) + (-0.02866 * i) + 0.97509);
			stringNode.setLocalScale(1, scale, 1);
			// TODO Use vibrating string animator
		}
		
		/**
		 * Update animation and notes.
		 *
		 * @param delta the amount of time since the last frame update
		 */
		public void tick(float delta) {
			final double inc = delta / (1 / 60f);
			this.frame += inc;
			if (vibrating) {
				string.setCullHint(Spatial.CullHint.Always);
				for (int i = 0; i < 5; i++) {
					frame = frame % 5;
					if (i == Math.floor(frame)) {
						vibratingStrings[i].setCullHint(Spatial.CullHint.Dynamic);
					} else {
						vibratingStrings[i].setCullHint(Spatial.CullHint.Always);
					}
				}
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
