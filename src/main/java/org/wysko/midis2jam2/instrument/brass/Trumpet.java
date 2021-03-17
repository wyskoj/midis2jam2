package org.wysko.midis2jam2.instrument.brass;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicClone;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.monophonic.StretchyClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Trumpet extends MonophonicInstrument {
	
	final HashMap<Integer, Boolean[]> KEY_MAPPING = new HashMap<Integer, Boolean[]>() {{
		put(52, new Boolean[] {true, true, true}); // F#
		put(53, new Boolean[] {true, false, true});
		put(54, new Boolean[] {false, true, true});
		put(55, new Boolean[] {true, true, false}); // A
		put(56, new Boolean[] {true, false, false});
		put(57, new Boolean[] {false, true, false});
		put(58, new Boolean[] {false, false, false}); // C
		put(59, new Boolean[] {true, true, true});
		put(60, new Boolean[] {true, false, true});
		put(61, new Boolean[] {false, true, true});
		put(62, new Boolean[] {true, true, false});
		put(63, new Boolean[] {true, false, false});// F
		put(64, new Boolean[] {false, true, false});
		put(65, new Boolean[] {false, false, false});
		put(66, new Boolean[] {false, true, true});
		put(67, new Boolean[] {true, true, false});// A
		put(68, new Boolean[] {true, false, false,});//
		put(69, new Boolean[] {false, true, false,});//
		put(70, new Boolean[] {false, false, false,});//
		put(71, new Boolean[] {true, true, false,});//
		put(72, new Boolean[] {true, false, false,});//
		put(73, new Boolean[] {false, true, false,});//
		put(74, new Boolean[] {false, false, false,});//
		put(75, new Boolean[] {true, false, false,});//
		put(76, new Boolean[] {false, true, false,});//
		put(77, new Boolean[] {false, false, false,});//
		put(78, new Boolean[] {false, true, true,});//
		put(79, new Boolean[] {true, true, false,});//
		put(80, new Boolean[] {true, false, false,});//
		put(81, new Boolean[] {false, true, false,});//
		put(82, new Boolean[] {false, false, false,});//
	}};
	Node cloneNode = new Node();
	
	public Trumpet(Midis2jam2 context,
	               List<MidiChannelSpecificEvent> eventList) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		super(context);
		
		List<MidiNoteEvent> justTheNotes = scrapeMidiNoteEvents(eventList);
		
		this.notePeriods = calculateNotePeriods(justTheNotes);
		calculateClones(this, TrumpetClone.class);
		
		for (MonophonicClone clone : clones) {
			TrumpetClone trumpetClone = ((TrumpetClone) clone);
			cloneNode.attachChild(trumpetClone.hornNode);
			cloneNode.setLocalTranslation(-31.5f, 60, 10);
			cloneNode.setLocalRotation(new Quaternion().fromAngles(rad(-2), rad(90), 0));
		}
		highestLevel.attachChild(cloneNode);
		
		context.getRootNode().attachChild(highestLevel);
	}
	
	@Override
	public void tick(double time, float delta) {
		setIdleVisibiltyByPeriods(notePeriods, time, highestLevel);
		updateClones(time, delta, new Vector3f(0, 10, 0));
	}
	
	public class TrumpetClone extends StretchyClone {
		Spatial[] keys = new Spatial[3];
		Node idleRotation = new Node();
		Node playRotation = new Node();
		Node cloneRotation = new Node();
		
		public TrumpetClone() {
			super();
			body = context.loadModel("TrumpetBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			Material material = new Material(context.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
			material.setVector3("FresnelParams", new Vector3f(0.1f, 0.9f, 0.1f));
			material.setBoolean("EnvMapAsSphereMap", true);
			material.setTexture("EnvMap", context.getAssetManager().loadTexture("Assets/HornSkinGrey.bmp"));
			((Node) body).getChild(1).setMaterial(material);
			
			bell = context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.setLocalTranslation(0, 0, 5.58f);
			
			for (int i = 0; i < 3; i++) {
				keys[i] = context.loadModel("TrumpetKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				idleRotation.attachChild(keys[i]);
			}
			idleRotation.setLocalRotation(new Quaternion().fromAngles(rad(-10), 0, 0));
			idleRotation.attachChild(body);
			idleRotation.attachChild(bell);
			playRotation.attachChild(idleRotation);
			playRotation.setLocalTranslation(0, 0, 10);
			cloneRotation.attachChild(playRotation);
			hornNode.attachChild(cloneRotation);
		}
		
		@Override
		public void tick(double time, float delta) {
			int indexThis = Trumpet.this.clones.indexOf(this);
			animation(time, indexThis, 0.9f, 0.15f);
		}
		
		protected void animation(double time, int indexThis, float stretchFactor, float rotationFactor) {
			
			/* Hide or show depending on degree of polyphony and current playing status */
			hideOrShowOnPolyphony(indexThis);
			
			int deg = -15 * indexThis;
			cloneRotation.setLocalRotation(new Quaternion().fromAngles(0, rad(deg), 0));
			cloneRotation.setLocalTranslation(0, -indexThis * 0.5f, 0);
			
			
			/* Collect note periods to execute */
			while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
				currentNotePeriod = notePeriods.remove(0);
			}
			
			/* Perform animation */
			if (currentNotePeriod != null) {
				if (time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime) {
					bell.setLocalScale(1,
							1,
							(float) ((stretchFactor * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1));
					playRotation.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * rotationFactor, 0, 0));
					currentlyPlaying = true;
					Boolean[] booleans = KEY_MAPPING.get(currentNotePeriod.midiNote);
					if (booleans != null) {
						for (int i = 0; i < 3; i++) {
							if (booleans[i]) {
								keys[i].setLocalTranslation(0, -0.5f, 0);
							} else {
								keys[i].setLocalTranslation(0, 0, 0);
							}
						}
					}
				} else {
					currentlyPlaying = false;
					bell.setLocalScale(1, 1, 1);
				}
			}
		}
		
	}
}
