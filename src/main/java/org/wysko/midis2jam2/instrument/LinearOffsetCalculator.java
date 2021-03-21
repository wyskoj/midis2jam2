package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.jetbrains.annotations.NotNull;

/**
 * For when instruments (or clones) that should simply move in a linear line when multiple of them are visible.
 */
public class LinearOffsetCalculator implements OffsetCalculator {
	
	@NotNull
	private final Vector3f locationOffset;
	
	@NotNull
	private final Quaternion rotationOffset;
	
	/**
	 * Instantiates a new linear offset calculator.
	 *
	 * @param locationOffset the location offset
	 */
	public LinearOffsetCalculator(@NotNull Vector3f locationOffset) {
		this.locationOffset = locationOffset;
		this.rotationOffset = new Quaternion();
	}
	
	/**
	 * Instantiates a new linear offset calculator.s
	 *
	 * @param locationOffset the location offset
	 * @param rotationOffset the rotation offset
	 */
	public LinearOffsetCalculator(@NotNull Vector3f locationOffset,
	                              @NotNull Quaternion rotationOffset) {
		this.locationOffset = locationOffset;
		this.rotationOffset = rotationOffset;
	}
	
	@Override
	public LocationAndRotation calc(int index) {
		return new LocationAndRotation(locationOffset.mult(index),
				index == 0 ? new Quaternion() : rotationOffset.mult(index));
	}
}
