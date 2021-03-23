package org.wysko.midis2jam2.instrument.guitar;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fretting engine handles the calculations of determining which frets to press.
 */
public interface FrettingEngine {
	
	/**
	 * Calculates the best fretboard location for the specified MIDI note with temporal consideration. If no possible
	 * positions exists (all the strings are occupied), returns null.
	 *
	 * @param midiNote the MIDI note to find the best fretboard position
	 * @return the best fretboard position, or null if one does not exist
	 */
	@Nullable
	@Contract(pure = true)
	FretboardPosition bestFretboardPosition(int midiNote);
	
	/**
	 * Applies the usage of this fretboard position, occupying the string. Adds the position to the running average.
	 *
	 * @param position the fretboard position to occupy
	 */
	void applyFretboardPosition(@NotNull FretboardPosition position);
	
	/**
	 * Releases a string, stopping the animation on it and allowing it to be used for another note.
	 *
	 * @param string the string to release
	 */
	void releaseString(int string);
	
	/**
	 * Returns the current fret configuration.
	 *
	 * @return the current fret configuration
	 */
	@Contract(pure = true)
	int[] getFrets();
	
	/**
	 * Represents a position on the fretboard.
	 */
	class FretboardPosition {
		
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
}
