/*
 * Copyright (C) 2021 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.instrument.family.guitar;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The fretting engine handles the calculations of determining which frets to press.
 * <p>
 * This fretting algorithm attempts to use a running average of the last used frets to avoid drifting across the
 * fretboard. This does however mean that certain shapes on the fretboard may change, even though the notes are the
 * same. This solves an issue from MIDIJam (reduces fretboard sliding) but introduces another (shapes are not
 * consistent).
 */
public class StandardFrettingEngine implements FrettingEngine {
	
	/**
	 * The number of note periods to maintain in the running average.
	 */
	private static final int RUNNING_AVERAGE_COUNT = 10;
	
	/**
	 * The array that keeps track of which strings are holding down which frets at any given time.
	 */
	private final int[] frets;
	
	/**
	 * The MIDI note of each open string.
	 */
	private final int[] openStringMidiNotes;
	
	/**
	 * The lowest note this engine with deal with.
	 */
	private final int rangeLow;
	
	/**
	 * The highest note this engine will deal with.
	 */
	private final int rangeHigh;
	
	/**
	 * The number of frets on this instrument.
	 */
	private final int numberOfFrets;
	
	/**
	 * The number of strings to associate this engine with.
	 */
	private final int numberOfStrings;
	
	/**
	 * The list of fretboard positions in the running average.
	 */
	private final List<FretboardPosition> runningAverage = new ArrayList<>();
	
	/**
	 * Constructs a FrettingEngine.
	 *
	 * @param numberOfStrings     the number of strings this instrument has (e.g., a guitar has 6)
	 * @param numberOfFrets       the number of frets on this instrument, that is, the highest fret value
	 * @param openStringMidiNotes the MIDI note of each string, if played with no pressed fret (open string)
	 * @param rangeLow            the lowest MIDI note this instrument can play (the MIDI note of the lowest open
	 *                            string)
	 * @param rangeHigh           the highest MIDI note this instrument can play (the MIDI note of the highest fret on
	 *                            the highest string)
	 */
	public StandardFrettingEngine(int numberOfStrings, int numberOfFrets, int[] openStringMidiNotes, int rangeLow,
	                              int rangeHigh) {
		if (openStringMidiNotes.length != numberOfStrings)
			throw new IllegalArgumentException("The number of strings does not equal the number of data in the open string MIDI notes.");
		
		/* Initialize frets array */
		frets = new int[numberOfStrings];
		Arrays.fill(frets, -1);
		
		this.openStringMidiNotes = openStringMidiNotes;
		this.rangeLow = rangeLow;
		this.rangeHigh = rangeHigh;
		this.numberOfFrets = numberOfFrets;
		this.numberOfStrings = numberOfStrings;
	}
	
	/**
	 * Calculates the best fretboard location for the specified MIDI note with temporal consideration. If no possible
	 * positions exists (all the strings are occupied), returns null.
	 *
	 * @param midiNote the MIDI note to find the best fretboard position
	 * @return the best fretboard position, or null if one does not exist
	 */
	@Override
	@Nullable
	@Contract(pure = true)
	public FretboardPosition bestFretboardPosition(int midiNote) {
		List<FretboardPosition> possiblePositions = new ArrayList<>();
		if (midiNote >= rangeLow && midiNote <= rangeHigh) {
			// String starting notes
			for (var i = 0; i < numberOfStrings; i++) {
				int fret = midiNote - openStringMidiNotes[i];
				if (fret < 0 || fret > numberOfFrets || frets[i] != -1) {
					// The note will not fit on this string, or we are not allowed to
					continue;
				}
				possiblePositions.add(new FretboardPosition(i, fret));
			}
			
		}
		possiblePositions.sort(Comparator.comparingDouble(o -> o.distance(runningAveragePosition())));
		return possiblePositions.isEmpty() ? null : possiblePositions.get(0);
	}
	
	/**
	 * Applies the usage of this fretboard position, occupying the string. Adds the position to the running average.
	 *
	 * @param position the fretboard position to occupy
	 */
	@Override
	public void applyFretboardPosition(@NotNull FretboardPosition position) {
		/* Fail if the position is already occupied */
		if (frets[position.string] != -1) throw new IllegalStateException("The specified string is already occupied.");
		
		frets[position.string] = position.fret;
		runningAverage.add(position);
		if (runningAverage.size() > RUNNING_AVERAGE_COUNT) runningAverage.remove(0);
	}
	
	/**
	 * Calculates the running average position. If no frets have been previously applied, returns the position at
	 * {@code 0,0}.
	 *
	 * @return the running average position, or {@code 0,0} if no frets have been applied yet
	 */
	@NotNull
	@Contract(pure = true)
	private FretboardPosition runningAveragePosition() {
		// TODO Experiment with harmonic/geometric mean?
		if (runningAverage.isEmpty()) {
			return new FretboardPosition(0, 0);
		}
		var stringAvg = 0;
		var fretAvg = 0;
		for (FretboardPosition pos : runningAverage) {
			stringAvg += pos.string;
			fretAvg += pos.fret;
		}
		return new FretboardPosition(
				(int) (Math.round((double) stringAvg / runningAverage.size())),
				(int) Math.round((double) fretAvg / runningAverage.size())
		);
	}
	
	/**
	 * Releases a string, stopping the animation on it and allowing it to be used for another note.
	 *
	 * @param string the string to release
	 */
	@Override
	public void releaseString(int string) {
		if (string >= numberOfStrings || string < 0)
			throw new IllegalArgumentException("Can't release a string that does not exist.");
		frets[string] = -1;
	}
	
	/**
	 * Returns the current fret configuration.
	 *
	 * @return the current fret configuration
	 */
	@Override
	@Contract(pure = true)
	public int[] getFrets() {
		return frets;
	}
	
}
