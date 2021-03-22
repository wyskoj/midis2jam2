package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.UpAndDownKeyClone;

import java.util.Map;

public abstract class SaxophoneClone extends UpAndDownKeyClone {
	
	private final static int NUMBER_OF_KEYS = 20;
	
	private final static float ROTATION_FACTOR = 0.1f;
	
	public SaxophoneClone(Saxophone parent, float stretchFactor, Map<Integer, Integer[]> keyMap) {
		super(NUMBER_OF_KEYS, parent, ROTATION_FACTOR, stretchFactor, keyMap);
		
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
		offsetNode.setLocalTranslation(20 * indexForMoving(), 0, 0);
	}
}
