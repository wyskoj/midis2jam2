package org.wysko.midis2jam2.instrument;

import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.brass.OneStageInstrument;
import org.wysko.midis2jam2.instrument.brass.StageHorns;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class StageInstrument extends Instrument {
	protected final List<NotePeriod> notePeriods;
	protected OneStageInstrument[] eachNote;
	protected Node highestLevel = new Node();
	
	protected StageInstrument(Midis2jam2 context,
	                          List<MidiChannelSpecificEvent> eventList) {
		super(context);
		List<MidiNoteEvent> midiNoteEvents = scrapeMidiNoteEvents(eventList);
		notePeriods = calculateNotePeriods(midiNoteEvents);
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
