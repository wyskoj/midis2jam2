package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Flute extends MonophonicInstrument {
	
	public static final HashMap<Integer, HandedClone.Hands> KEY_MAPPING = new HashMap<Integer, HandedClone.Hands>() {{
		put(60, new HandedClone.Hands(4, 0));
		put(61, new HandedClone.Hands(4, 2));
		put(62, new HandedClone.Hands(4, 3));
		put(63, new HandedClone.Hands(4, 4));
		put(64, new HandedClone.Hands(4, 5));
		put(65, new HandedClone.Hands(4, 6));
		put(66, new HandedClone.Hands(4, 7));
		put(67, new HandedClone.Hands(4, 8));
		put(68, new HandedClone.Hands(2, 8));
		put(69, new HandedClone.Hands(6, 8));
		put(70, new HandedClone.Hands(7, 6));
		put(71, new HandedClone.Hands(7, 8));
		put(72, new HandedClone.Hands(9, 8));
		put(73, new HandedClone.Hands(10, 8));
		put(74, new HandedClone.Hands(5, 3));
		put(75, new HandedClone.Hands(5, 4));
		put(76, new HandedClone.Hands(4, 5));
		put(77, new HandedClone.Hands(4, 6));
		put(78, new HandedClone.Hands(4, 7));
		put(79, new HandedClone.Hands(4, 8));
		put(80, new HandedClone.Hands(2, 8));
		put(81, new HandedClone.Hands(6, 8));
		put(82, new HandedClone.Hands(7, 6));
		put(83, new HandedClone.Hands(7, 8));
		put(84, new HandedClone.Hands(9, 8));
		put(85, new HandedClone.Hands(10, 8));
		put(86, new HandedClone.Hands(5, 8));
		put(87, new HandedClone.Hands(2, 4));
		put(88, new HandedClone.Hands(6, 5));
		put(89, new HandedClone.Hands(11, 6));
		put(90, new HandedClone.Hands(11, 7));
		put(91, new HandedClone.Hands(0, 8));
		put(92, new HandedClone.Hands(3, 8));
		put(93, new HandedClone.Hands(12, 6));
		put(94, new HandedClone.Hands(8, 9));
		put(95, new HandedClone.Hands(11, 10));
		put(96, new HandedClone.Hands(1, 11));
	}};
	
	/**
	 * Constructs a flute.
	 *
	 * @param context context to midis2jam2
	 * @param events  the events to play
	 * @param file    context to the midi file
	 */
	public Flute(Midis2jam2 context, List<MidiChannelSpecificEvent> events,
	             MidiFile file) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		super(context, file);
		List<MidiNoteEvent> notes =
				events.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> (MidiNoteEvent) e)
						.collect(Collectors.toList());
		
		this.notePeriods = calculateNotePeriods(notes);
		calculateClones(this, FluteClone.class);
		
		
		for (MonophonicClone clone : clones) {
			FluteClone fluteClone = ((FluteClone) clone);
			groupOfPolyphony.attachChild(fluteClone.cloneNode);
		}
		
		highestLevel.attachChild(groupOfPolyphony);
		
		// Flute positioning
		groupOfPolyphony.setLocalTranslation(5, 52, -20);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-80), rad(-53), rad(0)));
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		updateClones(time, delta, new Vector3f(0,10,0));
	}
	
	public class FluteClone extends HandedClone {
		final SteamPuffer puffer;
		
		public FluteClone() {
			// 0-12 left hand
			horn = Flute.this.context.loadModel("Flute.obj", "ShinySilver.bmp");
			leftHands = new Spatial[13];
			for (int i = 0; i < 13; i++) {
				leftHands[i] = Flute.this.context.loadModel(String.format("Flute_LeftHand%02d.obj", i), "hands.bmp");
				leftHandNode.attachChild(leftHands[i]);
				if (i != 0) {
					leftHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
			// 0-11 right hand
			rightHands = new Spatial[12];
			for (int i = 0; i < 12; i++) {
				rightHands[i] = Flute.this.context.loadModel(String.format("Flute_RightHand%02d.obj", i), "hands.bmp");
				rightHandNode.attachChild(rightHands[i]);
				if (i != 0) {
					rightHands[i].setCullHint(Spatial.CullHint.Always);
				}
			}
			
			puffer = new SteamPuffer(Flute.this.context, SteamPuffer.SteamPuffType.WHISTLE);
			cloneNode.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[] {0, 0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0, -12.3f, 0);
			cloneNode.attachChild(leftHandNode);
			cloneNode.attachChild(rightHandNode);
			cloneNode.attachChild(horn);
		}
		
		@Override
		public void tick(double time, float delta) {
			/* Collect note periods to execute */
			while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
				currentNotePeriod = notePeriods.remove(0);
			}
			if (currentNotePeriod != null) {
				currentlyPlaying = time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime;
				puffer.tick(time, delta, currentlyPlaying);
				
				/* Set the hands */
				final int midiNote = currentNotePeriod.midiNote;
				final Hands hands = KEY_MAPPING.get(midiNote);
				if (hands != null) {
					
					for (int i = 0; i < leftHands.length; i++) {
						if (i == hands.left) {
							leftHands[i].setCullHint(Spatial.CullHint.Dynamic);
						} else {
							leftHands[i].setCullHint(Spatial.CullHint.Always);
						}
					}
					
					for (int i = 0; i < rightHands.length; i++) {
						if (i == hands.right) {
							rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
						} else {
							rightHands[i].setCullHint(Spatial.CullHint.Always);
						}
					}
					
				}
			}
			/* Move if polyphonic */
			final int myIndex = Flute.this.clones.indexOf(this);
			hideOrShowOnPolyphony(myIndex);
			cloneNode.setLocalTranslation(myIndex * 5, 0, -myIndex * 5);
		}
	}
}
