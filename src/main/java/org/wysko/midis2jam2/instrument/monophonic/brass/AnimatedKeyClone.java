package org.wysko.midis2jam2.instrument.monophonic.brass;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.instrument.monophonic.Axis;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.monophonic.StretchyClone;

import java.util.Map;

public abstract class AnimatedKeyClone extends StretchyClone {
	
	/**
	 * The keys of the instrument.
	 */
	@NotNull
	protected Spatial[] keys;
	
	@NotNull
	Map<Integer, Boolean[]> keyMap;
	
	public AnimatedKeyClone(MonophonicInstrument parent,
	                        float rotationFactor,
	                        float stretchFactor,
	                        @NotNull Map<Integer, Boolean[]> keyMap,
	                        int numberOfKeys,
	                        Axis stretchAxis) {
		
		super(parent, rotationFactor, stretchFactor, stretchAxis);
		this.keys = new Spatial[numberOfKeys];
		this.keyMap = keyMap;
	}
	
	/**
	 * Animates a key.
	 *
	 * @param pressed  true if this key is pressed, false otherwise
	 */
	protected abstract void animateKeys(@NotNull Boolean[] pressed);
	
	@Override
	protected void tick(double time, float delta) {
		super.tick(time, delta);
		if (currentNotePeriod != null) {
			Boolean[] booleans = keyMap.get(currentNotePeriod.midiNote);
			if (booleans != null) {
				animateKeys(booleans);
			}
		}
	}
}
