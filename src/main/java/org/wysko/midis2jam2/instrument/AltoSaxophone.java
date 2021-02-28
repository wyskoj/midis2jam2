package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class AltoSaxophone extends Horn implements Instrument {
	public class AltoSaxophoneClone implements Instrument {
		
		Node polyphonicAlto = new Node();
		Node animAlto = new Node();
		Node modelAlto = new Node();
		private final List<NotePeriod> notePeriods;
		private boolean currentlyPlaying = false;
		
		
		public boolean isPlayingAtTime(long midiTick) {
			for (NotePeriod notePeriod : notePeriods) {
				if (midiTick >= notePeriod.startTick() && midiTick < notePeriod.endTick()) {
					return true;
				}
			}
			return false;
		}
		
		private final Spatial bell;
		private final Spatial body;
		
		NotePeriod currentNotePeriod;
		
		public AltoSaxophoneClone(List<NotePeriod> notePeriods) {
			this.notePeriods = notePeriods;
			this.body = AltoSaxophone.this.context.loadModel("AltoSaxBody.obj", "HornSkin.png");
			this.bell = AltoSaxophone.this.context.loadModel("AltoSaxHorn.obj", "HornSkin.png");
			
			modelAlto.attachChild(body);
			modelAlto.attachChild(bell);
			bell.move(0, -22, 0); // Move bell down to body
			
			animAlto.attachChild(modelAlto);
			polyphonicAlto.attachChild(animAlto);
		}
		
		@Override
		public void tick(double time, float delta) {
			// Prevent overlapping
			int clonesBeforeMe = 0;
			int indexThis = AltoSaxophone.this.clones.indexOf(this);
			
			if (currentlyPlaying || indexThis == 0) {
				// Show
				body.setCullHint(Spatial.CullHint.Dynamic);
				bell.setCullHint(Spatial.CullHint.Dynamic);
			} else {
				// Hide
				bell.setCullHint(Spatial.CullHint.Always);
				body.setCullHint(Spatial.CullHint.Always);
			}
			
			
			
			
			while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
				currentNotePeriod = notePeriods.remove(0);
			}
			
			if (currentNotePeriod != null) {
				if (time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime) {
					bell.setLocalScale(1, (float) ((0.5f * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1), 1);
					animAlto.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * 0.1f, 0, 0));
					currentlyPlaying = true;
				} else {
					currentlyPlaying = false;
					bell.setLocalScale(1, 1, 1);
				}
			}
			
			polyphonicAlto.setLocalTranslation(20 * indexThis, 0, 0);
			
		}
	}
	private final List<MidiNoteEvent> noteEvents = new ArrayList<>();
	private final Midis2jam2 context;
	Node highLevelAlto = new Node();
	Node groupOfPolyphony = new Node();
	MidiFile file;
	private List<NotePeriod> notePeriods;
	private List<AltoSaxophoneClone> clones;
	
	/*
		NODE STRUCTURE
		
		highLevelAlto -  For multiple channels ("instruments") of altos (Moves up if there are +1 altos)
		separateAlto - For setting the idle position and rotation
		polyphonicAlto - For polyphony (Rotates around a pivot to display multiple altos)
		animAlto - For visual rotation when playing (Tilts the alto while playing)
		modelAlto - For connecting the pieces of the alto together
		
	 */
	
	public AltoSaxophone(Midis2jam2 context, List<MidiChannelSpecificEvent> events, MidiFile file) {
		this.context = context;
		this.file = file;
		for (MidiChannelSpecificEvent event : events) {
			if (event instanceof MidiNoteOnEvent || event instanceof MidiNoteOffEvent) {
				noteEvents.add((MidiNoteEvent) event);
			}
		}
		
		calculateNotePeriods();
		calculateClones();
		
		for (AltoSaxophoneClone clone : clones) {
			groupOfPolyphony.attachChild(clone.polyphonicAlto);
		}
		
		highLevelAlto.attachChild(groupOfPolyphony);
		
		groupOfPolyphony.move(-14, 41.5f, -45);
		groupOfPolyphony.rotate(rad(13), rad(75), 0);
		
		context.getRootNode().attachChild(highLevelAlto);
	}
	
	/**
	 * This method is a mess. Your brain may rapidly combust if you try to understand it.
	 * <br>
	 * Essentially, it figures out when notes overlap and assigns them to "clones" of the instrument. This is used
	 * for monophonic instruments that need a polyphonic visualization.
	 * <br>
	 * TODO Make this less spaghetti
	 */
	private void calculateClones() {
		clones = new ArrayList<>();
		clones.add(new AltoSaxophoneClone(new ArrayList<>()));
		for (int i = 0; i < notePeriods.size(); i++) {
			for (int j = 0; j < notePeriods.size(); j++) {
				if (j == i) continue;
				NotePeriod comp1 = notePeriods.get(i);
				NotePeriod comp2 = notePeriods.get(j);
				if (comp1.startTick() > comp2.endTick()) continue;
				if (comp1.endTick() < comp2.startTick()) {
					clones.get(0).notePeriods.add(comp1);
					break;
				}
				if (comp1.startTick() >= comp2.startTick() && comp1.startTick() <= comp2.endTick()) { // Overlapping note
					boolean added = false;
					for (AltoSaxophoneClone clone : clones) {
						if (!clone.isPlayingAtTime(comp1.startTick())) {
							clone.notePeriods.add(comp1);
							added = true;
							break;
						}
					}
					if (!added) {
						AltoSaxophoneClone e = new AltoSaxophoneClone(new ArrayList<>());
						e.notePeriods.add(comp1);
						clones.add(e);
					}
				} else {
					clones.get(0).notePeriods.add(comp1);
				}
				break;
			}
		}
	}
	
	
	private void calculateNotePeriods() {
		notePeriods = new ArrayList<>();
		for (int i = 0, noteEventsSize = noteEvents.size(); i < noteEventsSize; i++) {
			MidiNoteEvent noteEvent = noteEvents.get(i);
			if (noteEvent instanceof MidiNoteOnEvent) {
				for (int j = i + 1; j < noteEventsSize; j++) {
					MidiNoteEvent check = noteEvents.get(j);
					if (check instanceof MidiNoteOffEvent && check.note == noteEvent.note) {
						// We found a block
						notePeriods.add(new NotePeriod(check.note, file.eventInSeconds(noteEvent),
								file.eventInSeconds(check), noteEvent.time, check.time, ((MidiNoteOnEvent) noteEvent)
								, ((MidiNoteOffEvent) check)));
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		// Prevent overlapping
		int altosBeforeMe = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (context.instruments.get(i) instanceof AltoSaxophone &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				altosBeforeMe++;
			}
		}
		
		highLevelAlto.setLocalTranslation(0, altosBeforeMe * 40, 0);
		
		for (AltoSaxophoneClone clone : clones) {
			clone.tick(time,delta);
		}
	}
	
	private static class NotePeriod {
		final int midiNote;
		final double startTime;
		final double endTime;
		final MidiNoteOnEvent noteOn;
		final MidiNoteOffEvent noteOff;
		
		public long startTick() {
			return noteOn.time;
		}
		public long endTick() {
			return noteOff.time;
		}
		
		public NotePeriod(int midiNote, double startTime, double endTime, long startTick, long endTick,
		                  MidiNoteOnEvent noteOn, MidiNoteOffEvent noteOff) {
			this.midiNote = midiNote;
			this.startTime = startTime;
			this.endTime = endTime;
			this.noteOn = noteOn;
			this.noteOff = noteOff;
		}
		
		double duration() {
			return endTime - startTime;
		}
	}
}
