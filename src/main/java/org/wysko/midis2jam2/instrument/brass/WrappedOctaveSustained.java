package org.wysko.midis2jam2.instrument.brass;

import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Iterator;
import java.util.List;

public abstract class WrappedOctaveSustained extends SustainedInstrument {
	
	protected TwelfthOfOctave[] twelfths = new TwelfthOfOctave[12];
	
	final boolean inverted;
	
	/**
	 * Instantiates a new wrapped octave sustained.
	 *
	 * @param context   the context to the main class
	 * @param eventList the event list
	 * @param inverted
	 */
	protected WrappedOctaveSustained(@NotNull Midis2jam2 context,
	                                 @NotNull List<MidiChannelSpecificEvent> eventList,
	                                 boolean inverted) {
		super(context, eventList);
		this.inverted = inverted;
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		setIdleVisibilityByPeriods(time);
		if (!currentNotePeriods.isEmpty()) {
			for (Iterator<NotePeriod> iterator = currentNotePeriods.iterator(); iterator.hasNext(); ) {
				NotePeriod currentNotePeriod = iterator.next();
				
				int midiNote = currentNotePeriod.midiNote;
				int index = 11 - ((midiNote + 3) % 12);
				if (inverted) index = 11 - index;
				twelfths[index].play(currentNotePeriod.duration());
				
				iterator.remove();
			}
		}
		
		for (TwelfthOfOctave twelfth : twelfths) {
			twelfth.tick(time, delta);
		}
	}
	
	public abstract static class TwelfthOfOctave {
		public final Node highestLevel = new Node();
		
		protected final Node animNode = new Node();
		
		protected double progress = 0;
		
		protected boolean playing = false;
		
		protected double duration = 0;
		
		public TwelfthOfOctave() {
			highestLevel.attachChild(animNode);
		}
		
		public abstract void play(double duration);
		
		public abstract void tick(double time, float delta);
	}
	
}
