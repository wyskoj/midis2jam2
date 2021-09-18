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
import static org.wysko.midis2jam2.midi.Midi.*;

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
	@SuppressWarnings("java:S103")
	public Percussion(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		super(context);
		/* Percussion only cares about note on. */
		this.noteOnEvents = events.stream()
				.filter(MidiNoteOnEvent.class::isInstance)
				.map(MidiNoteOnEvent.class::cast)
				.collect(Collectors.toList());
		
		
		instruments.add(new SnareDrum(context,
				noteOnEvents.stream().filter(e -> e.getNote() == ACOUSTIC_SNARE || e.getNote() == ELECTRIC_SNARE || e.getNote() == SIDE_STICK).collect(Collectors.toList())));
		
		/* For some reason, the bass drum needs special attention ?? */
		var e1 = new BassDrum(context,
				noteOnEvents.stream().filter(e -> e.getNote() == ACOUSTIC_BASS_DRUM || e.getNote() == ELECTRIC_BASS_DRUM).collect(Collectors.toList()));
		var drumSetNode = new Node();
		drumSetNode.attachChild(e1.highLevelNode);
		instruments.add(e1);
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.getNote() == LOW_FLOOR_TOM).collect(Collectors.toList()), Tom.TomPitch.LOW_FLOOR));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.getNote() == HIGH_FLOOR_TOM).collect(Collectors.toList()),
				Tom.TomPitch.HIGH_FLOOR));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.getNote() == LOW_TOM).collect(Collectors.toList()), Tom.TomPitch.LOW));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.getNote() == LOW_MID_TOM).collect(Collectors.toList()), Tom.TomPitch.LOW_MID));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.getNote() == HI_MID_TOM).collect(Collectors.toList()),
				Tom.TomPitch.HIGH_MID));
		
		instruments.add(new Tom(context,
				noteOnEvents.stream().filter(e -> e.getNote() == HIGH_TOM).collect(Collectors.toList()), Tom.TomPitch.HIGH));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.getNote() == CRASH_CYMBAL_1).collect(Collectors.toList()),
				Cymbal.CymbalType.CRASH_1));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.getNote() == CRASH_CYMBAL_2).collect(Collectors.toList()),
				Cymbal.CymbalType.CRASH_2));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.getNote() == SPLASH_CYMBAL).collect(Collectors.toList()),
				Cymbal.CymbalType.SPLASH));
		
		instruments.add(new Cymbal(context,
				noteOnEvents.stream().filter(e -> e.getNote() == CHINESE_CYMBAL).collect(Collectors.toList()),
				Cymbal.CymbalType.CHINA));
		
		// CALCULATE RIDE CYMBAL NOTES
		var allRideNotes =
				noteOnEvents.stream().filter(e -> e.getNote() == RIDE_CYMBAL_1 || e.getNote() == RIDE_CYMBAL_2 || e.getNote() == RIDE_BELL).collect(Collectors.toList());
		var currentRideCymbal = RIDE_1;
		List<MidiNoteOnEvent> ride1Notes = new ArrayList<>();
		List<MidiNoteOnEvent> ride2Notes = new ArrayList<>();
		for (MidiNoteOnEvent note : allRideNotes) {
			if (note.getNote() == RIDE_CYMBAL_1) {
				ride1Notes.add(note);
				currentRideCymbal = RIDE_1;
			} else if (note.getNote() == RIDE_CYMBAL_2) {
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
				noteOnEvents.stream().filter(e -> e.getNote() == CLOSED_HI_HAT || e.getNote() == OPEN_HI_HAT || e.getNote() == PEDAL_HI_HAT).collect(Collectors.toList())));
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == LOW_CONGA || e.getNote() == MUTE_HIGH_CONGA || e.getNote() == OPEN_HIGH_CONGA)) {
			instruments.add(new Congas(context, noteOnEvents.stream().filter(e -> e.getNote() == LOW_CONGA || e.getNote() == MUTE_HIGH_CONGA || e.getNote() == OPEN_HIGH_CONGA).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == COWBELL)) {
			instruments.add(new Cowbell(context,
					noteOnEvents.stream().filter(e -> e.getNote() == COWBELL).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == LOW_TIMBALE || e.getNote() == HIGH_TIMBALE)) {
			instruments.add(new Timbales(context,
					noteOnEvents.stream().filter(e -> e.getNote() == LOW_TIMBALE || e.getNote() == HIGH_TIMBALE).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == LOW_BONGO || e.getNote() == HIGH_BONGO)) {
			instruments.add(new Bongos(context,
					noteOnEvents.stream().filter(e -> e.getNote() == LOW_BONGO || e.getNote() == HIGH_BONGO).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == TAMBOURINE)) {
			instruments.add(new Tambourine(context,
					noteOnEvents.stream().filter(e -> e.getNote() == TAMBOURINE).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == HAND_CLAP)) {
			instruments.add(new HandClap(context,
					noteOnEvents.stream().filter(e -> e.getNote() == HAND_CLAP).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == STICKS)) {
			instruments.add(new Sticks(context,
					noteOnEvents.stream().filter(e -> e.getNote() == STICKS).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == JINGLE_BELL)) {
			instruments.add(new JingleBells(context,
					noteOnEvents.stream().filter(e -> e.getNote() == JINGLE_BELL).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == CASTANETS)) {
			instruments.add(new Castanets(context,
					noteOnEvents.stream().filter(e -> e.getNote() == CASTANETS).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == HIGH_Q)) {
			instruments.add(new HighQ(context,
					noteOnEvents.stream().filter(e -> e.getNote() == HIGH_Q).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == LOW_WOODBLOCK || e.getNote() == HIGH_WOODBLOCK)) {
			instruments.add(new Woodblock(context,
					noteOnEvents.stream().filter(e -> e.getNote() == LOW_WOODBLOCK || e.getNote() == HIGH_WOODBLOCK).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == LOW_AGOGO || e.getNote() == HIGH_AGOGO)) {
			instruments.add(new Agogo(context,
					noteOnEvents.stream().filter(e -> e.getNote() == LOW_AGOGO || e.getNote() == HIGH_AGOGO).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == SHAKER)) {
			instruments.add(new Shaker(context,
					noteOnEvents.stream().filter(e -> e.getNote() == SHAKER).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == CABASA)) {
			instruments.add(new Cabasa(context,
					noteOnEvents.stream().filter(e -> e.getNote() == CABASA).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == MARACAS)) {
			instruments.add(new Maracas(context,
					noteOnEvents.stream().filter(e -> e.getNote() == MARACAS).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == CLAVES)) {
			instruments.add(new Claves(context,
					noteOnEvents.stream().filter(e -> e.getNote() == CLAVES).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == OPEN_TRIANGLE)) {
			instruments.add(new Triangle(context,
					noteOnEvents.stream().filter(e -> e.getNote() == OPEN_TRIANGLE).collect(Collectors.toList()),
					Triangle.TriangleType.OPEN));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == MUTE_TRIANGLE)) {
			instruments.add(new Triangle(context,
					noteOnEvents.stream().filter(e -> e.getNote() == MUTE_TRIANGLE).collect(Collectors.toList()),
					Triangle.TriangleType.MUTED));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == SQUARE_CLICK)) {
			instruments.add(new SquareClick(context,
					noteOnEvents.stream().filter(e -> e.getNote() == SQUARE_CLICK).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == METRONOME_BELL || e.getNote() == METRONOME_CLICK)) {
			instruments.add(new Metronome(context,
					noteOnEvents.stream().filter(e -> e.getNote() == METRONOME_BELL || e.getNote() == METRONOME_CLICK).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == SHORT_WHISTLE || e.getNote() == LONG_WHISTLE)) {
			instruments.add(new Whistle(context,
					noteOnEvents.stream().filter(e -> e.getNote() == SHORT_WHISTLE || e.getNote() == LONG_WHISTLE).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == OPEN_SURDO || e.getNote() == MUTE_SURDO)) {
			instruments.add(new Surdo(context,
					noteOnEvents.stream().filter(e -> e.getNote() == OPEN_SURDO || e.getNote() == MUTE_SURDO).collect(Collectors.toList())));
		}
		
		if (noteOnEvents.stream().anyMatch(e -> e.getNote() == SLAP)) {
			instruments.add(new Slap(context,
					noteOnEvents.stream().filter(e -> e.getNote() == SLAP).collect(Collectors.toList())));
		}
		
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
