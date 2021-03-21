package org.wysko.midis2jam2.instrument.monophonic.brass;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.monophonic.Axis;
import org.wysko.midis2jam2.instrument.monophonic.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * <i>But there's no reply at all...</i>
 */
public class Trumpet extends MonophonicInstrument {
	
	private static final HashMap<Integer, Boolean[]> KEY_MAPPING = new HashMap<Integer, Boolean[]>() {{
		put(52, new Boolean[] {true, true, true});
		put(53, new Boolean[] {true, false, true});
		put(54, new Boolean[] {false, true, true});
		put(55, new Boolean[] {true, true, false});
		put(56, new Boolean[] {true, false, false});
		put(57, new Boolean[] {false, true, false});
		put(58, new Boolean[] {false, false, false});
		put(59, new Boolean[] {true, true, true});
		put(60, new Boolean[] {true, false, true});
		put(61, new Boolean[] {false, true, true});
		put(62, new Boolean[] {true, true, false});
		put(63, new Boolean[] {true, false, false});
		put(64, new Boolean[] {false, true, false});
		put(65, new Boolean[] {false, false, false});
		put(66, new Boolean[] {false, true, true});
		put(67, new Boolean[] {true, true, false});
		put(68, new Boolean[] {true, false, false,});
		put(69, new Boolean[] {false, true, false,});
		put(70, new Boolean[] {false, false, false,});
		put(71, new Boolean[] {true, true, false,});
		put(72, new Boolean[] {true, false, false,});
		put(73, new Boolean[] {false, true, false,});
		put(74, new Boolean[] {false, false, false,});
		put(75, new Boolean[] {true, false, false,});
		put(76, new Boolean[] {false, true, false,});
		put(77, new Boolean[] {false, false, false,});
		put(78, new Boolean[] {false, true, true,});
		put(79, new Boolean[] {true, true, false,});
		put(80, new Boolean[] {true, false, false,});
		put(81, new Boolean[] {false, true, false,});
		put(82, new Boolean[] {false, false, false,});
	}};
	
	public Trumpet(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				TrumpetClone.class
		);
		
		groupOfPolyphony.setLocalTranslation(-31.5f, 60, 10);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-2), rad(90), 0));
		
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0,10*indexForMoving(),0);
	}
	
	public class TrumpetClone extends AnimatedKeyClone {
		
		public TrumpetClone() {
			super(Trumpet.this, 0.15f, 0.9f, KEY_MAPPING, 3, Axis.Z);
			
			body = context.loadModel("TrumpetBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			Material material = new Material(context.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
			material.setVector3("FresnelParams", new Vector3f(0.1f, 0.9f, 0.1f));
			material.setBoolean("EnvMapAsSphereMap", true);
			material.setTexture("EnvMap", context.getAssetManager().loadTexture("Assets/HornSkinGrey.bmp"));
			((Node) body).getChild(1).setMaterial(material);
			
			this.bell = context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			this.bell.setLocalTranslation(0, 0, 5.58f);
			
			for (int i = 0; i < 3; i++) {
				keys[i] = context.loadModel("TrumpetKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			
			idleNode.setLocalRotation(new Quaternion().fromAngles(rad(-10), 0, 0));
		}
//
//		protected void animation(double time, int indexThis, float stretchFactor, float rotationFactor) {
//
//
//			int deg = -15 * indexThis;
//			cloneRotation.setLocalRotation(new Quaternion().fromAngles(0, rad(deg), 0));
//			cloneRotation.setLocalTranslation(0, -indexThis * 0.5f, 0);
//
//
//			/* Collect note periods to execute */
//			while (!notePeriods.isEmpty() && notePeriods.get(0).startTime <= time) {
//				currentNotePeriod = notePeriods.remove(0);
//			}
//
//			/* Perform animation */
//			if (currentNotePeriod != null) {
//				if (time >= currentNotePeriod.startTime && time <= currentNotePeriod.endTime) {
//					bell.setLocalScale(1,
//							1,
//							(float) ((stretchFactor * (currentNotePeriod.endTime - time) / currentNotePeriod.duration()) + 1));
//					playRotation.setLocalRotation(new Quaternion().fromAngles(-((float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration())) * rotationFactor, 0, 0));
//					currentlyPlaying = true;
//					Boolean[] booleans = KEY_MAPPING.get(currentNotePeriod.midiNote);
//					if (booleans != null) {
//						for (int i = 0; i < 3; i++) {
//							if (booleans[i]) {
//								keys[i].setLocalTranslation(0, -0.5f, 0);
//							} else {
//								keys[i].setLocalTranslation(0, 0, 0);
//							}
//						}
//					}
//				} else {
//					currentlyPlaying = false;
//					bell.setLocalScale(1, 1, 1);
//				}
//			}
//		}
		
		@Override
		protected void animateKeys(@NotNull Boolean[] pressed) {
			/* Trumpet keys move down when pressed */
			for (int i = 0; i < 3; i++) {
				if (pressed[i]) {
					keys[i].setLocalTranslation(0, -0.5f, 0);
				} else {
					keys[i].setLocalTranslation(0, 0, 0);
				}
			}
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalTranslation(10,0,0);
		}
	}
}
