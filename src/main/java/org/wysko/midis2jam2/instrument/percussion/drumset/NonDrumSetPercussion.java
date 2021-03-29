package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.scene.Spatial;
import org.jetbrains.annotations.Unmodifiable;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NonDrumSetPercussion extends PercussionInstrument {
	
	protected final @Unmodifiable List<MidiNoteOnEvent> finalHits;
	
	protected NonDrumSetPercussion(Midis2jam2 context,
	                               List<MidiNoteOnEvent> hits) {
		super(context, hits);
		
		this.finalHits = Collections.unmodifiableList(new ArrayList<>(hits));
	}
	
	@Override
	public void tick(double time, float delta) {
		instrumentNode.setCullHint(calculateVisibility(time) ? Spatial.CullHint.Dynamic : Spatial.CullHint.Always);
	}
	
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
