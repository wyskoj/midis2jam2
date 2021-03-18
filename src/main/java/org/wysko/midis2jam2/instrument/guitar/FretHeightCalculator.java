package org.wysko.midis2jam2.instrument.guitar;

/**
 * The fret height calculator returns the amount to scale upper and lower strings by.
 */
public interface FretHeightCalculator {
	
	/**
	 * Calculates the scale of the strings given a fret.
	 *
	 * @param fret the fret
	 * @return the scale of the strings
	 */
	float scale(int fret);
}
