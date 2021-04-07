package org.wysko.midis2jam2.instrument.pipe;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.pipe.Flute.FluteClone;
import org.wysko.midis2jam2.instrument.pipe.Piccolo.PiccoloClone;
import org.wysko.midis2jam2.particle.SteamPuffer;

/**
 * Contains shared code between the {@link FluteClone} and {@link PiccoloClone}
 */
public class FluteAndPiccoloClone extends PuffingClone {
	
	/**
	 * Instantiates a new flute/piccolo clone.
	 *
	 * @param parent    the parent
	 * @param puffType  the puff type
	 * @param puffScale the puff scale
	 */
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
