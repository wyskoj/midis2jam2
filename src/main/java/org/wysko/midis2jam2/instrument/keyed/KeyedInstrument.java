package org.wysko.midis2jam2.instrument.keyed;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class KeyedInstrument extends Instrument {
	protected final List<MidiChannelSpecificEvent> events;
	
	protected KeyedInstrument(Midis2jam2 context,
	                          List<MidiChannelSpecificEvent> eventList) {
		super(context);
		this.events = eventList;
	}
	
	protected void handleKeys(double time, float delta) {
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		if (!events.isEmpty()) {
			if (!(events.get(0) instanceof MidiNoteOnEvent) && !(events.get(0) instanceof MidiNoteOffEvent)) {
				events.remove(0);
			}
			while (!events.isEmpty() && ((events.get(0) instanceof MidiNoteOnEvent && context.file.eventInSeconds(events.get(0)) <= time) ||
					(events.get(0) instanceof MidiNoteOffEvent && context.file.eventInSeconds(events.get(0)) - time <= 0.05))) {
				eventsToPerform.add(events.remove(0));
			}
		}
		
		for (MidiEvent event : eventsToPerform) {
			if (event instanceof MidiNoteOnEvent) {
				pushKeyDown(((MidiNoteOnEvent) event).note);
			} else if (event instanceof MidiNoteOffEvent) {
				releaseKey(((MidiNoteOffEvent) event).note);
			}
		}
		
		transitionAnimation(delta);
	}
	
	static void handleAKey(float delta, boolean beingPressed, Node node, Node downNode, Node upNode,
	                       Key key) {
		if (!beingPressed) {
			float[] angles = new float[3];
			node.getLocalRotation().toAngles(angles);
			if (angles[0] > 0.0001) { // fuck floats
				node.setLocalRotation(new Quaternion(new float[]
						{Math.max(angles[0] - (0.02f * delta * 50), 0), 0, 0}
				));
			} else {
				node.setLocalRotation(new Quaternion(new float[] {0, 0, 0}));
				
				downNode.setCullHint(Spatial.CullHint.Always);
				upNode.setCullHint(Spatial.CullHint.Dynamic);
			}
		}
	}
	
	protected abstract void releaseKey(int note);
	
	public abstract void transitionAnimation(float delta);
	
	protected abstract void pushKeyDown(int note);
}
