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
	
}
