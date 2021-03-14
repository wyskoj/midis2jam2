package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.particle.SteamPuffer;

public class FluteAndPiccoloClone extends HandedClone {
	
	SteamPuffer puffer;
	HandedInstrument parent;
	
	public FluteAndPiccoloClone(HandedInstrument parent) {
		this.parent = parent;
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
			final Hands hands = parent.KEY_MAPPING.get(midiNote);
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
		final int myIndex = parent.clones.indexOf(this);
		hideOrShowOnPolyphony(myIndex);
		cloneNode.setLocalTranslation(myIndex * 5, 0, -myIndex * 5);
	}
	
	protected void loadHands() {
		leftHands = new Spatial[13];
		for (int i = 0; i < 13; i++) {
			leftHands[i] = parent.context.loadModel(String.format("Flute_LeftHand%02d.obj", i), "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			leftHandNode.attachChild(leftHands[i]);
			if (i != 0) {
				leftHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		// 0-11 right hand
		rightHands = new Spatial[12];
		for (int i = 0; i < 12; i++) {
			rightHands[i] = parent.context.loadModel(String.format("Flute_RightHand%02d.obj", i), "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			rightHandNode.attachChild(rightHands[i]);
			if (i != 0) {
				rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		cloneNode.attachChild(leftHandNode);
		cloneNode.attachChild(rightHandNode);
	}
}
