package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.instrument.percussion.CymbalAnimator;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.instrument.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.instrument.Stick.STRIKE_SPEED;

/**
 * Cymbals. Excludes the hi hat.
 */
public class RideCymbal extends Cymbal {
	protected RideCymbal(Midis2jam2 context,
	                     List<MidiNoteOnEvent> hits, Cymbal.CymbalType type) {
		super(context, hits, type);
		if (type != CymbalType.RIDE_1 && type != CymbalType.RIDE_2) throw new IllegalArgumentException();
		final Spatial cymbal = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp",
				Midis2jam2.MatType.REFLECTIVE, 0.7f);
		cymbalNode.attachChild(cymbal);
		cymbalNode.setLocalScale(type.size);
		highLevelNode.setLocalTranslation(type.location);
		highLevelNode.setLocalRotation(type.rotation);
		highLevelNode.attachChild(cymbalNode);
		stickNode.setLocalTranslation(0, 0, 15);
		this.animator = new CymbalAnimator(type.amplitude, type.wobbleSpeed, type.dampening);
	}
	
	@Override
	public void tick(double time, float delta) {
		MidiNoteOnEvent recoil = null;
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			recoil = hits.remove(0);
		}
		
		if (recoil != null) {
			animator.strike();
		}
		cymbalNode.setLocalRotation(new Quaternion().fromAngles(animator.rotationAmount(), 0, 0));
		animator.tick(delta);
		
		handleStick(time, delta, hits);
	}
	
	@Override
	void handleStick(double time, float delta, List<MidiNoteOnEvent> hits) {
		Stick.handleStick(context, stick, time, delta, hits, STRIKE_SPEED, MAX_ANGLE);
	}
}
