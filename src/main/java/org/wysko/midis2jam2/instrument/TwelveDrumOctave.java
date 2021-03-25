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

public abstract class TwelveDrumOctave extends DecayedInstrument {
	
	protected Node animNode = new Node();
	
	@NotNull
	protected Node[] malletNodes;
	
	protected List<MidiNoteOnEvent>[] malletStrikes;
	
	protected TwelfthOfOctaveDecayed[] decayeds = new TwelfthOfOctaveDecayed[12];
	
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
				decayeds[i].animNode.setLocalTranslation(0, -3, 0);
			}
			Vector3f localTranslation = decayeds[i].animNode.getLocalTranslation();
			if (localTranslation.y < -0.0001) {
				decayeds[i].animNode.setLocalTranslation(0, Math.min(0,
						localTranslation.y + (PercussionInstrument.DRUM_RECOIL_COMEBACK * delta)), 0);
			} else {
				decayeds[i].animNode.setLocalTranslation(0, 0, 0);
			}
		}
		
	}
	
	public abstract static class TwelfthOfOctaveDecayed {
		
		public final Node highestLevel = new Node();
		
		protected final Node animNode = new Node();
		
		public TwelfthOfOctaveDecayed() {
			highestLevel.attachChild(animNode);
		}
		
		public abstract void tick(double time, float delta);
	}
	
}
