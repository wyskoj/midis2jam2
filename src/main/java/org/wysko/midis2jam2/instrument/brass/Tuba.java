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
public class Tuba extends MonophonicInstrument {
	
	private static final HashMap<Integer, Integer[]> KEY_MAPPING = new HashMap<Integer, Integer[]>() {{
		put(22, new Integer[] {});
		put(23, new Integer[] {2, 3,});
		put(24, new Integer[] {1, 2, 3, 4,});
		put(25, new Integer[] {1, 3, 4,});
		put(26, new Integer[] {2, 3, 4,});
		put(27, new Integer[] {1, 2, 4,});
		put(28, new Integer[] {2, 4,});
		put(29, new Integer[] {4,});
		put(30, new Integer[] {2, 3,});
		put(31, new Integer[] {1, 2,});
		put(32, new Integer[] {1,});
		put(33, new Integer[] {2,});
		put(34, new Integer[] {});
		put(35, new Integer[] {2, 4,});
		put(36, new Integer[] {4,});
		put(37, new Integer[] {2, 3,});
		put(38, new Integer[] {1, 2,});
		put(39, new Integer[] {1,});
		put(40, new Integer[] {2,});
		put(41, new Integer[] {});
		put(42, new Integer[] {2, 3,});
		put(43, new Integer[] {1, 2,});
		put(44, new Integer[] {1,});
		put(45, new Integer[] {2,});
		put(46, new Integer[] {});
		put(47, new Integer[] {1, 2,});
		put(48, new Integer[] {1,});
		put(49, new Integer[] {2,});
		put(50, new Integer[] {});
		put(51, new Integer[] {1,});
		put(52, new Integer[] {2,});
		put(53, new Integer[] {});
		put(54, new Integer[] {2, 3,});
		put(55, new Integer[] {1, 2,});
		put(56, new Integer[] {1,});
		put(57, new Integer[] {2,});
		put(58, new Integer[] {});
		put(59, new Integer[] {1, 2,});
		put(60, new Integer[] {1,});
		put(61, new Integer[] {2,});
		put(62, new Integer[] {});
		put(63, new Integer[] {1,});
		put(64, new Integer[] {2,});
		put(65, new Integer[] {});
		put(66, new Integer[] {2, 3,});
		put(67, new Integer[] {1, 2,});
		put(68, new Integer[] {1,});
		put(69, new Integer[] {2,});
		put(70, new Integer[] {});
	}};
	
	public Tuba(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				TubaClone.class
		);
		
		groupOfPolyphony.setLocalTranslation(-71, 28.9f, -97.9f);
		
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 40 * indexForMoving(), 0);
	}
	
	public class TubaClone extends AnimatedKeyCloneByIntegers {
		
		public TubaClone() {
			super(Tuba.this, -0.05f, 0.8f, KEY_MAPPING, 4, Axis.Y, Axis.Z);
			
			body = context.loadModel("TubaBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.attachChild(context.loadModel("TubaHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			Material material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			((Node) body).getChild(1).setMaterial(material);

//			this.bell.attachChild(context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
//			this.bell.setLocalTranslation(0, 0, 5.58f);
//
			for (int i = 0; i < 4; i++) {
				keys[i] = context.loadModel("TubaKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
//
//			modelNode.attachChild(body);
//			modelNode.attachChild(bell);
//
			idleNode.setLocalRotation(new Quaternion().fromAngles(rad(-10), rad(90), 0));
			
			highestLevel.setLocalTranslation(10, 0, 0);
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(50 * indexForMoving()), 0));
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			/* Trumpet keys move down when pressed */
			for (int i = 0; i < 4; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					keys[i].setLocalTranslation(0, -0.5f, 0);
				} else {
					keys[i].setLocalTranslation(0, 0, 0);
				}
			}
		}
	}
}
