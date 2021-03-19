package org.wysko.midis2jam2.instrument;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A sustained instrument is any instrument that also depends on knowing the {@link MidiNoteOffEvent} for proper
 * animation. Examples include: saxophone, piano, guitar, telephone ring.
 */
public abstract class SustainedInstrument extends Instrument {
	
	/**
	 * The list of note periods. This class expects that this variable will be truncated as the MIDI file progresses.
	 *
	 * @see NotePeriod
	 */
	@NotNull
	protected final List<NotePeriod> notePeriods;
	
	/**
	 * This list shall not be updated and shall be used for visibility calculations.
	 */
	@Unmodifiable
	@NotNull
	protected final List<NotePeriod> unmodifiableNotePeriods;
	
	/**
	 * The list of current note periods. Will always be updating as the MIDI file progresses.
	 */
	@NotNull
	protected List<NotePeriod> currentNotePeriods = new ArrayList<>();
	
	/**
	 * Instantiates a new sustained instrument.
	 *
	 * @param context          the context to the main class
	 * @param offsetCalculator the offset calculator
	 * @see MultiChannelOffsetCalculator
	 */
	protected SustainedInstrument(@NotNull Midis2jam2 context,
	                              @NotNull MultiChannelOffsetCalculator offsetCalculator,
	                              @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, offsetCalculator);
		this.notePeriods = calculateNotePeriods(scrapeMidiNoteEvents(eventList));
		this.unmodifiableNotePeriods = Collections.unmodifiableList(new ArrayList<>(notePeriods));
	}
	
	/**
	 * Filters a list of MIDI channel specific events and returns only the {@link MidiNoteEvent}s.
	 *
	 * @param events the event list
	 * @return only the MidiNoteEvents
	 * @see MidiNoteEvent
	 */
	@NotNull
	@Contract(pure = true)
	protected static List<MidiNoteEvent> scrapeMidiNoteEvents(@NotNull List<MidiChannelSpecificEvent> events) {
		return events.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> ((MidiNoteEvent) e)).collect(Collectors.toList());
	}
	
	/**
	 * Determines which note periods should have starting animations at the specified time. Removes the returned
	 * elements from {@link #notePeriods}. The method also removes elapsed note periods. All results are stored in
	 * {@link #currentNotePeriods}.
	 *
	 * @param time the current time
	 * @see #currentNotePeriods
	 */
	protected void calculateCurrentNotePeriods(double time) {
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time)
			currentNotePeriods.add(notePeriods.remove(0));
		
		currentNotePeriods.removeIf(notePeriod -> notePeriod.endTime <= time);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		calculateCurrentNotePeriods(time);
	}
}
