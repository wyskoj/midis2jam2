package org.wysko.midis2jam2.instrument;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

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
	protected final Midis2jam2 context;
	
	protected Instrument(Midis2jam2 context) {
		this.context = context;
	}
	
	/**
	 * Updates the animation and other necessary frame-dependant calculations.
	 *
	 * @param time  the current time since the beginning of the MIDI file, expressed in seconds
	 * @param delta the amount of time since the last call this method, expressed in seconds
	 */
	public abstract void tick(double time, float delta);
	
	/**
	 * A MIDI file is a sequence of {@link MidiNoteOnEvent}s and {@link MidiNoteOffEvent}s. This method searches the
	 * files and connects corresponding events together. This is effectively calculating the "blocks" you would see
	 * in a piano roll editor.
	 *
	 * @param noteEvents the note events to calculate NotePeriods from
	 */
	protected List<NotePeriod> calculateNotePeriods(List<MidiNoteEvent> noteEvents) {
		List<NotePeriod> notePeriods = new ArrayList<>();
		for (int i = 0, noteEventsSize = noteEvents.size(); i < noteEventsSize; i++) {
			MidiNoteEvent noteEvent = noteEvents.get(i);
			if (noteEvent instanceof MidiNoteOnEvent) {
				for (int j = i + 1; j < noteEventsSize; j++) {
					MidiNoteEvent check = noteEvents.get(j);
					if (check instanceof MidiNoteOffEvent && check.note == noteEvent.note) {
						// We found a block
						notePeriods.add(new NotePeriod(check.note, context.file.eventInSeconds(noteEvent),
								context.file.eventInSeconds(check), ((MidiNoteOnEvent) noteEvent)
								, ((MidiNoteOffEvent) check)));
						break;
					}
				}
			}
		}
		for (int i = notePeriods.size() - 2; i >= 0; i--) {
			final NotePeriod a = notePeriods.get(i + 1);
			final NotePeriod b = notePeriods.get(i);
			if (a.startTick() == b.startTick() &&
					a.endTick() == b.endTick() &&
					a.midiNote == b.midiNote) {
				notePeriods.remove(i + 1);
			}
		}
		return notePeriods;
	}
}
