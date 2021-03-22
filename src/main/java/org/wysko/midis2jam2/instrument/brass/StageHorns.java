package org.wysko.midis2jam2.instrument.brass;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class StageHorns extends WrappedOctaveSustained {
	
	// Horns are 1.5 deg apart
	// First 16 left from center
	
	private static final Vector3f BASE_POSITION = new Vector3f(0, 29.5f, -152.65f);
	final Node[] hornNodes = new Node[12];
	
	
	public StageHorns(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList) {
		super(context,  eventList);
		
		twelfths = new StageHornNote[12];
		for (int i = 0; i < 12; i++) {
			hornNodes[i] = new Node();
			twelfths[i] = new StageHornNote();
			hornNodes[i].attachChild(twelfths[i].highestLevel);
			twelfths[i].highestLevel.setLocalTranslation(BASE_POSITION);
			hornNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(16 + i * 1.5), 0));
			instrumentNode.attachChild(hornNodes[i]);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalTranslation(-1.378f * indexForMoving(), 0, 4.806f * indexForMoving());
	}
	
	public class StageHornNote extends BouncyTwelfth {
		public StageHornNote() {
			super();
			// Load horn
			animNode.attachChild(context.loadModel("StageHorn.obj", "HornSkin.bmp", Midis2jam2.MatType.REFLECTIVE,
					0.9f));
		}
	}
}
