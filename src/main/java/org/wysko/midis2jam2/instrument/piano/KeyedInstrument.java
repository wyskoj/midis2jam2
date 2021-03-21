package org.wysko.midis2jam2.instrument.piano;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.OffsetCalculator;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

/**
 * Any instrument that visualizes notes by rotating piano keys.
 */
public abstract class KeyedInstrument extends SustainedInstrument {
	
	/**
	 * The lowest note this instrument can play.
	 */
	protected final int rangeLow;
	
	/**
	 * The highest note this instrument can play.
	 */
	protected final int rangeHigh;
	
	/**
	 * The keys of this instrument
	 */
	protected final Key[] keys;
	
	
	/**
	 * Instantiates a new Keyed instrument.
	 *
	 * @param context          the context
	 * @param offsetCalculator the offset calculator
	 * @param eventList        the event list
	 * @param rangeLow         the lowest note this instrument can play
	 * @param rangeHigh        the highest note this instrument can play
	 */
	protected KeyedInstrument(@NotNull Midis2jam2 context,
	                          @NotNull OffsetCalculator offsetCalculator,
	                          @NotNull List<MidiChannelSpecificEvent> eventList,
	                          int rangeLow,
	                          int rangeHigh) {
		super(context, eventList);
		this.rangeLow = rangeLow;
		this.rangeHigh = rangeHigh;
		keys = new Key[keyCount()];
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		handleKeys(time, delta);
	}
	
	/**
	 * Returns the number of keys on this instrument.
	 *
	 * @return the number of keys on this instrument
	 */
	@Contract(pure = true)
	public int keyCount() {
		return (rangeHigh - rangeLow) + 1;
	}
	
	/**
	 * Handles the pressing and releasing of keys, and their respective animations.
	 *
	 * @param time  the current time
	 * @param delta the amount of time since the last frame
	 */
	protected void handleKeys(double time, float delta) {
		context.debugText.setText(String.valueOf(currentNotePeriods));
		for (Key key : keys) {
			key.animate(currentNotePeriods.stream().anyMatch(p -> p.midiNote == key.midiNote && p.isPlayingAt(time) && p.endTime - time > 0.05f), delta);
		}
	}
	
	
	/**
	 * Keyboards have two different colored keys: white and black.
	 */
	public enum KeyColor {
		
		/**
		 * White key color.
		 */
		WHITE,
		
		/**
		 * Black key color.
		 */
		BLACK
	}
}
