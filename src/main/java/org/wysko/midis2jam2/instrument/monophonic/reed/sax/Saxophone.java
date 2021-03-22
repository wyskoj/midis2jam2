package org.wysko.midis2jam2.instrument.monophonic.reed.sax;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.Clone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

public abstract class Saxophone extends MonophonicInstrument {
	
	/**
	 * Constructs a saxophone.
	 *
	 * @param context    context to midis2jam2
	 * @param eventList  the list of events for this instrument
	 * @param cloneClass the class of the clone
	 */
	public Saxophone(Midis2jam2 context,
	                 @NotNull List<MidiChannelSpecificEvent> eventList,
	                 @NotNull Class<? extends Clone> cloneClass) throws ReflectiveOperationException {
		
		super(context,
				eventList,
				cloneClass);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 40 * indexForMoving(), 0);
	}
}
