package org.wysko.midis2jam2.instrument;

/**
 * Handles the lookup of fingerings for MIDI notes.
 *
 * @param <E> the type of fingerings to return
 */
public interface FingeringManager<E> {
	
	/**
	 * Given a MIDI note, returns the fingering associated with that note, or null if the note is outside the
	 * instrument's defined range.
	 *
	 * @param midiNote the MIDI note
	 */
	E fingering(int midiNote);
}
