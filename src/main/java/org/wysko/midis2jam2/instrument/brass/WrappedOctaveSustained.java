package org.wysko.midis2jam2.instrument.brass;

import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Iterator;
import java.util.List;

/**
 * A sustained instrument that wraps around the octave to visualize notes (e.g., choir, stage brass, stage strings).
 */
public abstract class WrappedOctaveSustained extends SustainedInstrument {
	
	/**
	 * True if the order of notes should be reversed, false otherwise.
	 */
	final boolean inverted;
	
	/**
	 * Each "twelfth" or note of the octave.
	 */
	protected TwelfthOfOctave[] twelfths = new TwelfthOfOctave[12];
	
	/**
	 * Instantiates a new wrapped octave sustained.
	 *
	 * @param context   the context to the main class
	 * @param eventList the event list
	 * @param inverted  should the notes be reversed?
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
	
	/**
	 * One note out of the twelve for the octave.
	 */
	public abstract static class TwelfthOfOctave {
		
		/**
		 * The highest level node.
		 */
		public final Node highestLevel = new Node();
		
		/**
		 * The animation node.
		 */
		protected final Node animNode = new Node();
		
		/**
		 * This note's current progress playing the note.
		 */
		protected double progress = 0;
		
		/**
		 * Is this twelfth currently playing?
		 */
		protected boolean playing = false;
		
		/**
		 * The amount of time, in seconds, this note should be playing for.
		 */
		protected double duration = 0;
		
		public TwelfthOfOctave() {
			highestLevel.attachChild(animNode);
		}
		
		public abstract void play(double duration);
		
		public abstract void tick(double time, float delta);
	}
}
