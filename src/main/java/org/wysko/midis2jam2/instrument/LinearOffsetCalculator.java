package org.wysko.midis2jam2.instrument;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;

/**
 * For when instruments that should simply move in a linear line when multiple of them are visible.
 */
public class LinearOffsetCalculator implements MultiChannelOffsetCalculator {
	private final Vector3f locationOffset;
	private final Node nodeToAdjust;
	
	/**
	 * Instantiates a new linear offset calculator.
	 *  @param locationOffset the location offset
	 * @param nodeToAdjust   the node to adjust
	 */
	public LinearOffsetCalculator(@NotNull Vector3f locationOffset,
	                              @NotNull Node nodeToAdjust) {
		this.locationOffset = locationOffset;
		this.nodeToAdjust = nodeToAdjust;
	}
	
	@Override
	public void move(int index) {
		this.nodeToAdjust.setLocalTranslation(locationOffset.mult(index + 1));
	}
}
