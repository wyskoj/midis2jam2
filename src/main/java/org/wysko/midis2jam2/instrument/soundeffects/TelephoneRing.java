package org.wysko.midis2jam2.instrument.soundeffects;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Instrument;
import org.wysko.midis2jam2.instrument.NotePeriod;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class TelephoneRing extends Instrument {
	private final List<NotePeriod> notePeriods;
	private final Node highestNode = new Node();
	Node telephoneNode = new Node();
	Spatial[] upKeys = new Spatial[12];
	Spatial[] downKeys = new Spatial[12];
	Node upNode = new Node();
	Node downNode = new Node();
	List<MidiNoteEvent> notes;
	boolean[] playing = new boolean[12];
	Spatial handle;
	float force = 0;
	
	public TelephoneRing(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context);
		List<MidiNoteEvent> notes = eventList.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> ((MidiNoteEvent) e)).collect(Collectors.toList());
		this.notes = notes;
		this.notePeriods = calculateNotePeriods(notes);
		
		Spatial base = context.loadModel("TelePhoneBase.fbx", "Telephonebase.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		
		Node base1 = (Node) base;
		Material rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		base1.getChild(0).setMaterial(rubberFoot);
		
		handle = context.loadModel("TelePhoneHandle.obj", "Telephonehandle.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		telephoneNode.attachChild(base);
		telephoneNode.attachChild(handle);
		
		for (int i = 0; i < 12; i++) {
			String val;
			if (i < 9) {
				val = String.valueOf(i + 1);
			} else if (i == 9) {
				val = "Star";
			} else if (i == 10) {
				val = "0";
			} else {
				val = "Pound";
			}
			upKeys[i] = context.loadModel("TelePhoneKey.obj", "TelePhoneKey" + val + "Dark.bmp",
					Midis2jam2.MatType.UNSHADED, 0.9f);
			downKeys[i] = context.loadModel("TelePhoneKey.obj", "TelePhoneKey" + val + ".bmp",
					Midis2jam2.MatType.UNSHADED,
					0.9f);
			
			
			//noinspection IntegerDivisionInFloatingPointContext
			upKeys[i].setLocalTranslation(1.2f * (i % 3 - 1), 3.89f, -2.7f - (1.2f * (-i / 3)));
			downKeys[i].setLocalTranslation(1.2f * (i % 3 - 1), 3.4f, -2.7f - (1.2f * (-i / 3)));
			
			downKeys[i].setCullHint(Spatial.CullHint.Always);
			upNode.attachChild(upKeys[i]);
			downNode.attachChild(downKeys[i]);
		}
		
		upNode.setLocalRotation(new Quaternion().fromAngles(rad(19), 0, 0));
		downNode.setLocalRotation(new Quaternion().fromAngles(rad(19), 0, 0));
		telephoneNode.attachChild(upNode);
		telephoneNode.attachChild(downNode);
		telephoneNode.setLocalTranslation(0, 1, -50);
		highestNode.attachChild(telephoneNode);
		context.getRootNode().attachChild(highestNode);
	}
	
	@Override
	public void tick(double time, float delta) {
		
		int othersOfMyType = 0;
		int mySpot = context.instruments.indexOf(this);
		for (int i = 0; i < context.instruments.size(); i++) {
			if (this.getClass().isInstance(context.instruments.get(i)) &&
					context.instruments.get(i) != this &&
					i < mySpot) {
				othersOfMyType++;
			}
		}
		
		highestNode.setLocalTranslation(othersOfMyType * 13,0,0);
		List<MidiEvent> eventsToPerform = new ArrayList<>();
		
		if (!notes.isEmpty())
			while (notes.size() != 0 &&
					((notes.get(0) instanceof MidiNoteOnEvent && context.file.eventInSeconds(notes.get(0)) <= time)
							||
							(notes.get(0) instanceof MidiNoteOffEvent && context.file.eventInSeconds(notes.get(0)) <= time - 0.01))
			) {
				eventsToPerform.add(notes.remove(0));
			}
		
		
		for (MidiEvent event : eventsToPerform) {
			if (event instanceof MidiNoteOnEvent) {
				MidiNoteOnEvent noteOn = (MidiNoteOnEvent) event;
				int keyIndex = (noteOn.note + 3) % 12;
				playing[keyIndex] = true;
				upKeys[keyIndex].setCullHint(Spatial.CullHint.Always);
				downKeys[keyIndex].setCullHint(Spatial.CullHint.Dynamic);
			} else if (event instanceof MidiNoteOffEvent) {
				MidiNoteOffEvent noteOff = (MidiNoteOffEvent) event;
				int keyIndex = (noteOff.note + 3) % 12;
				playing[keyIndex] = false;
				upKeys[keyIndex].setCullHint(Spatial.CullHint.Dynamic);
				downKeys[keyIndex].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		// Animate the phone
		boolean isPlaying = false;
		for (boolean b : playing) {
			if (b) {
				isPlaying = true;
				break;
			}
		}
		handle.setLocalTranslation(0, (float) (2 + (new Random().nextGaussian() * 0.3)) * force, 0);
		handle.setLocalRotation(new Quaternion().fromAngles(rad(new Random().nextGaussian() * 3) * force,
				rad(new Random().nextGaussian() * 3) * force, 0));
		if (isPlaying) {
			force += 10 * delta;
			force = Math.min(1,force);
		} else {
			force -= 10 * delta;
			force = Math.max(0,force);
		}
	}
}
