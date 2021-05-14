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

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.family.percussion.drumset.*;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.instrument.family.percussion.drumset.Cymbal.CymbalType.RIDE_1;
import static org.wysko.midis2jam2.instrument.family.percussion.drumset.Cymbal.CymbalType.RIDE_2;

/**
 * The Percussion.
 */
public class Percussion extends Instrument {
	
	
	/**
	 * The Percussion node.
	 */
	private final Node percussionNode = new Node();
	
	/**
	 * The Note on events.
	 */
	private final List<MidiNoteOnEvent> noteOnEvents;
	
	/**
	 * Each percussion instrument.
	 */
	private final List<PercussionInstrument> instruments = new ArrayList<>();
	
	/**
	 * Instantiates the percussion.
	 *
	 * @param context the context
	 * @param events  the events
	 */
	public Percussion(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context);
		/* Percussion only cares about note on. */
		this.noteOnEvents = events.stream()
				.filter(MidiNoteOnEvent.class::isInstance)
				.map(MidiNoteOnEvent.class::cast)
				.collect(Collectors.toList());
		
		
		instruments.add(new SnareDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 40 || e.note == 38 || e.note == 37).collect(Collectors.toList())));
		
		/* For some reason, the bass drum needs special attention ?? */
		var e1 = new BassDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 35 || e.note == 36).collect(Collectors.toList()));
		var drumSetNode = new Node();
		drumSetNode.attachChild(e1.highLevelNode);
		instruments.add(e1);
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 41).collect(Collectors.toList()), Tom.TomPitch.LOW_FLOOR));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 43).collect(Collectors.toList()), Tom.TomPitch.HIGH_FLOOR));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 45).collect(Collectors.toList()), Tom.TomPitch.LOW));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 47).collect(Collectors.toList()), Tom.TomPitch.LOW_MID));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 48).collect(Collectors.toList()), Tom.TomPitch.HIGH_MID));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.note == 50).collect(Collectors.toList()), Tom.TomPitch.HIGH));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 49).collect(Collectors.toList()), Cymbal.CymbalType.CRASH_1));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 57).collect(Collectors.toList()), Cymbal.CymbalType.CRASH_2));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 55).collect(Collectors.toList()), Cymbal.CymbalType.SPLASH));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.note == 52).collect(Collectors.toList()), Cymbal.CymbalType.CHINA));
		
		// CALCULATE RIDE CYMBAL NOTES
		var allRideNotes = noteOnEvents.stream().filter(e -> e.note == 51 || e.note == 59 || e.note == 53).collect(Collectors.toList());
		var currentRideCymbal = RIDE_1;
		List<MidiNoteOnEvent> ride1Notes = new ArrayList<>();
		List<MidiNoteOnEvent> ride2Notes = new ArrayList<>();
		for (MidiNoteOnEvent note : allRideNotes) {
			if (note.note == 51) {
				ride1Notes.add(note);
				currentRideCymbal = RIDE_1;
			} else if (note.note == 59) {
				ride2Notes.add(note);
				currentRideCymbal = RIDE_2;
			} else {
				if (currentRideCymbal == RIDE_1) {
					ride1Notes.add(note);
				} else {
					ride2Notes.add(note);
				}
			}
		}
		
		instruments.add(new RideCymbal(context,
				ride1Notes, RIDE_1));
		
		instruments.add(new RideCymbal(context,
				ride2Notes, RIDE_2));
		
		instruments.add(new HiHat(context,
				noteOnEvents.stream().filter(e -> e.note == 42 || e.note == 44 || e.note == 46).collect(Collectors.toList())));
		
		instruments.add(new Congas(context,
				noteOnEvents.stream().filter(e -> e.note <= 64 && e.note >= 62).collect(Collectors.toList())));
		
		instruments.add(new Cowbell(context,
				noteOnEvents.stream().filter(e -> e.note == 56).collect(Collectors.toList())));
		
		instruments.add(new Timbales(context,
				noteOnEvents.stream().filter(e -> e.note == 65 || e.note == 66).collect(Collectors.toList())));
		
		instruments.add(new Bongos(context,
				noteOnEvents.stream().filter(e -> e.note == 61 || e.note == 60).collect(Collectors.toList())));
		
		instruments.add(new Tambourine(context,
				noteOnEvents.stream().filter(e -> e.note == 54).collect(Collectors.toList())));
		
		instruments.add(new HandClap(context,
				noteOnEvents.stream().filter(e -> e.note == 39).collect(Collectors.toList())));
		
		instruments.add(new Sticks(context,
				noteOnEvents.stream().filter(e -> e.note == 31).collect(Collectors.toList())));
		
		instruments.add(new JingleBells(context,
				noteOnEvents.stream().filter(e -> e.note == 83).collect(Collectors.toList())));
		
		instruments.add(new Castanets(context,
				noteOnEvents.stream().filter(e -> e.note == 85).collect(Collectors.toList())));
		
		instruments.add(new HighQ(context,
				noteOnEvents.stream().filter(e -> e.note == 27).collect(Collectors.toList())));
		
		instruments.add(new Woodblock(context,
				noteOnEvents.stream().filter(e -> e.note == 77 || e.note == 76).collect(Collectors.toList())));
		
		instruments.add(new Agogo(context,
				noteOnEvents.stream().filter(e -> e.note == 67 || e.note == 68).collect(Collectors.toList())));
		
		instruments.add(new Shaker(context,
				noteOnEvents.stream().filter(e -> e.note == 82).collect(Collectors.toList())));
		
		instruments.add(new Cabasa(context,
				noteOnEvents.stream().filter(e -> e.note == 69).collect(Collectors.toList())));
		
		instruments.add(new Maracas(context,
				noteOnEvents.stream().filter(e -> e.note == 70).collect(Collectors.toList())));
		
		instruments.add(new Claves(context,
				noteOnEvents.stream().filter(e -> e.note == 75).collect(Collectors.toList())));
		
		instruments.add(new Triangle(context,
				noteOnEvents.stream().filter(e -> e.note == 81).collect(Collectors.toList()),
				Triangle.TriangleType.OPEN));
		
		instruments.add(new Triangle(context,
				noteOnEvents.stream().filter(e -> e.note == 80).collect(Collectors.toList()),
				Triangle.TriangleType.MUTED));
		
		instruments.add(new SquareClick(context,
				noteOnEvents.stream().filter(e -> e.note == 32).collect(Collectors.toList())));
		
		instruments.add(new Metronome(context,
				noteOnEvents.stream().filter(e -> e.note == 33 || e.note == 34).collect(Collectors.toList())));
		
		instruments.add(new Whistle(context,
				noteOnEvents.stream().filter(e -> e.note == 71 || e.note == 72).collect(Collectors.toList())));
		
		// Attach nodes to group node
		for (PercussionInstrument instrument : instruments) {
			if (instrument instanceof SnareDrum
					|| instrument instanceof BassDrum
					|| instrument instanceof Tom
					|| instrument instanceof Cymbal
					|| instrument instanceof HiHat) {
				drumSetNode.attachChild(instrument.highLevelNode);
			} else {
				percussionNode.attachChild(instrument.highLevelNode);
			}
		}
		
		/* Add shadow */
		Spatial shadow = context.getAssetManager().loadModel("Assets/DrumShadow.obj");
		var material = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		material.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/DrumShadow.png"));
		material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		shadow.setQueueBucket(RenderQueue.Bucket.Transparent);
		shadow.setMaterial(material);
		shadow.move(0, 0.1f, -80);
		
		percussionNode.attachChild(drumSetNode);
		percussionNode.attachChild(shadow);
		context.getRootNode().attachChild(percussionNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByStrikes(noteOnEvents, time, percussionNode);
		instruments.forEach(instrument -> instrument.tick(time, delta));
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		// Do nothing!
	}
}
