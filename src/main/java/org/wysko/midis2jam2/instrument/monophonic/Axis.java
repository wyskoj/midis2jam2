package org.wysko.midis2jam2.instrument.monophonic;

/**
 * An axis.
 */
public enum Axis {
	
	/**
	 * The X-axis.
	 */
	X(0),
	
	/**
	 * The Y-axis.
	 */
	Y(1),
	
	/**
	 * The Z-axis
	 */
	Z(2);
	
	public int index;
	
	Axis(int index) {
		this.index = index;
	}
}
