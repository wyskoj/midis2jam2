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
 * The tuba.
 */
public class Tuba extends MonophonicInstrument {
	
	public static final PressedKeysFingeringManager FINGERING_MANAGER = PressedKeysFingeringManager.from(Tuba.class);
	
	public Tuba(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				TubaClone.class,
				FINGERING_MANAGER
		);
		
		groupOfPolyphony.setLocalTranslation(-71, 28.9f, -97.9f);
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 40 * indexForMoving(), 0);
	}
	
	public class TubaClone extends AnimatedKeyCloneByIntegers {
		
		public TubaClone() {
			super(Tuba.this, -0.05f, 0.8f, 4, Axis.Y, Axis.Z);
			
			body = context.loadModel("TubaBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			bell.attachChild(context.loadModel("TubaHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			Material material = context.reflectiveMaterial("Assets/HornSkinGrey.bmp");
			((Node) body).getChild(1).setMaterial(material);
			
			for (int i = 0; i < 4; i++) {
				keys[i] = context.loadModel("TubaKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			
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
