package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Monophonic instruments ({@link MonophonicInstrument}) use MonophonicClones to visualize polyphony on monophonic
 * instruments. A clone is required for each degree of polyphony.
 *
 * @see MonophonicInstrument
 */
public abstract class MonophonicClone implements Instrument {
	
	/**
	 * The note periods for which this clone should be responsible for animating.
	 *
	 * @see NotePeriod
	 */
	protected final List<NotePeriod> notePeriods;
	/**
	 * Is this clone currently playing a note?
	 */
	protected boolean currentlyPlaying = false;
	/**
	 * The curent note period that is being handled.
	 */
	NotePeriod currentNotePeriod;
	Node animNode = new Node();
	Node modelNode = new Node();
	
	public MonophonicClone() {
		this.notePeriods = new ArrayList<>();
	}
	
	public boolean isPlayingAtTime(long midiTick) {
		for (NotePeriod notePeriod : notePeriods) {
			if (midiTick >= notePeriod.startTick() && midiTick < notePeriod.endTick()) {
				return true;
			}
		}
		return false;
	}
	
	public abstract void tick(double time, float delta);
}
