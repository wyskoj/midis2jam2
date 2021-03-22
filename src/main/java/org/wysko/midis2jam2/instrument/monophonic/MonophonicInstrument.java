package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * A monophonic instrument is any instrument that can only play one note at a time (e.g., saxophones, clarinets,
 * ocarinas, etc.). Because this limitation is lifted in MIDI files, midis2jam2 needs to visualize polyphony by
 * spawning "clones" of an instrument. These clones will only appear when necessary.
 * <p>
 * It happens to be that every monophonic instrument is also a {@link SustainedInstrument}.
 *
 * @see Clone
 */
public abstract class MonophonicInstrument extends SustainedInstrument {
	
	/**
	 * Node contains all clones.
	 */
	@NotNull
	protected final Node groupOfPolyphony = new Node();
	
	
	/**
	 * The list of clones this monophonic instrument needs to effectively display all notes.
	 */
	@NotNull
	public List<Clone> clones;
	
	/**
	 * Constructs a monophonic instrument.
	 *
	 * @param context   context to midis2jam2
	 * @param eventList the event list
	 */
	public MonophonicInstrument(@NotNull Midis2jam2 context,
	                            @NotNull List<MidiChannelSpecificEvent> eventList,
	                            @NotNull Class<? extends Clone> cloneClass) throws ReflectiveOperationException {
		super(context, eventList);
		this.clones = calculateClones(this, cloneClass);
		
		for (Clone clone : clones) {
			groupOfPolyphony.attachChild(clone.offsetNode);
		}
		
		this.instrumentNode.attachChild(groupOfPolyphony);
	}
	
	/**
	 * Since MIDI channels that play monophonic instruments can play with polyphony, we need to calculate the number
	 * of "clones" needed to visualize this and determine which note events shall be assigned to which clones, using
	 * the least number of clones.
	 *
	 * @param instrument the monophonic instrument that is handling the clones
	 * @param cloneClass the class of the {@link Clone} to instantiate
	 * @throws ReflectiveOperationException usually is thrown if an error occurs in the clone constructor
	 */
	protected List<Clone> calculateClones(@NotNull MonophonicInstrument instrument,
	                                      @NotNull Class<? extends Clone> cloneClass) throws ReflectiveOperationException {
		List<Clone> clones = new ArrayList<>();
		Constructor<?> constructor = cloneClass.getDeclaredConstructor(instrument.getClass());
		clones.add((Clone) constructor.newInstance(instrument));
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
//				/* If notes overlap by less than ~1000 adjusted ticks, just ignore those ticks */
//				double l = comp2.endTick() - comp1.startTick();
//				double adj = l / context.file.division;
//				if (adj < 10 && adj > 0) {
//					comp2.noteOff.time -= l;
//				}
				if (comp1.startTick() >= comp2.startTick() && comp1.startTick() <= comp2.endTick()) { // Overlapping note
					boolean added = false;
					for (Clone clone : clones) {
						if (!clone.isPlaying(comp1.startTick())) {
							clone.notePeriods.add(comp1);
							added = true;
							break;
						}
					}
					if (!added) {
						Clone e = (Clone) constructor.newInstance(instrument);
						e.notePeriods.add(comp1);
						clones.add(e);
					}
				} else {
					clones.get(0).notePeriods.add(comp1);
				}
				break;
			}
		}
		return clones;
	}
	
	/**
	 * Updates clones, performing the {@link Clone#tick(double, float)} method and calculating clone offsets.
	 *
	 * @param time  the current time, in seconds
	 * @param delta the amount of time since the last frame
	 */
	protected void updateClones(double time, float delta) {
		for (Clone clone : clones) {
			clone.tick(time, delta);
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		updateClones(time, delta);
		
		
	}
}
