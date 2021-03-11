package org.wysko.midis2jam2.instrument.percussion;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Tom extends StickDrum {
	public Tom(Midis2jam2 context, List<MidiNoteOnEvent> hits, TomPitch pitch) {
		super(context, hits);
		drum = context.loadModel("DrumSet_Tom.obj", "DrumShell.bmp", Midis2jam2.MatType.UNSHADED);
		drum.setLocalScale(pitch.scale);
		recoilNode.attachChild(drum);
		recoilNode.attachChild(stickNode);
		highLevelNode.attachChild(recoilNode);
		highLevelNode.setLocalTranslation(pitch.location);
		highLevelNode.setLocalRotation(pitch.rotation);
		
		
		if (pitch == TomPitch.HIGH_FLOOR || pitch == TomPitch.LOW_FLOOR) {
			stickNode.setLocalRotation(new Quaternion().fromAngles(0, rad(80), 0));
			stickNode.setLocalTranslation(10, 0, 0);
		} else {
			stickNode.setLocalTranslation(0, 0, 10);
		}
		
		stick.setLocalRotation(new Quaternion().fromAngles(rad(MAX_ANGLE), 0, 0));
	}
	
	@Override
	public void tick(double time, float delta) {
		drumRecoil(time, delta);
		handleStick(time, delta);
	}
	
	public enum TomPitch {
		LOW_FLOOR(
				new Vector3f(1.5f, 1.5f, 1.5f),
				new Vector3f(20, 20, -60),
				new Quaternion().fromAngles(rad(-2), rad(180), rad(-10))),
		
		HIGH_FLOOR(new Vector3f(1.4f, 1.4f, 1.4f),
				new Vector3f(17, 21, -75),
				new Quaternion().fromAngles(rad(-5), rad(180), rad(-15))),
		
		LOW(new Vector3f(1.2f, 1.2f, 1.2f),
				new Vector3f(10f, 27 + 2, -82),
				new Quaternion().fromAngles(rad(60), rad(-30), 0)),
		
		LOW_MID(new Vector3f(1f, 1f, 1f),
				new Vector3f(0, 30 + 2, -85),
				new Quaternion().fromAngles(rad(60), 0, 0)),
		
		HIGH_MID(new Vector3f(0.8f, 0.8f, 0.8f),
				new Vector3f(-9, 29 + 2, -82),
				new Quaternion().fromAngles(rad(60), rad(20), 0)),
		
		HIGH(new Vector3f(0.6f, 0.6f, 0.6f),
				new Vector3f(-15, 27 + 2, -78),
				new Quaternion().fromAngles(rad(50), rad(40), 0));
		
		final Vector3f scale;
		final Vector3f location;
		final Quaternion rotation;
		
		TomPitch(Vector3f scale, Vector3f location, Quaternion rotation) {
			this.scale = scale;
			this.location = location;
			this.rotation = rotation;
		}
	}
}
