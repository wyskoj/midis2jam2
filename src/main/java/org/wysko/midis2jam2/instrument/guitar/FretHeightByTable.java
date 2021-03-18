package org.wysko.midis2jam2.instrument.guitar;

import java.util.HashMap;

/**
 * Calculates fret heights using a lookup table.
 */
public class FretHeightByTable implements FretHeightCalculator {
	
	/**
	 * The lookup table. The key is the fret and the value is the scaling.
	 */
	final HashMap<Integer, Float> lookupTable;
	
	/**
	 * Instantiates a new Fret height by table.
	 *
	 * @param lookupTable the lookup table
	 */
	public FretHeightByTable(HashMap<Integer, Float> lookupTable) {
		this.lookupTable = lookupTable;
	}
	
	@Override
	public float scale(int fret) {
		return lookupTable.get(fret);
	}
}
