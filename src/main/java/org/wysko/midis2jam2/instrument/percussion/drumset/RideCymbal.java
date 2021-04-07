package org.wysko.midis2jam2.instrument.percussion.drumset;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.Stick;
import org.wysko.midis2jam2.instrument.percussion.CymbalAnimator;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.List;

import static org.wysko.midis2jam2.instrument.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.instrument.Stick.STRIKE_SPEED;

/**
 * The ride cymbal.
 */
public class RideCymbal extends Cymbal {
	
	/**
	 * Instantiates a new Ride cymbal.
	 *
	 * @param context the context
	 * @param hits    the hits
	 * @param type    the type
	 */
	protected RideCymbal(Midis2jam2 context,
	                     List<MidiNoteOnEvent> hits, Cymbal.CymbalType type) {
		super(context, hits, type);
		assert type == CymbalType.RIDE_1 || type == CymbalType.RIDE_2 : "Ride cymbal type is wrong.";
		
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
		handleCymbalStrikes(time, delta);
		handleStick(time, delta, hits);
	}
	
	@Override
	void handleStick(double time, float delta, List<MidiNoteOnEvent> hits) {
		Stick.handleStick(context, stick, time, delta, hits, STRIKE_SPEED, MAX_ANGLE);
	}
}
