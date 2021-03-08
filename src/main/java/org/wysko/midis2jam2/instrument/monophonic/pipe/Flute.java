package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.math.Quaternion;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Flute extends MonophonicInstrument {
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
		
		calculateNotePeriods(notes);
		calculateClones(this, FluteClone.class);
		
		
		for (MonophonicClone clone : clones) {
			FluteClone fluteClone = ((FluteClone) clone);
			groupOfPolyphony.attachChild(fluteClone.cloneNode);
		}
		
		highestLevel.attachChild(groupOfPolyphony);
		
		// Flute positioning
		groupOfPolyphony.setLocalTranslation(0, 70, 0);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-80), rad(-60), rad(0)));
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		clones.forEach(clone -> clone.tick(time, delta));
	}
	
	public class FluteClone extends HandedClone {
		SteamPuffer puffer;
		
		public FluteClone() {
			// 0-12 left hand
			horn = Flute.this.context.loadModel("Flute.obj", "HornSkinGrey.bmp");
			leftHands = new Spatial[13];
			for (int i = 0; i < 13; i++) {
				leftHands[i] = Flute.this.context.loadModel(String.format("Flute_LeftHand%02d.obj", i), "hands.bmp");
				leftHandNode.attachChild(leftHands[i]);
				if (i != 0) leftHands[i].setCullHint(Spatial.CullHint.Always);
			}
			// 0-12 left hand
			rightHands = new Spatial[12];
			for (int i = 0; i < 12; i++) {
				rightHands[i] = Flute.this.context.loadModel(String.format("Flute_RightHand%02d.obj", i), "hands.bmp");
				rightHandNode.attachChild(rightHands[i]);
				if (i != 0) rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
			
			puffer = new SteamPuffer(Flute.this.context, SteamPuffer.SteamPuffType.WHISTLE);
			cloneNode.attachChild(puffer.steamPuffNode);
			puffer.steamPuffNode.setLocalRotation(new Quaternion().fromAngles(new float[] {0,0, rad(-90)}));
			puffer.steamPuffNode.setLocalTranslation(0,-12.3f,0);
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
			}
		}
	}
}
