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

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.family.percussive.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.PI;
import static org.wysko.midis2jam2.util.MatType.REFLECTIVE;

/**
 * The music box has several animation components. The first is the spindle/cylinder. The spindle spins at a rate of 1/4
 * turn per beat = π/2 rad. To calculate this, the spindle is rotated by {@code 0.5 * PI * delta * (6E7 / bpm) / 60} on
 * each frame.
 */
public class MusicBox extends DecayedInstrument {
	
	/**
	 * Each of the hanging notes.
	 */
	@NotNull
	private final OneMusicBoxNote[] notes = new OneMusicBoxNote[12];
	
	/**
	 * Contains the spindle.
	 */
	@NotNull
	private final Node cylinder = new Node();
	
	/**
	 * List of hits for hanging key recoils.
	 */
	@NotNull
	private final List<MidiNoteOnEvent> hitsForRecoil;
	
	/**
	 * List of points that are currently active.
	 */
	@NotNull
	private final List<Spatial> points = new ArrayList<>();
	
	/**
	 * Keeps track of how many radians each point has rotated.
	 */
	@NotNull
	private final HashMap<Spatial, Float> pointRotations = new HashMap<>();
	
	/**
	 * Model of the music box point.
	 */
	@NotNull
	private final Spatial pointModel;
	
	/**
	 * Contains a pool of spatials that are music box notes. This is so that a new music box note doesn't have to be
	 * spawned every time there is a new note.
	 */
	@NotNull
	private final List<Spatial> pool = new ArrayList<>();
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public MusicBox(@NotNull Midis2jam2 context, @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		this.hitsForRecoil = eventList.stream()
				.filter(MidiNoteOnEvent.class::isInstance)
				.map(MidiNoteOnEvent.class::cast)
				.collect(Collectors.toList());
		
		for (var i = 0; i < 12; i++) {
			notes[i] = new OneMusicBoxNote(i);
			instrumentNode.attachChild(notes[i].highestLevel);
		}
		
		instrumentNode.attachChild(context.loadModel("MusicBoxCase.obj", "Wood.bmp"));
		instrumentNode.attachChild(context.loadModel("MusicBoxTopBlade.obj", "ShinySilver.bmp", REFLECTIVE, 0.9F));
		
		var spindle = context.loadModel("MusicBoxSpindle.obj", "ShinySilver.bmp", REFLECTIVE, 0.9F);
		cylinder.attachChild(spindle);
		instrumentNode.attachChild(cylinder);
		instrumentNode.setLocalTranslation(37, 7, -5);
		
		pointModel = context.loadModel("MusicBoxPoint.obj", "ShinySilver.bmp", REFLECTIVE, 0.9F);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		rotateCylinder(delta);
		
		for (Iterator<MidiNoteOnEvent> iterator = hits.iterator(); iterator.hasNext(); ) {
			MidiNoteOnEvent hit = iterator.next();
			// If it is within one quarter note of the hit
			if (context.getFile().eventInSeconds(hit.getTime() - context.getFile().getDivision()) <= time) {
				Spatial aPoint;
				if (pool.size() > 1) {
					aPoint = pool.remove(0);
				} else {
					aPoint = pointModel.clone();
				}
				instrumentNode.attachChild(aPoint);
				aPoint.setLocalRotation(new Quaternion().fromAngles((float) (-PI / 2), 0, 0));
				points.add(aPoint);
				pointRotations.put(aPoint, 0F);
				aPoint.setLocalTranslation(((hit.getNote() + 3) % 12) - 5.5F, 0, 0);
				iterator.remove();
			}
		}
		
		for (Iterator<Spatial> iterator = points.iterator(); iterator.hasNext(); ) {
			Spatial activePoint = iterator.next();
			var rotation = pointRotations.get(activePoint);
			if (rotation > 4.71) { // 3 pi / 2
				instrumentNode.detachChild(activePoint);
				iterator.remove();
				pool.add(activePoint);
			}
		}
		
		List<MidiNoteOnEvent> performingRecoils = new ArrayList<>();
		while (!hitsForRecoil.isEmpty() && context.getFile().eventInSeconds(hitsForRecoil.get(0)) <= time) {
			performingRecoils.add(hitsForRecoil.remove(0));
		}
		for (MidiNoteOnEvent performingRecoil : performingRecoils) {
			notes[(performingRecoil.getNote() + 3) % 12].play();
		}
		// Tick the hanging notes
		for (OneMusicBoxNote note : notes) {
			note.tick(delta);
		}
	}
	
	/**
	 * Rotates the cylinder. The cylinder rotates PI/2 radians for every quarter note.
	 *
	 * @param delta the amount of time since the last frame
	 */
	private void rotateCylinder(float delta) {
		var tick = context.getSequencer().getTickPosition();
		var xAngle = (float) (0.5 * PI * delta * (6E7 / context.getFile().tempoAt(tick).getNumber()) / 60.0);
		
		for (Spatial point : points) {
			point.rotate(xAngle, 0, 0);
			pointRotations.put(point, pointRotations.get(point) + xAngle);
		}
		
		cylinder.rotate(xAngle, 0, 0);
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		offsetNode.setLocalTranslation(0, 0, indexForMoving(delta) * -18f);
	}
	
	public class OneMusicBoxNote extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		/**
		 * The hanging key.
		 */
		private final Spatial key;
		
		/**
		 * The animation progress.
		 */
		private double animT;
		
		/**
		 * True if this note is recoiling, false otherwise.
		 */
		private boolean playing;
		
		/**
		 * Instantiates a music box note.
		 *
		 * @param i the index
		 */
		public OneMusicBoxNote(int i) {
			key = context.loadModel("MusicBoxKey.obj", "ShinySilver.bmp", REFLECTIVE, 0.9F);
			highestLevel.attachChild(key);
			key.setLocalTranslation(i - 5.5F, 7, 0);
			key.setLocalScale(-0.0454F * i + 1, 1, 1);
		}
		
		/**
		 * Call to begin recoiling.
		 */
		public void play() {
			playing = true;
			animT = 0;
		}
		
		@Override
		public void tick(float delta) {
			if (playing) {
				animT += delta * 4;
			}
			if (animT >= 1) {
				animT = 0;
				playing = false;
			}
			key.setLocalRotation(new Quaternion().fromAngles(rotationFactorFromProgress(), 0, 0));
		}
		
		/**
		 * Calculates the rotation factor from the current animation progress.
		 *
		 * @return the rotation factor
		 */
		private float rotationFactorFromProgress() {
			float rotation;
			if (animT <= 0.5) {
				rotation = (float) animT;
			} else {
				rotation = (float) (-animT + 1);
			}
			return -rotation;
		}
	}
}
