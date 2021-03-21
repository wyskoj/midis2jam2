package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.particle.SteamPuffer;

public class FluteAndPiccoloClone extends PuffingClone {
	
	public FluteAndPiccoloClone(HandedInstrument parent,
	                            SteamPuffer.SteamPuffType puffType,
	                            float puffScale) {
		
		super(parent, 0, puffType, puffScale);
		
	}
	
	
	/**
	 * Loads the left and right hands for flute and piccolo.
	 */
	@Override
	protected void loadHands() {
		leftHands = new Spatial[13];
		for (int i = 0; i < 13; i++) {
			leftHands[i] = parent.context.loadModel(String.format("Flute_LeftHand%02d.obj", i), "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			leftHandNode.attachChild(leftHands[i]);
			if (i != 0) {
				leftHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		rightHands = new Spatial[12];
		for (int i = 0; i < 12; i++) {
			rightHands[i] = parent.context.loadModel(String.format("Flute_RightHand%02d.obj", i), "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			rightHandNode.attachChild(rightHands[i]);
			if (i != 0) {
				rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		
	}
	
	@Override
	protected void moveForPolyphony() {
		offsetNode.setLocalTranslation(5 * indexForMoving(), 0, 5 * -indexForMoving());
	}
}
