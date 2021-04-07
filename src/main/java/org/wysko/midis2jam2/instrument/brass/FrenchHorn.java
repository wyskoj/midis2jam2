package org.wysko.midis2jam2.instrument.brass;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.AnimatedKeyCloneByIntegers;
import org.wysko.midis2jam2.instrument.Axis;
import org.wysko.midis2jam2.instrument.MonophonicInstrument;
import org.wysko.midis2jam2.instrument.PressedKeysFingeringManager;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.Arrays;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

/**
 * The French Horn.
 */
public class FrenchHorn extends MonophonicInstrument {
	
	public static final PressedKeysFingeringManager FINGERING_MANAGER = PressedKeysFingeringManager.from(FrenchHorn.class);
	
	public FrenchHorn(Midis2jam2 context,
	                  List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				FrenchHornClone.class,
				FINGERING_MANAGER
		);
		
		groupOfPolyphony.setLocalTranslation(-83.1f, 41.6f, -63.7f);
		
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 15 * indexForMoving(), 0);
	}
	
	public class FrenchHornClone extends AnimatedKeyCloneByIntegers {
		
		public FrenchHornClone() {
			super(FrenchHorn.this, 0.1f, 0.9f, 4, Axis.Y, Axis.X);
			
			body = context.loadModel("FrenchHornBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.attachChild(context.loadModel("FrenchHornHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE,
					0.9f));
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			Material material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			((Node) body).getChild(1).setMaterial(material);
			
			bell.setLocalTranslation(0, -4.63f, -1.87f);
			bell.setLocalRotation(new Quaternion().fromAngles(rad(112 - 90), 0, 0));
			
			for (int i = 0; i < 4; i++) {
				String id = i == 0 ? "Trigger" : "Key" + (i);
				keys[i] = context.loadModel("FrenchHorn" + id + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			keys[0].setLocalTranslation(0, 0, 1);
			highestLevel.setLocalRotation(new Quaternion().fromAngles(rad(110 - 90), rad(90), 0));
			
			animNode.setLocalTranslation(0, 0, 20);
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(47 * indexForMoving()), 0));
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			/* French horn keys rotate when pressed */
			for (int i = 0; i < 4; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					if (i == 0) // Trigger key, so rotate on different axis
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
