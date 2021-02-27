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
		private final List<Extension> extensions;
		
		
		public boolean isPlayingAtTime(long midiTick) {
			for (Extension extension : extensions) {
				if (midiTick >= extension.startTick && midiTick < extension.endTick) {
					return true;
				}
			}
			return false;
		}
		
		private final Spatial bell;
		private final Spatial body;
		
		Extension currentExtension;
		
		public AltoSaxophoneClone(List<Extension> extensions) {
			this.extensions = extensions;
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
			int mySpot = AltoSaxophone.this.clones.indexOf(this);
			
			polyphonicAlto.setLocalTranslation(20*mySpot,0,0);
			
			while (!extensions.isEmpty() && extensions.get(0).startTime <= time) {
				currentExtension = extensions.remove(0);
			}
			
			if (currentExtension != null) {
				if (time >= currentExtension.startTime && time <= currentExtension.endTime) {
					bell.setLocalScale(1, (float) ((0.5f * (currentExtension.endTime - time) / currentExtension.duration()) + 1), 1);
					animAlto.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentExtension.endTime - time) / currentExtension.duration())) * 0.1f, 0, 0));
				} else {
					bell.setLocalScale(1, 1, 1);
				}
			}
		}
	}
	private final List<MidiNoteEvent> noteEvents = new ArrayList<>();
	private final Midis2jam2 context;
	Node highLevelAlto = new Node();
	Node groupOfPolyphonies = new Node();
	MidiFile file;
	private List<Extension> extensions;
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
		
		calculateExtensions();
		calculateClones();
		
		for (AltoSaxophoneClone clone : clones) {
			groupOfPolyphonies.attachChild(clone.polyphonicAlto);
		}
		
		highLevelAlto.attachChild(groupOfPolyphonies);
		
		groupOfPolyphonies.move(-14, 41.5f, -45);
		groupOfPolyphonies.rotate(rad(13), rad(75), 0);
		
		context.getRootNode().attachChild(highLevelAlto);
	}
	
	private void calculateClones() {
		clones = new ArrayList<>();
		clones.add(new AltoSaxophoneClone(new ArrayList<>()));
		for (int i = 0; i < extensions.size(); i++) {
			for (int j = 0; j < extensions.size(); j++) {
				if (j == i) continue;
				Extension comp1 = extensions.get(i);
				Extension comp2 = extensions.get(j);
				if (comp1.startTick > comp2.endTick || comp1.endTick < comp2.startTick) continue;
				if (comp1.startTick >= comp2.startTick && comp1.startTick <= comp2.endTick) { // Overlapping note
					boolean added = false;
					for (AltoSaxophoneClone clone : clones) {
						if (!clone.isPlayingAtTime(comp1.startTick)) {
							clone.extensions.add(comp1);
							added = true;
							break;
						}
					}
					if (!added) {
						AltoSaxophoneClone e = new AltoSaxophoneClone(new ArrayList<>());
						e.extensions.add(comp1);
						clones.add(e);
					}
				} else {
					clones.get(0).extensions.add(comp1);
				}
				break;
			}
		}
	}
	
	private void calculateExtensions() {
		extensions = new ArrayList<>();
		for (int i = 0, noteEventsSize = noteEvents.size(); i < noteEventsSize; i++) {
			MidiNoteEvent noteEvent = noteEvents.get(i);
			if (noteEvent instanceof MidiNoteOnEvent) {
				for (int j = i + 1; j < noteEventsSize; j++) {
					MidiNoteEvent check = noteEvents.get(j);
					if (check instanceof MidiNoteOffEvent && check.note == noteEvent.note) {
						// We found a block
						extensions.add(new Extension(check.note, file.eventInSeconds(noteEvent),
								file.eventInSeconds(check), noteEvent.time, check.time));
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
	
	private static class Extension {
		final int midiNote;
		final double startTime;
		final double endTime;
		final long startTick;
		final long endTick;
		
		public Extension(int midiNote, double startTime, double endTime, long startTick, long endTick) {
			this.midiNote = midiNote;
			this.startTime = startTime;
			this.endTime = endTime;
			this.startTick = startTick;
			this.endTick = endTick;
		}
		
		double duration() {
			return endTime - startTime;
		}
	}
}
