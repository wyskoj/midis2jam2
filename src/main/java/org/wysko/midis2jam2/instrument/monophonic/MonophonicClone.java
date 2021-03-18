package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Contract;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;

import java.util.ArrayList;
import java.util.List;

/**
 * Monophonic instruments ({@link MonophonicInstrument}) use MonophonicClones to visualize polyphony on monophonic
 * instruments. A clone is required for each degree of polyphony.
 *
 * @see MonophonicInstrument
 */
public abstract class MonophonicClone {
	
	/**
	 * The note periods for which this clone should be responsible for animating.
	 *
	 * @see NotePeriod
	 */
	public final List<NotePeriod> notePeriods;
	public final Node animNode = new Node();
	public final Node modelNode = new Node();
	public Node hornNode = new Node();
	/**
	 * The current note period that is being handled.
	 */
	public NotePeriod currentNotePeriod;
	/**
	 * Is this clone currently playing a note?
	 */
	protected boolean currentlyPlaying = false;
	
	public MonophonicClone() {
		this.notePeriods = new ArrayList<>();
	}
	
	/**
	 * Determines if this clone is playing at a certain point.
	 *
	 * @param midiTick the current midi tick
	 * @return true if should be playing, false otherwise
	 */
	@Contract(pure = true)
	public boolean isPlayingAtTime(long midiTick) {
		for (NotePeriod notePeriod : notePeriods) {
			if (midiTick >= notePeriod.startTick() && midiTick < notePeriod.endTick()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Hides or shows this clone.
	 *
	 * @param indexThis the index of this clone
	 */
	protected void hideOrShowOnPolyphony(int indexThis) {
		if (indexThis != 0) {
			if (currentlyPlaying) {
				hornNode.setCullHint(Spatial.CullHint.Dynamic);
			} else {
				hornNode.setCullHint(Spatial.CullHint.Always);
			}
		} else {
			hornNode.setCullHint(Spatial.CullHint.Dynamic);
		}
	}
	
	/**
	 * Similar to {@link Instrument#tick(double, float)}.
	 * @param time the current time
	 * @param delta the amount of time since last frame
	 * @see Instrument#tick(double, float)
	 */
	protected abstract void tick(double time, float delta);
}
