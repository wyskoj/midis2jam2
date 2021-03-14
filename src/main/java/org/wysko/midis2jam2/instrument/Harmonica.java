package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiEvent;
import org.wysko.midis2jam2.midi.MidiNoteOffEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.particle.SteamPuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Harmonica extends Instrument {
	private final Node highestLevel = new Node();
	SteamPuffer[] puffers = new SteamPuffer[12];
	Node[] pufferNodes = new Node[12];
	Node harmonicaNode = new Node();
	Spatial harmonica;
	List<MidiChannelSpecificEvent> events;
	boolean[] activities = new boolean[12];
	
	public Harmonica(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context);
		this.events = eventList;
		harmonica = context.loadModel("Harmonica.obj", "Harmonica.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		harmonicaNode.attachChild(harmonica);
		
		for (int i = 0; i < 12; i++) {
			pufferNodes[i] = new Node();
			puffers[i] = new SteamPuffer(context, SteamPuffer.SteamPuffType.HARMONICA, 0.75);
			puffers[i].steamPuffNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-90), 0));
			puffers[i].steamPuffNode.setLocalTranslation(0, 0, 7.2f);
			pufferNodes[i].attachChild(puffers[i].steamPuffNode);
			harmonicaNode.attachChild(pufferNodes[i]);
			pufferNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(5 * (i - 5.5)), 0));
		}
		
		// Position harmonica
		
		
		highestLevel.attachChild(harmonicaNode);
		context.getRootNode().attachChild(highestLevel);
		
		highestLevel.setLocalTranslation(74, 32, -38);
		highestLevel.setLocalRotation(new Quaternion().fromAngles(0, rad(-90), 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		/* Collect note periods to execute */
		
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
				MidiNoteOnEvent noteOn = (MidiNoteOnEvent) event;
				int i = (noteOn.note + 3) % 12;
				activities[i] = true;
			} else if (event instanceof MidiNoteOffEvent) {
				MidiNoteOffEvent noteOff = (MidiNoteOffEvent) event;
				int i = (noteOff.note + 3) % 12;
				activities[i] = false;
			}
		}
		
		for (int i = 0; i < puffers.length; i++) {
			puffers[i].tick(time, delta, activities[i]);
		}
		
		int mySpot = context.instruments.stream().filter(i -> i instanceof Harmonica).collect(Collectors.toList()).indexOf(this);
		harmonicaNode.setLocalTranslation(0,mySpot * 10,0);
	}
}
