package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.MidiFile;

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
	protected final Node groupOfPolyphony = new Node();
	protected final Node highestLevel = new Node();
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
	 * Constructs a monophonic instrument.
	 *  @param context context to midis2jam2
	 *
	 */
	public MonophonicInstrument(
			Midis2jam2 context) {
		super(context);
		this.context = context;
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
				/* If notes overlap by less than 5 midi ticks, just ignore those ticks */
				final long l = comp2.endTick() - comp1.startTick();
				if (l < 5 && l > 0) {
					comp2.noteOff.time -= l;
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
	
	
	protected void updateClones(double time, float delta, Vector3f multiChannelOffset) {
		int othersOfMyType = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (this.getClass().isInstance(context.instruments.get(i)) &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				othersOfMyType++;
			}
		}
		
		highestLevel.setLocalTranslation(multiChannelOffset.mult(othersOfMyType));
		
		for (MonophonicClone clone : clones) {
			clone.tick(time, delta);
		}
	}
}
