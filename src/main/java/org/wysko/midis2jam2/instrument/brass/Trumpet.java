package org.wysko.midis2jam2.instrument.brass;

import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
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
 * <i>But there's no reply at all...</i>
 */
public class Trumpet extends MonophonicInstrument {
	
	public static final PressedKeysFingeringManager FINGERING_MANAGER = PressedKeysFingeringManager.from(Trumpet.class);
	
	public Trumpet(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList,
	               TrumpetType type) throws ReflectiveOperationException {
		super(
				context,
				eventList,
				type == TrumpetType.NORMAL ? TrumpetClone.class : MutedTrumpetClone.class,
				FINGERING_MANAGER
		);
		
		groupOfPolyphony.setLocalTranslation(-36.5f, 60, 10);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(rad(-2), rad(90), 0));
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(0, 10 * indexForMoving(), 0);
	}
	
	/**
	 * The type of trumpet.
	 */
	public enum TrumpetType {
		/**
		 * The normal, open trumpet.
		 */
		NORMAL,
		
		/**
		 * The muted trumpet.
		 */
		MUTED
	}
	
	public class TrumpetClone extends AnimatedKeyCloneByIntegers {
		
		public TrumpetClone() {
			super(Trumpet.this, 0.15f, 0.9f, 3, Axis.Z, Axis.X);
			
			body = context.loadModel("TrumpetBody.fbx", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f);
			Material material = new Material(context.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
			material.setVector3("FresnelParams", new Vector3f(0.1f, 0.9f, 0.1f));
			material.setBoolean("EnvMapAsSphereMap", true);
			material.setTexture("EnvMap", context.getAssetManager().loadTexture("Assets/HornSkinGrey.bmp"));
			((Node) body).getChild(1).setMaterial(material);
			
			this.bell.attachChild(context.loadModel("TrumpetHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE, 0.9f));
			this.bell.setLocalTranslation(0, 0, 5.58f);
			
			for (int i = 0; i < 3; i++) {
				keys[i] = context.loadModel("TrumpetKey" + (i + 1) + ".obj", "HornSkinGrey.bmp",
						Midis2jam2.MatType.REFLECTIVE, 0.9f);
				modelNode.attachChild(keys[i]);
			}
			
			modelNode.attachChild(body);
			modelNode.attachChild(bell);
			
			idleNode.setLocalRotation(new Quaternion().fromAngles(rad(-10), 0, 0));
			
			animNode.setLocalTranslation(0, 0, 15);
		}
		
		@Override
		protected void animateKeys(@NotNull Integer[] pressed) {
			for (int i = 0; i < 3; i++) {
				int finalI = i;
				if (Arrays.stream(pressed).anyMatch(integer -> integer == finalI)) {
					keys[i].setLocalTranslation(0, -0.5f, 0);
				} else {
					keys[i].setLocalTranslation(0, 0, 0);
				}
			}
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-10 * indexForMoving()), 0));
			offsetNode.setLocalTranslation(0, indexForMoving() * -1, 0);
		}
	}
	
	public class MutedTrumpetClone extends TrumpetClone {
		
		public MutedTrumpetClone() {
			super();
			this.bell.attachChild(context.loadModel("TrumpetMute.obj", "RubberFoot.bmp"));
		}
	}
}
