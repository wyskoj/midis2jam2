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

package org.wysko.midis2jam2.instrument.family.soundeffects;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * <i>You used to call me on my cellphone...</i>
 */
public class TelephoneRing extends SustainedInstrument {
	
	/**
	 * The Up keys.
	 */
	final Spatial[] upKeys = new Spatial[12];
	
	/**
	 * The Down keys.
	 */
	final Spatial[] downKeys = new Spatial[12];
	
	/**
	 * The Up node.
	 */
	final Node upNode = new Node();
	
	/**
	 * The Down node.
	 */
	final Node downNode = new Node();
	
	/**
	 * For each key, is it playing?
	 */
	final boolean[] playing = new boolean[12];
	
	/**
	 * The Handle.
	 */
	final Spatial handle;
	
	/**
	 * The Notes.
	 */
	private final List<MidiNoteEvent> notes;
	
	/**
	 * The amount to shake the handle.
	 */
	float force = 0;
	
	/**
	 * Random for phone animation.
	 */
	private final Random random = new Random();
	
	/**
	 * Instantiates a new Telephone ring.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public TelephoneRing(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		notes = eventList.stream()
				.filter(MidiNoteEvent.class::isInstance)
				.map(MidiNoteEvent.class::cast)
				.collect(Collectors.toList());
		this.notePeriods = calculateNotePeriods(notes);
		
		Spatial base = context.loadModel("TelePhoneBase.fbx", "TelephoneBase.bmp");
		
		Node base1 = (Node) base;
		var rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		base1.getChild(0).setMaterial(rubberFoot);
		
		handle = context.loadModel("TelePhoneHandle.obj", "TelephoneHandle.bmp");
		instrumentNode.attachChild(base);
		instrumentNode.attachChild(handle);
		
		for (var i = 0; i < 12; i++) {
			String val;
			if (i < 9) {
				val = String.valueOf(i + 1);
			} else if (i == 9) {
				val = "Star";
			} else if (i == 10) {
				val = "0";
			} else {
				val = "Pound";
			}
			upKeys[i] = context.loadModel("TelePhoneKey.obj", "TelePhoneKey" + val + "Dark.bmp",
					Midis2jam2.MatType.UNSHADED, 0.9f);
			downKeys[i] = context.loadModel("TelePhoneKey.obj", "TelePhoneKey" + val + ".bmp",
					Midis2jam2.MatType.UNSHADED,
					0.9f);
			
			
			final var row = -i / 3;
			upKeys[i].setLocalTranslation(1.2f * (i % 3 - 1), 3.89f, -2.7f - (1.2f * row));
			downKeys[i].setLocalTranslation(1.2f * (i % 3 - 1), 3.4f, -2.7f - (1.2f * row));
			
			downKeys[i].setCullHint(Spatial.CullHint.Always);
			upNode.attachChild(upKeys[i]);
			downNode.attachChild(downKeys[i]);
		}
		
		upNode.setLocalRotation(new Quaternion().fromAngles(rad(19), 0, 0));
		downNode.setLocalRotation(new Quaternion().fromAngles(rad(19), 0, 0));
		instrumentNode.attachChild(upNode);
		instrumentNode.attachChild(downNode);
		instrumentNode.setLocalTranslation(0, 1, -50);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		
		if (!notes.isEmpty())
			while (!notes.isEmpty() &&
					((notes.get(0) instanceof MidiNoteOnEvent && context.getFile().eventInSeconds(notes.get(0)) <= time)
							||
							(notes.get(0) instanceof MidiNoteOffEvent && context.getFile().eventInSeconds(notes.get(0)) <= time - 0.01))
			) {
				eventsToPerform.add(notes.remove(0));
			}
		
		
		for (MidiEvent event : eventsToPerform) {
			if (event instanceof MidiNoteOnEvent) {
				MidiNoteOnEvent noteOn = (MidiNoteOnEvent) event;
				int keyIndex = (noteOn.note + 3) % 12;
				playing[keyIndex] = true;
				upKeys[keyIndex].setCullHint(Spatial.CullHint.Always);
				downKeys[keyIndex].setCullHint(Spatial.CullHint.Dynamic);
			} else if (event instanceof MidiNoteOffEvent) {
				MidiNoteOffEvent noteOff = (MidiNoteOffEvent) event;
				int keyIndex = (noteOff.note + 3) % 12;
				playing[keyIndex] = false;
				upKeys[keyIndex].setCullHint(Spatial.CullHint.Dynamic);
				downKeys[keyIndex].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		// Animate the phone
		var isPlaying = false;
		for (boolean b : playing) {
			if (b) {
				isPlaying = true;
				break;
			}
		}
		handle.setLocalTranslation(0, (float) (2 + (random.nextGaussian() * 0.3)) * force, 0);
		handle.setLocalRotation(new Quaternion().fromAngles(rad(random.nextGaussian() * 3) * force,
				rad(random.nextGaussian() * 3) * force, 0));
		if (isPlaying) {
			force += 12 * delta;
			force = Math.min(1, force);
		} else {
			force -= 12 * delta;
			force = Math.max(0, force);
		}
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(13f * indexForMoving(delta), 0, 0);
	}
}
