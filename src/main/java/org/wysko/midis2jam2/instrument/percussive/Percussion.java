package org.wysko.midis2jam2.instrument.percussive;

import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;
import java.util.stream.Collectors;

public class Percussion extends Instrument {
	
	public final SnareDrum snareDrum;
	public final Node percussionNode = new Node();
	
	public Percussion(Midis2jam2 context, List<MidiChannelSpecificEvent> events) {
		/* Percussion only cares about note on. */
		List<MidiNoteOnEvent> noteOnEvents = events.stream()
						.filter(e -> e instanceof MidiNoteOnEvent)
						.map(e -> ((MidiNoteOnEvent) e))
						.collect(Collectors.toList());
		
		
		snareDrum = new SnareDrum(context,
				noteOnEvents.stream().filter(e -> e.note == 40 || e.note == 38).collect(Collectors.toList()));
		
		// Attach nodes to group node
		percussionNode.attachChild(snareDrum.highLevelNode);
		
		context.getRootNode().attachChild(percussionNode);
		
		System.out.println(events);
	}
	
	@Override
	public void tick(double time, float delta) {
		snareDrum.tick(time,delta);
	}
}
