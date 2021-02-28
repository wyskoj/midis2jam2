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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The alto saxophone.
 */
public class AltoSaxophone extends MonophonicInstrument<AltoSaxophone.AltoSaxophoneClone> implements Instrument {
	
	private final static int KEY_COUNT = 20;
	private final static float STRETCH_FACTOR = 0.65f;
	/**
	 * Defines which keys need to be pressed given the corresponding MIDI note.
	 */
	private final static HashMap<Integer, Integer[]> KEY_MAPPING = new HashMap<Integer, Integer[]>() {{
		/*
		0 - Palm F
		1 - Palm E
		2 - Palm D
		3 - B
		4 - Bis
		5 - A/C
		6 - G
		7 - G#
		8 - Low B
		9 - Low C#
		10 - Low Bb
		11 - Side E
		12 - Side C
		13 - Side Bb
		14 - F
		15 - E
		16 - Side F#
		17 - D
		18 - Eb
		19 - Low C
		 */
		
		put(80, new Integer[] {2, 1, 0, 11}); // 15va F
		put(79, new Integer[] {2, 1, 11}); // 15va E
		put(78, new Integer[] {2, 1}); // 15va Eb
		put(77, new Integer[] {2}); // 15va D
		put(76, new Integer[] {}); // C#
		put(75, new Integer[] {5}); // C
		put(74, new Integer[] {3}); // B
		put(73, new Integer[] {3, 5, 13}); // Bb
		put(72, new Integer[] {3, 5}); // A
		put(71, new Integer[] {3, 5, 6, 7}); // G#
		put(70, new Integer[] {3, 5, 6}); // G
		put(69, new Integer[] {3, 5, 6, 15}); // F#
		put(68, new Integer[] {3, 5, 6, 14}); // F
		put(67, new Integer[] {3, 5, 6, 14, 15}); // E
		put(66, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(65, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(64, new Integer[] {}); // C#
		put(63, new Integer[] {5}); // C
		put(62, new Integer[] {3}); // B
		put(61, new Integer[] {3, 5, 13}); // Bb
		put(60, new Integer[] {3, 5}); // A
		put(59, new Integer[] {3, 5, 6, 7}); // G#
		put(58, new Integer[] {3, 5, 6}); // G
		put(57, new Integer[] {3, 5, 6, 15}); // F#
		put(56, new Integer[] {3, 5, 6, 14}); // F
		put(55, new Integer[] {3, 5, 6, 14, 15}); // E
		put(54, new Integer[] {3, 5, 6, 14, 15, 17, 18}); // Eb
		put(53, new Integer[] {3, 5, 6, 14, 15, 17}); // D
		put(52, new Integer[] {3, 5, 6, 14, 15, 17, 9, 19}); // C#
		put(51, new Integer[] {3, 5, 6, 14, 15, 17, 19}); // C
		put(50, new Integer[] {3, 5, 6, 14, 15, 17, 19, 8}); // B
		put(49, new Integer[] {3, 5, 6, 14, 15, 17, 19, 10}); // Bb
	}};
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
		private final Spatial[] KEYS_UP = new Spatial[KEY_COUNT];
		private final Spatial[] KEYS_DOWN = new Spatial[KEY_COUNT];
		Node polyphonicAlto = new Node();
		
		public AltoSaxophoneClone() {
			this.body = AltoSaxophone.this.context.loadModel("AltoSaxBody.obj", "HornSkin.png");
			this.bell = AltoSaxophone.this.context.loadModel("AltoSaxHorn.obj", "HornSkin.png");
			for (int i = 0; i < KEY_COUNT; i++) {
				
				KEYS_UP[i] = AltoSaxophone.this.context.loadModel(String.format("AltoSaxKeyUp%d.obj", i),
						"HornSkinGrey" +
								".png");
				
				KEYS_DOWN[i] = AltoSaxophone.this.context.loadModel(String.format("AltoSaxKeyDown%d.obj", i),
						"HornSkinGrey" +
								".png");
				
				modelNode.attachChild(KEYS_UP[i]);
			}
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
					bell.setLocalScale(1,
							(float) ((STRETCH_FACTOR * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1),
							1);
					animNode.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * 0.1f, 0, 0));
					currentlyPlaying = true;
				} else {
					currentlyPlaying = false;
					bell.setLocalScale(1, 1, 1);
				}
				
				/* Show hide correct keys */
				Integer[] keysToGoDown = KEY_MAPPING.get(currentNotePeriod.midiNote);
				for (int i = 0; i < KEY_COUNT; i++) {
					int finalI = i;
					if (Arrays.stream(keysToGoDown).anyMatch(a -> a == finalI)) {
						// This is a key that needs to be pressed down.
						KEYS_DOWN[i].setCullHint(Spatial.CullHint.Dynamic); // Show the key down
						KEYS_UP[i].setCullHint(Spatial.CullHint.Always); // Hide the key up
					} else {
						// This is a key that needs to be released.
						KEYS_DOWN[i].setCullHint(Spatial.CullHint.Always); // Hide the key down
						KEYS_UP[i].setCullHint(Spatial.CullHint.Dynamic); // Show the key up
					}
				}
			}
			
			
			
			/* Move depending on degree of polyphony */
			polyphonicAlto.setLocalTranslation(20 * indexThis, 0, 0);
			
		}
	}
	
}
