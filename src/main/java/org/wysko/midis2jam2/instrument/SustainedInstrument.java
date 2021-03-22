package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A sustained instrument is any instrument that also depends on knowing the {@link MidiNoteOffEvent} for proper
 * animation. Examples include: saxophone, piano, guitar, telephone ring.
 */
public abstract class SustainedInstrument extends Instrument {
	
	/**
	 * This list shall not be updated and shall be used for visibility calculations.
	 */
	@Unmodifiable
	@NotNull
	protected final List<NotePeriod> unmodifiableNotePeriods;
	/**
	 * The list of note periods. This class expects that this variable will be truncated as the MIDI file progresses.
	 *
	 * @see NotePeriod
	 */
	@NotNull
	protected List<NotePeriod> notePeriods;
	/**
	 * The list of current note periods. Will always be updating as the MIDI file progresses.
	 */
	@NotNull
	protected List<NotePeriod> currentNotePeriods = new ArrayList<>();
	
	/**
	 * Instantiates a new sustained instrument.
	 *
	 * @param context          the context to the main class
	 * @see OffsetCalculator
	 */
	protected SustainedInstrument(@NotNull Midis2jam2 context,
	                              @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context);
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
	public static List<MidiNoteEvent> scrapeMidiNoteEvents(@NotNull List<MidiChannelSpecificEvent> events) {
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
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
		currentNotePeriods.removeIf(notePeriod -> notePeriod.endTime <= time);
	}
	
	@Override
	public void tick(double time, float delta) {
		calculateCurrentNotePeriods(time);
		setIdleVisibilityByPeriods(time);
		moveForMultiChannel();
	}
	
	
	/**
	 * Determines whether this instrument should be visible at the time, and sets the visibility accordingly.
	 * <p>
	 * The instrument should be visible if:
	 * <ul>
	 *     <li>There is at least 1 second between now and the start of any note period,</li>
	 *     <li>There is at least 4 seconds between now and the end of any note period, or</li>
	 *     <li>Any note period is currently playing</li>
	 * </ul>
	 *  @param time        the current time
	 */
	protected void setIdleVisibilityByPeriods(double time) {
		boolean b = calcVisibility(time, unmodifiableNotePeriods);
		visible = b;
		instrumentNode.setCullHint(b ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
	}
	
	public static boolean calcVisibility(double time, @NotNull List<NotePeriod> unmodifiableNotePeriods) {
		boolean show = false;
		for (NotePeriod notePeriod : unmodifiableNotePeriods) {
			// Within 1 second of a note on,
			// within 4 seconds of a note off,
			// or during a note, be visible
			if (notePeriod.isPlayingAt(time)
					|| Math.abs(time - notePeriod.startTime) < 1
					|| (Math.abs(time - notePeriod.endTime) < 4 && time > notePeriod.endTime)) {
				show = true;
				break;
			}
		}
		return true;
	}
	
}
