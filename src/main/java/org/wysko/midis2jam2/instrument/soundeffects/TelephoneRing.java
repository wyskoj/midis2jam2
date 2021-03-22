package org.wysko.midis2jam2.instrument.soundeffects;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.SustainedInstrument;
import org.wysko.midis2jam2.midi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class TelephoneRing extends SustainedInstrument {
	final Spatial[] upKeys = new Spatial[12];
	final Spatial[] downKeys = new Spatial[12];
	final Node upNode = new Node();
	final Node downNode = new Node();
	final boolean[] playing = new boolean[12];
	final Spatial handle;
	float force = 0;
	private final List<MidiNoteEvent> notes;
	
	public TelephoneRing(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		notes = eventList.stream().filter(e -> e instanceof MidiNoteEvent).map(e -> ((MidiNoteEvent) e)).collect(Collectors.toList());
		this.notePeriods = calculateNotePeriods(notes);
		
		Spatial base = context.loadModel("TelePhoneBase.fbx", "Telephonebase.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		
		Node base1 = (Node) base;
		Material rubberFoot = new Material(context.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		rubberFoot.setTexture("ColorMap", context.getAssetManager().loadTexture("Assets/RubberFoot.bmp"));
		base1.getChild(0).setMaterial(rubberFoot);
		
		handle = context.loadModel("TelePhoneHandle.obj", "Telephonehandle.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
		instrumentNode.attachChild(base);
		instrumentNode.attachChild(handle);
		
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
			//noinspection IntegerDivisionInFloatingPointContext
			downKeys[i].setLocalTranslation(1.2f * (i % 3 - 1), 3.4f, -2.7f - (1.2f * (-i / 3)));
			
			downKeys[i].setCullHint(Spatial.CullHint.Always);
			upNode.attachChild(upKeys[i]);
			downNode.attachChild(downKeys[i]);
		}
		
		upNode.setLocalRotation(new Quaternion().fromAngles(rad(19), 0, 0));
		downNode.setLocalRotation(new Quaternion().fromAngles(rad(19), 0, 0));
		instrumentNode.attachChild(upNode);
		instrumentNode.attachChild(downNode);
		instrumentNode.setLocalTranslation(0, 1, -50);
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
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
			force += 12 * delta;
			force = Math.min(1, force);
		} else {
			force -= 12 * delta;
			force = Math.max(0, force);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(13 * indexForMoving(),0,0);
	}
}
