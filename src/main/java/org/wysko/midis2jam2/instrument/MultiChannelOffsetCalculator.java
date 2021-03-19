package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;

public abstract class MultiChannelOffsetCalculator {
	protected final Node nodeToAdjust;
	
	public MultiChannelOffsetCalculator(@NotNull Node nodeToAdjust) {
		this.nodeToAdjust = nodeToAdjust;
	}
	
	abstract void move(int index);
	
	public Node getNodeToAdjust() {
		return nodeToAdjust;
	}
}
