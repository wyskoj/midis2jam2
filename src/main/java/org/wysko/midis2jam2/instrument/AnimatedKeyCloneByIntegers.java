package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class AnimatedKeyCloneByIntegers extends StretchyClone {
	
	/**
	 * The keys of the instrument.
	 */
	@NotNull
	protected final Spatial[] keys;
	
	@NotNull
	final
	Map<Integer, Integer[]> keyMap;
	
	public AnimatedKeyCloneByIntegers(MonophonicInstrument parent,
	                                  float rotationFactor,
	                                  float stretchFactor,
	                                  @NotNull Map<Integer, Integer[]> keyMap,
	                                  int numberOfKeys,
	                                  Axis stretchAxis, Axis rotationAxis) {
		
		super(parent, rotationFactor, stretchFactor, stretchAxis, rotationAxis);
		this.keys = new Spatial[numberOfKeys];
		this.keyMap = keyMap;
	}
	
	/**
	 * Animates a key.
	 *
	 * @param pressed true if this key is pressed, false otherwise
	 */
	protected abstract void animateKeys(@NotNull Integer[] pressed);
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		if (currentNotePeriod != null) {
			Integer[] Integers = keyMap.get(currentNotePeriod.midiNote);
			if (Integers != null) {
				animateKeys(Integers);
			}
		}
	}
}
