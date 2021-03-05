package org.wysko.midis2jam2.instrument;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiFile;

/**
 * An <i>Instrument</i> is any visual representation of a MIDI instrument. midis2jam2 displays separate instruments
 * for each channel, and also creates new instruments when the program of a channel changes (i.e., the MIDI
 * instrument of the channel changes).
 * <p>
 * Classes that implement Instrument are responsible for handling {@link #tick}, which updates the
 * current animation and note handling for every call.
 * <p>
 * Monophonic clones are also considered to be instruments, as well as their parent.
 *
 * @see MonophonicInstrument
 * @see MonophonicClone
 */
public abstract class Instrument {
	/**
	 * Updates the animation and other necessary frame-dependant calculations.
	 *
	 * @param time  the current time since the beginning of the MIDI file, expressed in seconds
	 * @param delta the amount of time since the last call this method, expressed in seconds
	 */
	public abstract void tick(double time, float delta);
	protected Midis2jam2 context;
}
