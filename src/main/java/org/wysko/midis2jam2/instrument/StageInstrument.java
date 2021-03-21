package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.brass.WrappedOctaveSustained.TwelfthOfOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class StageInstrument extends SustainedInstrument {
	protected TwelfthOfOctave[] eachNote;
	protected Node highestLevel = new Node();
	
	protected StageInstrument(Midis2jam2 context,
	                          List<MidiChannelSpecificEvent> eventList,
	                          @NotNull OffsetCalculator offsetCalculator) {
		super(context, eventList);
	}
	
	protected void playStageInstruments(double time) {
		List<NotePeriod> currentNotePeriods = new ArrayList<>();
		while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
			currentNotePeriods.add(notePeriods.remove(0));
		}
		
		if (!currentNotePeriods.isEmpty()) {
			for (NotePeriod currentNotePeriod : currentNotePeriods) {
				int midiNote = currentNotePeriod.midiNote;
				int index = 11 - ((midiNote + 3) % 12);
				eachNote[index].play(currentNotePeriod.duration());
			}
		}
	}
}
