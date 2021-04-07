package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;

/**
 * Animates keys by indices of keys.
 */
public abstract class AnimatedKeyCloneByIntegers extends StretchyClone {
	
	/**
	 * The keys of the instrument.
	 */
	@NotNull
	protected final Spatial[] keys;
	
	
	/**
	 * Instantiates a new Animated key clone by integers.
	 *
	 * @param parent         the parent
	 * @param rotationFactor the rotation factor
	 * @param stretchFactor  the stretch factor
	 * @param numberOfKeys   the number of keys
	 * @param stretchAxis    the stretch axis
	 * @param rotationAxis   the rotation axis
	 */
	public AnimatedKeyCloneByIntegers(MonophonicInstrument parent,
	                                  float rotationFactor,
	                                  float stretchFactor,
	                                  int numberOfKeys,
	                                  Axis stretchAxis, Axis rotationAxis) {
		
		super(parent, rotationFactor, stretchFactor, stretchAxis, rotationAxis);
		this.keys = new Spatial[numberOfKeys];
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
			assert parent.manager != null;
			Integer[] Integers = (Integer[]) parent.manager.fingering(currentNotePeriod.midiNote);
			if (Integers != null) {
				animateKeys(Integers);
			}
		}
	}
}
