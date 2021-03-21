package org.wysko.midis2jam2.instrument.monophonic.pipe;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.Clone;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;
import java.util.Map;

public abstract class HandedInstrument extends MonophonicInstrument {
	
	@NotNull
	public Map<Integer, HandedClone.Hands> handMap;
	
	public HandedInstrument(@NotNull Midis2jam2 context,
	                        @NotNull List<MidiChannelSpecificEvent> eventList,
	                        @NotNull Class<? extends Clone> clazz,
	                        @NotNull Map<Integer, HandedClone.Hands> handMap) throws ReflectiveOperationException {
		super(context, eventList, clazz);
		this.handMap = handMap;
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10 * indexForMoving(), 0);
	}
}
