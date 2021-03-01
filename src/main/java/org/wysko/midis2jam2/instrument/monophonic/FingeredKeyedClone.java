package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.instrument.monophonic.reed.sax.Saxophone;

import java.util.Arrays;
import java.util.HashMap;

public abstract class FingeredKeyedClone extends MonophonicClone {
	protected Spatial[] KEYS_UP;
	protected Spatial[] KEYS_DOWN;
	
	protected void pushOrReleaseKeys(HashMap<Integer, Integer[]> keyMap) {
		Integer[] keysToGoDown = keyMap.get(currentNotePeriod.midiNote);
		
		if (keysToGoDown == null) { // A note outside of the range of the instrument
			keysToGoDown = new Integer[0];
		}
		
		for (int i = 0; i < Saxophone.KEY_COUNT; i++) {
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
}
