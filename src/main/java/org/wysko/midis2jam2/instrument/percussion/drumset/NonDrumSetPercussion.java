package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Any percussion instrument that is not attached to the drum set and should therefore only appear when playing.
 */
public class NonDrumSetPercussion extends PercussionInstrument {
	
	/**
	 * The unmodifiable list of hits.
	 */
	protected final @Unmodifiable List<MidiNoteOnEvent> finalHits;
	
	/**
	 * Instantiates a new non drum set percussion.
	 *
	 * @param context the context
	 * @param hits    the hits
	 */
	protected NonDrumSetPercussion(Midis2jam2 context,
	                               List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		this.finalHits = Collections.unmodifiableList(new ArrayList<>(hits));
	}
	
	@Override
	public void tick(double time, float delta) {
		instrumentNode.setCullHint(calculateVisibility(time) ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
	}
	
	/**
	 * Calculates whether this instrument should be visible.
	 *
	 * @param time the current time
	 * @return true if this instrument should be visible, false otherwise
	 */
	public boolean calculateVisibility(double time) {
		for (MidiNoteOnEvent hit : finalHits) {
			double leftMarginTime = context.file.midiTickInSeconds(hit.time - (context.file.division / 2));
			double rightMarginTime = context.file.midiTickInSeconds(hit.time + (context.file.division / 2));
			if (time >= leftMarginTime && time <= rightMarginTime) {
				return true;
			}
		}
		return false;
	}
}
