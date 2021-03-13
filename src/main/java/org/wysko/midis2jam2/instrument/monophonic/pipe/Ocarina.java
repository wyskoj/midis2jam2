package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiNoteEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Ocarina extends HandedInstrument {
	
	/**
	 * Constructs an ocarina.
	 *
	 * @param context context to midis2jam2
	 * @param file    context to the midi file
	 */
	public Ocarina(Midis2jam2 context, List<MidiChannelSpecificEvent> events,
	               MidiFile file) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		super(context, file);
		
		List<MidiNoteEvent> notes =
				events.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> (MidiNoteEvent) e)
						.collect(Collectors.toList());
		
		this.notePeriods = calculateNotePeriods(notes);
		calculateClones(this, Ocarina.OcarinaClone.class);
		
		
		for (MonophonicClone clone : clones) {
			OcarinaClone ocarinaClone = ((OcarinaClone) clone);
			groupOfPolyphony.attachChild(ocarinaClone.cloneNode);
		}
		
		highestLevel.attachChild(groupOfPolyphony);
		context.getRootNode().attachChild(highestLevel);
		groupOfPolyphony.setLocalTranslation(45, 47, 20);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(0, rad(135), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		updateClones(time, delta, new Vector3f(0, 10, 0));
	}
	
	public class OcarinaClone extends HandedClone {
		public OcarinaClone() {
			super();
			cloneNode = new Node();
			horn = context.loadModel("Ocarina.obj", "Ocarina.bmp", Midis2jam2.MatType.UNSHADED);
			animNode.attachChild(horn);
			cloneNode.attachChild(animNode);
			loadHands();
			for (int i = 0; i < rightHands.length; i++) {
				if (i == 0) rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
				else rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		private void loadHands() {
			rightHands = new Spatial[12];
			for (int i = 0; i < 12; i++) {
				rightHands[i] = context.loadModel("OcarinaHand" + i + ".obj", "hands.bmp", Midis2jam2.MatType.UNSHADED);
			}
			for (Spatial rightHand : rightHands) {
				rightHandNode.attachChild(rightHand);
			}
			animNode.attachChild(rightHandNode);
		}
		
		@Override
		public void tick(double time, float delta) {
			/* Collect note periods to execute */
			while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
				currentNotePeriod = notePeriods.remove(0);
			}
			if (currentNotePeriod != null) {
				currentlyPlaying = time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime;
				/* Set the hands */
				final int midiNote = currentNotePeriod.midiNote;
				int hand = (midiNote + 3) % 12;
				for (int i = 0; i < rightHands.length; i++) {
					if (i == hand) rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
					else rightHands[i].setCullHint(Spatial.CullHint.Always);
				}
				if (currentlyPlaying) {
					animNode.setLocalTranslation(0,
							0, 3 * (float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration()));
				}
			}
			final int myIndex = Ocarina.this.clones.indexOf(this);
			hideOrShowOnPolyphony(myIndex);
			cloneNode.setLocalTranslation(myIndex * 10, 0, 0);
		}
	}
}
