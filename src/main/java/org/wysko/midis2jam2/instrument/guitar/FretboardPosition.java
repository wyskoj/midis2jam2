package org.wysko.midis2jam2.instrument.guitar;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a position on the fretboard.
 */
public class FretboardPosition {
	
	/**
	 * The string of the position.
	 */
	public final int string;
	
	/**
	 * The fret of the position
	 */
	public final int fret;
	
	/**
	 * Constructs a FretboardPosition.
	 *
	 * @param string the string of the position
	 * @param fret   the fret of the position
	 */
	public FretboardPosition(int string, int fret) {
		this.string = string;
		this.fret = fret;
	}
	
	/**
	 * Calculates the distance from this position to another, but ignores variable spacing. Good ol' distance
	 * formula.
	 *
	 * @param o the other fret to find the distance
	 * @return the distance
	 */
	@Contract(pure = true)
	double distance(@NotNull FretboardPosition o) {
		return Math.sqrt(Math.pow(string - o.string, 2) + Math.pow(fret - o.fret, 2));
	}
}
