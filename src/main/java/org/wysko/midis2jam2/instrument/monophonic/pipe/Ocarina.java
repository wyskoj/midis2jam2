package org.wysko.midis2jam2.instrument.monophonic.pipe;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.LinearOffsetCalculator;
import org.wysko.midis2jam2.instrument.monophonic.HandedClone;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.HashMap;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Ocarina extends HandedInstrument {
	
	/**
	 * Constructs an ocarina.
	 *
	 * @param context context to midis2jam2
	 */
	public Ocarina(Midis2jam2 context, List<MidiChannelSpecificEvent> events) throws ReflectiveOperationException {
		super(context,
				events,
				OcarinaClone.class,
				new OcarinaHandGenerator()
		);
		
		groupOfPolyphony.setLocalTranslation(45, 47, 20);
		groupOfPolyphony.setLocalRotation(new Quaternion().fromAngles(0, rad(135), 0));
		
	}
	
	static class OcarinaHandGenerator extends HashMap<Integer, HandedClone.Hands> {
		@Override
		public HandedClone.Hands get(Object key) {
			return new HandedClone.Hands(0, ((int) key + 3) % 12);
		}
	}
	
	public class OcarinaClone extends HandedClone {
		public OcarinaClone() {
			super(Ocarina.this, 0);
			highestLevel = new Node();
			Spatial horn = context.loadModel("Ocarina.obj", "Ocarina.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			animNode.attachChild(horn);
			highestLevel.attachChild(animNode);
			loadHands();
			for (int i = 0; i < rightHands.length; i++) {
				if (i == 0) rightHands[i].setCullHint(Spatial.CullHint.Dynamic);
				else rightHands[i].setCullHint(Spatial.CullHint.Always);
			}
		}
		
		@Override
		protected void loadHands() {
			rightHands = new Spatial[12];
			for (int i = 0; i < 12; i++) {
				rightHands[i] = context.loadModel("OcarinaHand" + i + ".obj", "hands.bmp", Midis2jam2.MatType.UNSHADED, 0.9f);
			}
			for (Spatial rightHand : rightHands) {
				rightHandNode.attachChild(rightHand);
			}
		}
		
		@Override
		public void tick(double time, float delta) {
			super.tick(time, delta);
			/* Collect note periods to execute */
			if (isPlaying()) {
				animNode.setLocalTranslation(0,
						0, 3 * (float) ((currentNotePeriod.endTime - time) / currentNotePeriod.duration()));
			}
		}
		
		@Override
		protected void moveForPolyphony() {
			offsetNode.setLocalTranslation(10*indexForMoving(),0,0);
		}
	}
}
