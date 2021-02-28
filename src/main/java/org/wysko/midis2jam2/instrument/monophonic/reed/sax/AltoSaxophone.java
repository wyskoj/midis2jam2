package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The alto saxophone.
 */
public class AltoSaxophone extends MonophonicInstrument<AltoSaxophone.AltoSaxophoneClone> implements Instrument {
	
	Node highLevelAlto = new Node();
	Node groupOfPolyphony = new Node();
	
	/**
	 * Constructs an alto saxophone.
	 *
	 * @param context context to midis2jam2
	 * @param events  all events that pertain to this instance of an alto saxophone
	 * @param file    context to the MIDI file
	 */
	public AltoSaxophone(Midis2jam2 context, List<MidiChannelSpecificEvent> events, MidiFile file) {
		super(context, file);
		
		List<MidiNoteEvent> noteEvents = events.stream()
				.filter(event -> event instanceof MidiNoteOnEvent || event instanceof MidiNoteOffEvent)
				.map(event -> (MidiNoteEvent) event).collect(Collectors.toList());
		
		calculateNotePeriods(noteEvents);
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
	 * TODO Can this pulled up somehow?
	 */
	private void calculateClones() {
		clones = new ArrayList<>();
		clones.add(new AltoSaxophoneClone());
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
						AltoSaxophoneClone e = new AltoSaxophoneClone();
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
			clone.tick(time, delta);
		}
	}
	
	/**
	 * Implements {@link MonophonicClone}, as alto saxophone clones.
	 */
	public class AltoSaxophoneClone extends MonophonicClone {
		
		private final Spatial bell;
		private final Spatial body;
		Node polyphonicAlto = new Node();
		
		public AltoSaxophoneClone() {
			this.body = AltoSaxophone.this.context.loadModel("AltoSaxBody.obj", "HornSkin.png");
			this.bell = AltoSaxophone.this.context.loadModel("AltoSaxHorn.obj", "HornSkin.png");
			
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			bell.move(0, -22, 0); // Move bell down to body
			
			animNode.attachChild(modelNode);
			polyphonicAlto.attachChild(animNode);
		}
		
		@Override
		public void tick(double time, float delta) {
			int indexThis = AltoSaxophone.this.clones.indexOf(this);
			
			/* Hide or show depending on degree of polyphony and current playing status */
			if (currentlyPlaying || indexThis == 0) {
				// Show
				body.setCullHint(Spatial.CullHint.Dynamic);
				bell.setCullHint(Spatial.CullHint.Dynamic);
			} else {
				// Hide
				bell.setCullHint(Spatial.CullHint.Always);
				body.setCullHint(Spatial.CullHint.Always);
			}
			
			/* Collect note periods to execute */
			while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
				currentNotePeriod = notePeriods.remove(0);
			}
			
			/* Perform animation */
			if (currentNotePeriod != null) {
				if (time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime) {
					bell.setLocalScale(1, (float) ((0.5f * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1), 1);
					animNode.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * 0.1f, 0, 0));
					currentlyPlaying = true;
				} else {
					currentlyPlaying = false;
					bell.setLocalScale(1, 1, 1);
				}
			}
			
			/* Move depending on degree of polyphony */
			polyphonicAlto.setLocalTranslation(20 * indexThis, 0, 0);
			
		}
	}
	
}
