package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

/**
 * Instruments that have separate geometry for up keys and down keys.
 */
public abstract class UpAndDownKeyClone extends StretchyClone {
	
	/**
	 * Instantiates a new Up and down key clone.
	 *
	 * @param keyCount the key count
	 */
	public UpAndDownKeyClone(int keyCount,
	                         @NotNull MonophonicInstrument parent,
	                         float rotationFactor,
	                         float stretchFactor,
	                         @NotNull Map<Integer, Integer[]> keyMap) {
		
		super(parent, rotationFactor, stretchFactor, Axis.Y);
		this.keyCount = keyCount;
		this.keyMap = keyMap;
		
		keysUp = new Spatial[keyCount];
		keysDown = new Spatial[keyCount];
	}
	
	/**
	 * Geometry for keys up.
	 */
	@NotNull
	protected Spatial[] keysUp;
	
	/**
	 * Geometry for keys down.
	 */
	@NotNull
	protected Spatial[] keysDown;
	
	/**
	 * The number of keys on this clone.
	 */
	protected final int keyCount;
	
	/**
	 * The key mapping.
	 *
	 * The key is the MIDI note and the value is an array of indices relating to the {@link Spatial}s in
	 * {@link #keysUp} and {@link #keysDown}.
	 */
	@NotNull
	protected final Map<Integer, Integer[]> keyMap;
	
	/**
	 * Given a keymap, presses or releases keys.
	 * <p>
	 * If the instrument cannot play the specified MIDI note, the instrument plays will all keys up (this is
	 * technically incorrect on saxophones, since all open keys is a standard fingering for middle C#, but whatever).
	 *
	 * @param midiNote the MIDI note
	 */
	protected void pushOrReleaseKeys(int midiNote) {
		Integer[] keysToGoDown = keyMap.get(midiNote);
		
		if (keysToGoDown == null) { // A note outside of the range of the instrument
			keysToGoDown = new Integer[0];
		}
		
		for (int i = 0; i < keyCount; i++) {
			int finalI = i;
			if (Arrays.stream(keysToGoDown).anyMatch(a -> a == finalI)) {
				// This is a key that needs to be pressed down.
				keysDown[i].setCullHint(Spatial.CullHint.Dynamic); // Show the key down
				keysUp[i].setCullHint(Spatial.CullHint.Always); // Hide the key up
			} else {
				// This is a key that needs to be released.
				keysDown[i].setCullHint(Spatial.CullHint.Always); // Hide the key down
				keysUp[i].setCullHint(Spatial.CullHint.Dynamic); // Show the key up
			}
		}
		
	}
	
//	protected void animation(double time, int indexThis, float stretchFactor, float rotationFactor, HashMap<Integer, Integer[]> keyMap) {
//		/* Perform animation */
//		if (currentNotePeriod != null) {
//			if (time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime) {
//				bell.setLocalScale(1,
//						(float) ((stretchFactor * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1),
//						1);
//				currentlyPlaying = true;
//			} else {
//				currentlyPlaying = false;
//				bell.setLocalScale(1, 1, 1);
//			}
//			/* Show hide correct keys */
//
//		}
//	}
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		if (isPlaying()) {
			pushOrReleaseKeys(currentNotePeriod.midiNote);
		}
	}
	
	protected void attachKeys() {
		for (int i = 0; i < keyCount; i++) {
			modelNode.attachChild(keysUp[i]);
			modelNode.attachChild(keysDown[i]);
			
			/* Hide the down keys on startup */
			keysDown[i].setCullHint(Spatial.CullHint.Always);
		}
	}
}
