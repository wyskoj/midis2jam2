package org.wysko.midis2jam2.instrument.monophonic;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A monophonic instrument is any instrument that can only play one note at a time (e.g., saxophones, clarinets,
 * ocarinas, etc.). Because this limitation is lifted in MIDI files, midis2jam2 needs to visualize polyphony by
 * spawning "clones" of an instrument. These clones will only appear when necessary.
 *
 * @param <C> the type of clone for this instrument
 * @see MonophonicClone
 */
public abstract class MonophonicInstrument<C> implements Instrument {
	/**
	 * Since this is effectively static, we need reference to midis2jam2.
	 */
	protected final Midis2jam2 context;
	/**
	 * Populated by {@link #calculateNotePeriods(List)}.
	 *
	 * @see #calculateNotePeriods(List)
	 */
	public List<NotePeriod> notePeriods;
	/**
	 * The list of clones this monophonic instrument needs to effectively display all notes.
	 */
	public List<C> clones;
	/**
	 * Reference to the midi file.
	 */
	MidiFile file;
	
	/**
	 * Constructs a monophonic instrument.
	 *
	 * @param context context to midis2jam2
	 * @param file    context to the midi file
	 */
	public MonophonicInstrument(
			Midis2jam2 context, MidiFile file) {
		this.context = context;
		this.file = file;
	}
	
	/**
	 * A MIDI file is a sequence of {@link MidiNoteOnEvent}s and {@link MidiNoteOffEvent}s. This method searches the
	 * files and connects corresponding events together. This is effectively calculating the "blocks" you would see
	 * in a piano roll editor. The method saves the results to {@link #notePeriods}.
	 *
	 * @param noteEvents the note events to calculate NotePeriods from
	 */
	protected void calculateNotePeriods(List<MidiNoteEvent> noteEvents) {
		notePeriods = new ArrayList<>();
		for (int i = 0, noteEventsSize = noteEvents.size(); i < noteEventsSize; i++) {
			MidiNoteEvent noteEvent = noteEvents.get(i);
			if (noteEvent instanceof MidiNoteOnEvent) {
				for (int j = i + 1; j < noteEventsSize; j++) {
					MidiNoteEvent check = noteEvents.get(j);
					if (check instanceof MidiNoteOffEvent && check.note == noteEvent.note) {
						// We found a block
						notePeriods.add(new NotePeriod(check.note, file.eventInSeconds(noteEvent),
								file.eventInSeconds(check), noteEvent.time, check.time, ((MidiNoteOnEvent) noteEvent)
								, ((MidiNoteOffEvent) check)));
						break;
					}
				}
			}
		}
	}
}
