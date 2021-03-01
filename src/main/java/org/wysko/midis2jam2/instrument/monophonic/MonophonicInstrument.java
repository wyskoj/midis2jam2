package org.wysko.midis2jam2.instrument.monophonic;

import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.monophonic.reed.sax.AltoSaxophone;
import org.wysko.midis2jam2.midi.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A monophonic instrument is any instrument that can only play one note at a time (e.g., saxophones, clarinets,
 * ocarinas, etc.). Because this limitation is lifted in MIDI files, midis2jam2 needs to visualize polyphony by
 * spawning "clones" of an instrument. These clones will only appear when necessary.
 *
 * @see MonophonicClone
 */
public abstract class MonophonicInstrument implements Instrument {
	/**
	 * Since this is effectively static, we need reference to midis2jam2.
	 */
	protected final Midis2jam2 context;
	protected final List<MidiNoteEvent> noteEvents;
	/**
	 * Populated by {@link #calculateNotePeriods(List)}.
	 *
	 * @see #calculateNotePeriods(List)
	 */
	public List<NotePeriod> notePeriods;
	/**
	 * The list of clones this monophonic instrument needs to effectively display all notes.
	 */
	public List<MonophonicClone> clones;
	/**
	 * Reference to the midi file.
	 */
	MidiFile file;
	List<MidiChannelSpecificEvent> events;
	
	/**
	 * Constructs a monophonic instrument.
	 *
	 * @param context context to midis2jam2
	 * @param file    context to the midi file
	 * @param events  the MIDI events related to this instrument
	 */
	public MonophonicInstrument(
			Midis2jam2 context, MidiFile file, List<MidiChannelSpecificEvent> events) {
		noteEvents = events.stream()
				.filter(event -> event instanceof MidiNoteOnEvent || event instanceof MidiNoteOffEvent)
				.map(event -> (MidiNoteEvent) event).collect(Collectors.toList());
		this.context = context;
		this.file = file;
		this.events = events;
		
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
								file.eventInSeconds(check), noteEvent.time, check.time, ((MidiNoteOnEvent) noteEvent), ((MidiNoteOffEvent) check)));
						break;
					}
				}
			}
		}
	}
	
	/**
	 * This method is a mess. Your brain may rapidly combust if you try to understand it.
	 * <br>
	 * Essentially, it figures out when notes overlap and assigns them to "clones" of the instrument. This is used
	 * for monophonic instruments that need a polyphonic visualization.
	 * <br>
	 * TODO Make this less spaghetti
	 * TODO Can this pulled up somehow?
	 */
	protected void calculateClones(MonophonicInstrument instrument, Class<? extends MonophonicClone> cloneClass) throws InstantiationException,
			IllegalAccessException, NoSuchMethodException, InvocationTargetException {
		clones = new ArrayList<>();
		Constructor<?> constructor = cloneClass.getDeclaredConstructor(instrument.getClass());
		clones.add((MonophonicClone) constructor.newInstance(instrument));
		for (int i = 0; i < notePeriods.size(); i++) {
			for (int j = 0; j < notePeriods.size(); j++) {
				if (j == i) continue;
				NotePeriod comp1 = notePeriods.get(i);
				NotePeriod comp2 = notePeriods.get(j);
				if (comp1.startTick() > comp2.endTick()) continue;
				if (comp1.endTick() < comp2.startTick()) {
					clones.get(0).notePeriods.add(comp1);
					break;
				}
				if (comp1.startTick() >= comp2.startTick() && comp1.startTick() <= comp2.endTick()) { // Overlapping note
					boolean added = false;
					for (MonophonicClone clone : clones) {
						if (!clone.isPlayingAtTime(comp1.startTick())) {
							clone.notePeriods.add(comp1);
							added = true;
							break;
						}
					}
					if (!added) {
						MonophonicClone e = (MonophonicClone) constructor.newInstance(instrument);
						e.notePeriods.add(comp1);
						clones.add(e);
					}
				} else {
					clones.get(0).notePeriods.add(comp1);
				}
				break;
			}
		}
	}
}
