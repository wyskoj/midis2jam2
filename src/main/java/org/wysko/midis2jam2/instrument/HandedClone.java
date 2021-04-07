package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.instrument.pipe.HandedInstrument;

import static org.wysko.midis2jam2.instrument.HandPositionFingeringManager.Hands;

/**
 * Instruments that have hands. Includes piccolo, flute, recorder, ocarina.
 */
public abstract class HandedClone extends Clone {
	
	/**
	 * The Left hand node.
	 */
	protected final Node leftHandNode = new Node();
	
	/**
	 * The Right hand node.
	 */
	protected final Node rightHandNode = new Node();
	
	/**
	 * The Left hands.
	 */
	protected Spatial[] leftHands;
	
	/**
	 * The Right hands.
	 */
	protected Spatial[] rightHands;
	
	/**
	 * Instantiates a new clone.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 */
	public HandedClone(HandedInstrument parent, float rotationFactor) {
		super(parent, rotationFactor, Axis.X);
		modelNode.attachChild(leftHandNode);
		modelNode.attachChild(rightHandNode);
	}
	
	/**
	 * Loads the hands appropriate to this instrument.
	 */
	protected abstract void loadHands();
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		
		if (isPlaying()) {
			/* Set the hands */
			assert currentNotePeriod != null;
			final int midiNote = currentNotePeriod.midiNote;
			final Hands hands = (Hands) parent.manager.fingering(midiNote);
			if (hands != null) {
				// Set the left hands
				if (leftHands != null) {
					/* May be null because ocarina does not implement left hands */
					for (int i = 0; i < leftHands.length; i++) {
						if (i == hands.left) {
							leftHands[i].setCullHint(Spatial.CullHint.Dynamic);
						} else {
							leftHands[i].setCullHint(Spatial.CullHint.Always);
						}
					}
				}
				// Set the right hands
				for (int i = 0; i < rightHands.length; i++) {
					if (i == hands.right) {
						rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
					} else {
						rightHands[i].setCullHint(Spatial.CullHint.Always);
					}
				}
			}
		}
	}
}
