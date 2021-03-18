package org.wysko.midis2jam2.instrument;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;

/**
 * A sustained instrument is any instrument that also depends on knowing the {@link MidiNoteOffEvent} for proper
 * animation. Examples include: saxophone, piano, guitar, telephone ring.
 */
public class SustainedInstrument extends Instrument {
	/**
	 * Instantiates a new sustained instrument.
	 *
	 * @param context          the context to the main class
	 * @param offsetCalculator the offset calculator
	 * @see MultiChannelOffsetCalculator
	 */
	protected SustainedInstrument(@NotNull Midis2jam2 context,
	                              @NotNull MultiChannelOffsetCalculator offsetCalculator) {
		super(context, offsetCalculator);
	}
}
