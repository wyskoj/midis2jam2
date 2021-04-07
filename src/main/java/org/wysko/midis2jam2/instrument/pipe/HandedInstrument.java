package org.wysko.midis2jam2.instrument.pipe;

import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Clone;
import org.wysko.midis2jam2.instrument.HandPositionFingeringManager;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

/**
 * Any instrument that animates using hands.
 */
public abstract class HandedInstrument extends MonophonicInstrument {
	
	/**
	 * Instantiates a new Handed instrument.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 * @param clazz     the class of the clone
	 * @param manager   the fingering manager
	 * @throws ReflectiveOperationException if clone constructor errors
	 */
	public HandedInstrument(@NotNull Midis2jam2 context,
	                        @NotNull List<MidiChannelSpecificEvent> eventList,
	                        @NotNull Class<? extends Clone> clazz,
	                        @NotNull HandPositionFingeringManager manager) throws ReflectiveOperationException {
		super(context, eventList, clazz, manager);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10 * indexForMoving(), 0);
	}
}
