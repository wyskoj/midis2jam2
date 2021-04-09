/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.instrument.clone;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.world.Axis;

import java.util.Arrays;

/**
 * Instruments that have separate geometry for up keys and down keys.
 */
public abstract class UpAndDownKeyClone extends StretchyClone {
	
	/**
	 * Geometry for keys up.
	 */
	@NotNull
	protected final Spatial[] keysUp;
	
	/**
	 * Geometry for keys down.
	 */
	@NotNull
	protected final Spatial[] keysDown;
	
	/**
	 * The number of keys on this clone.
	 */
	protected final int keyCount;
	
	/**
	 * Instantiates a new Up and down key clone.
	 *
	 * @param keyCount the key count
	 */
	public UpAndDownKeyClone(int keyCount,
	                         @NotNull MonophonicInstrument parent,
	                         float rotationFactor,
	                         float stretchFactor) {
		
		super(parent, rotationFactor, stretchFactor, Axis.Y, Axis.X);
		this.keyCount = keyCount;
		
		keysUp = new Spatial[keyCount];
		keysDown = new Spatial[keyCount];
	}
	
	/**
	 * Given a keymap, presses or releases keys.
	 * <p>
	 * If the instrument cannot play the specified MIDI note, the instrument plays will all keys up (this is
	 * technically incorrect on saxophones, since all open keys is a standard fingering for middle C#, but whatever).
	 *
	 * @param midiNote the MIDI note
	 */
	protected void pushOrReleaseKeys(int midiNote) {
		assert parent.manager != null;
		Integer[] keysToGoDown = (Integer[]) parent.manager.fingering(midiNote);
		
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
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		if (isPlaying()) {
			assert currentNotePeriod != null;
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
