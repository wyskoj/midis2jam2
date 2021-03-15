package org.wysko.midis2jam2.instrument.strings;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Range;

/**
 * Represents a position on the fretboard of the guitar, including its string and fret.
 */
class FretboardPosition {
	
	/**
	 * The string of the guitar.
	 */
	@Range(from = 0, to = 4)
	final
	int string;
	/**
	 * The fret of the guitar. A fret of 0 is an open string.
	 */
	@Range(from = 0, to = 22)
	final
	int fret;
	
	public FretboardPosition(int string, int fret) {
		this.string = string;
		this.fret = fret;
	}
	
	/**
	 * Calculates the distance from this fret to another, but ignores variable spacing. Good ol' distance formula.
	 *
	 * @param other the other fret to find the distance
	 * @return the distance
	 */
	@SuppressWarnings("unused") // TODO Use this when implementing a better fretting algorithm
	@Contract(pure = true)
	double distance(FretboardPosition other) {
		return Math.sqrt(Math.pow(string - other.string, 2) + Math.pow(fret - other.fret, 2));
	}
	
	@Override
	public String toString() {
		return "GuitarPosition{string=" + string +
				", fret=" + fret +
				'}';
	}
	
}
