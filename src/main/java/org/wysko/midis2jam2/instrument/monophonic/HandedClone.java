package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Instruments that have hands. Includes piccolo, flute, recorder, ocarina (for now).
 */
public abstract class HandedClone extends MonophonicClone {
	protected Spatial horn;
	protected Node leftHandNode = new Node();
	protected Node rightHandNode = new Node();
	protected Spatial[] leftHands;
	protected Spatial[] rightHands;
	public static class Hands {
		public final int left;
		public final int right;
		
		public Hands(int left, int right) {
			this.left = left;
			this.right = right;
		}
		
		@Override
		public String toString() {
			return "Hands{" +
					"left=" + left +
					", right=" + right +
					'}';
		}
	}
}
