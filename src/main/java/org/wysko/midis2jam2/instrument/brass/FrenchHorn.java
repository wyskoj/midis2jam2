package org.wysko.midis2jam2.instrument.brass;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.AnimatedKeyCloneByIntegers;
import org.wysko.midis2jam2.instrument.Axis;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 *
 */
public class FrenchHorn extends MonophonicInstrument {
	
	private static final HashMap<Integer, Integer[]> KEY_MAPPING = new HashMap<Integer, Integer[]>() {{
		put(46, new Integer[] {1,});
		put(47, new Integer[] {2, 3, 4,});
		put(48, new Integer[] {2, 4,});
		put(49, new Integer[] {3, 4,});
		put(50, new Integer[] {2, 3,});
		put(51, new Integer[] {2,});
		put(52, new Integer[] {3,});
		put(53, new Integer[] {});
		put(54, new Integer[] {3, 4,});
		put(55, new Integer[] {2, 3,});
		put(56, new Integer[] {2,});
		put(57, new Integer[] {3,});
		put(58, new Integer[] {});
		put(59, new Integer[] {2, 3,});
		put(60, new Integer[] {2,});
		put(61, new Integer[] {1, 3, 4,});
		put(62, new Integer[] {1, 2, 3,});
		put(63, new Integer[] {1, 2,});
		put(64, new Integer[] {1, 3,});
		put(65, new Integer[] {1,});
		put(66, new Integer[] {1, 3, 4,});
		put(67, new Integer[] {1, 2, 3,});
		put(68, new Integer[] {1, 2,});
		put(69, new Integer[] {1, 3,});
		put(70, new Integer[] {1,});
		put(71, new Integer[] {1, 3,});
		put(72, new Integer[] {1,});
		put(73, new Integer[] {1, 3, 4,});
		put(74, new Integer[] {1, 2, 3,});
		put(75, new Integer[] {1, 2,});
		put(76, new Integer[] {1, 3,});
		put(77, new Integer[] {1,});
	}};
	
	public FrenchHorn(Midis2jam2 context,
	                  List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				FrenchHornClone.class
		);
		
		groupOfPolyphony.setLocalTranslation(-63.1f, 41.6f, -63.7f);
		
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 15 * indexForMoving(), 0);
	}
	
	public class FrenchHornClone extends AnimatedKeyCloneByIntegers {
		
		public FrenchHornClone() {
			super(FrenchHorn.this, -0.2f, 0.9f, KEY_MAPPING, 4, Axis.Y, Axis.Z);
			
			body = context.loadModel("FrenchHornBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.attachChild(context.loadModel("FrenchHornHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE,
					0.9f));
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			Material material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			((Node) body).getChild(1).setMaterial(material);
			
			bell.setLocalTranslation(0, -4.63f, -1.87f);
			bell.setLocalRotation(new Quaternion().fromAngles(rad(112 - 90), 0, 0));

//			this.bell.attachChild(context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
//			this.bell.setLocalTranslation(0, 0, 5.58f);
//
			for (int i = 0; i < 4; i++) {
				String id = i == 0 ? "Trigger" : "Key" + (i);
				keys[i] = context.loadModel("FrenchHorn" + id + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			keys[0].setLocalTranslation(0, 0, 1);
//
//			modelNode.attachChild(body);
//			modelNode.attachChild(bell);
//
			idleNode.setLocalRotation(new Quaternion().fromAngles(rad(110 - 90), rad(90), 0));
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalTranslation(0, 0, -15 * indexForMoving());
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			/* Trumpet keys move down when pressed */
			for (int i = 0; i < 4; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					if (i == 0)
						keys[i].setLocalRotation(new Quaternion().fromAngles(rad(-25), 0, 0));
					else
						keys[i].setLocalRotation(new Quaternion().fromAngles(0, 0, rad(-30)));
				} else {
					keys[i].setLocalRotation(new Quaternion().fromAngles(0, 0, 0));
				}
			}
		}
	}
}
