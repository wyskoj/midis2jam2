package org.wysko.midis2jam2.instrument;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.percussion.drumset.PercussionInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Twelve drums for each note.
 */
public abstract class TwelveDrumOctave extends DecayedInstrument {
	
	/**
	 * The Anim node.
	 */
	protected Node animNode = new Node();
	
	/**
	 * The Mallet nodes.
	 */
	@NotNull
	protected Node[] malletNodes;
	
	/**
	 * The Mallet strikes.
	 */
	protected List<MidiNoteOnEvent>[] malletStrikes;
	
	/**
	 * Each twelfth of the octave.
	 */
	protected TwelfthOfOctaveDecayed[] twelfths = new TwelfthOfOctaveDecayed[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	protected TwelveDrumOctave(@NotNull Midis2jam2 context,
	                           @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		malletNodes = new Node[12];
		malletStrikes = new ArrayList[12];
		for (int i = 0; i < 12; i++) {
			malletStrikes[i] = new ArrayList<>();
		}
		List<MidiNoteOnEvent> collect =
				eventList.stream().filter(e -> e instanceof MidiNoteOnEvent).map(e -> ((MidiNoteOnEvent) e)).collect(Collectors.toList());
		for (MidiNoteOnEvent noteOn : collect) {
			malletStrikes[(noteOn.note + 3) % 12].add(noteOn);
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		for (int i = 0; i < 12; i++) {
			Stick.StickStatus stickStatus = Stick.handleStick(context, malletNodes[i], time, delta, malletStrikes[i], 5, 50);
			if (stickStatus.justStruck()) {
				twelfths[i].animNode.setLocalTranslation(0, -3, 0);
			}
			Vector3f localTranslation = twelfths[i].animNode.getLocalTranslation();
			if (localTranslation.y < -0.0001) {
				twelfths[i].animNode.setLocalTranslation(0, Math.min(0,
						localTranslation.y + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)), 0);
			} else {
				twelfths[i].animNode.setLocalTranslation(0, 0, 0);
			}
		}
		
	}
	
	/**
	 * The Twelfth of octave that is decayed.
	 */
	public abstract static class TwelfthOfOctaveDecayed {
		
		/**
		 * The Highest level.
		 */
		public final Node highestLevel = new Node();
		
		/**
		 * The Anim node.
		 */
		protected final Node animNode = new Node();
		
		/**
		 * Instantiates a new Twelfth of octave decayed.
		 */
		public TwelfthOfOctaveDecayed() {
			highestLevel.attachChild(animNode);
		}
		
		/**
		 * Update animation and note handling.
		 *
		 * @param time  the current time
		 * @param delta the amount of time since the last frame update
		 */
		public abstract void tick(double time, float delta);
	}
	
}
