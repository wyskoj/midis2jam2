package org.wysko.midis2jam2.instrument;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A DecayedInstrument is any instrument that only depends on {@link MidiNoteOnEvent}s to function. The note off
 * event is discarded.
 */
public abstract class DecayedInstrument extends Instrument {
	
	/**
	 * List of events this instrument should play.
	 */
	@NotNull
	protected final List<MidiNoteOnEvent> hits;
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	protected DecayedInstrument(@NotNull Midis2jam2 context,
	                            @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context);
		this.hits = eventList.stream()
				.filter(e -> e instanceof MidiNoteOnEvent)
				.map(e -> ((MidiNoteOnEvent) e))
				.collect(Collectors.toList());
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibilityByStrikes(hits, time, instrumentNode);
		moveForMultiChannel();
	}
}
