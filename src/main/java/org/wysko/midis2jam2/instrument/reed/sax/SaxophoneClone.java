package org.wysko.midis2jam2.instrument.reed.sax;

import com.jme3.math.Quaternion;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.UpAndDownKeyClone;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * Shared code for sax clones.
 */
public abstract class SaxophoneClone extends UpAndDownKeyClone {
	
	/**
	 * The number of keys on a saxophone.
	 */
	private final static int NUMBER_OF_KEYS = 20;
	
	/**
	 * The amount to rotate the sax by when playing.
	 */
	private final static float ROTATION_FACTOR = 0.1f;
	
	/**
	 * Instantiates a new Saxophone clone.
	 *
	 * @param parent        the parent
	 * @param stretchFactor the stretch factor
	 */
	public SaxophoneClone(Saxophone parent, float stretchFactor) {
		super(NUMBER_OF_KEYS, parent, ROTATION_FACTOR, stretchFactor);
		
		for (int i = 0; i < keyCount; i++) {
			keysUp[i] = parent.context.loadModel(String.format("AltoSaxKeyUp%d.obj", i),
					"HornSkinGrey.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			
			keysDown[i] = parent.context.loadModel(String.format("AltoSaxKeyDown%d.obj", i),
					"HornSkinGrey.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
		}
		
		attachKeys();
	}
	
	@Override
	protected void moveForPolyphony() {
		offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(25 * indexForMoving()), 0));
	}
}
