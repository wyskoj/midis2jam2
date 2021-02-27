package org.wysko.midis2jam2.instrument;

import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class AltoSaxophone extends Horn implements Instrument {
	private final List<MidiChannelSpecificEvent> events;
	private final List<MidiNoteEvent> noteEvents = new ArrayList<>();
	private final Spatial horn;
	private final Spatial body;
	private final Midis2jam2 context;
	Node altoSaxes = new Node();
	Node thisSaxRoot = new Node();
	Node anAlto = new Node();
	MidiFile file;
	Extension currentExtension;
	private List<Extension> extensions;
	
	public AltoSaxophone(Midis2jam2 context, List<MidiChannelSpecificEvent> events, MidiFile file) {
		this.context = context;
		this.file = file;
		this.events = events;
		body = context.loadModel("AltoSaxBody.obj", "HornSkin.png");
		horn = context.loadModel("AltoSaxHorn.obj", "HornSkin.png");
		
		anAlto.attachChild(body);
		anAlto.attachChild(horn);
		altoSaxes.attachChild(anAlto);
		context.getRootNode().attachChild(altoSaxes);
		
		horn.move(0, -22, 0);
		altoSaxes.move(-14, 41.5f, -45);
		altoSaxes.rotate(rad(13), rad(75),0);
		
		for (MidiChannelSpecificEvent event : events) {
			if (event instanceof MidiNoteOnEvent || event instanceof MidiNoteOffEvent) {
				noteEvents.add((MidiNoteEvent) event);
			}
		}
		
		calculateExtensions();
	}
	
	private void calculateExtensions() {
		extensions = new ArrayList<>();
		for (int i = 0, noteEventsSize = noteEvents.size(); i < noteEventsSize; i++) {
			MidiNoteEvent noteEvent = noteEvents.get(i);
			if (noteEvent instanceof MidiNoteOnEvent) {
				for (int j = i + 1; j < noteEventsSize; j++) {
					MidiNoteEvent check = noteEvents.get(j);
					if (check instanceof MidiNoteOffEvent && check.note == noteEvent.note) {
						// We found a block
						extensions.add(new Extension(check.note, file.eventInSeconds(noteEvent), file.eventInSeconds(check)));
						break;
					}
				}
			}
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		
		// Prevent overlapping
		int altosBeforeMe = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (context.instruments.get(i) instanceof AltoSaxophone &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				altosBeforeMe++;
			}
		}
		
		anAlto.setLocalTranslation(0, altosBeforeMe * 40, 0);
		
		while (!extensions.isEmpty() && extensions.get(0).startTime <= time) {
			currentExtension = extensions.remove(0);
		}
		
		if (currentExtension != null) {
			if (time >= currentExtension.startTime && time <= currentExtension.endTime) {
				horn.setLocalScale(1, (float) ((0.5f * (currentExtension.endTime - time) / currentExtension.duration()) + 1), 1);
				anAlto.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentExtension.endTime - time) / currentExtension.duration())) * 0.1f, 0, 0));
			} else {
				horn.setLocalScale(1, 1, 1);
			}
		}
		
		
	}
	
	private static class Extension {
		final int midiNote;
		final double startTime;
		final double endTime;
		
		public Extension(int midiNote, double startTime, double endTime) {
			this.midiNote = midiNote;
			this.startTime = startTime;
			this.endTime = endTime;
		}
		
		double duration() {
			return endTime - startTime;
		}
	}
}
