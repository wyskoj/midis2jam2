package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.MidiFile;
import org.wysko.midis2jam2.midi.MidiNoteEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * A monophonic instrument is any instrument that can only play one note at a time (e.g., saxophones, clarinets,
 * ocarinas, etc.). Because this limitation is lifted in MIDI files, midis2jam2 needs to visualize polyphony by
 * spawning "clones" of an instrument. These clones will only appear when necessary.
 *
 * @see MonophonicClone
 */
public abstract class MonophonicInstrument extends Instrument {
	/**
	 * Since this is effectively static, we need reference to midis2jam2.
	 */
	public final Midis2jam2 context;
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
	final MidiFile file;
	protected final Node highestLevel = new Node();
	
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
	
	protected void calculateClones(MonophonicInstrument instrument,
	                               Class<? extends MonophonicClone> cloneClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
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
	
	
	protected void updateClones(double time, float delta) {
		int clonesBeforeMe = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (this.getClass().isInstance(context.instruments.get(i)) &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				clonesBeforeMe++;
			}
		}
		
		highestLevel.setLocalTranslation(0, clonesBeforeMe * 40, 0);
		
		for (MonophonicClone clone : clones) {
			clone.tick(time, delta);
		}
	}
}
